package composable.domain.platform.composition.eventregistration;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.registration.api.CreateRegistration;
import composable.domain.platform.registration.api.CreateRegistrationCommand;
import composable.domain.platform.registration.api.FindRegistration;
import composable.domain.platform.registration.api.InvalidRegistrationDefinitionException;
import composable.domain.platform.registration.api.RegistrantReference;
import composable.domain.platform.registration.api.RegistrationUniquenessConflictException;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.api.TargetReference;
import java.util.Objects;
import java.util.Optional;

public final class EventRegistrationService
        implements CreateEventRegistration, FindEventRegistration {

    static final String PARTICIPANT_NAMESPACE = "participant";
    static final String EVENT_NAMESPACE = "event";

    private final FindEvent findEvent;
    private final CreateRegistration createRegistration;
    private final FindRegistration findRegistration;

    public EventRegistrationService(
            FindEvent findEvent,
            CreateRegistration createRegistration,
            FindRegistration findRegistration) {
        this.findEvent = Objects.requireNonNull(findEvent, "findEvent must not be null");
        this.createRegistration =
                Objects.requireNonNull(createRegistration, "createRegistration must not be null");
        this.findRegistration =
                Objects.requireNonNull(findRegistration, "findRegistration must not be null");
    }

    @Override
    public EventRegistrationView create(
            ExecutionContext context,
            CreateEventRegistrationCommand command) {
        Objects.requireNonNull(context, "context must not be null");

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
                            new RegistrantReference(
                                    PARTICIPANT_NAMESPACE,
                                    command.participantReference()),
                            new TargetReference(
                                    EVENT_NAMESPACE,
                                    command.eventId())));

            return toView(registration);
        } catch (InvalidRegistrationDefinitionException exception) {
            throw new InvalidEventRegistrationDefinitionException();
        } catch (RegistrationUniquenessConflictException exception) {
            throw new EventRegistrationUniquenessConflictException();
        }
    }

    @Override
    public Optional<EventRegistrationView> findById(
            ExecutionContext context,
            String registrationId) {
        Objects.requireNonNull(context, "context must not be null");

        return findRegistration.findById(context, registrationId)
                .filter(registration ->
                        EVENT_NAMESPACE.equals(registration.targetReference().namespace()))
                .map(EventRegistrationService::toView);
    }

    private static EventRegistrationView toView(RegistrationView registration) {
        return new EventRegistrationView(
                registration.registrationId(),
                registration.targetReference().reference(),
                registration.registrantReference().reference());
    }
}
