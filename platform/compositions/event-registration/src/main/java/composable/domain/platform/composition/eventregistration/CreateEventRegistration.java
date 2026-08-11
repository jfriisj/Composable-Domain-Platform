package composable.domain.platform.composition.eventregistration;

import composable.domain.platform.core.execution.ExecutionContext;

public interface CreateEventRegistration {

    EventRegistrationView create(
            ExecutionContext context,
            CreateEventRegistrationCommand command);
}
