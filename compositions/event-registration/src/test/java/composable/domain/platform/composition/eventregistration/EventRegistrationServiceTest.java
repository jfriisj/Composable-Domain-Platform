package composable.domain.platform.composition.eventregistration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.registration.api.CreateRegistration;
import composable.domain.platform.registration.api.CreateRegistrationCommand;
import composable.domain.platform.registration.api.FindRegistration;
import composable.domain.platform.registration.api.InvalidRegistrationDefinitionException;
import composable.domain.platform.registration.api.RegistrantReference;
import composable.domain.platform.registration.api.RegistrationUniquenessConflictException;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.api.TargetReference;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class EventRegistrationServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("event-registration-test"));

    @Test
    void createsRegistrationForExistingEventAndMapsAcceptedNamespaces() {
        AtomicReference<ExecutionContext> eventContext = new AtomicReference<>();
        AtomicReference<ExecutionContext> registrationContext = new AtomicReference<>();
        AtomicReference<CreateRegistrationCommand> registrationCommand = new AtomicReference<>();

        FindEvent findEvent = (context, eventId) -> {
            eventContext.set(context);
            return Optional.of(event(eventId));
        };
        CreateRegistration createRegistration = (context, command) -> {
            registrationContext.set(context);
            registrationCommand.set(command);
            return new RegistrationView(
                    command.registrationId(),
                    command.registrantReference(),
                    command.targetReference());
        };

        EventRegistrationView created = new EventRegistrationService(
                findEvent,
                createRegistration,
                noRegistrationLookup())
                .create(
                        CONTEXT,
                        new CreateEventRegistrationCommand(
                                "registration-1",
                                "event-1",
                                "participant-opaque"));

        assertEquals(
                new EventRegistrationView(
                        "registration-1",
                        "event-1",
                        "participant-opaque"),
                created);
        assertEquals(
                new CreateRegistrationCommand(
                        "registration-1",
                        new RegistrantReference("participant", "participant-opaque"),
                        new TargetReference("event", "event-1")),
                registrationCommand.get());
        assertSame(CONTEXT, eventContext.get());
        assertSame(CONTEXT, registrationContext.get());
    }

    @Test
    void unknownEventDoesNotCreateRegistration() {
        AtomicBoolean registrationCalled = new AtomicBoolean();

        CreateRegistration createRegistration = (context, command) -> {
            registrationCalled.set(true);
            throw new AssertionError("Registration must not be invoked");
        };

        EventRegistrationService service = new EventRegistrationService(
                (context, eventId) -> Optional.empty(),
                createRegistration,
                noRegistrationLookup());

        assertThrows(
                UnknownEventForRegistrationException.class,
                () -> service.create(
                        CONTEXT,
                        new CreateEventRegistrationCommand(
                                "registration-1",
                                "missing-event",
                                "participant-opaque")));

        assertFalse(registrationCalled.get());
    }

    @Test
    void mapsRegistrationInvalidDefinitionToWorkflowFailure() {
        EventRegistrationService service = new EventRegistrationService(
                (context, eventId) -> Optional.of(event(eventId)),
                (context, command) -> {
                    throw new InvalidRegistrationDefinitionException();
                },
                noRegistrationLookup());

        assertThrows(
                InvalidEventRegistrationDefinitionException.class,
                () -> service.create(
                        CONTEXT,
                        new CreateEventRegistrationCommand(
                                " ",
                                "event-1",
                                "participant-opaque")));
    }

    @Test
    void mapsRegistrationUniquenessConflictToWorkflowFailure() {
        EventRegistrationService service = new EventRegistrationService(
                (context, eventId) -> Optional.of(event(eventId)),
                (context, command) -> {
                    throw new RegistrationUniquenessConflictException();
                },
                noRegistrationLookup());

        assertThrows(
                EventRegistrationUniquenessConflictException.class,
                () -> service.create(
                        CONTEXT,
                        new CreateEventRegistrationCommand(
                                "registration-1",
                                "event-1",
                                "participant-opaque")));
    }

    @Test
    void retrievesEventTargetRegistrationAsEventRegistrationState() {
        RegistrationView registration = new RegistrationView(
                "registration-1",
                new RegistrantReference("participant", "participant-opaque"),
                new TargetReference("event", "event-1"));

        EventRegistrationService service = new EventRegistrationService(
                noEventLookup(),
                noRegistrationCreate(),
                (context, registrationId) -> Optional.of(registration));

        assertEquals(
                Optional.of(new EventRegistrationView(
                        "registration-1",
                        "event-1",
                        "participant-opaque")),
                service.findById(CONTEXT, "registration-1"));
    }

    @Test
    void returnsEmptyForUnknownRegistration() {
        EventRegistrationService service = new EventRegistrationService(
                noEventLookup(),
                noRegistrationCreate(),
                (context, registrationId) -> Optional.empty());

        assertTrue(service.findById(CONTEXT, "registration-missing").isEmpty());
    }

    @Test
    void doesNotExposeNonEventTargetRegistration() {
        RegistrationView registration = new RegistrationView(
                "registration-1",
                new RegistrantReference("participant", "participant-opaque"),
                new TargetReference("course", "course-1"));

        EventRegistrationService service = new EventRegistrationService(
                noEventLookup(),
                noRegistrationCreate(),
                (context, registrationId) -> Optional.of(registration));

        assertTrue(service.findById(CONTEXT, "registration-1").isEmpty());
    }

    private static FindEvent noEventLookup() {
        return (context, eventId) -> {
            throw new AssertionError("Event lookup must not be invoked");
        };
    }

    private static CreateRegistration noRegistrationCreate() {
        return (context, command) -> {
            throw new AssertionError("Registration creation must not be invoked");
        };
    }

    private static FindRegistration noRegistrationLookup() {
        return (context, registrationId) -> {
            throw new AssertionError("Registration lookup must not be invoked");
        };
    }

    private static EventView event(String eventId) {
        return new EventView(
                eventId,
                "Event",
                "event",
                Instant.parse("2026-08-10T10:00:00Z"),
                Instant.parse("2026-08-10T11:00:00Z"),
                ZoneId.of("Europe/Copenhagen"));
    }
}
