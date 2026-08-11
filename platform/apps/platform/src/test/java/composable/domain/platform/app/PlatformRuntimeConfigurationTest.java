package composable.domain.platform.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.DefineEvent;
import composable.domain.platform.event.api.DefineEventCommand;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.http.EventHttpAdapter;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

class PlatformRuntimeConfigurationTest {

    private static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:18.4");

    @BeforeAll
    static void startDatabase() {
        POSTGRESQL.start();
    }

    @AfterAll
    static void stopDatabase() {
        POSTGRESQL.stop();
    }

    @Test
    void startsCompositionWithMigratedEventPersistence() {
        SpringApplication application = new SpringApplication(PlatformApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);

        try (ConfigurableApplicationContext context = application.run(
                "--platform.database.url=" + POSTGRESQL.getJdbcUrl(),
                "--platform.database.username=" + POSTGRESQL.getUsername(),
                "--platform.database.password=" + POSTGRESQL.getPassword())) {
            assertNotNull(context.getBean(EventHttpAdapter.class));

            DefineEvent defineEvent = context.getBean(DefineEvent.class);
            FindEvent findEvent = context.getBean(FindEvent.class);
            ExecutionContext executionContext =
                    new ExecutionContext(new CorrelationId("runtime-composition-test"));

            DefineEventCommand command = new DefineEventCommand(
                    "runtime-event-1",
                    "Runtime Platform Day",
                    "runtime-platform-day",
                    Instant.parse("2026-11-01T08:00:00.123456789Z"),
                    Instant.parse("2026-11-01T10:00:00.987654321Z"),
                    ZoneId.of("Europe/Copenhagen"));

            EventView defined = defineEvent.define(executionContext, command);
            EventView retrieved = findEvent
                    .findById(executionContext, command.eventId())
                    .orElseThrow();

            assertEquals(defined, retrieved);
            assertEquals(command.eventId(), retrieved.eventId());
            assertEquals(command.name(), retrieved.name());
            assertEquals(command.slug(), retrieved.slug());
            assertEquals(command.startsAt(), retrieved.startsAt());
            assertEquals(command.endsAt(), retrieved.endsAt());
            assertEquals(command.timezone(), retrieved.timezone());
        }
    }
}
