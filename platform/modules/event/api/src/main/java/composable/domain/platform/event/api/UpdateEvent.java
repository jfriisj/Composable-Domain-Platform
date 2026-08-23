package composable.domain.platform.event.api;

import composable.domain.platform.core.execution.ExecutionContext;

public interface UpdateEvent {

    /**
     * Updates the mutable definition of an existing unpublished Event.
     *
     * @throws EventNotFoundException if the Event does not exist
     * @throws EventAlreadyPublishedException if the Event is already published
     * @throws InvalidEventDefinitionException if the updated definition is invalid
     */
    EventView update(ExecutionContext context, UpdateEventCommand command);
}
