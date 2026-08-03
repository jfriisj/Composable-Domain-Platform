package composable.domain.platform.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

class PlatformEventRegistrationHttpE2ETest {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:18.4");

    private static ConfigurableApplicationContext application;
    private static URI baseUri;
    private static DataSource dataSource;

    @BeforeAll
    static void startRuntime() {
        POSTGRESQL.start();

        application = new SpringApplication(PlatformApplication.class).run(
                "--server.port=0",
                "--platform.database.url=" + POSTGRESQL.getJdbcUrl(),
                "--platform.database.username=" + POSTGRESQL.getUsername(),
                "--platform.database.password=" + POSTGRESQL.getPassword());

        Integer port = application.getEnvironment()
                .getRequiredProperty("local.server.port", Integer.class);
        baseUri = URI.create("http://127.0.0.1:" + port);
        dataSource = application.getBean(DataSource.class);
    }

    @AfterAll
    static void stopRuntime() {
        if (application != null) {
            application.close();
        }
        POSTGRESQL.stop();
    }

    @Test
    void registersExistingEventAndRetrievesDurableEventRegistrationWithCorrelation()
            throws Exception {
        defineEvent("registration-event-1");

        HttpResponse<String> created = postRegistration(
                registrationJson(
                        "registration-http-1",
                        "registration-event-1",
                        "participant-alpha"),
                "corr-registration-create");

        assertEquals(201, created.statusCode());
        assertCorrelation(created, "corr-registration-create");
        assertRegistrationBody(
                created.body(),
                "registration-http-1",
                "registration-event-1",
                "participant-alpha");
        assertPersistedRegistration(
                "registration-http-1",
                "participant",
                "participant-alpha",
                "event",
                "registration-event-1");

        HttpResponse<String> retrieved =
                getRegistration("registration-http-1", null);

        assertEquals(200, retrieved.statusCode());
        assertGeneratedCorrelation(retrieved);
        assertRegistrationBody(
                retrieved.body(),
                "registration-http-1",
                "registration-event-1",
                "participant-alpha");
    }

    @Test
    void unknownEventReturnsNotFoundAndCreatesNoRegistration() throws Exception {
        HttpResponse<String> create = postRegistration(
                registrationJson(
                        "registration-http-missing-event",
                        "event-does-not-exist",
                        "participant-beta"),
                "corr-unknown-event");

        assertEquals(404, create.statusCode());
        assertCorrelation(create, "corr-unknown-event");
        assertJsonString(create.body(), "code", "event_not_found");
        assertEquals(0, registrationCount("registration-http-missing-event"));

        HttpResponse<String> retrieve =
                getRegistration("registration-http-missing-event", "corr-after-unknown");

        assertEquals(404, retrieve.statusCode());
        assertCorrelation(retrieve, "corr-after-unknown");
        assertJsonString(retrieve.body(), "code", "event_registration_not_found");
    }

    @Test
    void duplicateParticipantEventPairReturnsConflictWithoutReplacingDurableState()
            throws Exception {
        defineEvent("registration-event-duplicate");

        assertEquals(
                201,
                postRegistration(
                        registrationJson(
                                "registration-http-original",
                                "registration-event-duplicate",
                                "participant-duplicate"),
                        "corr-original")
                        .statusCode());

        HttpResponse<String> duplicate = postRegistration(
                registrationJson(
                        "registration-http-conflicting",
                        "registration-event-duplicate",
                        "participant-duplicate"),
                "corr-conflict");

        assertEquals(409, duplicate.statusCode());
        assertCorrelation(duplicate, "corr-conflict");
        assertJsonString(duplicate.body(), "code", "registration_conflict");

        assertPersistedRegistration(
                "registration-http-original",
                "participant",
                "participant-duplicate",
                "event",
                "registration-event-duplicate");
        assertEquals(0, registrationCount("registration-http-conflicting"));
    }

    @Test
    void structurallyInvalidRequestReturnsBadRequest() throws Exception {
        String body = "{"
                + "\"registrationId\":\"registration-http-invalid\","
                + "\"eventId\":\"registration-event-invalid\""
                + "}";

        HttpResponse<String> response =
                postRegistration(body, "corr-invalid-registration");

        assertEquals(400, response.statusCode());
        assertCorrelation(response, "corr-invalid-registration");
        assertJsonString(response.body(), "code", "invalid_request");
        assertEquals(0, registrationCount("registration-http-invalid"));
    }

    @Test
    void unknownRegistrationReturnsNotFound() throws Exception {
        HttpResponse<String> response =
                getRegistration("registration-http-unknown", "corr-unknown-registration");

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
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        assertEquals(
                201,
                HTTP.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    private static HttpResponse<String> postRegistration(
            String body,
            String correlationId) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        baseUri.resolve("/api/v1/event-registrations"))
                .header("Content-Type", "application/json");

        if (correlationId != null) {
            builder.header(CORRELATION_HEADER, correlationId);
        }

        return HTTP.send(
                builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> getRegistration(
            String registrationId,
            String correlationId) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                baseUri.resolve("/api/v1/event-registrations/" + registrationId));

        if (correlationId != null) {
            builder.header(CORRELATION_HEADER, correlationId);
        }

        return HTTP.send(
                builder.GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String registrationJson(
            String registrationId,
            String eventId,
            String participantReference) {
        return "{"
                + "\"registrationId\":\"" + registrationId + "\","
                + "\"eventId\":\"" + eventId + "\","
                + "\"participantReference\":\"" + participantReference + "\""
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
            String participantReference) {
        assertJsonString(body, "registrationId", registrationId);
        assertJsonString(body, "eventId", eventId);
        assertJsonString(body, "participantReference", participantReference);
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
