package composable.domain.platform.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.DefineEventCommand;
import composable.domain.platform.event.api.EventNotPublishedException;
import composable.domain.platform.event.api.EventOwnerReference;
import composable.domain.platform.event.api.EventRegistrationAvailability;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.EventWithdrawnException;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SetEventRegistrationAvailabilityServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("availability-test"));

    private InMemoryEventRepository repository;
    private DefineEventService define;
    private PublishEventService publish;
    private SetEventRegistrationAvailabilityService availability;

    @BeforeEach
    void setUp() {
        repository = new InMemoryEventRepository();
        define = new DefineEventService(repository);
        publish = new PublishEventService(repository);
        availability = new SetEventRegistrationAvailabilityService(repository);
    }

    @Test
    void publishedEventCanCloseReopenAndRepeatCurrentAvailabilityIdempotently() {
        EventView defined = defineEvent("availability-event");
        assertEquals(
                EventRegistrationAvailability.OPEN,
                defined.registrationAvailability());

        publish.publish(CONTEXT, defined.eventId());

        EventView closed = availability.setRegistrationAvailability(
                CONTEXT,
                defined.eventId(),
                EventRegistrationAvailability.CLOSED);
        EventView repeated = availability.setRegistrationAvailability(
                CONTEXT,
                defined.eventId(),
                EventRegistrationAvailability.CLOSED);
        EventView reopened = availability.setRegistrationAvailability(
                CONTEXT,
                defined.eventId(),
                EventRegistrationAvailability.OPEN);

        assertEquals(EventRegistrationAvailability.CLOSED, closed.registrationAvailability());
        assertEquals(closed, repeated);
        assertEquals(EventRegistrationAvailability.OPEN, reopened.registrationAvailability());
    }

    @Test
    void availabilityCannotChangeBeforePublicationOrAfterWithdrawal() {
        EventView defined = defineEvent("availability-lifecycle-event");

        assertThrows(
                EventNotPublishedException.class,
                () -> availability.setRegistrationAvailability(
                        CONTEXT,
                        defined.eventId(),
                        EventRegistrationAvailability.CLOSED));

        publish.publish(CONTEXT, defined.eventId());
        new WithdrawEventService(repository).withdraw(CONTEXT, defined.eventId());

        assertThrows(
                EventWithdrawnException.class,
                () -> availability.setRegistrationAvailability(
                        CONTEXT,
                        defined.eventId(),
                        EventRegistrationAvailability.OPEN));
    }

    private EventView defineEvent(String eventId) {
        return define.define(
                CONTEXT,
                new DefineEventCommand(
                        eventId,
                        "Availability Event",
                        eventId,
                        Instant.parse("2026-09-01T08:00:00Z"),
                        Instant.parse("2026-09-01T10:00:00Z"),
                        ZoneId.of("Europe/Copenhagen"),
                        new EventOwnerReference("organizer-1")));
    }
}
