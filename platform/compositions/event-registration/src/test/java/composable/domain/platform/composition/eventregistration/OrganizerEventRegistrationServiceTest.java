package composable.domain.platform.composition.eventregistration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventOwnerReference;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.registration.api.FindRegistrationsByTarget;
import composable.domain.platform.registration.api.RegistrantReference;
import composable.domain.platform.registration.api.RegistrationLifecycle;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.api.TargetReference;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import composable.domain.platform.security.api.AuthorizationDecision;
import composable.domain.platform.security.api.AuthorizeResourceOwnership;
import composable.domain.platform.security.api.ResourceOwnerReference;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OrganizerEventRegistrationServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("corr-organizer-test"));
    private static final AuthenticatedActorReference ACTOR =
            new AuthenticatedActorReference("actor-organizer-1");

    @Test
    void findByEventIdReturnsRegistrationsForAuthorizedOwner() {
        AtomicReference<ExecutionContext> capturedEventContext = new AtomicReference<>();
        AtomicReference<ExecutionContext> capturedRegContext = new AtomicReference<>();
        AtomicReference<TargetReference> capturedTarget = new AtomicReference<>();
        AtomicReference<AuthenticatedActorReference> capturedActor = new AtomicReference<>();
        AtomicReference<ResourceOwnerReference> capturedOwner = new AtomicReference<>();

        RegistrationView reg1 = new RegistrationView(
                "reg-1",
                new RegistrantReference("participant", "participant-1"),
                new TargetReference("event", "event-1"),
                RegistrationLifecycle.ACTIVE);
        RegistrationView reg2 = new RegistrationView(
                "reg-2",
                new RegistrantReference("participant", "participant-2"),
                new TargetReference("event", "event-1"),
                RegistrationLifecycle.CANCELLED);

        FindEvent findEvent = (context, eventId) -> {
            capturedEventContext.set(context);
            return Optional.of(ownedEvent(eventId, EventPublicationState.PUBLISHED, "owner-opaque-1"));
        };

        FindRegistrationsByTarget findRegistrations = (context, targetReference) -> {
            capturedRegContext.set(context);
            capturedTarget.set(targetReference);
            return List.of(reg1, reg2);
        };

        AuthorizeResourceOwnership authorizeOwnership = (actor, owner) -> {
            capturedActor.set(actor);
            capturedOwner.set(owner);
            return AuthorizationDecision.ALLOWED;
        };

        OrganizerEventRegistrationService service = new OrganizerEventRegistrationService(
                findEvent,
                findRegistrations,
                authorizeOwnership);

        List<OrganizerEventRegistrationView> results =
                service.findByEventId(CONTEXT, ACTOR, "event-1");

        assertEquals(2, results.size());
        assertEquals(new OrganizerEventRegistrationView("reg-1", "event-1", EventRegistrationLifecycle.ACTIVE), results.get(0));
        assertEquals(new OrganizerEventRegistrationView("reg-2", "event-1", EventRegistrationLifecycle.CANCELLED), results.get(1));

        assertSame(CONTEXT, capturedEventContext.get());
        assertSame(CONTEXT, capturedRegContext.get());
        assertEquals(new TargetReference("event", "event-1"), capturedTarget.get());
        assertEquals(ACTOR, capturedActor.get());
        assertEquals(new ResourceOwnerReference("owner-opaque-1"), capturedOwner.get());
    }

    @Test
    void findByEventIdReturnsEmptyListWhenNoRegistrationsExist() {
        FindEvent findEvent = (context, eventId) ->
                Optional.of(ownedEvent(eventId, EventPublicationState.PUBLISHED, "owner-opaque-1"));

        FindRegistrationsByTarget findRegistrations = (context, targetReference) -> List.of();

        OrganizerEventRegistrationService service = new OrganizerEventRegistrationService(
                findEvent,
                findRegistrations,
                (actor, owner) -> AuthorizationDecision.ALLOWED);

        List<OrganizerEventRegistrationView> results =
                service.findByEventId(CONTEXT, ACTOR, "event-1");

        assertTrue(results.isEmpty());
    }

    @Test
    void findByEventIdThrowsUnknownEventWhenEventNotFoundBeforeQueryingRegistrations() {
        FindRegistrationsByTarget findRegistrations = (context, targetReference) -> {
            throw new AssertionError("Registrations must not be queried when Event is not found");
        };

        AuthorizeResourceOwnership authorizeOwnership = (actor, owner) -> {
            throw new AssertionError("Authorization must not be queried when Event is not found");
        };

        OrganizerEventRegistrationService service = new OrganizerEventRegistrationService(
                (context, eventId) -> Optional.empty(),
                findRegistrations,
                authorizeOwnership);

        assertThrows(
                UnknownEventForRegistrationException.class,
                () -> service.findByEventId(CONTEXT, ACTOR, "missing-event"));
    }

    @Test
    void findByEventIdThrowsOrganizerAuthorizationDeniedWhenEventIsOwnerless() {
        FindEvent findEvent = (context, eventId) ->
                Optional.of(ownerlessEvent(eventId, EventPublicationState.PUBLISHED));

        FindRegistrationsByTarget findRegistrations = (context, targetReference) -> {
            throw new AssertionError("Registrations must not be queried for ownerless Event");
        };

        AuthorizeResourceOwnership authorizeOwnership = (actor, owner) -> {
            throw new AssertionError("Authorization must not be queried for ownerless Event");
        };

        OrganizerEventRegistrationService service = new OrganizerEventRegistrationService(
                findEvent,
                findRegistrations,
                authorizeOwnership);

        assertThrows(
                OrganizerEventRegistrationAuthorizationDeniedException.class,
                () -> service.findByEventId(CONTEXT, ACTOR, "event-1"));
    }

    @Test
    void findByEventIdThrowsOrganizerAuthorizationDeniedWhenActorIsNotOwner() {
        FindEvent findEvent = (context, eventId) ->
                Optional.of(ownedEvent(eventId, EventPublicationState.PUBLISHED, "owner-opaque-1"));

        FindRegistrationsByTarget findRegistrations = (context, targetReference) -> {
            throw new AssertionError("Registrations must not be queried when authorization is denied");
        };

        AuthorizeResourceOwnership authorizeOwnership = (actor, owner) -> AuthorizationDecision.DENIED;

        OrganizerEventRegistrationService service = new OrganizerEventRegistrationService(
                findEvent,
                findRegistrations,
                authorizeOwnership);

        assertThrows(
                OrganizerEventRegistrationAuthorizationDeniedException.class,
                () -> service.findByEventId(CONTEXT, ACTOR, "event-1"));
    }

    @Test
    void findByEventIdValidatesArguments() {
        OrganizerEventRegistrationService service = new OrganizerEventRegistrationService(
                (context, eventId) -> Optional.empty(),
                (context, targetReference) -> List.of(),
                (actor, owner) -> AuthorizationDecision.ALLOWED);

        assertThrows(NullPointerException.class, () -> service.findByEventId(null, ACTOR, "event-1"));
        assertThrows(IllegalArgumentException.class, () -> service.findByEventId(CONTEXT, null, "event-1"));
        assertThrows(InvalidOrganizerEventRegistrationRequestException.class, () -> service.findByEventId(CONTEXT, ACTOR, null));
        assertThrows(InvalidOrganizerEventRegistrationRequestException.class, () -> service.findByEventId(CONTEXT, ACTOR, "   "));
    }

    private static EventView ownedEvent(
            String eventId,
            EventPublicationState publicationState,
            String ownerReference) {
        return new EventView(
                eventId,
                "Event",
                "event",
                Instant.parse("2026-08-10T10:00:00Z"),
                Instant.parse("2026-08-10T11:00:00Z"),
                ZoneId.of("Europe/Copenhagen"),
                publicationState,
                new EventOwnerReference(ownerReference));
    }

    private static EventView ownerlessEvent(
            String eventId,
            EventPublicationState publicationState) {
        return new EventView(
                eventId,
                "Event",
                "event",
                Instant.parse("2026-08-10T10:00:00Z"),
                Instant.parse("2026-08-10T11:00:00Z"),
                ZoneId.of("Europe/Copenhagen"),
                publicationState);
    }
}
