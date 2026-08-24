package composable.domain.platform.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.postgresql.PostgreSQLContainer;

class PlatformEventRegistrationHttpE2ETest {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String PRINCIPAL_A = "opaque-7f31a";
    private static final String PRINCIPAL_B = "opaque-9c42b";
    private static final String PRINCIPAL_C = "opaque-3e88c";
    private static final String PASSWORD_A = "test-proof-secret-a";
    private static final String PASSWORD_B = "test-proof-secret-b";
    private static final String PASSWORD_C = "test-proof-secret-c";

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:18.4");

    private static ConfigurableApplicationContext application;
    private static URI baseUri;
    private static DataSource dataSource;

    @BeforeAll
    static void startRuntime() {
        POSTGRESQL.start();
        startApplication();
    }

    @AfterAll
    static void stopRuntime() {
        if (application != null) {
            application.close();
        }
        POSTGRESQL.stop();
    }

    private static void startApplication() {
        PasswordEncoder encoder =
                PasswordEncoderFactories.createDelegatingPasswordEncoder();

        application = new SpringApplication(PlatformApplication.class).run(
                "--server.port=0",
                "--platform.database.url=" + POSTGRESQL.getJdbcUrl(),
                "--platform.database.username=" + POSTGRESQL.getUsername(),
                "--platform.database.password=" + POSTGRESQL.getPassword(),
                "--platform.security.participants[0].principal=" + PRINCIPAL_A,
                "--platform.security.participants[0].password-verifier="
                        + encoder.encode(PASSWORD_A),
                "--platform.security.participants[1].principal=" + PRINCIPAL_B,
                "--platform.security.participants[1].password-verifier="
                        + encoder.encode(PASSWORD_B),
                "--platform.security.participants[2].principal=" + PRINCIPAL_C,
                "--platform.security.participants[2].password-verifier="
                        + encoder.encode(PASSWORD_C));

        Integer port = application.getEnvironment()
                .getRequiredProperty("local.server.port", Integer.class);
        baseUri = URI.create("http://127.0.0.1:" + port);
        dataSource = application.getBean(DataSource.class);
    }

    @Test
    void authenticatesOwnerAndDerivesDurableParticipantOwnership() throws Exception {
        defineAndPublishEvent("registration-event-1");

        HttpResponse<String> created = postRegistration(
                registrationJson(
                        "registration-http-1",
                        "registration-event-1"),
                "corr-registration-create",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(201, created.statusCode());
        assertCorrelation(created, "corr-registration-create");
        assertRegistrationBody(
                created.body(),
                "registration-http-1",
                "registration-event-1",
                "active");
        assertFalse(created.body().contains("participantReference"));
        assertPersistedRegistration(
                "registration-http-1",
                "participant",
                PRINCIPAL_A,
                "event",
                "registration-event-1");

        HttpResponse<String> retrieved = getRegistration(
                "registration-http-1",
                null,
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(200, retrieved.statusCode());
        assertGeneratedCorrelation(retrieved);
        assertRegistrationBody(
                retrieved.body(),
                "registration-http-1",
                "registration-event-1",
                "active");
    }

    @Test
    void missingAndInvalidCredentialsUseSameAuthenticationFailureSemantics()
            throws Exception {
        defineAndPublishEvent("registration-event-auth");

        HttpResponse<String> missing = postRegistration(
                registrationJson(
                        "registration-http-auth-missing",
                        "registration-event-auth"),
                "corr-auth-missing",
                null,
                null);

        HttpResponse<String> invalid = postRegistration(
                registrationJson(
                        "registration-http-auth-invalid",
                        "registration-event-auth"),
                "corr-auth-invalid",
                PRINCIPAL_A,
                "wrong-secret");

        assertEquals(401, missing.statusCode());
        assertEquals(401, invalid.statusCode());
        assertCorrelation(missing, "corr-auth-missing");
        assertCorrelation(invalid, "corr-auth-invalid");
        assertJsonString(missing.body(), "code", "authentication_required");
        assertJsonString(invalid.body(), "code", "authentication_required");
        assertJsonString(missing.body(), "message", "Authentication required");
        assertJsonString(invalid.body(), "message", "Authentication required");
        assertEquals(0, registrationCount("registration-http-auth-missing"));
        assertEquals(0, registrationCount("registration-http-auth-invalid"));
    }

    @Test
    void unauthenticatedRetrieveRemainsDistinctAuthenticationFailure() throws Exception {
        HttpResponse<String> response = getRegistration(
                "registration-http-unauthenticated",
                "corr-unauthenticated-retrieve",
                null,
                null);

        assertEquals(401, response.statusCode());
        assertCorrelation(response, "corr-unauthenticated-retrieve");
        assertJsonString(response.body(), "code", "authentication_required");
    }

    @Test
    void authenticatedNonOwnerAndUnknownRegistrationHaveSameExternalDisclosure()
            throws Exception {
        defineAndPublishEvent("registration-event-private");

        assertEquals(
                201,
                postRegistration(
                                registrationJson(
                                        "registration-http-private",
                                        "registration-event-private"),
                                "corr-private-create",
                                PRINCIPAL_A,
                                PASSWORD_A)
                        .statusCode());

        HttpResponse<String> nonOwner = getRegistration(
                "registration-http-private",
                "corr-private-non-owner",
                PRINCIPAL_B,
                PASSWORD_B);

        HttpResponse<String> unknown = getRegistration(
                "registration-http-private-unknown",
                "corr-private-unknown",
                PRINCIPAL_B,
                PASSWORD_B);

        assertEquals(404, nonOwner.statusCode());
        assertEquals(404, unknown.statusCode());
        assertJsonString(nonOwner.body(), "code", "event_registration_not_found");
        assertJsonString(unknown.body(), "code", "event_registration_not_found");
        assertJsonString(nonOwner.body(), "message", "Event registration was not found");
        assertJsonString(unknown.body(), "message", "Event registration was not found");
    }

    @Test
    void ownerCancelsIdempotentlyAndObservesCancelledLifecycle() throws Exception {
        defineAndPublishEvent("registration-event-cancel");

        assertEquals(
                201,
                postRegistration(
                                registrationJson(
                                        "registration-http-cancel",
                                        "registration-event-cancel"),
                                "corr-cancel-create",
                                PRINCIPAL_A,
                                PASSWORD_A)
                        .statusCode());

        HttpResponse<String> cancelled = cancelRegistration(
                "registration-http-cancel",
                "corr-cancel-first",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(200, cancelled.statusCode());
        assertCorrelation(cancelled, "corr-cancel-first");
        assertRegistrationBody(
                cancelled.body(),
                "registration-http-cancel",
                "registration-event-cancel",
                "cancelled");

        HttpResponse<String> repeated = cancelRegistration(
                "registration-http-cancel",
                "corr-cancel-repeat",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(200, repeated.statusCode());
        assertRegistrationBody(
                repeated.body(),
                "registration-http-cancel",
                "registration-event-cancel",
                "cancelled");

        HttpResponse<String> retrieved = getRegistration(
                "registration-http-cancel",
                "corr-cancel-retrieve",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(200, retrieved.statusCode());
        assertRegistrationBody(
                retrieved.body(),
                "registration-http-cancel",
                "registration-event-cancel",
                "cancelled");

        HttpResponse<String> nonOwnerCancel = cancelRegistration(
                "registration-http-cancel",
                "corr-cancel-non-owner",
                PRINCIPAL_B,
                PASSWORD_B);

        assertEquals(404, nonOwnerCancel.statusCode());
        assertJsonString(
                nonOwnerCancel.body(),
                "code",
                "event_registration_not_found");

        HttpResponse<String> duplicateAfterCancellation = postRegistration(
                registrationJson(
                        "registration-http-cancel-conflict",
                        "registration-event-cancel"),
                "corr-cancel-conflict",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(409, duplicateAfterCancellation.statusCode());
        assertJsonString(
                duplicateAfterCancellation.body(),
                "code",
                "registration_conflict");
        assertEquals(0, registrationCount("registration-http-cancel-conflict"));
    }

    @Test
    void ownerAndCancelledLifecycleSurviveApplicationRestartAgainstSamePostgresql()
            throws Exception {
        defineAndPublishEvent("registration-event-restart");

        assertEquals(
                201,
                postRegistration(
                                registrationJson(
                                        "registration-http-restart",
                                        "registration-event-restart"),
                                "corr-restart-create",
                                PRINCIPAL_A,
                                PASSWORD_A)
                        .statusCode());

        assertEquals(
                200,
                cancelRegistration(
                                "registration-http-restart",
                                "corr-restart-cancel",
                                PRINCIPAL_A,
                                PASSWORD_A)
                        .statusCode());

        application.close();
        application = null;
        startApplication();

        HttpResponse<String> owner = getRegistration(
                "registration-http-restart",
                "corr-restart-owner",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(200, owner.statusCode());
        assertRegistrationBody(
                owner.body(),
                "registration-http-restart",
                "registration-event-restart",
                "cancelled");
        assertPersistedRegistration(
                "registration-http-restart",
                "participant",
                PRINCIPAL_A,
                "event",
                "registration-event-restart");

        HttpResponse<String> nonOwner = getRegistration(
                "registration-http-restart",
                "corr-restart-non-owner",
                PRINCIPAL_B,
                PASSWORD_B);

        assertEquals(404, nonOwner.statusCode());
        assertJsonString(
                nonOwner.body(),
                "code",
                "event_registration_not_found");
    }

    @Test
    void eventDefineRequiresAuthenticationWhileRetrieveRemainsAnonymous()
            throws Exception {
        String eventId = "registration-event-public-security-isolation";
        String body = "{"
                + "\"eventId\":\"" + eventId + "\","
                + "\"name\":\"Registration Event\","
                + "\"slug\":\"" + eventId + "\","
                + "\"startsAt\":\"2026-09-01T08:00:00Z\","
                + "\"endsAt\":\"2026-09-01T10:00:00Z\","
                + "\"timezone\":\"Europe/Copenhagen\""
                + "}";

        HttpRequest defineUnauthenticated = HttpRequest.newBuilder(baseUri.resolve("/api/v1/events"))
                .header("Content-Type", "application/json")
                .header(CORRELATION_HEADER, "corr-event-define-unauthenticated")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> definedUnauthenticated =
                HTTP.send(defineUnauthenticated, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, definedUnauthenticated.statusCode());

        HttpRequest define = HttpRequest.newBuilder(baseUri.resolve("/api/v1/events"))
                .header("Content-Type", "application/json")
                .header(CORRELATION_HEADER, "corr-event-define-authenticated")
                .header("Authorization", basicAuthorization(PRINCIPAL_A, PASSWORD_A))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> defined =
                HTTP.send(define, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, defined.statusCode());

        HttpRequest retrieve = HttpRequest.newBuilder(
                        baseUri.resolve("/api/v1/events/" + eventId))
                .header(CORRELATION_HEADER, "corr-anonymous-event-retrieve")
                .GET()
                .build();

        HttpResponse<String> retrieved =
                HTTP.send(retrieve, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, retrieved.statusCode());
        assertJsonString(retrieved.body(), "eventId", eventId);
    }

    @Test
    void publishedEventDiscoveryCompletesParticipantLifecycleAcrossRestart()
            throws Exception {
        String publishedEventId = "goal-57-published-event";
        String unpublishedEventId = "goal-57-unpublished-event";
        String otherEventId = "goal-57-other-event";
        String registrationId = "goal-57-registration";
        String otherRegistrationId = "goal-57-other-registration";

        // 1. organizer (PRINCIPAL_A) defines Event and becomes owner
        defineEvent(publishedEventId, "Initial Event Name", PRINCIPAL_A, PASSWORD_A);
        defineEvent(unpublishedEventId, "Unpublished Event", PRINCIPAL_A, PASSWORD_A);
        defineEvent(otherEventId, "Other Event", PRINCIPAL_A, PASSWORD_A);

        // 2. organizer modifies the Event while unpublished and the changed value is observable
        HttpResponse<String> updatedWhileUnpublished = updateEvent(
                publishedEventId,
                "Modified Event Name",
                "corr-goal-update-unpub",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(200, updatedWhileUnpublished.statusCode());
        assertCorrelation(updatedWhileUnpublished, "corr-goal-update-unpub");
        assertJsonString(updatedWhileUnpublished.body(), "name", "Modified Event Name");

        HttpResponse<String> observedEvent = getEvent(
                publishedEventId,
                "corr-goal-observe-modified");
        assertEquals(200, observedEvent.statusCode());
        assertJsonString(observedEvent.body(), "name", "Modified Event Name");

        // 3. unpublished Event remains absent from discovery
        HttpResponse<String> undiscovered = discoverEvents("corr-goal-discover-before");
        assertEquals(200, undiscovered.statusCode());
        assertCorrelation(undiscovered, "corr-goal-discover-before");
        assertFalse(undiscovered.body().contains(publishedEventId));
        assertFalse(undiscovered.body().contains(unpublishedEventId));

        HttpResponse<String> knownUnpublished = getEvent(
                unpublishedEventId,
                "corr-goal-known-unpublished");
        assertEquals(200, knownUnpublished.statusCode());
        assertCorrelation(knownUnpublished, "corr-goal-known-unpublished");
        assertJsonString(knownUnpublished.body(), "eventId", unpublishedEventId);

        // 4. participant (PRINCIPAL_B) Registration against unpublished Event is rejected with 409 event_not_published; no state created
        HttpResponse<String> unpublishedRegistration = postRegistration(
                registrationJson(registrationId, publishedEventId),
                "corr-goal-registration-unpublished",
                PRINCIPAL_B,
                PASSWORD_B);
        assertEquals(409, unpublishedRegistration.statusCode());
        assertCorrelation(unpublishedRegistration, "corr-goal-registration-unpublished");
        assertJsonString(unpublishedRegistration.body(), "code", "event_not_published");
        assertJsonString(unpublishedRegistration.body(), "message", "Referenced Event is not published");
        assertEquals(0, registrationCount(registrationId));

        // 5. organizer (PRINCIPAL_A) publishes Event
        HttpResponse<String> published =
                publishEvent(publishedEventId, "corr-goal-publish");
        assertEquals(204, published.statusCode());
        assertCorrelation(published, "corr-goal-publish");

        HttpResponse<String> repeated =
                publishEvent(publishedEventId, "corr-goal-republish");
        assertEquals(409, repeated.statusCode());
        assertCorrelation(repeated, "corr-goal-republish");
        assertJsonString(repeated.body(), "code", "event_already_published");

        HttpResponse<String> missing =
                publishEvent("goal-57-missing-event", "corr-goal-publish-missing");
        assertEquals(404, missing.statusCode());
        assertCorrelation(missing, "corr-goal-publish-missing");
        assertJsonString(missing.body(), "code", "event_not_found");

        HttpResponse<String> publishedOther = publishEvent(otherEventId, "corr-goal-publish-other");
        assertEquals(204, publishedOther.statusCode());

        // 6. discovery returns published Event
        HttpResponse<String> discovered = discoverEvents("corr-goal-discover-after");
        assertEquals(200, discovered.statusCode());
        assertCorrelation(discovered, "corr-goal-discover-after");
        assertJsonString(discovered.body(), "eventId", publishedEventId);
        assertFalse(discovered.body().contains(unpublishedEventId));

        HttpResponse<String> organizerEmpty = getEventRegistrations(
                publishedEventId,
                "corr-goal-organizer-empty",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(200, organizerEmpty.statusCode());
        assertCorrelation(organizerEmpty, "corr-goal-organizer-empty");
        assertEquals("[]", organizerEmpty.body().trim());

        // 7. a distinct participant (PRINCIPAL_B) registers
        HttpResponse<String> created = postRegistration(
                registrationJson(registrationId, publishedEventId),
                "corr-goal-registration-create",
                PRINCIPAL_B,
                PASSWORD_B);
        assertEquals(201, created.statusCode());
        assertRegistrationBody(created.body(), registrationId, publishedEventId, "active");

        // 8. participant (PRINCIPAL_B) retrieves active private Registration
        HttpResponse<String> retrieved = getRegistration(
                registrationId,
                "corr-goal-registration-retrieve",
                PRINCIPAL_B,
                PASSWORD_B);
        assertEquals(200, retrieved.statusCode());
        assertRegistrationBody(retrieved.body(), registrationId, publishedEventId, "active");

        // 9. organizer (PRINCIPAL_A) retrieves Event Registrations and sees active state without participant identity
        HttpResponse<String> organizerActive = getEventRegistrations(
                publishedEventId,
                "corr-goal-organizer-active",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(200, organizerActive.statusCode());
        assertCorrelation(organizerActive, "corr-goal-organizer-active");
        assertRegistrationBody(organizerActive.body(), registrationId, publishedEventId, "active");
        assertFalse(organizerActive.body().contains(PRINCIPAL_B));
        assertFalse(organizerActive.body().contains("participant"));

        // 10. known non-owner (PRINCIPAL_C) gets organizer-view 403 forbidden; unauthenticated gets 401; unknown event gets 404
        HttpResponse<String> nonOwnerView = getEventRegistrations(
                publishedEventId,
                "corr-goal-non-owner-view",
                PRINCIPAL_C,
                PASSWORD_C);
        assertEquals(403, nonOwnerView.statusCode());
        assertCorrelation(nonOwnerView, "corr-goal-non-owner-view");
        assertJsonString(nonOwnerView.body(), "code", "forbidden");

        HttpResponse<String> unauthView = getEventRegistrations(
                publishedEventId,
                "corr-goal-unauth-view",
                null,
                null);
        assertEquals(401, unauthView.statusCode());
        assertCorrelation(unauthView, "corr-goal-unauth-view");
        assertJsonString(unauthView.body(), "code", "authentication_required");

        HttpResponse<String> invalidCredsView = getEventRegistrations(
                publishedEventId,
                "corr-goal-invalid-creds-view",
                PRINCIPAL_A,
                "invalid-secret-proof");
        assertEquals(401, invalidCredsView.statusCode());
        assertCorrelation(invalidCredsView, "corr-goal-invalid-creds-view");
        assertJsonString(invalidCredsView.body(), "code", "authentication_required");

        HttpResponse<String> unknownEventView = getEventRegistrations(
                "goal-57-unknown-event",
                "corr-goal-unknown-event-view",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(404, unknownEventView.statusCode());
        assertCorrelation(unknownEventView, "corr-goal-unknown-event-view");
        assertJsonString(unknownEventView.body(), "code", "event_not_found");

        // 11. organizer (PRINCIPAL_A) does not gain participant-private retrieval/cancellation rights over distinct participant's registration (404)
        HttpResponse<String> organizerPrivateRetrieve = getRegistration(
                registrationId,
                "corr-goal-organizer-private-retrieve",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(404, organizerPrivateRetrieve.statusCode());
        assertJsonString(organizerPrivateRetrieve.body(), "code", "event_registration_not_found");

        HttpResponse<String> organizerPrivateCancel = cancelRegistration(
                registrationId,
                "corr-goal-organizer-private-cancel",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(404, organizerPrivateCancel.statusCode());
        assertJsonString(organizerPrivateCancel.body(), "code", "event_registration_not_found");

        HttpResponse<String> thirdPartyRetrieve = getRegistration(
                registrationId,
                "corr-goal-third-party-retrieve",
                PRINCIPAL_C,
                PASSWORD_C);
        assertEquals(404, thirdPartyRetrieve.statusCode());
        assertJsonString(thirdPartyRetrieve.body(), "code", "event_registration_not_found");

        // 12. create a Registration targeting another Event and prove it is excluded from the queried organizer collection
        HttpResponse<String> otherRegistrationCreated = postRegistration(
                registrationJson(otherRegistrationId, otherEventId),
                "corr-goal-other-reg-create",
                PRINCIPAL_B,
                PASSWORD_B);
        assertEquals(201, otherRegistrationCreated.statusCode());

        HttpResponse<String> organizerMainCollection = getEventRegistrations(
                publishedEventId,
                "corr-goal-organizer-main-col",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(200, organizerMainCollection.statusCode());
        assertRegistrationBody(organizerMainCollection.body(), registrationId, publishedEventId, "active");
        assertFalse(organizerMainCollection.body().contains(otherRegistrationId));

        HttpResponse<String> organizerOtherCollection = getEventRegistrations(
                otherEventId,
                "corr-goal-organizer-other-col",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(200, organizerOtherCollection.statusCode());
        assertRegistrationBody(organizerOtherCollection.body(), otherRegistrationId, otherEventId, "active");
        assertFalse(organizerOtherCollection.body().contains(registrationId));

        // 13. participant (PRINCIPAL_B) cancels registration
        HttpResponse<String> cancelled = cancelRegistration(
                registrationId,
                "corr-goal-registration-cancel",
                PRINCIPAL_B,
                PASSWORD_B);
        assertEquals(200, cancelled.statusCode());
        assertRegistrationBody(cancelled.body(), registrationId, publishedEventId, "cancelled");

        // 14. organizer sees the same Registration as cancelled
        HttpResponse<String> organizerCancelled = getEventRegistrations(
                publishedEventId,
                "corr-goal-organizer-cancelled",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(200, organizerCancelled.statusCode());
        assertRegistrationBody(organizerCancelled.body(), registrationId, publishedEventId, "cancelled");

        // 15. repeated cancellation remains idempotent; same-pair re-registration remains conflict
        HttpResponse<String> repeatedCancel = cancelRegistration(
                registrationId,
                "corr-goal-registration-cancel-repeat",
                PRINCIPAL_B,
                PASSWORD_B);
        assertEquals(200, repeatedCancel.statusCode());
        assertRegistrationBody(repeatedCancel.body(), registrationId, publishedEventId, "cancelled");

        HttpResponse<String> duplicateRegistration = postRegistration(
                registrationJson("goal-57-registration-conflict", publishedEventId),
                "corr-goal-registration-conflict",
                PRINCIPAL_B,
                PASSWORD_B);
        assertEquals(409, duplicateRegistration.statusCode());
        assertCorrelation(duplicateRegistration, "corr-goal-registration-conflict");
        assertJsonString(duplicateRegistration.body(), "code", "registration_conflict");
        assertEquals(0, registrationCount("goal-57-registration-conflict"));

        // 16. modification after publication is rejected according to accepted Event semantics (409 event_already_published)
        HttpResponse<String> updateAfterPublication = updateEvent(
                publishedEventId,
                "Attempted Update After Publish",
                "corr-goal-update-after-publish",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(409, updateAfterPublication.statusCode());
        assertCorrelation(updateAfterPublication, "corr-goal-update-after-publish");
        assertJsonString(updateAfterPublication.body(), "code", "event_already_published");

        // 17. restart against same PostgreSQL
        application.close();
        application = null;
        startApplication();

        // 18. Event ownership/publication and Registration cancellation remain durable
        HttpResponse<String> discoveredAfterRestart =
                discoverEvents("corr-goal-discover-restart");
        assertEquals(200, discoveredAfterRestart.statusCode());
        assertCorrelation(discoveredAfterRestart, "corr-goal-discover-restart");
        assertJsonString(discoveredAfterRestart.body(), "eventId", publishedEventId);
        assertFalse(discoveredAfterRestart.body().contains(unpublishedEventId));

        // 19. organizer still sees cancelled Registration after restart
        HttpResponse<String> organizerAfterRestart = getEventRegistrations(
                publishedEventId,
                "corr-goal-organizer-restart",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(200, organizerAfterRestart.statusCode());
        assertRegistrationBody(organizerAfterRestart.body(), registrationId, publishedEventId, "cancelled");

        // 20. participant-private (PRINCIPAL_B) cancelled retrieval and non-owner privacy remain intact after restart
        HttpResponse<String> retrievedAfterRestart = getRegistration(
                registrationId,
                "corr-goal-restart-retrieve-owner",
                PRINCIPAL_B,
                PASSWORD_B);
        assertEquals(200, retrievedAfterRestart.statusCode());
        assertRegistrationBody(retrievedAfterRestart.body(), registrationId, publishedEventId, "cancelled");
        assertPersistedRegistration(
                registrationId,
                "participant",
                PRINCIPAL_B,
                "event",
                publishedEventId);

        HttpResponse<String> nonOwnerAfterRestart = getRegistration(
                registrationId,
                "corr-goal-restart-retrieve-non-owner",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(404, nonOwnerAfterRestart.statusCode());
        assertJsonString(nonOwnerAfterRestart.body(), "code", "event_registration_not_found");

        HttpResponse<String> thirdPartyAfterRestart = getRegistration(
                registrationId,
                "corr-goal-restart-retrieve-third-party",
                PRINCIPAL_C,
                PASSWORD_C);
        assertEquals(404, thirdPartyAfterRestart.statusCode());
        assertJsonString(thirdPartyAfterRestart.body(), "code", "event_registration_not_found");

        HttpResponse<String> nonOwnerViewAfterRestart = getEventRegistrations(
                publishedEventId,
                "corr-goal-restart-non-owner-view",
                PRINCIPAL_C,
                PASSWORD_C);
        assertEquals(403, nonOwnerViewAfterRestart.statusCode());
        assertJsonString(nonOwnerViewAfterRestart.body(), "code", "forbidden");

        HttpResponse<String> unauthViewAfterRestart = getEventRegistrations(
                publishedEventId,
                "corr-goal-restart-unauth-view",
                null,
                null);
        assertEquals(401, unauthViewAfterRestart.statusCode());
        assertJsonString(unauthViewAfterRestart.body(), "code", "authentication_required");

        HttpResponse<String> invalidViewAfterRestart = getEventRegistrations(
                publishedEventId,
                "corr-goal-restart-invalid-view",
                PRINCIPAL_A,
                "invalid-secret-proof");
        assertEquals(401, invalidViewAfterRestart.statusCode());
        assertCorrelation(invalidViewAfterRestart, "corr-goal-restart-invalid-view");
        assertJsonString(invalidViewAfterRestart.body(), "code", "authentication_required");
    }

    @Test
    void unknownEventReturnsNotFoundAndCreatesNoRegistration() throws Exception {
        HttpResponse<String> create = postRegistration(
                registrationJson(
                        "registration-http-missing-event",
                        "event-does-not-exist"),
                "corr-unknown-event",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(404, create.statusCode());
        assertCorrelation(create, "corr-unknown-event");
        assertJsonString(create.body(), "code", "event_not_found");
        assertEquals(0, registrationCount("registration-http-missing-event"));
    }

    @Test
    void unpublishedEventReturnsConflictAndCreatesNoRegistration() throws Exception {
        defineEvent("registration-event-unpublished-only");

        HttpResponse<String> create = postRegistration(
                registrationJson(
                        "registration-http-unpublished-only",
                        "registration-event-unpublished-only"),
                "corr-unpublished-event-only",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(409, create.statusCode());
        assertCorrelation(create, "corr-unpublished-event-only");
        assertJsonString(create.body(), "code", "event_not_published");
        assertJsonString(create.body(), "message", "Referenced Event is not published");
        assertEquals(0, registrationCount("registration-http-unpublished-only"));
    }

    @Test
    void duplicateParticipantEventPairReturnsConflictWithoutReplacingDurableState()
            throws Exception {
        defineAndPublishEvent("registration-event-duplicate");

        assertEquals(
                201,
                postRegistration(
                                registrationJson(
                                        "registration-http-original",
                                        "registration-event-duplicate"),
                                "corr-original",
                                PRINCIPAL_A,
                                PASSWORD_A)
                        .statusCode());

        HttpResponse<String> duplicate = postRegistration(
                registrationJson(
                        "registration-http-conflicting",
                        "registration-event-duplicate"),
                "corr-conflict",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(409, duplicate.statusCode());
        assertCorrelation(duplicate, "corr-conflict");
        assertJsonString(duplicate.body(), "code", "registration_conflict");

        assertPersistedRegistration(
                "registration-http-original",
                "participant",
                PRINCIPAL_A,
                "event",
                "registration-event-duplicate");
        assertEquals(0, registrationCount("registration-http-conflicting"));
    }

    @Test
    void structurallyInvalidAuthenticatedRequestReturnsBadRequest() throws Exception {
        String body = "{"
                + "\"registrationId\":\"registration-http-invalid\""
                + "}";

        HttpResponse<String> response = postRegistration(
                body,
                "corr-invalid-registration",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(400, response.statusCode());
        assertCorrelation(response, "corr-invalid-registration");
        assertJsonString(response.body(), "code", "invalid_request");
        assertEquals(0, registrationCount("registration-http-invalid"));
    }

    @Test
    void authenticatedUnknownRegistrationReturnsNotFound() throws Exception {
        HttpResponse<String> response = getRegistration(
                "registration-http-unknown",
                "corr-unknown-registration",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(404, response.statusCode());
        assertCorrelation(response, "corr-unknown-registration");
        assertJsonString(response.body(), "code", "event_registration_not_found");
    }

    @Test
    void organizerViewsMultipleRegistrationsWithActiveAndCancelledLifecycles() throws Exception {
        String eventId = "organizer-view-event-multi";
        defineAndPublishEvent(eventId);

        postRegistration(
                registrationJson("reg-multi-1", eventId),
                "corr-multi-1",
                PRINCIPAL_A,
                PASSWORD_A);

        postRegistration(
                registrationJson("reg-multi-2", eventId),
                "corr-multi-2",
                PRINCIPAL_B,
                PASSWORD_B);

        cancelRegistration("reg-multi-2", "corr-multi-cancel", PRINCIPAL_B, PASSWORD_B);

        HttpResponse<String> response = getEventRegistrations(
                eventId,
                "corr-organizer-multi-view",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(200, response.statusCode());
        assertCorrelation(response, "corr-organizer-multi-view");
        assertRegistrationBody(response.body(), "reg-multi-1", eventId, "active");
        assertRegistrationBody(response.body(), "reg-multi-2", eventId, "cancelled");
        assertFalse(response.body().contains(PRINCIPAL_A));
        assertFalse(response.body().contains(PRINCIPAL_B));
        assertFalse(response.body().contains("participant"));
    }

    @Test
    void organizerEventRegistrationsRejectsNonOwner() throws Exception {
        String eventId = "organizer-view-event-authz";
        defineAndPublishEvent(eventId);

        HttpResponse<String> response = getEventRegistrations(
                eventId,
                "corr-organizer-authz-denied",
                PRINCIPAL_B,
                PASSWORD_B);

        assertEquals(403, response.statusCode());
        assertCorrelation(response, "corr-organizer-authz-denied");
        assertJsonString(response.body(), "code", "forbidden");
        assertJsonString(response.body(), "message", "Authenticated actor is not the Event owner");
    }

    @Test
    void organizerEventRegistrationsRejectsUnauthenticated() throws Exception {
        HttpResponse<String> missing = getEventRegistrations(
                "any-event",
                "corr-organizer-unauth-missing",
                null,
                null);

        assertEquals(401, missing.statusCode());
        assertCorrelation(missing, "corr-organizer-unauth-missing");
        assertJsonString(missing.body(), "code", "authentication_required");

        HttpResponse<String> invalid = getEventRegistrations(
                "any-event",
                "corr-organizer-unauth-invalid",
                PRINCIPAL_A,
                "wrong-secret");

        assertEquals(401, invalid.statusCode());
        assertCorrelation(invalid, "corr-organizer-unauth-invalid");
        assertJsonString(invalid.body(), "code", "authentication_required");
    }

    @Test
    void organizerEventRegistrationsReturnsNotFoundForUnknownEvent() throws Exception {
        HttpResponse<String> response = getEventRegistrations(
                "unknown-event-id-999",
                "corr-organizer-unknown-event",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(404, response.statusCode());
        assertCorrelation(response, "corr-organizer-unknown-event");
        assertJsonString(response.body(), "code", "event_not_found");
    }

    @Test
    void withdrawnEventRejectsNewRegistrationsAndPreservesExistingRegistrationsAcrossProcessRestart()
            throws Exception {
        String eventId = "withdrawn-lifecycle-event";
        String regIdB = "registration-b-lifecycle";
        String regIdC = "registration-c-lifecycle";

        // 1. Organizer A defines and publishes event
        defineAndPublishEvent(eventId);

        HttpResponse<String> publishedEvent = getEvent(eventId, "corr-get-published");
        assertEquals(200, publishedEvent.statusCode());
        assertCorrelation(publishedEvent, "corr-get-published");
        assertJsonString(publishedEvent.body(), "publicationState", "published");

        // 1a. Anonymous discovery contains this Event with publicationState=published before withdrawal
        HttpResponse<String> discoveredBeforeWithdrawal = discoverEvents("corr-discover-before-withdraw");
        assertEquals(200, discoveredBeforeWithdrawal.statusCode());
        assertCorrelation(discoveredBeforeWithdrawal, "corr-discover-before-withdraw");
        assertTrue(discoveredBeforeWithdrawal.body().contains("\"eventId\":\"" + eventId + "\""));
        assertJsonString(discoveredBeforeWithdrawal.body(), "publicationState", "published");

        // 1b. Missing credentials on withdrawal return 401 authentication_required with preserved correlation
        HttpResponse<String> unauthWithdraw = withdrawEvent(
                eventId,
                "corr-unauth-withdraw",
                null,
                null);
        assertEquals(401, unauthWithdraw.statusCode());
        assertCorrelation(unauthWithdraw, "corr-unauth-withdraw");
        assertJsonString(unauthWithdraw.body(), "code", "authentication_required");

        // 1c. Invalid credentials on withdrawal return 401 authentication_required with preserved correlation
        HttpResponse<String> invalidCredsWithdraw = withdrawEvent(
                eventId,
                "corr-invalid-withdraw",
                PRINCIPAL_A,
                "wrong-secret");
        assertEquals(401, invalidCredsWithdraw.statusCode());
        assertCorrelation(invalidCredsWithdraw, "corr-invalid-withdraw");
        assertJsonString(invalidCredsWithdraw.body(), "code", "authentication_required");

        // 2. Participant B registers for published event -> 201 active
        HttpResponse<String> createdB = postRegistration(
                registrationJson(regIdB, eventId),
                "corr-reg-b",
                PRINCIPAL_B,
                PASSWORD_B);
        assertEquals(201, createdB.statusCode());
        assertCorrelation(createdB, "corr-reg-b");
        assertRegistrationBody(createdB.body(), regIdB, eventId, "active");

        // 3. Organizer A views registrations for event -> 200 containing B as active
        HttpResponse<String> orgRegistrations = getEventRegistrations(
                eventId,
                "corr-org-regs",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(200, orgRegistrations.statusCode());
        assertCorrelation(orgRegistrations, "corr-org-regs");
        assertRegistrationBody(orgRegistrations.body(), regIdB, eventId, "active");

        // 4. Participant B (non-owner) is forbidden from withdrawing event -> 403 forbidden with preserved correlation
        HttpResponse<String> nonOwnerWithdraw = withdrawEvent(
                eventId,
                "corr-non-owner-withdraw",
                PRINCIPAL_B,
                PASSWORD_B);
        assertEquals(403, nonOwnerWithdraw.statusCode());
        assertCorrelation(nonOwnerWithdraw, "corr-non-owner-withdraw");
        assertJsonString(nonOwnerWithdraw.body(), "code", "forbidden");

        // 4a. Retrieve Event by known ID and prove it is still published after non-owner denial
        HttpResponse<String> eventAfterDeniedWithdraw = getEvent(eventId, "corr-get-after-denied-withdraw");
        assertEquals(200, eventAfterDeniedWithdraw.statusCode());
        assertCorrelation(eventAfterDeniedWithdraw, "corr-get-after-denied-withdraw");
        assertJsonString(eventAfterDeniedWithdraw.body(), "publicationState", "published");

        // 5. Organizer A withdraws event -> 204 with preserved correlation
        HttpResponse<String> ownerWithdraw = withdrawEvent(
                eventId,
                "corr-owner-withdraw",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(204, ownerWithdraw.statusCode());
        assertCorrelation(ownerWithdraw, "corr-owner-withdraw");

        // 6. Direct retrieval of withdrawn event -> 200 with publicationState=withdrawn
        HttpResponse<String> withdrawnEvent = getEvent(eventId, "corr-get-withdrawn");
        assertEquals(200, withdrawnEvent.statusCode());
        assertCorrelation(withdrawnEvent, "corr-get-withdrawn");
        assertJsonString(withdrawnEvent.body(), "publicationState", "withdrawn");

        // 7. Discovery excludes withdrawn event
        HttpResponse<String> discovered = discoverEvents("corr-discover-withdrawn");
        assertEquals(200, discovered.statusCode());
        assertCorrelation(discovered, "corr-discover-withdrawn");
        assertFalse(discovered.body().contains("\"eventId\":\"" + eventId + "\""));

        // 7a. Immediately after owner withdrawal, Organizer retrieves Event registrations and sees existing Registration as active
        HttpResponse<String> orgRegistrationsAfterWithdrawal = getEventRegistrations(
                eventId,
                "corr-org-regs-after-withdrawal",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(200, orgRegistrationsAfterWithdrawal.statusCode());
        assertCorrelation(orgRegistrationsAfterWithdrawal, "corr-org-regs-after-withdrawal");
        assertRegistrationBody(orgRegistrationsAfterWithdrawal.body(), regIdB, eventId, "active");

        // 8. Participant C attempts registration against withdrawn event -> 409 event_not_published
        HttpResponse<String> createdC = postRegistration(
                registrationJson(regIdC, eventId),
                "corr-reg-c",
                PRINCIPAL_C,
                PASSWORD_C);
        assertEquals(409, createdC.statusCode());
        assertCorrelation(createdC, "corr-reg-c");
        assertJsonString(createdC.body(), "code", "event_not_published");
        assertEquals(0, registrationCount(regIdC));
        assertEquals(1, registrationCount(regIdB));

        // 9. Organizer attempts update, publication, or withdrawal on withdrawn event -> 409 event_withdrawn
        HttpResponse<String> updateWithdrawn = updateEvent(
                eventId,
                "Updated Name",
                "corr-update-withdrawn",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(409, updateWithdrawn.statusCode());
        assertCorrelation(updateWithdrawn, "corr-update-withdrawn");
        assertJsonString(updateWithdrawn.body(), "code", "event_withdrawn");

        HttpResponse<String> republishWithdrawn = publishEvent(eventId, "corr-repub-withdrawn");
        assertEquals(409, republishWithdrawn.statusCode());
        assertCorrelation(republishWithdrawn, "corr-repub-withdrawn");
        assertJsonString(republishWithdrawn.body(), "code", "event_withdrawn");

        HttpResponse<String> rewithdraw = withdrawEvent(
                eventId,
                "corr-re-withdraw",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(409, rewithdraw.statusCode());
        assertCorrelation(rewithdraw, "corr-re-withdraw");
        assertJsonString(rewithdraw.body(), "code", "event_withdrawn");

        // 10. Existing participant registration for B remains intact and can be cancelled
        HttpResponse<String> retrievedB = getRegistration(
                regIdB,
                "corr-get-reg-b",
                PRINCIPAL_B,
                PASSWORD_B);
        assertEquals(200, retrievedB.statusCode());
        assertCorrelation(retrievedB, "corr-get-reg-b");
        assertRegistrationBody(retrievedB.body(), regIdB, eventId, "active");

        HttpResponse<String> cancelledB = cancelRegistration(
                regIdB,
                "corr-cancel-b",
                PRINCIPAL_B,
                PASSWORD_B);
        assertEquals(200, cancelledB.statusCode());
        assertCorrelation(cancelledB, "corr-cancel-b");
        assertRegistrationBody(cancelledB.body(), regIdB, eventId, "cancelled");

        // 10a. After participant cancellation, before restart: Organizer views registrations and sees Registration as cancelled
        HttpResponse<String> orgRegistrationsAfterCancellation = getEventRegistrations(
                eventId,
                "corr-org-regs-after-cancellation",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(200, orgRegistrationsAfterCancellation.statusCode());
        assertCorrelation(orgRegistrationsAfterCancellation, "corr-org-regs-after-cancellation");
        assertRegistrationBody(orgRegistrationsAfterCancellation.body(), regIdB, eventId, "cancelled");

        // 11. Application restart against PostgreSQL to prove durability across process restart
        application.close();
        startApplication();

        // 12. Direct retrieval after restart -> 200 with publicationState=withdrawn
        HttpResponse<String> eventAfterRestart = getEvent(eventId, "corr-get-event-restart");
        assertEquals(200, eventAfterRestart.statusCode());
        assertCorrelation(eventAfterRestart, "corr-get-event-restart");
        assertJsonString(eventAfterRestart.body(), "publicationState", "withdrawn");

        // 13. Discovery after restart still excludes withdrawn event
        HttpResponse<String> discoverAfterRestart = discoverEvents("corr-discover-restart");
        assertEquals(200, discoverAfterRestart.statusCode());
        assertCorrelation(discoverAfterRestart, "corr-discover-restart");
        assertFalse(discoverAfterRestart.body().contains("\"eventId\":\"" + eventId + "\""));

        // 14. Registration against withdrawn event after restart is still rejected -> 409 event_not_published
        HttpResponse<String> createdCAfterRestart = postRegistration(
                registrationJson(regIdC, eventId),
                "corr-reg-c-restart",
                PRINCIPAL_C,
                PASSWORD_C);
        assertEquals(409, createdCAfterRestart.statusCode());
        assertCorrelation(createdCAfterRestart, "corr-reg-c-restart");
        assertJsonString(createdCAfterRestart.body(), "code", "event_not_published");
        assertEquals(0, registrationCount(regIdC));
        assertEquals(1, registrationCount(regIdB));

        // 15. Existing registration state after restart remains cancelled
        HttpResponse<String> retrievedBAfterRestart = getRegistration(
                regIdB,
                "corr-get-reg-b-restart",
                PRINCIPAL_B,
                PASSWORD_B);
        assertEquals(200, retrievedBAfterRestart.statusCode());
        assertCorrelation(retrievedBAfterRestart, "corr-get-reg-b-restart");
        assertRegistrationBody(retrievedBAfterRestart.body(), regIdB, eventId, "cancelled");

        // 16. Organizer registration view after restart returns the existing registration as cancelled
        HttpResponse<String> orgRegistrationsAfterRestart = getEventRegistrations(
                eventId,
                "corr-org-regs-restart",
                PRINCIPAL_A,
                PASSWORD_A);
        assertEquals(200, orgRegistrationsAfterRestart.statusCode());
        assertCorrelation(orgRegistrationsAfterRestart, "corr-org-regs-restart");
        assertRegistrationBody(orgRegistrationsAfterRestart.body(), regIdB, eventId, "cancelled");
    }

    private static void defineEvent(String eventId) throws Exception {
        defineEvent(eventId, "Registration Event", PRINCIPAL_A, PASSWORD_A);
    }

    private static void defineEvent(
            String eventId,
            String name,
            String principal,
            String password) throws Exception {
        String body = "{"
                + "\"eventId\":\"" + eventId + "\","
                + "\"name\":\"" + name + "\","
                + "\"slug\":\"" + eventId + "\","
                + "\"startsAt\":\"2026-09-01T08:00:00Z\","
                + "\"endsAt\":\"2026-09-01T10:00:00Z\","
                + "\"timezone\":\"Europe/Copenhagen\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/v1/events"))
                .header("Content-Type", "application/json")
                .header(CORRELATION_HEADER, "corr-define-" + eventId)
                .header("Authorization", basicAuthorization(principal, password))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        assertEquals(
                201,
                HTTP.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    private static HttpResponse<String> updateEvent(
            String eventId,
            String name,
            String correlationId,
            String principal,
            String password) throws Exception {
        String body = "{"
                + "\"name\":\"" + name + "\","
                + "\"slug\":\"" + eventId + "\","
                + "\"startsAt\":\"2026-09-01T08:00:00Z\","
                + "\"endsAt\":\"2026-09-01T10:00:00Z\","
                + "\"timezone\":\"Europe/Copenhagen\""
                + "}";

        HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri.resolve("/api/v1/events/" + eventId))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body));

        if (principal != null && password != null) {
            builder.header("Authorization", basicAuthorization(principal, password));
        }
        if (correlationId != null) {
            builder.header(CORRELATION_HEADER, correlationId);
        }

        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void defineAndPublishEvent(String eventId) throws Exception {
        defineEvent(eventId);
        HttpResponse<String> published = publishEvent(eventId, "corr-publish-" + eventId);
        assertEquals(204, published.statusCode());
    }

    private static HttpResponse<String> discoverEvents(String correlationId)
            throws Exception {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events"));

        if (correlationId != null) {
            builder.header(CORRELATION_HEADER, correlationId);
        }

        return HTTP.send(
                builder.GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> getEvent(String eventId, String correlationId)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                baseUri.resolve("/api/v1/events/" + eventId));

        if (correlationId != null) {
            builder.header(CORRELATION_HEADER, correlationId);
        }

        return HTTP.send(
                builder.GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> publishEvent(String eventId, String correlationId)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                baseUri.resolve("/api/v1/events/" + eventId + "/publication"))
                .header("Authorization", basicAuthorization(PRINCIPAL_A, PASSWORD_A));

        if (correlationId != null) {
            builder.header(CORRELATION_HEADER, correlationId);
        }

        return HTTP.send(
                builder.POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> withdrawEvent(
            String eventId,
            String correlationId,
            String principal,
            String password) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                baseUri.resolve("/api/v1/events/" + eventId + "/publication"));

        addCommonHeaders(builder, correlationId, principal, password);

        return HTTP.send(
                builder.DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> postRegistration(
            String body,
            String correlationId,
            String principal,
            String password) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        baseUri.resolve("/api/v1/event-registrations"))
                .header("Content-Type", "application/json");

        addCommonHeaders(builder, correlationId, principal, password);

        return HTTP.send(
                builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> getRegistration(
            String registrationId,
            String correlationId,
            String principal,
            String password) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                baseUri.resolve("/api/v1/event-registrations/" + registrationId));

        addCommonHeaders(builder, correlationId, principal, password);

        return HTTP.send(
                builder.GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> getEventRegistrations(
            String eventId,
            String correlationId,
            String principal,
            String password) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                baseUri.resolve("/api/v1/events/" + eventId + "/registrations"));

        addCommonHeaders(builder, correlationId, principal, password);

        return HTTP.send(
                builder.GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> cancelRegistration(
            String registrationId,
            String correlationId,
            String principal,
            String password) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                baseUri.resolve("/api/v1/event-registrations/" + registrationId));

        addCommonHeaders(builder, correlationId, principal, password);

        return HTTP.send(
                builder.DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static void addCommonHeaders(
            HttpRequest.Builder builder,
            String correlationId,
            String principal,
            String password) {
        if (correlationId != null) {
            builder.header(CORRELATION_HEADER, correlationId);
        }

        if (principal != null && password != null) {
            builder.header(
                    "Authorization",
                    basicAuthorization(principal, password));
        }
    }

    private static String basicAuthorization(String principal, String password) {
        String credentials = principal + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
    }

    private static String registrationJson(
            String registrationId,
            String eventId) {
        return "{"
                + "\"registrationId\":\"" + registrationId + "\","
                + "\"eventId\":\"" + eventId + "\""
                + "}";
    }

    private static void assertPersistedRegistration(
            String registrationId,
            String registrantNamespace,
            String registrantReference,
            String targetNamespace,
            String targetReference) throws Exception {
        String sql = """
                select registrant_namespace, registrant_reference, target_namespace, target_reference
                from registration.registrations
                where registration_id = ?
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, registrationId);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(registrantNamespace, result.getString("registrant_namespace"));
                assertEquals(registrantReference, result.getString("registrant_reference"));
                assertEquals(targetNamespace, result.getString("target_namespace"));
                assertEquals(targetReference, result.getString("target_reference"));
                assertFalse(result.next());
            }
        }
    }

    private static int registrationCount(String registrationId) throws Exception {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "select count(*) from registration.registrations where registration_id = ?")) {
            statement.setString(1, registrationId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static void assertRegistrationBody(
            String body,
            String registrationId,
            String eventId,
            String lifecycle) {
        assertJsonString(body, "registrationId", registrationId);
        assertJsonString(body, "eventId", eventId);
        assertJsonString(body, "lifecycle", lifecycle);
    }

    private static void assertJsonString(String body, String field, String value) {
        Pattern expected = Pattern.compile(
                "\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\""
                        + Pattern.quote(value) + "\\\"");
        assertTrue(
                expected.matcher(body).find(),
                () -> "Expected JSON field %s=%s in: %s".formatted(field, value, body));
    }

    private static void assertCorrelation(
            HttpResponse<?> response,
            String expectedCorrelation) {
        assertEquals(
                expectedCorrelation,
                response.headers().firstValue(CORRELATION_HEADER).orElseThrow());
    }

    private static void assertGeneratedCorrelation(HttpResponse<?> response) {
        String correlation = response.headers()
                .firstValue(CORRELATION_HEADER)
                .orElseThrow();
        assertFalse(correlation.isBlank());
    }
}
