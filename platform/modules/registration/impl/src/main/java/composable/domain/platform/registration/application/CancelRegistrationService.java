package composable.domain.platform.registration.application;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.registration.api.CancelRegistration;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.domain.Registration;
import java.util.Objects;
import java.util.Optional;

public final class CancelRegistrationService implements CancelRegistration {

    private final RegistrationRepository repository;

    public CancelRegistrationService(RegistrationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Optional<RegistrationView> cancel(
            ExecutionContext context,
            String registrationId) {
        Objects.requireNonNull(context, "context must not be null");

        if (registrationId == null || registrationId.isBlank()) {
            throw new IllegalArgumentException("registrationId must not be blank");
        }

        return repository.findById(registrationId).map(this::cancel);
    }

    private RegistrationView cancel(Registration registration) {
        Registration cancelled = registration.cancel();

        if (cancelled != registration) {
            repository.updateLifecycle(cancelled);
        }

        return CreateRegistrationService.toView(cancelled);
    }
}
