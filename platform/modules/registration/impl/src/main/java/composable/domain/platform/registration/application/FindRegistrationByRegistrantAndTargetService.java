package composable.domain.platform.registration.application;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.registration.api.FindRegistrationByRegistrantAndTarget;
import composable.domain.platform.registration.api.RegistrantReference;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.api.TargetReference;
import java.util.Objects;
import java.util.Optional;

public final class FindRegistrationByRegistrantAndTargetService
        implements FindRegistrationByRegistrantAndTarget {

    private final RegistrationRepository repository;

    public FindRegistrationByRegistrantAndTargetService(
            RegistrationRepository repository) {
        this.repository =
                Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Optional<RegistrationView> findByRegistrantAndTarget(
            ExecutionContext context,
            RegistrantReference registrantReference,
            TargetReference targetReference) {
        Objects.requireNonNull(context, "context must not be null");
        requireReference(
                registrantReference == null
                        ? null
                        : registrantReference.namespace(),
                registrantReference == null
                        ? null
                        : registrantReference.reference(),
                "registrantReference");
        requireReference(
                targetReference == null
                        ? null
                        : targetReference.namespace(),
                targetReference == null
                        ? null
                        : targetReference.reference(),
                "targetReference");

        return repository.findByRegistrantAndTarget(
                        new composable.domain.platform.registration.domain.RegistrantReference(
                                registrantReference.namespace(),
                                registrantReference.reference()),
                        new composable.domain.platform.registration.domain.TargetReference(
                                targetReference.namespace(),
                                targetReference.reference()))
                .map(CreateRegistrationService::toView);
    }

    private static void requireReference(
            String namespace,
            String reference,
            String name) {
        if (namespace == null
                || namespace.isBlank()
                || reference == null
                || reference.isBlank()) {
            throw new IllegalArgumentException(
                    name + " must have non-blank namespace and reference");
        }
    }
}
