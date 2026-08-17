package composable.domain.platform.app.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

class EventApplicationHttpE2ETest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:18.4");

    private static ConfigurableApplicationContext application;
    private static URI baseUri;

    @BeforeAll
    static void startRuntime() {
        POSTGRESQL.start();

        application = new SpringApplication(EventApplication.class).run(
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
    void servesEventFlowWithOnlyEventOwnedPersistence() throws Exception {
        String eventId = "event-only-composition-1";
        String requestBody = """
                {
                  "eventId": "%s",
                  "name": "Event-only Composition",
                  "slug": "event-only-composition",
                  "startsAt": "2026-09-01T08:00:00Z",
                  "endsAt": "2026-09-01T09:00:00Z",
                  "timezone": "Europe/Copenhagen"
                }
                """.formatted(eventId);

        HttpResponse<String> defined = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(201, defined.statusCode());

        HttpResponse<String> retrieved = HTTP.send(
                HttpRequest.newBuilder(baseUri.resolve("/api/v1/events/" + eventId))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, retrieved.statusCode());
        assertTrue(retrieved.body().contains("\"eventId\":\"" + eventId + "\""));
        assertSchemaState();
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
