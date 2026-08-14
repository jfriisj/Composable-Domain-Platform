package composable.domain.platform.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.postgresql.PostgreSQLContainer;

class PlatformReadinessHttpE2ETest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Test
    void readinessTracksPostgresAvailabilityWithoutDiagnosticPayload() throws Exception {
        PostgreSQLContainer postgresql = new PostgreSQLContainer("postgres:18.4");
        ConfigurableApplicationContext application = null;

        try {
            postgresql.start();

            PasswordEncoder participantPasswordEncoder =
        PasswordEncoderFactories.createDelegatingPasswordEncoder();

application = new SpringApplication(PlatformApplication.class).run(
                    "--server.port=0",
                    "--platform.database.url=" + postgresql.getJdbcUrl(),
                    "--platform.database.username=" + postgresql.getUsername(),
                    "--platform.database.password=" + postgresql.getPassword(),
                    "--platform.security.participants[0].principal=opaque-platform-readiness-e2e",
                    "--platform.security.participants[0].password-verifier="
                            + participantPasswordEncoder.encode("unused-test-secret"));

            Integer port = application.getEnvironment()
                    .getRequiredProperty("local.server.port", Integer.class);
            URI readinessUri =
                    URI.create("http://127.0.0.1:" + port + "/internal/readiness");

            HttpResponse<String> ready = get(readinessUri);

            assertEquals(204, ready.statusCode());
            assertEquals("", ready.body());

            postgresql.stop();

            HttpResponse<String> unavailable = get(readinessUri);

            assertEquals(503, unavailable.statusCode());
            assertEquals("", unavailable.body());
        } finally {
            if (application != null) {
                application.close();
            }
            if (postgresql.isRunning()) {
                postgresql.stop();
            }
        }
    }

    private static HttpResponse<String> get(URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
