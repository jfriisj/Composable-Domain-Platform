package composable.domain.platform.composition.eventregistration;

import composable.domain.platform.core.execution.ExecutionContext;
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
import java.util.Objects;
import java.util.Optional;

public final class ParticipantEventRegistrationService
        implements CreateParticipantEventRegistration,
                FindParticipantEventRegistration,
                CancelParticipantEventRegistration {

    static final String PARTICIPANT_NAMESPACE = "participant";
    static final String EVENT_NAMESPACE = "event";

    private final FindEvent findEvent;
    private final CreateRegistration createRegistration;
    private final FindRegistration findRegistration;
    private final CancelRegistration cancelRegistration;

    public ParticipantEventRegistrationService(
            FindEvent findEvent,
            CreateRegistration createRegistration,
            FindRegistration findRegistration,
            CancelRegistration cancelRegistration) {
        this.findEvent = Objects.requireNonNull(findEvent, "findEvent must not be null");
        this.createRegistration =
                Objects.requireNonNull(createRegistration, "createRegistration must not be null");
        this.findRegistration =
                Objects.requireNonNull(findRegistration, "findRegistration must not be null");
        this.cancelRegistration =
                Objects.requireNonNull(cancelRegistration, "cancelRegistration must not be null");
    }

    @Override
    public ParticipantEventRegistrationView create(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            CreateParticipantEventRegistrationCommand command) {
        Objects.requireNonNull(context, "context must not be null");
        RegistrantReference participantReference = participantReference(actorReference);

        if (command == null || command.eventId() == null || command.eventId().isBlank()) {
            throw new InvalidEventRegistrationDefinitionException();
        }

        if (findEvent.findById(context, command.eventId()).isEmpty()) {
            throw new UnknownEventForRegistrationException();
        }

        try {
            RegistrationView registration = createRegistration.create(
                    context,
                    new CreateRegistrationCommand(
                            command.registrationId(),
                            participantReference,
                            new TargetReference(EVENT_NAMESPACE, command.eventId())));

            return toView(registration);
        } catch (InvalidRegistrationDefinitionException exception) {
            throw new InvalidEventRegistrationDefinitionException();
        } catch (RegistrationUniquenessConflictException exception) {
            throw new EventRegistrationUniquenessConflictException();
        }
    }

    @Override
    public Optional<ParticipantEventRegistrationView> findById(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            String registrationId) {
        Objects.requireNonNull(context, "context must not be null");
        RegistrantReference participantReference = participantReference(actorReference);

        return findOwnedRegistration(context, participantReference, registrationId)
                .map(ParticipantEventRegistrationService::toView);
    }

    @Override
    public Optional<ParticipantEventRegistrationView> cancel(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            String registrationId) {
        Objects.requireNonNull(context, "context must not be null");
        RegistrantReference participantReference = participantReference(actorReference);

        Optional<RegistrationView> ownedRegistration =
                findOwnedRegistration(context, participantReference, registrationId);
        if (ownedRegistration.isEmpty()) {
            return Optional.empty();
        }

        return cancelRegistration.cancel(context, registrationId)
                .map(ParticipantEventRegistrationService::toView);
    }

    private Optional<RegistrationView> findOwnedRegistration(
            ExecutionContext context,
            RegistrantReference participantReference,
            String registrationId) {
        Optional<RegistrationView> found = findRegistration.findById(context, registrationId);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        RegistrationView registration = found.orElseThrow();
        if (!EVENT_NAMESPACE.equals(registration.targetReference().namespace())) {
            return Optional.empty();
        }

        if (!participantReference.equals(registration.registrantReference())) {
            throw new EventRegistrationAuthorizationDeniedException();
        }

        return Optional.of(registration);
    }

    private static RegistrantReference participantReference(
            AuthenticatedActorReference actorReference) {
        if (actorReference == null) {
            throw new IllegalArgumentException("actorReference must not be null");
        }

        return new RegistrantReference(PARTICIPANT_NAMESPACE, actorReference.reference());
    }

    private static ParticipantEventRegistrationView toView(RegistrationView registration) {
        return new ParticipantEventRegistrationView(
                registration.registrationId(),
                registration.targetReference().reference(),
                toLifecycle(registration.lifecycle()));
    }

    private static EventRegistrationLifecycle toLifecycle(RegistrationLifecycle lifecycle) {
        return switch (lifecycle) {
            case ACTIVE -> EventRegistrationLifecycle.ACTIVE;
            case CANCELLED -> EventRegistrationLifecycle.CANCELLED;
        };
    }
}
