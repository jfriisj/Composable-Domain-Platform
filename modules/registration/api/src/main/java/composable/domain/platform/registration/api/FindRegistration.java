package composable.domain.platform.registration.api;

import composable.domain.platform.core.execution.ExecutionContext;
import java.util.Optional;

public interface FindRegistration {

    Optional<RegistrationView> findById(ExecutionContext context, String registrationId);
}
