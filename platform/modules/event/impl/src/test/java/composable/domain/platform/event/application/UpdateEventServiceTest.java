package composable.domain.platform.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventAlreadyPublishedException;
import composable.domain.platform.event.api.EventNotFoundException;
import composable.domain.platform.event.api.EventOwnerReference;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.InvalidEventDefinitionException;
import composable.domain.platform.event.api.UpdateEventCommand;
import composable.domain.platform.event.domain.Event;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateEventServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("update-test-correlation"));

    @Test
    void updatesUnpublishedEventAndPreservesOwnerAndPublicationState() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        Event event = new Event(
                "event-1",
                "Original Event",
                "original-event",
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T10:00:00Z"),
                ZoneId.of("Europe/Copenhagen"),
                "organizer-1");
        repository.addIfAbsent(event);

        Instant newStart = Instant.parse("2026-10-01T09:00:00Z");
        Instant newEnd = Instant.parse("2026-10-01T11:00:00Z");
        ZoneId newTz = ZoneId.of("Europe/Oslo");

        UpdateEventService service = new UpdateEventService(repository);
        EventView updated = service.update(
                CONTEXT,
                new UpdateEventCommand(
                        "event-1",
                        "Updated Event",
                        "updated-event",
                        newStart,
                        newEnd,
                        newTz));

        assertEquals("event-1", updated.eventId());
        assertEquals("Updated Event", updated.name());
        assertEquals("updated-event", updated.slug());
        assertEquals(newStart, updated.startsAt());
        assertEquals(newEnd, updated.endsAt());
        assertEquals(newTz, updated.timezone());
        assertEquals(EventPublicationState.UNPUBLISHED, updated.publicationState());
        assertEquals(Optional.of(new EventOwnerReference("organizer-1")), updated.owner());

        EventView persisted = new FindEventService(repository)
                .findById(CONTEXT, "event-1")
                .orElseThrow();
        assertEquals(updated, persisted);
    }

    @Test
    void updateFailsIfEventNotFound() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        UpdateEventService service = new UpdateEventService(repository);

        EventNotFoundException error = assertThrows(
                EventNotFoundException.class,
                () -> service.update(
                        CONTEXT,
                        new UpdateEventCommand(
                                "missing-event",
                                "Updated Event",
                                "updated-event",
                                Instant.parse("2026-10-01T09:00:00Z"),
                                Instant.parse("2026-10-01T11:00:00Z"),
                                ZoneId.of("Europe/Oslo"))));

        assertEquals("missing-event", error.eventId());
    }

    @Test
    void updateFailsIfEventAlreadyPublished() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        Event event = new Event(
                "event-1",
                "Original Event",
                "original-event",
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T10:00:00Z"),
                ZoneId.of("Europe/Copenhagen"),
                "organizer-1");
        repository.addIfAbsent(event);
        repository.updatePublicationState(event.publish(), event.publicationState());

        UpdateEventService service = new UpdateEventService(repository);

        EventAlreadyPublishedException error = assertThrows(
                EventAlreadyPublishedException.class,
                () -> service.update(
                        CONTEXT,
                        new UpdateEventCommand(
                                "event-1",
                                "Updated Event",
                                "updated-event",
                                Instant.parse("2026-10-01T09:00:00Z"),
                                Instant.parse("2026-10-01T11:00:00Z"),
                                ZoneId.of("Europe/Oslo"))));

        assertEquals("event-1", error.eventId());
    }

    @Test
    void updateFailsIfEventWithdrawn() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        Event event = new Event(
                "event-1",
                "Original Event",
                "original-event",
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T10:00:00Z"),
                ZoneId.of("Europe/Copenhagen"),
                "organizer-1");
        repository.addIfAbsent(event);
        repository.updatePublicationState(event.publish().withdraw(), event.publicationState());

        UpdateEventService service = new UpdateEventService(repository);

        composable.domain.platform.event.api.EventWithdrawnException error = assertThrows(
                composable.domain.platform.event.api.EventWithdrawnException.class,
                () -> service.update(
                        CONTEXT,
                        new UpdateEventCommand(
                                "event-1",
                                "Updated Event",
                                "updated-event",
                                Instant.parse("2026-10-01T09:00:00Z"),
                                Instant.parse("2026-10-01T11:00:00Z"),
                                ZoneId.of("Europe/Oslo"))));

        assertEquals("event-1", error.eventId());
    }

    @Test
    void updateFailsIfInvalidDefinition() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        Event event = new Event(
                "event-1",
                "Original Event",
                "original-event",
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T10:00:00Z"),
                ZoneId.of("Europe/Copenhagen"),
                "organizer-1");
        repository.addIfAbsent(event);

        UpdateEventService service = new UpdateEventService(repository);

        assertThrows(
                InvalidEventDefinitionException.class,
                () -> service.update(
                        CONTEXT,
                        new UpdateEventCommand(
                                "event-1",
                                " ",
                                "updated-event",
                                Instant.parse("2026-10-01T09:00:00Z"),
                                Instant.parse("2026-10-01T11:00:00Z"),
                                ZoneId.of("Europe/Oslo"))));

        assertThrows(
                InvalidEventDefinitionException.class,
                () -> service.update(
                        CONTEXT,
                        new UpdateEventCommand(
                                "event-1",
                                "Updated Event",
                                "updated-event",
                                Instant.parse("2026-10-01T12:00:00Z"),
                                Instant.parse("2026-10-01T11:00:00Z"),
                                ZoneId.of("Europe/Oslo"))));
    }

    @Test
    void rejectsMissingExecutionContextAsProgrammingError() {
        UpdateEventService service = new UpdateEventService(new InMemoryEventRepository());

        assertThrows(
                NullPointerException.class,
                () -> service.update(
                        null,
                        new UpdateEventCommand(
                                "event-1",
                                "Updated Event",
                                "updated-event",
                                Instant.parse("2026-10-01T09:00:00Z"),
                                Instant.parse("2026-10-01T11:00:00Z"),
                                ZoneId.of("Europe/Oslo"))));
    }
}
