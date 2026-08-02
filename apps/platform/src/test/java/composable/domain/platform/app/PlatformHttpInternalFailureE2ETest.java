package composable.domain.platform.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

class PlatformHttpInternalFailureE2ETest {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Test
    void unavailablePersistenceReturnsSanitizedContractInternalError() throws Exception {
        PostgreSQLContainer postgresql = new PostgreSQLContainer("postgres:18.4");
        postgresql.start();

        ConfigurableApplicationContext application = null;
        try {
            application = new SpringApplication(PlatformApplication.class).run(
                    "--server.port=0",
                    "--platform.database.url=" + postgresql.getJdbcUrl(),
                    "--platform.database.username=" + postgresql.getUsername(),
                    "--platform.database.password=" + postgresql.getPassword());

            Integer port = application.getEnvironment()
                    .getRequiredProperty("local.server.port", Integer.class);
            URI endpoint = URI.create(
                    "http://127.0.0.1:" + port + "/api/v1/events/internal-failure");

            postgresql.stop();

            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .header(CORRELATION_HEADER, "corr-internal-e2e")
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(500, response.statusCode());
            assertEquals(
                    "corr-internal-e2e",
                    response.headers().firstValue(CORRELATION_HEADER).orElseThrow());
            assertJsonString(response.body(), "code", "internal_error");
            assertJsonString(response.body(), "message", "Internal server error");

            String lowerBody = response.body().toLowerCase(Locale.ROOT);
            assertFalse(lowerBody.contains("postgres"));
            assertFalse(lowerBody.contains("jdbc"));
            assertFalse(lowerBody.contains("jooq"));
            assertFalse(lowerBody.contains("sql"));
            assertFalse(lowerBody.contains("exception"));
            assertFalse(lowerBody.contains("stack"));
        } finally {
            if (application != null) {
                application.close();
            }
            if (postgresql.isRunning()) {
                postgresql.stop();
            }
        }
    }

    private static void assertJsonString(String body, String field, String value) {
        Pattern expected = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"" + Pattern.quote(value) + "\"");
        assertTrue(
                expected.matcher(body).find(),
                () -> "Expected JSON field %s=%s in: %s".formatted(field, value, body));
    }
}
