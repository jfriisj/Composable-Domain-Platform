package composable.domain.platform.registration.api;

import composable.domain.platform.core.execution.ExecutionContext;
import java.util.Optional;

@FunctionalInterface
public interface FindRegistrationByRegistrantAndTarget {

    Optional<RegistrationView> findByRegistrantAndTarget(
            ExecutionContext context,
            RegistrantReference registrantReference,
            TargetReference targetReference);
}
