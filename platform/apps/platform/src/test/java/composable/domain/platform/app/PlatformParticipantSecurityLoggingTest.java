package composable.domain.platform.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.postgresql.PostgreSQLContainer;

class PlatformParticipantSecurityLoggingTest {

    private static final String PRINCIPAL_A = "opaque-privacy-a91";
    private static final String PRINCIPAL_B = "opaque-privacy-b91";
    private static final String PASSWORD_A = "privacy-secret-a91";
    private static final String PASSWORD_B = "privacy-secret-b91";

    @Test
    void normalRuntimeLoggingExcludesParticipantSecurityAndOwnershipValues()
            throws Exception {
        PostgreSQLContainer postgresql =
                new PostgreSQLContainer("postgres:18.4");
        postgresql.start();

        Logger root = loggerContext().getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext(loggerContext());
        appender.start();
        root.addAppender(appender);

        ConfigurableApplicationContext application = null;
        try {
            PasswordEncoder encoder =
                    PasswordEncoderFactories.createDelegatingPasswordEncoder();
            String verifierA = encoder.encode(PASSWORD_A);
            String verifierB = encoder.encode(PASSWORD_B);
            String authorizationA = basicAuthorization(PRINCIPAL_A, PASSWORD_A);
            String invalidAuthorization =
                    basicAuthorization(PRINCIPAL_A, "wrong-privacy-secret");

            application = new SpringApplication(PlatformApplication.class).run(
                    "--server.port=0",
                    "--platform.database.url=" + postgresql.getJdbcUrl(),
                    "--platform.database.username=" + postgresql.getUsername(),
                    "--platform.database.password=" + postgresql.getPassword(),
                    "--platform.security.participants[0].principal=" + PRINCIPAL_A,
                    "--platform.security.participants[0].password-verifier=" + verifierA,
                    "--platform.security.participants[1].principal=" + PRINCIPAL_B,
                    "--platform.security.participants[1].password-verifier=" + verifierB);

            Integer port = application.getEnvironment()
                    .getRequiredProperty("local.server.port", Integer.class);
            URI baseUri = URI.create("http://127.0.0.1:" + port);
            HttpClient http = HttpClient.newHttpClient();

            String defineBody = "{"
                    + "\"eventId\":\"privacy-event-91\","
                    + "\"name\":\"Privacy Event\","
                    + "\"slug\":\"privacy-event-91\","
                    + "\"startsAt\":\"2026-09-01T08:00:00Z\","
                    + "\"endsAt\":\"2026-09-01T10:00:00Z\","
                    + "\"timezone\":\"Europe/Copenhagen\""
                    + "}";

            HttpRequest define = HttpRequest.newBuilder(baseUri.resolve("/api/v1/events"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", authorizationA)
                    .POST(HttpRequest.BodyPublishers.ofString(defineBody))
                    .build();
            assertEquals(
                    201,
                    http.send(define, HttpResponse.BodyHandlers.ofString()).statusCode());

            String invalidBody = "{"
                    + "\"registrationId\":\"privacy-registration-invalid-91\","
                    + "\"eventId\":\"privacy-event-91\""
                    + "}";

            HttpRequest invalid = HttpRequest.newBuilder(
                            baseUri.resolve("/api/v1/event-registrations"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", invalidAuthorization)
                    .POST(HttpRequest.BodyPublishers.ofString(invalidBody))
                    .build();
            assertEquals(
                    401,
                    http.send(invalid, HttpResponse.BodyHandlers.ofString()).statusCode());

            String createBody = "{"
                    + "\"registrationId\":\"privacy-registration-91\","
                    + "\"eventId\":\"privacy-event-91\""
                    + "}";

            HttpRequest create = HttpRequest.newBuilder(
                            baseUri.resolve("/api/v1/event-registrations"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", authorizationA)
                    .POST(HttpRequest.BodyPublishers.ofString(createBody))
                    .build();
            assertEquals(
                    201,
                    http.send(create, HttpResponse.BodyHandlers.ofString()).statusCode());

            String logs = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (left, right) -> left + "\n" + right);

            for (String forbidden : new String[] {
                PRINCIPAL_A,
                PRINCIPAL_B,
                PASSWORD_A,
                PASSWORD_B,
                verifierA,
                verifierB,
                authorizationA,
                invalidAuthorization,
                "participant:" + PRINCIPAL_A
            }) {
                assertFalse(
                        logs.contains(forbidden),
                        () -> "normal runtime logs contained forbidden participant value");
            }
        } finally {
            if (application != null) {
                application.close();
            }
            root.detachAppender(appender);
            appender.stop();
            postgresql.stop();
        }
    }

    private static LoggerContext loggerContext() {
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    private static String basicAuthorization(String principal, String password) {
        String credentials = principal + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
    }
}
