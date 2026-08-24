package composable.domain.platform.registration.application;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.registration.api.FindRegistrationsByTarget;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.api.TargetReference;
import java.util.List;
import java.util.Objects;

public final class FindRegistrationsByTargetService implements FindRegistrationsByTarget {

    private final RegistrationRepository repository;

    public FindRegistrationsByTargetService(RegistrationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public List<RegistrationView> findByTarget(
            ExecutionContext context,
            TargetReference targetReference) {
        Objects.requireNonNull(context, "context must not be null");

        if (targetReference == null
                || targetReference.namespace() == null
                || targetReference.namespace().isBlank()
                || targetReference.reference() == null
                || targetReference.reference().isBlank()) {
            throw new IllegalArgumentException(
                    "targetReference must have non-blank namespace and reference");
        }

        return repository.findByTarget(
                new composable.domain.platform.registration.domain.TargetReference(
                        targetReference.namespace(),
                        targetReference.reference()))
                .stream()
                .map(CreateRegistrationService::toView)
                .toList();
    }
}
