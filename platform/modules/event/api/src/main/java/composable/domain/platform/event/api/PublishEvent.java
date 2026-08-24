package composable.domain.platform.event.api;

import composable.domain.platform.core.execution.ExecutionContext;

public interface PublishEvent {

    /**
     * Publishes an existing unpublished Event.
     *
     * @throws EventNotFoundException if the Event does not exist
     * @throws EventAlreadyPublishedException if the Event is already published
     * @throws EventWithdrawnException if the Event is withdrawn
     */
    EventView publish(ExecutionContext context, String eventId);
}
