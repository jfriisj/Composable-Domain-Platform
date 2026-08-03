package composable.domain.platform.registration.application;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.registration.api.CreateRegistration;
import composable.domain.platform.registration.api.CreateRegistrationCommand;
import composable.domain.platform.registration.api.InvalidRegistrationDefinitionException;
import composable.domain.platform.registration.api.RegistrationUniquenessConflictException;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.domain.Registration;
import java.util.Objects;

public final class CreateRegistrationService implements CreateRegistration {

    private final RegistrationRepository repository;

    public CreateRegistrationService(RegistrationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public RegistrationView create(
            ExecutionContext context,
            CreateRegistrationCommand command) {
        Objects.requireNonNull(context, "context must not be null");

        Registration registration = createRegistration(command);

        if (!repository.addIfAbsent(registration)) {
            throw new RegistrationUniquenessConflictException();
        }

        return toView(registration);
    }

    private static Registration createRegistration(CreateRegistrationCommand command) {
        if (command == null
                || command.registrantReference() == null
                || command.targetReference() == null) {
            throw new InvalidRegistrationDefinitionException();
        }

        try {
            return new Registration(
                    command.registrationId(),
                    new composable.domain.platform.registration.domain.RegistrantReference(
                            command.registrantReference().namespace(),
                            command.registrantReference().reference()),
                    new composable.domain.platform.registration.domain.TargetReference(
                            command.targetReference().namespace(),
                            command.targetReference().reference()));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidRegistrationDefinitionException();
        }
    }

    static RegistrationView toView(Registration registration) {
        return new RegistrationView(
                registration.id(),
                new composable.domain.platform.registration.api.RegistrantReference(
                        registration.registrantReference().namespace(),
                        registration.registrantReference().reference()),
                new composable.domain.platform.registration.api.TargetReference(
                        registration.targetReference().namespace(),
                        registration.targetReference().reference()));
    }
}
