package composable.domain.platform.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

class PlatformHttpE2ETest {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:18.4");

    private static ConfigurableApplicationContext application;
    private static URI baseUri;

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
    }

    @AfterAll
    static void stopRuntime() {
        if (application != null) {
            application.close();
        }
        POSTGRESQL.stop();
    }

    @Test
    void definesAndRetrievesDurableEventWithExactFieldsAndCorrelation() throws Exception {
        String eventId = "http-round-trip-1";
        String body = eventJson(
                eventId,
                "HTTP Platform Day",
                "http-platform-day",
                "2026-09-01T08:00:00.123456789Z",
                "2026-09-01T10:00:00.987654321Z",
                "Europe/Copenhagen");

        HttpResponse<String> defined = post(body, "corr-round-trip");

        assertEquals(201, defined.statusCode());
        assertCorrelation(defined, "corr-round-trip");
        assertEventBody(
                defined.body(),
                eventId,
                "HTTP Platform Day",
                "http-platform-day",
                "2026-09-01T08:00:00.123456789Z",
                "2026-09-01T10:00:00.987654321Z",
                "Europe/Copenhagen");

        HttpResponse<String> retrieved = get(eventId, null);

        assertEquals(200, retrieved.statusCode());
        assertGeneratedCorrelation(retrieved);
        assertEventBody(
                retrieved.body(),
                eventId,
                "HTTP Platform Day",
                "http-platform-day",
                "2026-09-01T08:00:00.123456789Z",
                "2026-09-01T10:00:00.987654321Z",
                "Europe/Copenhagen");
    }

    @Test
    void duplicateIdentityReturnsConflictWithoutChangingPersistedEvent() throws Exception {
        String eventId = "http-duplicate-1";
        String original = eventJson(
                eventId,
                "Original HTTP Event",
                "original-http-event",
                "2026-10-01T08:00:00Z",
                "2026-10-01T09:00:00Z",
                "Europe/Copenhagen");
        String replacement = eventJson(
                eventId,
                "Replacement HTTP Event",
                "replacement-http-event",
                "2026-10-01T08:00:00Z",
                "2026-10-01T09:00:00Z",
                "Europe/Copenhagen");

        assertEquals(201, post(original, "corr-original").statusCode());

        HttpResponse<String> duplicate = post(replacement, "corr-duplicate");

        assertEquals(409, duplicate.statusCode());
        assertCorrelation(duplicate, "corr-duplicate");
        assertJsonString(duplicate.body(), "code", "event_already_defined");

        HttpResponse<String> persisted = get(eventId, "corr-after-duplicate");

        assertEquals(200, persisted.statusCode());
        assertCorrelation(persisted, "corr-after-duplicate");
        assertJsonString(persisted.body(), "name", "Original HTTP Event");
        assertJsonString(persisted.body(), "slug", "original-http-event");
        assertFalse(persisted.body().contains("Replacement HTTP Event"));
        assertFalse(persisted.body().contains("replacement-http-event"));
    }

    @Test
    void unknownIdentityReturnsContractNotFound() throws Exception {
        HttpResponse<String> response = get("http-missing-1", "corr-missing");

        assertEquals(404, response.statusCode());
        assertCorrelation(response, "corr-missing");
        assertJsonString(response.body(), "code", "event_not_found");
        assertJsonString(response.body(), "message", "Event was not found");
    }

    @Test
    void structurallyInvalidRequestReturnsContractBadRequest() throws Exception {
        String missingEventId = """
                {
                  "name": "Invalid HTTP Event",
                  "slug": "invalid-http-event",
                  "startsAt": "2026-12-01T08:00:00Z",
                  "endsAt": "2026-12-01T09:00:00Z",
                  "timezone": "Europe/Copenhagen"
                }
                """;

        HttpResponse<String> response = post(missingEventId, "corr-invalid");

        assertEquals(400, response.statusCode());
        assertCorrelation(response, "corr-invalid");
        assertJsonString(response.body(), "code", "invalid_request");
        assertJsonString(response.body(), "message", "Request is invalid");
    }

    private static HttpResponse<String> post(String body, String correlationId) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri.resolve("/api/v1/events"))
                .header("Content-Type", "application/json");

        if (correlationId != null) {
            builder.header(CORRELATION_HEADER, correlationId);
        }

        return HTTP.send(
                builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> get(String eventId, String correlationId) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                baseUri.resolve("/api/v1/events/" + eventId));

        if (correlationId != null) {
            builder.header(CORRELATION_HEADER, correlationId);
        }

        return HTTP.send(
                builder.GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String eventJson(
            String eventId,
            String name,
            String slug,
            String startsAt,
            String endsAt,
            String timezone) {
        return """
                {
                  "eventId": "%s",
                  "name": "%s",
                  "slug": "%s",
                  "startsAt": "%s",
                  "endsAt": "%s",
                  "timezone": "%s"
                }
                """.formatted(eventId, name, slug, startsAt, endsAt, timezone);
    }

    private static void assertEventBody(
            String body,
            String eventId,
            String name,
            String slug,
            String startsAt,
            String endsAt,
            String timezone) {
        assertJsonString(body, "eventId", eventId);
        assertJsonString(body, "name", name);
        assertJsonString(body, "slug", slug);
        assertJsonString(body, "startsAt", startsAt);
        assertJsonString(body, "endsAt", endsAt);
        assertJsonString(body, "timezone", timezone);
    }

    private static void assertJsonString(String body, String field, String value) {
        Pattern expected = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"" + Pattern.quote(value) + "\"");
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
