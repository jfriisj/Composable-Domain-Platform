package composable.domain.platform.registration.api;

import composable.domain.platform.core.execution.ExecutionContext;
import java.util.Optional;

public interface ReactivateRegistration {

    Optional<RegistrationView> reactivate(
            ExecutionContext context,
            String registrationId);
}
