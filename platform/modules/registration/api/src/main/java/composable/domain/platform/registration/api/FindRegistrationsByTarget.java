package composable.domain.platform.registration.api;

import composable.domain.platform.core.execution.ExecutionContext;
import java.util.List;

@FunctionalInterface
public interface FindRegistrationsByTarget {

    List<RegistrationView> findByTarget(ExecutionContext context, TargetReference targetReference);
}
