package composable.domain.platform.app.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.postgresql.PostgreSQLContainer;

class EventApplicationHttpE2ETest {

    private static final String PRINCIPAL_OWNER = "opaque-organizer-a";
    private static final String PRINCIPAL_OTHER = "opaque-organizer-b";
    private static final String PASSWORD = "test-secret";

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:18.4");

    private static ConfigurableApplicationContext application;
    private static URI baseUri;

    @BeforeAll
    static void startRuntime() {
        POSTGRESQL.start();
        application = startApplication();
    }

    @AfterAll
    static void stopRuntime() {
        if (application != null) {
            application.close();
        }
        POSTGRESQL.stop();
    }

    private static ConfigurableApplicationContext startApplication() {
        PasswordEncoder encoder =
                PasswordEncoderFactories.createDelegatingPasswordEncoder();

        ConfigurableApplicationContext context = new SpringApplication(EventApplication.class).run(
                "--server.port=0",
                "--platform.database.url=" + POSTGRESQL.getJdbcUrl(),
                "--platform.database.username=" + POSTGRESQL.getUsername(),
                "--platform.database.password=" + POSTGRESQL.getPassword(),
                "--platform.security.participants[0].principal=" + PRINCIPAL_OWNER,
                "--platform.security.participants[0].password-verifier="
                        + encoder.encode(PASSWORD),
                "--platform.security.participants[1].principal=" + PRINCIPAL_OTHER,
                "--platform.security.participants[1].password-verifier="
                        + encoder.encode(PASSWORD));

        Integer port = context.getEnvironment()
                .getRequiredProperty("local.server.port", Integer.class);
        baseUri = URI.create("http://127.0.0.1:" + port);
        return context;
    }

    @Test
    void unauthenticatedAndInvalidCredentialOrganizerOperationsReturn401() throws Exception {
        String eventId = "event-auth-test-1";
        String defineBody = "{\n"
                + "  \"eventId\": \"" + eventId + "\",\n"
                + "  \"name\": \"Auth Test Event\",\n"
                + "  \"slug\": \"auth-test-event\",\n"
                + "  \"startsAt\": \"2026-09-01T08:00:00Z\",\n"
                + "  \"endsAt\": \"2026-09-01T09:00:00Z\",\n"
                + "  \"timezone\": \"Europe/Copenhagen\"\n"
                + "}";
        String updateBody = "{\n"
                + "  \"name\": \"Updated Name\",\n"
                + "  \"slug\": \"updated-slug\",\n"
                + "  \"startsAt\": \"2026-09-01T10:00:00Z\",\n"
                + "  \"endsAt\": \"2026-09-01T11:00:00Z\",\n"
                + "  \"timezone\": \"Europe/Copenhagen\"\n"
                + "}";

        // Unauthenticated define -> 401
        HttpResponse<String> unauthDefine = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(defineBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(401, unauthDefine.statusCode());

        // Unauthenticated update -> 401
        HttpResponse<String> unauthUpdate = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events/" + eventId))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(updateBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(401, unauthUpdate.statusCode());

        // Unauthenticated publish -> 401
        HttpResponse<String> unauthPublish = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events/" + eventId + "/publication"))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(401, unauthPublish.statusCode());

        // Invalid credentials define -> 401
        HttpResponse<String> invalidCredsDefine = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", basicAuth(PRINCIPAL_OWNER, "wrong-secret"))
                        .POST(HttpRequest.BodyPublishers.ofString(defineBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(401, invalidCredsDefine.statusCode());
    }

    @Test
    void servesOrganizerLifecycleAcrossApplicationRestartWithOnlyEventOwnedPersistence() throws Exception {
        String eventId = "event-restart-lifecycle-1";

        // 1. actor A authenticates and defines Event E -> 201
        String defineBody = "{\n"
                + "  \"eventId\": \"" + eventId + "\",\n"
                + "  \"name\": \"Event-only Composition\",\n"
                + "  \"slug\": \"event-only-composition\",\n"
                + "  \"startsAt\": \"2026-09-01T08:00:00Z\",\n"
                + "  \"endsAt\": \"2026-09-01T09:00:00Z\",\n"
                + "  \"timezone\": \"Europe/Copenhagen\"\n"
                + "}";

        HttpResponse<String> defined = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", basicAuth(PRINCIPAL_OWNER, PASSWORD))
                        .POST(HttpRequest.BodyPublishers.ofString(defineBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(201, defined.statusCode());

        // 2. anonymous known-id retrieval succeeds -> 200 (no owner field in public response)
        HttpResponse<String> retrieved = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events/" + eventId))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, retrieved.statusCode());
        assertTrue(retrieved.body().contains("\"eventId\":\"" + eventId + "\""));
        assertTrue(retrieved.body().contains("\"name\":\"Event-only Composition\""));
        assertFalse(retrieved.body().contains("owner"));
        assertFalse(retrieved.body().contains("organizer"));

        // 3. actor A updates E while unpublished -> 200
        String updateBody = "{\n"
                + "  \"name\": \"Updated Composition\",\n"
                + "  \"slug\": \"updated-composition\",\n"
                + "  \"startsAt\": \"2026-09-01T10:00:00Z\",\n"
                + "  \"endsAt\": \"2026-09-01T11:00:00Z\",\n"
                + "  \"timezone\": \"Europe/Copenhagen\"\n"
                + "}";

        HttpResponse<String> updated = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events/" + eventId))
                        .header("Content-Type", "application/json")
                        .header("Authorization", basicAuth(PRINCIPAL_OWNER, PASSWORD))
                        .PUT(HttpRequest.BodyPublishers.ofString(updateBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, updated.statusCode());
        assertTrue(updated.body().contains("\"name\":\"Updated Composition\""));

        // 4. actor B is denied update (403) and publication (403)
        HttpResponse<String> nonOwnerUpdate = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events/" + eventId))
                        .header("Content-Type", "application/json")
                        .header("Authorization", basicAuth(PRINCIPAL_OTHER, PASSWORD))
                        .PUT(HttpRequest.BodyPublishers.ofString(updateBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(403, nonOwnerUpdate.statusCode());

        HttpResponse<String> nonOwnerPublish = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events/" + eventId + "/publication"))
                        .header("Authorization", basicAuth(PRINCIPAL_OTHER, PASSWORD))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(403, nonOwnerPublish.statusCode());

        // 5. actor A publishes E -> 204
        HttpResponse<String> published = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events/" + eventId + "/publication"))
                        .header("Authorization", basicAuth(PRINCIPAL_OWNER, PASSWORD))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(204, published.statusCode());

        // 6. anonymous discovery contains E -> 200
        HttpResponse<String> discovered = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, discovered.statusCode());
        assertTrue(discovered.body().contains("\"eventId\":\"" + eventId + "\""));
        assertTrue(discovered.body().contains("\"name\":\"Updated Composition\""));

        // 7. actor A is rejected from updating the published Event -> 409
        HttpResponse<String> updatePublished = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events/" + eventId))
                        .header("Content-Type", "application/json")
                        .header("Authorization", basicAuth(PRINCIPAL_OWNER, PASSWORD))
                        .PUT(HttpRequest.BodyPublishers.ofString(updateBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(409, updatePublished.statusCode());

        // 8. close the Spring application
        application.close();

        // 9. start EventApplication again against the same PostgreSQL container
        application = startApplication();

        // 10. anonymously retrieve E and prove the updated definition survived -> 200
        HttpResponse<String> retrievedAfterRestart = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events/" + eventId))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, retrievedAfterRestart.statusCode());
        assertTrue(retrievedAfterRestart.body().contains("\"name\":\"Updated Composition\""));
        assertFalse(retrievedAfterRestart.body().contains("owner"));

        // 11. anonymously discover E and prove publication survived -> 200
        HttpResponse<String> discoveredAfterRestart = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, discoveredAfterRestart.statusCode());
        assertTrue(discoveredAfterRestart.body().contains("\"eventId\":\"" + eventId + "\""));

        // 12. actor B attempts organizer management and receives 403, proving the persisted owner is still actor A
        HttpResponse<String> nonOwnerUpdateAfterRestart = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events/" + eventId))
                        .header("Content-Type", "application/json")
                        .header("Authorization", basicAuth(PRINCIPAL_OTHER, PASSWORD))
                        .PUT(HttpRequest.BodyPublishers.ofString(updateBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(403, nonOwnerUpdateAfterRestart.statusCode());

        HttpResponse<String> nonOwnerPublishAfterRestart = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events/" + eventId + "/publication"))
                        .header("Authorization", basicAuth(PRINCIPAL_OTHER, PASSWORD))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(403, nonOwnerPublishAfterRestart.statusCode());

        // 13. actor A attempts update and receives 409 because the persisted Event is published
        HttpResponse<String> ownerUpdateAfterRestart = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events/" + eventId))
                        .header("Content-Type", "application/json")
                        .header("Authorization", basicAuth(PRINCIPAL_OWNER, PASSWORD))
                        .PUT(HttpRequest.BodyPublishers.ofString(updateBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(409, ownerUpdateAfterRestart.statusCode());

        // 14. repeated publication with owner credentials -> 409
        HttpResponse<String> republishAfterRestart = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events/" + eventId + "/publication"))
                        .header("Authorization", basicAuth(PRINCIPAL_OWNER, PASSWORD))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(409, republishAfterRestart.statusCode());

        assertSchemaState();
    }

    private static String basicAuth(String principal, String password) {
        String token = principal + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertSchemaState() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                        POSTGRESQL.getJdbcUrl(),
                        POSTGRESQL.getUsername(),
                        POSTGRESQL.getPassword());
                PreparedStatement statement = connection.prepareStatement(
                        """
                        select
                            exists(select 1 from pg_namespace where nspname = ?),
                            exists(select 1 from pg_namespace where nspname = ?)
                        """)) {
            statement.setString(1, "event");
            statement.setString(2, "registration");

            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertTrue(result.getBoolean(1));
                assertFalse(result.getBoolean(2));
            }
        }
    }
}
