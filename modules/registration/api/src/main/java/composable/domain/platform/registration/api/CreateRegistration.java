package composable.domain.platform.registration.api;

import composable.domain.platform.core.execution.ExecutionContext;

public interface CreateRegistration {

    RegistrationView create(ExecutionContext context, CreateRegistrationCommand command);
}
