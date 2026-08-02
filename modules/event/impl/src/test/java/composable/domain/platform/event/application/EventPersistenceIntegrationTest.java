package composable.domain.platform.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.DefineEventCommand;
import composable.domain.platform.event.api.EventAlreadyDefinedException;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.persistence.JooqEventRepository;
import java.time.Instant;
import java.time.ZoneId;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

class EventPersistenceIntegrationTest {

    private static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:18.4");
    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("persistence-test-correlation"));

    private static DataSource dataSource;

    @BeforeAll
    static void startDatabase() {
        POSTGRESQL.start();

        PGSimpleDataSource postgresDataSource = new PGSimpleDataSource();
        postgresDataSource.setURL(POSTGRESQL.getJdbcUrl());
        postgresDataSource.setUser(POSTGRESQL.getUsername());
        postgresDataSource.setPassword(POSTGRESQL.getPassword());
        dataSource = postgresDataSource;

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/event")
                .load()
                .migrate();
    }

    @AfterAll
    static void stopDatabase() {
        POSTGRESQL.stop();
    }

    @Test
    void persistsAndRetrievesEventAcrossFreshApplicationServices() {
        Instant startsAt = Instant.parse("2026-09-01T08:00:00.123456789Z");
        Instant endsAt = Instant.parse("2026-09-01T10:00:00.987654321Z");
        ZoneId timezone = ZoneId.of("Europe/Copenhagen");

        DefineEventCommand command = new DefineEventCommand(
                "persistent-event-1",
                "Persistent Platform Day",
                "persistent-platform-day",
                startsAt,
                endsAt,
                timezone);

        EventView defined =
                new DefineEventService(new JooqEventRepository(dataSource))
                        .define(CONTEXT, command);

        EventView retrieved =
                new FindEventService(new JooqEventRepository(dataSource))
                        .findById(CONTEXT, "persistent-event-1")
                        .orElseThrow();

        assertEquals(defined, retrieved);
        assertEquals(command.eventId(), retrieved.eventId());
        assertEquals(command.name(), retrieved.name());
        assertEquals(command.slug(), retrieved.slug());
        assertEquals(command.startsAt(), retrieved.startsAt());
        assertEquals(command.endsAt(), retrieved.endsAt());
        assertEquals(command.timezone(), retrieved.timezone());
    }

    @Test
    void rejectsDuplicateIdentityWithoutChangingPersistedEvent() {
        Instant startsAt = Instant.parse("2026-10-01T08:00:00Z");
        Instant endsAt = Instant.parse("2026-10-01T09:00:00Z");
        ZoneId timezone = ZoneId.of("Europe/Copenhagen");
        DefineEventService service =
                new DefineEventService(new JooqEventRepository(dataSource));

        service.define(CONTEXT, new DefineEventCommand(
                "persistent-duplicate-1",
                "Original Persistent Event",
                "original-persistent-event",
                startsAt,
                endsAt,
                timezone));

        EventAlreadyDefinedException error = assertThrows(
                EventAlreadyDefinedException.class,
                () -> new DefineEventService(new JooqEventRepository(dataSource))
                        .define(CONTEXT, new DefineEventCommand(
                                "persistent-duplicate-1",
                                "Replacement Persistent Event",
                                "replacement-persistent-event",
                                startsAt,
                                endsAt,
                                timezone)));

        EventView persisted =
                new FindEventService(new JooqEventRepository(dataSource))
                        .findById(CONTEXT, "persistent-duplicate-1")
                        .orElseThrow();

        assertEquals("persistent-duplicate-1", error.eventId());
        assertEquals("Original Persistent Event", persisted.name());
        assertEquals("original-persistent-event", persisted.slug());
    }

    @Test
    void returnsEmptyForUnknownPersistentIdentity() {
        assertTrue(
                new FindEventService(new JooqEventRepository(dataSource))
                        .findById(CONTEXT, "persistent-missing")
                        .isEmpty());
    }
}
