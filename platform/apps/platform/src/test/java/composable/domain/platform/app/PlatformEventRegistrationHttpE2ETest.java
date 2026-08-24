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
    private static final String PASSWORD_A = "test-proof-secret-a";
    private static final String PASSWORD_B = "test-proof-secret-b";

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
                        + encoder.encode(PASSWORD_B));

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
        String registrationId = "goal-57-registration";

        // 1. define an Event and leave it unpublished
        defineEvent(publishedEventId);
        defineEvent(unpublishedEventId);

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

        // 2. authenticated participant Registration attempt is rejected with 409 event_not_published
        HttpResponse<String> unpublishedRegistration = postRegistration(
                registrationJson(registrationId, publishedEventId),
                "corr-goal-registration-unpublished",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(409, unpublishedRegistration.statusCode());
        assertCorrelation(unpublishedRegistration, "corr-goal-registration-unpublished");
        assertJsonString(unpublishedRegistration.body(), "code", "event_not_published");
        assertJsonString(unpublishedRegistration.body(), "message", "Referenced Event is not published");

        // 3. no Registration row/state is created by that rejection
        assertEquals(0, registrationCount(registrationId));

        // 4. publish the same Event through the accepted organizer flow
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

        HttpResponse<String> discovered = discoverEvents("corr-goal-discover-after");

        assertEquals(200, discovered.statusCode());
        assertCorrelation(discovered, "corr-goal-discover-after");
        assertJsonString(discovered.body(), "eventId", publishedEventId);
        assertFalse(discovered.body().contains(unpublishedEventId));

        // 5. participant Registration succeeds with 201
        HttpResponse<String> created = postRegistration(
                registrationJson(registrationId, publishedEventId),
                "corr-goal-registration-create",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(201, created.statusCode());
        assertRegistrationBody(created.body(), registrationId, publishedEventId, "active");

        // 6. participant retrieves the active Registration
        HttpResponse<String> retrieved = getRegistration(
                registrationId,
                "corr-goal-registration-retrieve",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(200, retrieved.statusCode());
        assertRegistrationBody(retrieved.body(), registrationId, publishedEventId, "active");

        // 7. another authenticated participant cannot retrieve/cancel it and receives private-state 404 behavior
        HttpResponse<String> nonOwnerRetrieve = getRegistration(
                registrationId,
                "corr-goal-non-owner-retrieve",
                PRINCIPAL_B,
                PASSWORD_B);
        assertEquals(404, nonOwnerRetrieve.statusCode());
        assertJsonString(nonOwnerRetrieve.body(), "code", "event_registration_not_found");

        HttpResponse<String> nonOwnerCancel = cancelRegistration(
                registrationId,
                "corr-goal-non-owner-cancel",
                PRINCIPAL_B,
                PASSWORD_B);
        assertEquals(404, nonOwnerCancel.statusCode());
        assertJsonString(nonOwnerCancel.body(), "code", "event_registration_not_found");

        // 8. participant cancels it
        HttpResponse<String> cancelled = cancelRegistration(
                registrationId,
                "corr-goal-registration-cancel",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(200, cancelled.statusCode());
        assertRegistrationBody(cancelled.body(), registrationId, publishedEventId, "cancelled");

        // 9. repeated cancellation remains idempotent
        HttpResponse<String> repeatedCancel = cancelRegistration(
                registrationId,
                "corr-goal-registration-cancel-repeat",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(200, repeatedCancel.statusCode());
        assertRegistrationBody(repeatedCancel.body(), registrationId, publishedEventId, "cancelled");

        // 10. same-pair re-registration remains a uniqueness conflict
        HttpResponse<String> duplicateRegistration = postRegistration(
                registrationJson("goal-57-registration-conflict", publishedEventId),
                "corr-goal-registration-conflict",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(409, duplicateRegistration.statusCode());
        assertCorrelation(duplicateRegistration, "corr-goal-registration-conflict");
        assertJsonString(duplicateRegistration.body(), "code", "registration_conflict");
        assertEquals(0, registrationCount("goal-57-registration-conflict"));

        // 11. after application restart against the same PostgreSQL database, the cancelled Registration remains durable/private
        application.close();
        application = null;
        startApplication();

        HttpResponse<String> discoveredAfterRestart =
                discoverEvents("corr-goal-discover-restart");

        assertEquals(200, discoveredAfterRestart.statusCode());
        assertCorrelation(discoveredAfterRestart, "corr-goal-discover-restart");
        assertJsonString(discoveredAfterRestart.body(), "eventId", publishedEventId);
        assertFalse(discoveredAfterRestart.body().contains(unpublishedEventId));

        HttpResponse<String> retrievedAfterRestart = getRegistration(
                registrationId,
                "corr-goal-restart-retrieve-owner",
                PRINCIPAL_A,
                PASSWORD_A);

        assertEquals(200, retrievedAfterRestart.statusCode());
        assertRegistrationBody(retrievedAfterRestart.body(), registrationId, publishedEventId, "cancelled");
        assertPersistedRegistration(
                registrationId,
                "participant",
                PRINCIPAL_A,
                "event",
                publishedEventId);

        HttpResponse<String> nonOwnerAfterRestart = getRegistration(
                registrationId,
                "corr-goal-restart-retrieve-non-owner",
                PRINCIPAL_B,
                PASSWORD_B);

        assertEquals(404, nonOwnerAfterRestart.statusCode());
        assertJsonString(nonOwnerAfterRestart.body(), "code", "event_registration_not_found");
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

    private static void defineEvent(String eventId) throws Exception {
        String body = "{"
                + "\"eventId\":\"" + eventId + "\","
                + "\"name\":\"Registration Event\","
                + "\"slug\":\"" + eventId + "\","
                + "\"startsAt\":\"2026-09-01T08:00:00Z\","
                + "\"endsAt\":\"2026-09-01T10:00:00Z\","
                + "\"timezone\":\"Europe/Copenhagen\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/v1/events"))
                .header("Content-Type", "application/json")
                .header(CORRELATION_HEADER, "corr-define-" + eventId)
                .header("Authorization", basicAuthorization(PRINCIPAL_A, PASSWORD_A))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        assertEquals(
                201,
                HTTP.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
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
