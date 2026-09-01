package composable.domain.platform.registration.application;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.registration.api.ReactivateRegistration;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.domain.Registration;
import composable.domain.platform.registration.domain.RegistrationLifecycle;
import java.util.Objects;
import java.util.Optional;

public final class ReactivateRegistrationService
        implements ReactivateRegistration {

    private final RegistrationRepository repository;

    public ReactivateRegistrationService(
            RegistrationRepository repository) {
        this.repository =
                Objects.requireNonNull(
                        repository,
                        "repository must not be null");
    }

    @Override
    public Optional<RegistrationView> reactivate(
            ExecutionContext context,
            String registrationId) {
        Objects.requireNonNull(context, "context must not be null");

        if (registrationId == null || registrationId.isBlank()) {
            throw new IllegalArgumentException(
                    "registrationId must not be blank");
        }

        return repository.findById(registrationId)
                .map(this::reactivate);
    }

    private RegistrationView reactivate(
            Registration registration) {
        Registration reactivated = registration.reactivate();

        if (reactivated == registration) {
            return CreateRegistrationService.toView(reactivated);
        }

        if (repository.updateLifecycle(
                reactivated,
                registration.lifecycle())) {
            return CreateRegistrationService.toView(reactivated);
        }

        Registration current = repository.findById(registration.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Registration disappeared during lifecycle transition"));

        if (current.lifecycle() != RegistrationLifecycle.ACTIVE) {
            throw new IllegalStateException(
                    "Registration lifecycle changed concurrently");
        }

        return CreateRegistrationService.toView(current);
    }
}
