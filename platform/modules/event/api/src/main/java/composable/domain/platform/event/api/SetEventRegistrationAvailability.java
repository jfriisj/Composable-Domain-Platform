package composable.domain.platform.event.api;

import composable.domain.platform.core.execution.ExecutionContext;

public interface SetEventRegistrationAvailability {

    EventView setRegistrationAvailability(
            ExecutionContext context,
            String eventId,
            EventRegistrationAvailability availability);
}
