package composable.domain.platform.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventAlreadyPublishedException;
import composable.domain.platform.event.api.EventNotFoundException;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.domain.Event;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class PublishEventServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("publish-test-correlation"));

    @Test
    void publishesExistingUnpublishedEventAndPreservesDefinition() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        Event event = event("event-1");
        repository.addIfAbsent(event);

        EventView published = new PublishEventService(repository).publish(CONTEXT, event.id());

        assertEquals(event.id(), published.eventId());
        assertEquals(event.name(), published.name());
        assertEquals(event.slug(), published.slug());
        assertEquals(event.startsAt(), published.startsAt());
        assertEquals(event.endsAt(), published.endsAt());
        assertEquals(event.timezone(), published.timezone());
        assertEquals(EventPublicationState.PUBLISHED, published.publicationState());
        assertEquals(
                published,
                new FindEventService(repository).findById(CONTEXT, event.id()).orElseThrow());
    }

    @Test
    void unknownEventPublicationFailsExplicitly() {
        EventNotFoundException error = assertThrows(
                EventNotFoundException.class,
                () -> new PublishEventService(new InMemoryEventRepository())
                        .publish(CONTEXT, "missing-event"));

        assertEquals("missing-event", error.eventId());
    }

    @Test
    void alreadyPublishedEventCannotBeRepublished() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        Event event = event("event-1");
        repository.addIfAbsent(event);
        PublishEventService service = new PublishEventService(repository);

        service.publish(CONTEXT, event.id());

        EventAlreadyPublishedException error = assertThrows(
                EventAlreadyPublishedException.class,
                () -> service.publish(CONTEXT, event.id()));

        assertEquals(event.id(), error.eventId());
    }

    @Test
    void withdrawnEventCannotBePublished() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        Event event = event("event-1").publish().withdraw();
        repository.addIfAbsent(event);
        PublishEventService service = new PublishEventService(repository);

        composable.domain.platform.event.api.EventWithdrawnException error = assertThrows(
                composable.domain.platform.event.api.EventWithdrawnException.class,
                () -> service.publish(CONTEXT, event.id()));

        assertEquals(event.id(), error.eventId());
    }

    @Test
    void rejectsBlankIdentity() {
        PublishEventService service = new PublishEventService(new InMemoryEventRepository());

        assertThrows(IllegalArgumentException.class, () -> service.publish(CONTEXT, " "));
    }

    @Test
    void rejectsMissingExecutionContextAsProgrammingError() {
        PublishEventService service = new PublishEventService(new InMemoryEventRepository());

        assertThrows(NullPointerException.class, () -> service.publish(null, "event-1"));
    }

    private static Event event(String eventId) {
        return new Event(
                eventId,
                "Platform Day",
                "platform-day",
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T10:00:00Z"),
                ZoneId.of("Europe/Copenhagen"),
                "organizer-1");
    }
}
