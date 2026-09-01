package composable.domain.platform.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.regex.Matcher;
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

class PlatformEventWaitlistHttpE2ETest {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String ORGANIZER = "opaque-waitlist-organizer";
    private static final String PARTICIPANT = "opaque-waitlist-participant";
    private static final String OTHER = "opaque-waitlist-other";
    private static final String ORGANIZER_PASSWORD = "waitlist-organizer-secret";
    private static final String PARTICIPANT_PASSWORD = "waitlist-participant-secret";
    private static final String OTHER_PASSWORD = "waitlist-other-secret";

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

    @Test
    void participantJourneyIsIdempotentPrivateAndDurable() throws Exception {
        String eventId = "waitlist-journey-event";
        defineEvent(eventId);
        publishEvent(eventId);

        HttpResponse<String> open = putWaitlist(
                eventId,
                "corr-waitlist-open",
                PARTICIPANT,
                PARTICIPANT_PASSWORD);
        assertEquals(409, open.statusCode());
        assertJsonString(open.body(), "code", "event_waitlist_unavailable");
        assertEquals(0, waitlistPairCount(PARTICIPANT, eventId));

        setAvailability(eventId, "closed");

        HttpResponse<String> first = putWaitlist(
                eventId,
                "corr-waitlist-first",
                PARTICIPANT,
                PARTICIPANT_PASSWORD);
        assertEquals(200, first.statusCode());
        assertCorrelation(first, "corr-waitlist-first");
        String participationId =
                jsonString(first.body(), "waitlistParticipationId");
        assertJsonString(first.body(), "eventId", eventId);
        assertFalse(first.body().contains("participantReference"));
        assertEquals(1, waitlistPairCount(PARTICIPANT, eventId));
        assertEquals(0, registrationPairCount(PARTICIPANT, eventId));

        HttpResponse<String> repeated = putWaitlist(
                eventId,
                "corr-waitlist-repeat",
                PARTICIPANT,
                PARTICIPANT_PASSWORD);
        assertEquals(200, repeated.statusCode());
        assertJsonString(
                repeated.body(),
                "waitlistParticipationId",
                participationId);
        assertEquals(1, waitlistPairCount(PARTICIPANT, eventId));
        assertEquals(0, registrationPairCount(PARTICIPANT, eventId));

        HttpResponse<String> ownerView = getWaitlist(
                eventId,
                "corr-waitlist-owner-view",
                PARTICIPANT,
                PARTICIPANT_PASSWORD);
        assertEquals(200, ownerView.statusCode());
        assertJsonString(
                ownerView.body(),
                "waitlistParticipationId",
                participationId);

        HttpResponse<String> nonOwner = getWaitlist(
                eventId,
                "corr-waitlist-non-owner",
                OTHER,
                OTHER_PASSWORD);
        assertEquals(404, nonOwner.statusCode());
        assertJsonString(
                nonOwner.body(),
                "code",
                "waitlist_participation_not_found");

        HttpResponse<String> unknownPrivate = getWaitlist(
                "waitlist-private-unknown",
                "corr-waitlist-private-unknown",
                OTHER,
                OTHER_PASSWORD);
        assertEquals(404, unknownPrivate.statusCode());
        assertJsonString(
                unknownPrivate.body(),
                "code",
                "waitlist_participation_not_found");

        setAvailability(eventId, "open");

        HttpResponse<String> afterReopen = getWaitlist(
                eventId,
                "corr-waitlist-reopen",
                PARTICIPANT,
                PARTICIPANT_PASSWORD);
        assertEquals(200, afterReopen.statusCode());
        assertJsonString(
                afterReopen.body(),
                "waitlistParticipationId",
                participationId);

        withdrawEvent(eventId);

        HttpResponse<String> afterWithdrawal = getWaitlist(
                eventId,
                "corr-waitlist-withdrawn",
                PARTICIPANT,
                PARTICIPANT_PASSWORD);
        assertEquals(200, afterWithdrawal.statusCode());
        assertJsonString(
                afterWithdrawal.body(),
                "waitlistParticipationId",
                participationId);

        application.close();
        application = null;
        startApplication();

        HttpResponse<String> afterRestart = getWaitlist(
                eventId,
                "corr-waitlist-restart",
                PARTICIPANT,
                PARTICIPANT_PASSWORD);
        assertEquals(200, afterRestart.statusCode());
        assertJsonString(
                afterRestart.body(),
                "waitlistParticipationId",
                participationId);
        assertEquals(1, waitlistPairCount(PARTICIPANT, eventId));
    }

    @Test
    void joinRejectsUnknownUnpublishedAndWithdrawnEventsWithoutMutation()
            throws Exception {
        String unknownId = "waitlist-unknown-event";
        HttpResponse<String> unknown = putWaitlist(
                unknownId,
                "corr-waitlist-unknown",
                PARTICIPANT,
                PARTICIPANT_PASSWORD);
        assertEquals(404, unknown.statusCode());
        assertJsonString(unknown.body(), "code", "event_not_found");
        assertEquals(0, waitlistPairCount(PARTICIPANT, unknownId));

        String unpublishedId = "waitlist-unpublished-event";
        defineEvent(unpublishedId);

        HttpResponse<String> unpublished = putWaitlist(
                unpublishedId,
                "corr-waitlist-unpublished",
                PARTICIPANT,
                PARTICIPANT_PASSWORD);
        assertEquals(409, unpublished.statusCode());
        assertJsonString(unpublished.body(), "code", "event_not_published");
        assertEquals(0, waitlistPairCount(PARTICIPANT, unpublishedId));

        String withdrawnId = "waitlist-withdrawn-event";
        defineEvent(withdrawnId);
        publishEvent(withdrawnId);
        withdrawEvent(withdrawnId);

        HttpResponse<String> withdrawn = putWaitlist(
                withdrawnId,
                "corr-waitlist-withdrawn-join",
                PARTICIPANT,
                PARTICIPANT_PASSWORD);
        assertEquals(409, withdrawn.statusCode());
        assertJsonString(withdrawn.body(), "code", "event_not_published");
        assertEquals(0, waitlistPairCount(PARTICIPANT, withdrawnId));
    }

    @Test
    void activeAndCancelledRegistrationPairsRemainIneligible()
            throws Exception {
        String activeEvent = "waitlist-active-registration-event";
        defineEvent(activeEvent);
        publishEvent(activeEvent);
        assertEquals(
                201,
                postRegistration(
                                "waitlist-active-registration",
                                activeEvent,
                                PARTICIPANT,
                                PARTICIPANT_PASSWORD)
                        .statusCode());
        setAvailability(activeEvent, "closed");

        HttpResponse<String> activeConflict = putWaitlist(
                activeEvent,
                "corr-waitlist-active-conflict",
                PARTICIPANT,
                PARTICIPANT_PASSWORD);
        assertEquals(409, activeConflict.statusCode());
        assertJsonString(
                activeConflict.body(),
                "code",
                "event_registration_exists");
        assertEquals(0, waitlistPairCount(PARTICIPANT, activeEvent));

        String cancelledEvent = "waitlist-cancelled-registration-event";
        String cancelledRegistration = "waitlist-cancelled-registration";
        defineEvent(cancelledEvent);
        publishEvent(cancelledEvent);
        assertEquals(
                201,
                postRegistration(
                                cancelledRegistration,
                                cancelledEvent,
                                PARTICIPANT,
                                PARTICIPANT_PASSWORD)
                        .statusCode());
        assertEquals(
                200,
                cancelRegistration(
                                cancelledRegistration,
                                PARTICIPANT,
                                PARTICIPANT_PASSWORD)
                        .statusCode());
        setAvailability(cancelledEvent, "closed");

        HttpResponse<String> cancelledConflict = putWaitlist(
                cancelledEvent,
                "corr-waitlist-cancelled-conflict",
                PARTICIPANT,
                PARTICIPANT_PASSWORD);
        assertEquals(409, cancelledConflict.statusCode());
        assertJsonString(
                cancelledConflict.body(),
                "code",
                "event_registration_exists");
        assertEquals(0, waitlistPairCount(PARTICIPANT, cancelledEvent));
    }

    @Test
    void waitlistEndpointsRequireAuthentication() throws Exception {
        HttpResponse<String> put = putWaitlist(
                "waitlist-auth-event",
                "corr-waitlist-auth-put",
                null,
                null);
        HttpResponse<String> get = getWaitlist(
                "waitlist-auth-event",
                "corr-waitlist-auth-get",
                null,
                null);

        assertEquals(401, put.statusCode());
        assertEquals(401, get.statusCode());
        assertJsonString(put.body(), "code", "authentication_required");
        assertJsonString(get.body(), "code", "authentication_required");
        assertCorrelation(put, "corr-waitlist-auth-put");
        assertCorrelation(get, "corr-waitlist-auth-get");
    }

    private static void startApplication() {
        PasswordEncoder encoder =
                PasswordEncoderFactories.createDelegatingPasswordEncoder();

        application = new SpringApplication(PlatformApplication.class).run(
                "--server.port=0",
                "--platform.database.url=" + POSTGRESQL.getJdbcUrl(),
                "--platform.database.username=" + POSTGRESQL.getUsername(),
                "--platform.database.password=" + POSTGRESQL.getPassword(),
                "--platform.security.participants[0].principal=" + ORGANIZER,
                "--platform.security.participants[0].password-verifier="
                        + encoder.encode(ORGANIZER_PASSWORD),
                "--platform.security.participants[1].principal=" + PARTICIPANT,
                "--platform.security.participants[1].password-verifier="
                        + encoder.encode(PARTICIPANT_PASSWORD),
                "--platform.security.participants[2].principal=" + OTHER,
                "--platform.security.participants[2].password-verifier="
                        + encoder.encode(OTHER_PASSWORD));

        Integer port = application.getEnvironment()
                .getRequiredProperty("local.server.port", Integer.class);
        baseUri = URI.create("http://127.0.0.1:" + port);
        dataSource = application.getBean(DataSource.class);
    }

    private static void defineEvent(String eventId) throws Exception {
        String body = "{"
                + "\"eventId\":\"" + eventId + "\","
                + "\"name\":\"Waitlist Event\","
                + "\"slug\":\"" + eventId + "\","
                + "\"startsAt\":\"2026-09-01T08:00:00Z\","
                + "\"endsAt\":\"2026-09-01T10:00:00Z\","
                + "\"timezone\":\"Europe/Copenhagen\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/v1/events"))
                .header("Content-Type", "application/json")
                .header(
                        "Authorization",
                        basicAuthorization(ORGANIZER, ORGANIZER_PASSWORD))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        assertEquals(
                201,
                HTTP.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    private static void publishEvent(String eventId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        baseUri.resolve(
                                "/api/v1/events/" + eventId + "/publication"))
                .header(
                        "Authorization",
                        basicAuthorization(ORGANIZER, ORGANIZER_PASSWORD))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        assertEquals(
                204,
                HTTP.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    private static void withdrawEvent(String eventId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        baseUri.resolve(
                                "/api/v1/events/" + eventId + "/publication"))
                .header(
                        "Authorization",
                        basicAuthorization(ORGANIZER, ORGANIZER_PASSWORD))
                .DELETE()
                .build();

        assertEquals(
                204,
                HTTP.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    private static void setAvailability(String eventId, String availability)
            throws Exception {
        String body = "{\"availability\":\"" + availability + "\"}";
        HttpRequest request = HttpRequest.newBuilder(
                        baseUri.resolve(
                                "/api/v1/events/"
                                        + eventId
                                        + "/registration-availability"))
                .header("Content-Type", "application/json")
                .header(
                        "Authorization",
                        basicAuthorization(ORGANIZER, ORGANIZER_PASSWORD))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        assertEquals(
                204,
                HTTP.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    private static HttpResponse<String> putWaitlist(
            String eventId,
            String correlationId,
            String principal,
            String password) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        baseUri.resolve(
                                "/api/v1/events/"
                                        + eventId
                                        + "/waitlist-participation"))
                .header(CORRELATION_HEADER, correlationId)
                .PUT(HttpRequest.BodyPublishers.noBody());

        withAuthorization(builder, principal, password);

        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> getWaitlist(
            String eventId,
            String correlationId,
            String principal,
            String password) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        baseUri.resolve(
                                "/api/v1/events/"
                                        + eventId
                                        + "/waitlist-participation"))
                .header(CORRELATION_HEADER, correlationId)
                .GET();

        withAuthorization(builder, principal, password);

        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> postRegistration(
            String registrationId,
            String eventId,
            String principal,
            String password) throws Exception {
        String body = "{"
                + "\"registrationId\":\"" + registrationId + "\","
                + "\"eventId\":\"" + eventId + "\""
                + "}";

        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        baseUri.resolve("/api/v1/event-registrations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        withAuthorization(builder, principal, password);

        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> cancelRegistration(
            String registrationId,
            String principal,
            String password) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        baseUri.resolve(
                                "/api/v1/event-registrations/" + registrationId))
                .DELETE();
        withAuthorization(builder, principal, password);

        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void withAuthorization(
            HttpRequest.Builder builder,
            String principal,
            String password) {
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

    private static void assertCorrelation(
            HttpResponse<String> response,
            String expected) {
        assertEquals(
                expected,
                response.headers()
                        .firstValue(CORRELATION_HEADER)
                        .orElseThrow());
    }

    private static void assertJsonString(
            String body,
            String property,
            String expected) {
        assertEquals(expected, jsonString(body, property));
    }

    private static String jsonString(String body, String property) {
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(property) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new AssertionError(
                    "Missing JSON string property "
                            + property
                            + " in "
                            + body);
        }
        return matcher.group(1);
    }

    private static int waitlistPairCount(
            String participantReference,
            String eventReference) throws Exception {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "select count(*) "
                                + "from waitlist.participations "
                                + "where participant_reference = ? "
                                + "and event_reference = ?")) {
            statement.setString(1, participantReference);
            statement.setString(2, eventReference);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static int registrationPairCount(
            String participantReference,
            String eventReference) throws Exception {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "select count(*) "
                                + "from registration.registrations "
                                + "where registrant_namespace = 'participant' "
                                + "and registrant_reference = ? "
                                + "and target_namespace = 'event' "
                                + "and target_reference = ?")) {
            statement.setString(1, participantReference);
            statement.setString(2, eventReference);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }
}
