package composable.domain.platform.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.event.domain.Event;
import composable.domain.platform.event.domain.PublicationState;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.DefineEventCommand;
import composable.domain.platform.event.api.EventAlreadyDefinedException;
import composable.domain.platform.event.api.EventAlreadyPublishedException;
import composable.domain.platform.event.api.EventOwnerReference;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.UpdateEventCommand;
import composable.domain.platform.event.persistence.JooqEventRepository;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
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
    static void startDatabase() throws SQLException {
        POSTGRESQL.start();

        PGSimpleDataSource postgresDataSource = new PGSimpleDataSource();
        postgresDataSource.setURL(POSTGRESQL.getJdbcUrl());
        postgresDataSource.setUser(POSTGRESQL.getUsername());
        postgresDataSource.setPassword(POSTGRESQL.getPassword());
        dataSource = postgresDataSource;

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/event")
                .target(MigrationVersion.fromVersion("1"))
                .load()
                .migrate();

        insertPrePublicationEvent();

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
    void migratesPrePublicationEventAsUnpublishedWithoutOwner() {
        EventView migrated =
                new FindEventService(new JooqEventRepository(dataSource))
                        .findById(CONTEXT, "pre-publication-event")
                        .orElseThrow();

        assertEquals(EventPublicationState.UNPUBLISHED, migrated.publicationState());
        assertEquals("Legacy Event", migrated.name());
        assertEquals("legacy-event", migrated.slug());
        assertEquals(Optional.empty(), migrated.owner());
    }

    @Test
    void persistsAndRetrievesEventWithDurableOwnerAcrossFreshApplicationServices() {
        Instant startsAt = Instant.parse("2026-09-01T08:00:00.123456789Z");
        Instant endsAt = Instant.parse("2026-09-01T10:00:00.987654321Z");
        ZoneId timezone = ZoneId.of("Europe/Copenhagen");

        DefineEventCommand command = new DefineEventCommand(
                "persistent-event-1",
                "Persistent Platform Day",
                "persistent-platform-day",
                startsAt,
                endsAt,
                timezone,
                new EventOwnerReference("organizer-alpha"));

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
        assertEquals(EventPublicationState.UNPUBLISHED, retrieved.publicationState());
        assertEquals(Optional.of(new EventOwnerReference("organizer-alpha")), retrieved.owner());
    }

    @Test
    void updatesUnpublishedPersistedEventAndRejectsUpdateAfterPublication() {
        Instant startsAt = Instant.parse("2026-09-01T08:00:00Z");
        Instant endsAt = Instant.parse("2026-09-01T10:00:00Z");
        ZoneId timezone = ZoneId.of("Europe/Copenhagen");

        DefineEventCommand command = new DefineEventCommand(
                "persistent-update-event",
                "Initial Event",
                "initial-event",
                startsAt,
                endsAt,
                timezone,
                new EventOwnerReference("organizer-beta"));

        new DefineEventService(new JooqEventRepository(dataSource)).define(CONTEXT, command);

        Instant updatedStart = Instant.parse("2026-10-01T09:00:00Z");
        Instant updatedEnd = Instant.parse("2026-10-01T11:00:00Z");
        ZoneId updatedTz = ZoneId.of("Europe/Stockholm");

        EventView updated = new UpdateEventService(new JooqEventRepository(dataSource)).update(
                CONTEXT,
                new UpdateEventCommand(
                        "persistent-update-event",
                        "Updated Persistent Event",
                        "updated-persistent-event",
                        updatedStart,
                        updatedEnd,
                        updatedTz));

        assertEquals("Updated Persistent Event", updated.name());
        assertEquals("updated-persistent-event", updated.slug());
        assertEquals(updatedStart, updated.startsAt());
        assertEquals(updatedEnd, updated.endsAt());
        assertEquals(updatedTz, updated.timezone());
        assertEquals(EventPublicationState.UNPUBLISHED, updated.publicationState());
        assertEquals(Optional.of(new EventOwnerReference("organizer-beta")), updated.owner());

        new PublishEventService(new JooqEventRepository(dataSource))
                .publish(CONTEXT, "persistent-update-event");

        assertThrows(
                EventAlreadyPublishedException.class,
                () -> new UpdateEventService(new JooqEventRepository(dataSource)).update(
                        CONTEXT,
                        new UpdateEventCommand(
                                "persistent-update-event",
                                "Mutated After Published",
                                "mutated-after-published",
                                updatedStart,
                                updatedEnd,
                                updatedTz)));
    }

    @Test
    void staleReadUpdateDefinitionFailsWhenEventIsPublishedConcurrently() {
        JooqEventRepository repository = new JooqEventRepository(dataSource);
        Instant startsAt = Instant.parse("2026-09-01T08:00:00Z");
        Instant endsAt = Instant.parse("2026-09-01T10:00:00Z");
        ZoneId timezone = ZoneId.of("Europe/Copenhagen");

        DefineEventCommand command = new DefineEventCommand(
                "persistent-stale-update",
                "Initial Event",
                "initial-event",
                startsAt,
                endsAt,
                timezone,
                new EventOwnerReference("organizer-stale"));

        new DefineEventService(repository).define(CONTEXT, command);

        Event staleUnpublished = repository.findById("persistent-stale-update").orElseThrow();
        assertEquals(composable.domain.platform.event.domain.PublicationState.UNPUBLISHED, staleUnpublished.publicationState());

        new PublishEventService(repository).publish(CONTEXT, "persistent-stale-update");

        Event publishedInDb = repository.findById("persistent-stale-update").orElseThrow();
        assertEquals(composable.domain.platform.event.domain.PublicationState.PUBLISHED, publishedInDb.publicationState());

        Event staleModified = staleUnpublished.updateDefinition(
                "Stale Overwrite Attempt",
                "stale-overwrite",
                startsAt.plusSeconds(3600),
                endsAt.plusSeconds(3600),
                ZoneId.of("Europe/Oslo"));

        boolean updated = repository.updateDefinition(staleModified);
        assertFalse(updated);

        Event currentInDb = repository.findById("persistent-stale-update").orElseThrow();
        assertEquals(composable.domain.platform.event.domain.PublicationState.PUBLISHED, currentInDb.publicationState());
        assertEquals("Initial Event", currentInDb.name());
        assertEquals("initial-event", currentInDb.slug());
        assertEquals(startsAt, currentInDb.startsAt());
        assertEquals(endsAt, currentInDb.endsAt());
        assertEquals(timezone, currentInDb.timezone());
    }

    @Test
    void persistsPublicationAndDiscoversPersistedPublishedEventsOnly() {
        DefineEventService definition =
                new DefineEventService(new JooqEventRepository(dataSource));

        EventView toPublish = definition.define(
                CONTEXT,
                command(
                        "persistent-publish",
                        "Published Persistent Event",
                        "published-persistent-event"));
        EventView toRemainUnpublished = definition.define(
                CONTEXT,
                command(
                        "persistent-unpublished",
                        "Unpublished Persistent Event",
                        "unpublished-persistent-event"));

        EventView published =
                new PublishEventService(new JooqEventRepository(dataSource))
                        .publish(CONTEXT, toPublish.eventId());

        EventView freshRead =
                new FindEventService(new JooqEventRepository(dataSource))
                        .findById(CONTEXT, toPublish.eventId())
                        .orElseThrow();

        assertEquals(toPublish.eventId(), freshRead.eventId());
        assertEquals(toPublish.name(), freshRead.name());
        assertEquals(toPublish.slug(), freshRead.slug());
        assertEquals(toPublish.startsAt(), freshRead.startsAt());
        assertEquals(toPublish.endsAt(), freshRead.endsAt());
        assertEquals(toPublish.timezone(), freshRead.timezone());
        assertEquals(EventPublicationState.PUBLISHED, freshRead.publicationState());
        assertEquals(published, freshRead);

        var discovered =
                new DiscoverEventsService(new JooqEventRepository(dataSource))
                        .discover(CONTEXT);

        assertTrue(discovered.stream()
                .anyMatch(event -> event.eventId().equals(toPublish.eventId())));
        assertTrue(discovered.stream()
                .noneMatch(event -> event.eventId().equals(toRemainUnpublished.eventId())));
    }

    @Test
    void knownIdRetrievalWorksForBothPublicationStates() {
        DefineEventService definition =
                new DefineEventService(new JooqEventRepository(dataSource));
        EventView unpublished = definition.define(
                CONTEXT,
                command(
                        "persistent-known-unpublished",
                        "Known Unpublished",
                        "known-unpublished"));
        EventView published = definition.define(
                CONTEXT,
                command(
                        "persistent-known-published",
                        "Known Published",
                        "known-published"));

        new PublishEventService(new JooqEventRepository(dataSource))
                .publish(CONTEXT, published.eventId());

        assertEquals(
                EventPublicationState.UNPUBLISHED,
                new FindEventService(new JooqEventRepository(dataSource))
                        .findById(CONTEXT, unpublished.eventId())
                        .orElseThrow()
                        .publicationState());
        assertEquals(
                EventPublicationState.PUBLISHED,
                new FindEventService(new JooqEventRepository(dataSource))
                        .findById(CONTEXT, published.eventId())
                        .orElseThrow()
                        .publicationState());
    }

    @Test
    void databaseRejectsUnsupportedPublicationState() {
        assertThrows(
                SQLException.class,
                () -> {
                    try (var connection = dataSource.getConnection();
                            PreparedStatement statement = connection.prepareStatement(
                                    "update event.events "
                                            + "set publication_state = ? where event_id = ?")) {
                        statement.setString(1, "unsupported");
                        statement.setString(2, "pre-publication-event");
                        statement.executeUpdate();
                    }
                });
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
                timezone,
                new EventOwnerReference("organizer-alpha")));

        EventAlreadyDefinedException error = assertThrows(
                EventAlreadyDefinedException.class,
                () -> new DefineEventService(new JooqEventRepository(dataSource))
                        .define(CONTEXT, new DefineEventCommand(
                                "persistent-duplicate-1",
                                "Replacement Persistent Event",
                                "replacement-persistent-event",
                                startsAt,
                                endsAt,
                                timezone,
                                new EventOwnerReference("organizer-alpha"))));

        EventView persisted =
                new FindEventService(new JooqEventRepository(dataSource))
                        .findById(CONTEXT, "persistent-duplicate-1")
                        .orElseThrow();

        assertEquals("persistent-duplicate-1", error.eventId());
        assertEquals("Original Persistent Event", persisted.name());
        assertEquals("original-persistent-event", persisted.slug());
        assertEquals(EventPublicationState.UNPUBLISHED, persisted.publicationState());
    }

    @Test
    void returnsEmptyForUnknownPersistentIdentity() {
        assertTrue(
                new FindEventService(new JooqEventRepository(dataSource))
                        .findById(CONTEXT, "persistent-missing")
                        .isEmpty());
    }

    private static void insertPrePublicationEvent() throws SQLException {
        try (var connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "insert into event.events "
                                + "(event_id, name, slug, starts_at_epoch_second, starts_at_nano, "
                                + "ends_at_epoch_second, ends_at_nano, timezone) "
                                + "values (?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, "pre-publication-event");
            statement.setString(2, "Legacy Event");
            statement.setString(3, "legacy-event");
            statement.setLong(4, Instant.parse("2026-08-01T08:00:00Z").getEpochSecond());
            statement.setInt(5, 0);
            statement.setLong(6, Instant.parse("2026-08-01T10:00:00Z").getEpochSecond());
            statement.setInt(7, 0);
            statement.setString(8, "Europe/Copenhagen");
            statement.executeUpdate();
        }
    }

    private static DefineEventCommand command(
            String eventId,
            String name,
            String slug) {
        return new DefineEventCommand(
                eventId,
                name,
                slug,
                Instant.parse("2026-11-01T08:00:00Z"),
                Instant.parse("2026-11-01T10:00:00Z"),
                ZoneId.of("Europe/Copenhagen"),
                new EventOwnerReference("organizer-default"));
    }
}
