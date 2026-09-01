package composable.domain.platform.composition.eventregistration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventRegistrationAvailability;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.registration.api.CancelRegistration;
import composable.domain.platform.registration.api.CreateRegistration;
import composable.domain.platform.registration.api.FindRegistration;
import composable.domain.platform.registration.api.ReactivateRegistration;
import composable.domain.platform.registration.api.RegistrantReference;
import composable.domain.platform.registration.api.RegistrationLifecycle;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.api.TargetReference;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import composable.domain.platform.security.api.AuthorizationDecision;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ParticipantEventRegistrationReactivationTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("event-registration-reactivation-test"));
    private static final AuthenticatedActorReference ACTOR =
            new AuthenticatedActorReference("actor-opaque");

    @Test
    void activeRegistrationIsReturnedWithoutRecheckingEventEligibility() {
        AtomicBoolean eventLookupCalled = new AtomicBoolean();
        AtomicBoolean reactivationCalled = new AtomicBoolean();

        ParticipantEventRegistrationView result = service(
                        (context, eventId) -> {
                            eventLookupCalled.set(true);
                            throw new AssertionError("Event lookup must not be invoked");
                        },
                        Optional.of(registration(RegistrationLifecycle.ACTIVE)),
                        (context, registrationId) -> {
                            reactivationCalled.set(true);
                            throw new AssertionError("Reactivation must not be invoked");
                        })
                .reactivate(CONTEXT, ACTOR, "registration-1")
                .orElseThrow();

        assertEquals(EventRegistrationLifecycle.ACTIVE, result.lifecycle());
        assertFalse(eventLookupCalled.get());
        assertFalse(reactivationCalled.get());
    }

    @Test
    void cancelledRegistrationReactivatesWhenEventIsPublishedAndOpen() {
        AtomicReference<ExecutionContext> eventContext = new AtomicReference<>();
        AtomicReference<ExecutionContext> reactivateContext = new AtomicReference<>();

        FindEvent findEvent = (context, eventId) -> {
            eventContext.set(context);
            return Optional.of(event(
                    EventPublicationState.PUBLISHED,
                    EventRegistrationAvailability.OPEN));
        };
        ReactivateRegistration reactivateRegistration = (context, registrationId) -> {
            reactivateContext.set(context);
            return Optional.of(registration(RegistrationLifecycle.ACTIVE));
        };

        ParticipantEventRegistrationView result = service(
                        findEvent,
                        Optional.of(registration(RegistrationLifecycle.CANCELLED)),
                        reactivateRegistration)
                .reactivate(CONTEXT, ACTOR, "registration-1")
                .orElseThrow();

        assertEquals(
                new ParticipantEventRegistrationView(
                        "registration-1",
                        "event-1",
                        EventRegistrationLifecycle.ACTIVE),
                result);
        assertSame(CONTEXT, eventContext.get());
        assertSame(CONTEXT, reactivateContext.get());
    }

    @Test
    void unpublishedEventRejectsWithoutReactivation() {
        AtomicBoolean reactivationCalled = new AtomicBoolean();

        ReactivateRegistration reactivateRegistration = (context, registrationId) -> {
            reactivationCalled.set(true);
            throw new AssertionError("Reactivation must not be invoked");
        };

        ParticipantEventRegistrationService service = service(
                (context, eventId) -> Optional.of(event(
                        EventPublicationState.UNPUBLISHED,
                        EventRegistrationAvailability.OPEN)),
                Optional.of(registration(RegistrationLifecycle.CANCELLED)),
                reactivateRegistration);

        assertThrows(
                EventNotPublishedForRegistrationException.class,
                () -> service.reactivate(CONTEXT, ACTOR, "registration-1"));
        assertFalse(reactivationCalled.get());
    }

    @Test
    void closedEventRejectsWithoutReactivation() {
        AtomicBoolean reactivationCalled = new AtomicBoolean();

        ReactivateRegistration reactivateRegistration = (context, registrationId) -> {
            reactivationCalled.set(true);
            throw new AssertionError("Reactivation must not be invoked");
        };

        ParticipantEventRegistrationService service = service(
                (context, eventId) -> Optional.of(event(
                        EventPublicationState.PUBLISHED,
                        EventRegistrationAvailability.CLOSED)),
                Optional.of(registration(RegistrationLifecycle.CANCELLED)),
                reactivateRegistration);

        assertThrows(
                EventRegistrationClosedException.class,
                () -> service.reactivate(CONTEXT, ACTOR, "registration-1"));
        assertFalse(reactivationCalled.get());
    }

    @Test
    void withdrawnEventRejectsWithoutReactivation() {
        AtomicBoolean reactivationCalled = new AtomicBoolean();
        ReactivateRegistration reactivateRegistration = (context, registrationId) -> {
            reactivationCalled.set(true);
            throw new AssertionError("Reactivation must not be invoked");
        };

        ParticipantEventRegistrationService service = service(
                (context, eventId) -> Optional.of(event(
                        EventPublicationState.WITHDRAWN,
                        EventRegistrationAvailability.OPEN)),
                Optional.of(registration(RegistrationLifecycle.CANCELLED)),
                reactivateRegistration);

        assertThrows(
                EventNotPublishedForRegistrationException.class,
                () -> service.reactivate(CONTEXT, ACTOR, "registration-1"));
        assertFalse(reactivationCalled.get());
    }

    @Test
    void unknownEventRejectsWithoutReactivation() {
        AtomicBoolean reactivationCalled = new AtomicBoolean();
        ReactivateRegistration reactivateRegistration = (context, registrationId) -> {
            reactivationCalled.set(true);
            throw new AssertionError("Reactivation must not be invoked");
        };

        ParticipantEventRegistrationService service = service(
                (context, eventId) -> Optional.empty(),
                Optional.of(registration(RegistrationLifecycle.CANCELLED)),
                reactivateRegistration);

        assertThrows(
                UnknownEventForRegistrationException.class,
                () -> service.reactivate(CONTEXT, ACTOR, "registration-1"));
        assertFalse(reactivationCalled.get());
    }

    @Test
    void unknownPrivateOrNonEventRegistrationDoesNotReactivate() {
        AtomicBoolean reactivationCalled = new AtomicBoolean();
        ReactivateRegistration reactivateRegistration = (context, registrationId) -> {
            reactivationCalled.set(true);
            throw new AssertionError("Reactivation must not be invoked");
        };

        assertTrue(service(noEventLookup(), Optional.empty(), reactivateRegistration)
                .reactivate(CONTEXT, ACTOR, "missing")
                .isEmpty());

        RegistrationView nonEvent = new RegistrationView(
                "registration-1",
                new RegistrantReference("participant", "actor-opaque"),
                new TargetReference("course", "course-1"),
                RegistrationLifecycle.CANCELLED);
        assertTrue(service(noEventLookup(), Optional.of(nonEvent), reactivateRegistration)
                .reactivate(CONTEXT, ACTOR, "registration-1")
                .isEmpty());

        RegistrationView privateRegistration = new RegistrationView(
                "registration-1",
                new RegistrantReference("participant", "other-actor"),
                new TargetReference("event", "event-1"),
                RegistrationLifecycle.CANCELLED);
        assertThrows(
                EventRegistrationAuthorizationDeniedException.class,
                () -> service(noEventLookup(), Optional.of(privateRegistration), reactivateRegistration)
                        .reactivate(CONTEXT, ACTOR, "registration-1"));

        assertFalse(reactivationCalled.get());
    }

    private static ParticipantEventRegistrationService service(
            FindEvent findEvent,
            Optional<RegistrationView> registration,
            ReactivateRegistration reactivateRegistration) {
        FindRegistration findRegistration = (context, registrationId) -> registration;
        CreateRegistration createRegistration = (context, command) -> {
            throw new AssertionError("Creation must not be invoked");
        };
        CancelRegistration cancelRegistration = (context, registrationId) -> {
            throw new AssertionError("Cancellation must not be invoked");
        };

        return new ParticipantEventRegistrationService(
                findEvent,
                createRegistration,
                findRegistration,
                cancelRegistration,
                reactivateRegistration,
                (actor, owner) -> actor.reference().equals(owner.reference())
                        ? AuthorizationDecision.ALLOWED
                        : AuthorizationDecision.DENIED);
    }

    private static FindEvent noEventLookup() {
        return (context, eventId) -> {
            throw new AssertionError("Event lookup must not be invoked");
        };
    }

    private static RegistrationView registration(RegistrationLifecycle lifecycle) {
        return new RegistrationView(
                "registration-1",
                new RegistrantReference("participant", "actor-opaque"),
                new TargetReference("event", "event-1"),
                lifecycle);
    }

    private static EventView event(
            EventPublicationState publicationState,
            EventRegistrationAvailability registrationAvailability) {
        return new EventView(
                "event-1",
                "Event",
                "event",
                Instant.parse("2026-08-10T10:00:00Z"),
                Instant.parse("2026-08-10T11:00:00Z"),
                ZoneId.of("Europe/Copenhagen"),
                publicationState,
                registrationAvailability);
    }
}
