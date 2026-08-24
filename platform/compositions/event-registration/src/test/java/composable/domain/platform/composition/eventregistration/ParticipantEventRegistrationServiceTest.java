package composable.domain.platform.composition.eventregistration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.registration.api.CancelRegistration;
import composable.domain.platform.registration.api.CreateRegistration;
import composable.domain.platform.registration.api.CreateRegistrationCommand;
import composable.domain.platform.registration.api.FindRegistration;
import composable.domain.platform.registration.api.InvalidRegistrationDefinitionException;
import composable.domain.platform.registration.api.RegistrantReference;
import composable.domain.platform.registration.api.RegistrationLifecycle;
import composable.domain.platform.registration.api.RegistrationUniquenessConflictException;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.api.TargetReference;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import composable.domain.platform.security.api.AuthorizationDecision;
import composable.domain.platform.security.api.AuthorizeResourceOwnership;
import composable.domain.platform.security.api.ResourceOwnerReference;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ParticipantEventRegistrationServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("participant-event-registration-test"));
    private static final AuthenticatedActorReference ACTOR =
            new AuthenticatedActorReference("actor-opaque");

    @Test
    void createRejectsMissingActorBeforeCallingDependencies() {
        ParticipantEventRegistrationService service = service(
                noEventLookup(),
                noRegistrationCreate(),
                noRegistrationLookup(),
                noRegistrationCancellation());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(
                        CONTEXT,
                        null,
                        new CreateParticipantEventRegistrationCommand(
                                "registration-1",
                                "event-1")));
    }

    @Test
    void createsForAuthenticatedActorAndMapsActiveLifecycleWhenEventIsPublished() {
        AtomicReference<ExecutionContext> eventContext = new AtomicReference<>();
        AtomicReference<ExecutionContext> registrationContext = new AtomicReference<>();
        AtomicReference<CreateRegistrationCommand> registrationCommand = new AtomicReference<>();

        FindEvent findEvent = (context, eventId) -> {
            eventContext.set(context);
            return Optional.of(event(eventId, EventPublicationState.PUBLISHED));
        };
        CreateRegistration createRegistration = (context, command) -> {
            registrationContext.set(context);
            registrationCommand.set(command);
            return new RegistrationView(
                    command.registrationId(),
                    command.registrantReference(),
                    command.targetReference(),
                    RegistrationLifecycle.ACTIVE);
        };

        ParticipantEventRegistrationView created = service(
                findEvent,
                createRegistration,
                noRegistrationLookup(),
                noRegistrationCancellation())
                .create(
                        CONTEXT,
                        ACTOR,
                        new CreateParticipantEventRegistrationCommand(
                                "registration-1",
                                "event-1"));

        assertEquals(
                new ParticipantEventRegistrationView(
                        "registration-1",
                        "event-1",
                        EventRegistrationLifecycle.ACTIVE),
                created);
        assertEquals(
                new CreateRegistrationCommand(
                        "registration-1",
                        new RegistrantReference("participant", "actor-opaque"),
                        new TargetReference("event", "event-1")),
                registrationCommand.get());
        assertSame(CONTEXT, eventContext.get());
        assertSame(CONTEXT, registrationContext.get());
    }

    @Test
    void unpublishedEventDoesNotCreateParticipantRegistration() {
        AtomicBoolean registrationCalled = new AtomicBoolean();

        CreateRegistration createRegistration = (context, command) -> {
            registrationCalled.set(true);
            throw new AssertionError("Registration must not be invoked");
        };

        ParticipantEventRegistrationService service = service(
                (context, eventId) -> Optional.of(event(eventId, EventPublicationState.UNPUBLISHED)),
                createRegistration,
                noRegistrationLookup(),
                noRegistrationCancellation());

        assertThrows(
                EventNotPublishedForRegistrationException.class,
                () -> service.create(
                        CONTEXT,
                        ACTOR,
                        new CreateParticipantEventRegistrationCommand(
                                "registration-1",
                                "event-1")));

        assertFalse(registrationCalled.get());
    }

    @Test
    void withdrawnEventDoesNotCreateParticipantRegistration() {
        AtomicBoolean registrationCalled = new AtomicBoolean();

        CreateRegistration createRegistration = (context, command) -> {
            registrationCalled.set(true);
            throw new AssertionError("Registration must not be invoked");
        };

        ParticipantEventRegistrationService service = service(
                (context, eventId) -> Optional.of(event(eventId, EventPublicationState.WITHDRAWN)),
                createRegistration,
                noRegistrationLookup(),
                noRegistrationCancellation());

        assertThrows(
                EventNotPublishedForRegistrationException.class,
                () -> service.create(
                        CONTEXT,
                        ACTOR,
                        new CreateParticipantEventRegistrationCommand(
                                "registration-1",
                                "event-1")));

        assertFalse(registrationCalled.get());
    }

    @Test
    void unknownEventDoesNotCreateParticipantRegistration() {
        AtomicBoolean registrationCalled = new AtomicBoolean();

        CreateRegistration createRegistration = (context, command) -> {
            registrationCalled.set(true);
            throw new AssertionError("Registration must not be invoked");
        };

        ParticipantEventRegistrationService service = service(
                (context, eventId) -> Optional.empty(),
                createRegistration,
                noRegistrationLookup(),
                noRegistrationCancellation());

        assertThrows(
                UnknownEventForRegistrationException.class,
                () -> service.create(
                        CONTEXT,
                        ACTOR,
                        new CreateParticipantEventRegistrationCommand(
                                "registration-1",
                                "missing-event")));

        assertFalse(registrationCalled.get());
    }

    @Test
    void preservesCreateInvalidDefinitionFailure() {
        ParticipantEventRegistrationService service = service(
                (context, eventId) -> Optional.of(event(eventId, EventPublicationState.PUBLISHED)),
                (context, command) -> {
                    throw new InvalidRegistrationDefinitionException();
                },
                noRegistrationLookup(),
                noRegistrationCancellation());

        assertThrows(
                InvalidEventRegistrationDefinitionException.class,
                () -> service.create(
                        CONTEXT,
                        ACTOR,
                        new CreateParticipantEventRegistrationCommand(
                                " ",
                                "event-1")));
    }

    @Test
    void preservesCreateUniquenessConflictFailure() {
        ParticipantEventRegistrationService service = service(
                (context, eventId) -> Optional.of(event(eventId, EventPublicationState.PUBLISHED)),
                (context, command) -> {
                    throw new RegistrationUniquenessConflictException();
                },
                noRegistrationLookup(),
                noRegistrationCancellation());

        assertThrows(
                EventRegistrationUniquenessConflictException.class,
                () -> service.create(
                        CONTEXT,
                        ACTOR,
                        new CreateParticipantEventRegistrationCommand(
                                "registration-1",
                                "event-1")));
    }

    @Test
    void owningActorRetrievesEventRegistrationWithLifecycleAndContext() {
        AtomicReference<ExecutionContext> registrationContext = new AtomicReference<>();
        RegistrationView registration = registration(
                "registration-1",
                "actor-opaque",
                "event",
                "event-1",
                RegistrationLifecycle.CANCELLED);

        ParticipantEventRegistrationService service = service(
                noEventLookup(),
                noRegistrationCreate(),
                (context, registrationId) -> {
                    registrationContext.set(context);
                    return Optional.of(registration);
                },
                noRegistrationCancellation());

        assertEquals(
                Optional.of(new ParticipantEventRegistrationView(
                        "registration-1",
                        "event-1",
                        EventRegistrationLifecycle.CANCELLED)),
                service.findById(CONTEXT, ACTOR, "registration-1"));
        assertSame(CONTEXT, registrationContext.get());
    }

    @Test
    void unknownOrNonEventTargetIsNotFound() {
        RegistrationView nonEvent = registration(
                "registration-1",
                "actor-opaque",
                "course",
                "course-1",
                RegistrationLifecycle.ACTIVE);

        ParticipantEventRegistrationService unknownService =
                service(
                        noEventLookup(),
                        noRegistrationCreate(),
                        (context, registrationId) -> Optional.empty(),
                        noRegistrationCancellation());

        ParticipantEventRegistrationService nonEventService =
                service(
                        noEventLookup(),
                        noRegistrationCreate(),
                        (context, registrationId) -> Optional.of(nonEvent),
                        noRegistrationCancellation());

        assertTrue(unknownService.findById(CONTEXT, ACTOR, "missing").isEmpty());
        assertTrue(nonEventService.findById(CONTEXT, ACTOR, "registration-1").isEmpty());
    }

    @Test
    void nonOwnerRetrievalHasDistinctAuthorizationDeniedSemantic() {
        RegistrationView registration = registration(
                "registration-1",
                "other-actor",
                "event",
                "event-1",
                RegistrationLifecycle.ACTIVE);

        ParticipantEventRegistrationService service = service(
                noEventLookup(),
                noRegistrationCreate(),
                (context, registrationId) -> Optional.of(registration),
                noRegistrationCancellation());

        assertThrows(
                EventRegistrationAuthorizationDeniedException.class,
                () -> service.findById(CONTEXT, ACTOR, "registration-1"));
    }

    @Test
    void delegatesFinalOwnershipDecisionUsingOnlyOpaqueExpectedOwner() {
        RegistrationView registration = registration(
                "registration-1",
                "owner-opaque",
                "event",
                "event-1",
                RegistrationLifecycle.ACTIVE);
        AtomicReference<AuthenticatedActorReference> authorizedActor =
                new AtomicReference<>();
        AtomicReference<ResourceOwnerReference> authorizedOwner =
                new AtomicReference<>();

        AuthorizeResourceOwnership authorization = (actor, owner) -> {
            authorizedActor.set(actor);
            authorizedOwner.set(owner);
            return AuthorizationDecision.DENIED;
        };

        ParticipantEventRegistrationService service =
                new ParticipantEventRegistrationService(
                        noEventLookup(),
                        noRegistrationCreate(),
                        (context, registrationId) -> Optional.of(registration),
                        noRegistrationCancellation(),
                        authorization);

        assertThrows(
                EventRegistrationAuthorizationDeniedException.class,
                () -> service.findById(CONTEXT, ACTOR, "registration-1"));
        assertSame(ACTOR, authorizedActor.get());
        assertEquals(
                new ResourceOwnerReference("owner-opaque"),
                authorizedOwner.get());
    }

    @Test
    void nonParticipantRegistrantIsNotFoundBeforeSecurityAuthorization() {
        RegistrationView registration = new RegistrationView(
                "registration-1",
                new RegistrantReference("member", "actor-opaque"),
                new TargetReference("event", "event-1"),
                RegistrationLifecycle.ACTIVE);
        AtomicBoolean authorizationCalled = new AtomicBoolean();

        ParticipantEventRegistrationService service =
                new ParticipantEventRegistrationService(
                        noEventLookup(),
                        noRegistrationCreate(),
                        (context, registrationId) -> Optional.of(registration),
                        noRegistrationCancellation(),
                        (actor, owner) -> {
                            authorizationCalled.set(true);
                            return AuthorizationDecision.ALLOWED;
                        });

        assertTrue(service.findById(CONTEXT, ACTOR, "registration-1").isEmpty());
        assertFalse(authorizationCalled.get());
    }

    @Test
    void owningActorCancelsAfterAuthorizationAndPreservesContext() {
        AtomicReference<RegistrationView> state = new AtomicReference<>(registration(
                "registration-1",
                "actor-opaque",
                "event",
                "event-1",
                RegistrationLifecycle.ACTIVE));
        AtomicReference<ExecutionContext> findContext = new AtomicReference<>();
        AtomicReference<ExecutionContext> cancelContext = new AtomicReference<>();
        AtomicInteger cancelCalls = new AtomicInteger();

        FindRegistration findRegistration = (context, registrationId) -> {
            findContext.set(context);
            return Optional.of(state.get());
        };
        CancelRegistration cancelRegistration = (context, registrationId) -> {
            cancelContext.set(context);
            cancelCalls.incrementAndGet();
            RegistrationView current = state.get();
            RegistrationView cancelled = new RegistrationView(
                    current.registrationId(),
                    current.registrantReference(),
                    current.targetReference(),
                    RegistrationLifecycle.CANCELLED);
            state.set(cancelled);
            return Optional.of(cancelled);
        };

        ParticipantEventRegistrationService service = service(
                noEventLookup(),
                noRegistrationCreate(),
                findRegistration,
                cancelRegistration);

        ParticipantEventRegistrationView first =
                service.cancel(CONTEXT, ACTOR, "registration-1").orElseThrow();
        ParticipantEventRegistrationView second =
                service.cancel(CONTEXT, ACTOR, "registration-1").orElseThrow();

        assertEquals(EventRegistrationLifecycle.CANCELLED, first.lifecycle());
        assertEquals(first, second);
        assertEquals(2, cancelCalls.get());
        assertSame(CONTEXT, findContext.get());
        assertSame(CONTEXT, cancelContext.get());
    }

    @Test
    void nonOwnerCannotInvokeRegistrationCancellation() {
        AtomicBoolean cancellationCalled = new AtomicBoolean();
        RegistrationView registration = registration(
                "registration-1",
                "other-actor",
                "event",
                "event-1",
                RegistrationLifecycle.ACTIVE);

        CancelRegistration cancelRegistration = (context, registrationId) -> {
            cancellationCalled.set(true);
            throw new AssertionError("Registration cancellation must not be invoked");
        };

        ParticipantEventRegistrationService service = service(
                noEventLookup(),
                noRegistrationCreate(),
                (context, registrationId) -> Optional.of(registration),
                cancelRegistration);

        assertThrows(
                EventRegistrationAuthorizationDeniedException.class,
                () -> service.cancel(CONTEXT, ACTOR, "registration-1"));
        assertFalse(cancellationCalled.get());
    }

    @Test
    void cancelReturnsNotFoundWithoutInvokingRegistrationCancellation() {
        AtomicBoolean cancellationCalled = new AtomicBoolean();

        CancelRegistration cancelRegistration = (context, registrationId) -> {
            cancellationCalled.set(true);
            throw new AssertionError("Registration cancellation must not be invoked");
        };

        ParticipantEventRegistrationService service = service(
                noEventLookup(),
                noRegistrationCreate(),
                (context, registrationId) -> Optional.empty(),
                cancelRegistration);

        assertTrue(service.cancel(CONTEXT, ACTOR, "missing").isEmpty());
        assertFalse(cancellationCalled.get());
    }

    private static ParticipantEventRegistrationService service(
            FindEvent findEvent,
            CreateRegistration createRegistration,
            FindRegistration findRegistration,
            CancelRegistration cancelRegistration) {
        return new ParticipantEventRegistrationService(
                findEvent,
                createRegistration,
                findRegistration,
                cancelRegistration,
                ownershipByOpaqueEquality());
    }

    private static AuthorizeResourceOwnership ownershipByOpaqueEquality() {
        return (actor, owner) -> actor.reference().equals(owner.reference())
                ? AuthorizationDecision.ALLOWED
                : AuthorizationDecision.DENIED;
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

    private static CancelRegistration noRegistrationCancellation() {
        return (context, registrationId) -> {
            throw new AssertionError("Registration cancellation must not be invoked");
        };
    }

    private static RegistrationView registration(
            String registrationId,
            String actorReference,
            String targetNamespace,
            String targetReference,
            RegistrationLifecycle lifecycle) {
        return new RegistrationView(
                registrationId,
                new RegistrantReference("participant", actorReference),
                new TargetReference(targetNamespace, targetReference),
                lifecycle);
    }

    private static EventView event(
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
