package composable.domain.platform.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventNotFoundException;
import composable.domain.platform.event.api.EventNotPublishedException;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.EventWithdrawnException;
import composable.domain.platform.event.domain.Event;
import composable.domain.platform.event.domain.PublicationState;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WithdrawEventServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("withdraw-test-correlation"));

    @Test
    void withdrawsExistingPublishedEventAndPreservesDefinition() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        Event event = event("event-1").publish();
        repository.addIfAbsent(event);

        EventView withdrawn = new WithdrawEventService(repository).withdraw(CONTEXT, event.id());

        assertEquals(event.id(), withdrawn.eventId());
        assertEquals(event.name(), withdrawn.name());
        assertEquals(event.slug(), withdrawn.slug());
        assertEquals(event.startsAt(), withdrawn.startsAt());
        assertEquals(event.endsAt(), withdrawn.endsAt());
        assertEquals(event.timezone(), withdrawn.timezone());
        assertEquals(EventPublicationState.WITHDRAWN, withdrawn.publicationState());
        assertEquals(
                withdrawn,
                new FindEventService(repository).findById(CONTEXT, event.id()).orElseThrow());
    }

    @Test
    void unknownEventWithdrawalFailsExplicitly() {
        EventNotFoundException error = assertThrows(
                EventNotFoundException.class,
                () -> new WithdrawEventService(new InMemoryEventRepository())
                        .withdraw(CONTEXT, "missing-event"));

        assertEquals("missing-event", error.eventId());
    }

    @Test
    void unpublishedEventCannotBeWithdrawn() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        Event event = event("event-1");
        repository.addIfAbsent(event);
        WithdrawEventService service = new WithdrawEventService(repository);

        EventNotPublishedException error = assertThrows(
                EventNotPublishedException.class,
                () -> service.withdraw(CONTEXT, event.id()));

        assertEquals(event.id(), error.eventId());
    }

    @Test
    void alreadyWithdrawnEventCannotBeWithdrawnAgain() {
        InMemoryEventRepository repository = new InMemoryEventRepository();
        Event event = event("event-1").publish();
        repository.addIfAbsent(event);
        WithdrawEventService service = new WithdrawEventService(repository);

        service.withdraw(CONTEXT, event.id());

        EventWithdrawnException error = assertThrows(
                EventWithdrawnException.class,
                () -> service.withdraw(CONTEXT, event.id()));

        assertEquals(event.id(), error.eventId());
    }

    @Test
    void concurrentWithdrawalFailureTranslatesToEventWithdrawnException() {
        Event published = event("event-1").publish();
        Event withdrawn = published.withdraw();
        AtomicInteger findCalls = new AtomicInteger();

        EventRepository repository = new EventRepository() {
            @Override
            public boolean addIfAbsent(Event event) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<Event> findById(String eventId) {
                if (findCalls.getAndIncrement() == 0) {
                    return Optional.of(published);
                }
                return Optional.of(withdrawn);
            }

            @Override
            public boolean updatePublicationState(Event event, PublicationState expectedState) {
                return false;
            }

            @Override
            public boolean updateDefinition(Event event) {
                throw new UnsupportedOperationException();
            }

            @Override
            public java.util.Collection<Event> findPublished() {
                throw new UnsupportedOperationException();
            }
        };

        WithdrawEventService service = new WithdrawEventService(repository);

        EventWithdrawnException error = assertThrows(
                EventWithdrawnException.class,
                () -> service.withdraw(CONTEXT, "event-1"));

        assertEquals("event-1", error.eventId());
        assertEquals(2, findCalls.get());
    }

    @Test
    void rejectsBlankIdentity() {
        WithdrawEventService service = new WithdrawEventService(new InMemoryEventRepository());

        assertThrows(IllegalArgumentException.class, () -> service.withdraw(CONTEXT, " "));
    }

    @Test
    void rejectsMissingExecutionContextAsProgrammingError() {
        WithdrawEventService service = new WithdrawEventService(new InMemoryEventRepository());

        assertThrows(NullPointerException.class, () -> service.withdraw(null, "event-1"));
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
