package composable.domain.platform.registration.application;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.registration.api.FindRegistration;
import composable.domain.platform.registration.api.RegistrationView;
import java.util.Objects;
import java.util.Optional;

public final class FindRegistrationService implements FindRegistration {

    private final RegistrationRepository repository;

    public FindRegistrationService(RegistrationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Optional<RegistrationView> findById(
            ExecutionContext context,
            String registrationId) {
        Objects.requireNonNull(context, "context must not be null");

        if (registrationId == null || registrationId.isBlank()) {
            throw new IllegalArgumentException("registrationId must not be blank");
        }

        return repository.findById(registrationId).map(CreateRegistrationService::toView);
    }
}
