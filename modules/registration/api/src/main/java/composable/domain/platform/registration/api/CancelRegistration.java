package composable.domain.platform.registration.api;

import composable.domain.platform.core.execution.ExecutionContext;
import java.util.Optional;

public interface CancelRegistration {

    Optional<RegistrationView> cancel(ExecutionContext context, String registrationId);
}
