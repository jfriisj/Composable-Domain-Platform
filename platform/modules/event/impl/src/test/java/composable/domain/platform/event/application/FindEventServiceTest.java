package composable.domain.platform.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.domain.Event;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FindEventServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("test-correlation"));

    @Test
    void findsExistingUnpublishedEventByIdentity() {
        Instant startsAt = Instant.parse("2026-09-01T08:00:00Z");
        Instant endsAt = Instant.parse("2026-09-01T10:00:00Z");
        ZoneId timezone = ZoneId.of("Europe/Copenhagen");
        InMemoryEventRepository repository = new InMemoryEventRepository();
        repository.addIfAbsent(new Event(
                "event-1",
                "Platform Day",
                "platform-day",
                startsAt,
                endsAt,
                timezone,
                "organizer-1"));

        Optional<EventView> result =
                new FindEventService(repository).findById(CONTEXT, "event-1");

        assertEquals(
                Optional.of(new EventView(
                        "event-1",
                        "Platform Day",
                        "platform-day",
                        startsAt,
                        endsAt,
                        timezone,
                        EventPublicationState.UNPUBLISHED,
                        new composable.domain.platform.event.api.EventOwnerReference("organizer-1"))),
                result);
    }

    @Test
    void knownIdRetrievalReturnsPublishedEvent() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        Event event = new Event(
                "event-published",
                "Published Event",
                "published-event",
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T10:00:00Z"),
                ZoneId.of("Europe/Copenhagen"),
                "organizer-1");
        repository.addIfAbsent(event);
        repository.updatePublicationState(event.publish(), event.publicationState());

        EventView result = new FindEventService(repository)
                .findById(CONTEXT, "event-published")
                .orElseThrow();

        assertEquals(EventPublicationState.PUBLISHED, result.publicationState());
    }

    @Test
    void returnsEmptyForUnknownIdentity() {
        Optional<EventView> result =
                new FindEventService(new InMemoryEventRepository())
                        .findById(CONTEXT, "missing-event");

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsBlankIdentity() {
        FindEventService service = new FindEventService(new InMemoryEventRepository());

        assertThrows(IllegalArgumentException.class, () -> service.findById(CONTEXT, " "));
    }

    @Test
    void rejectsMissingExecutionContextAsProgrammingError() {
        FindEventService service = new FindEventService(new InMemoryEventRepository());

        assertThrows(NullPointerException.class, () -> service.findById(null, "event-1"));
    }
}
