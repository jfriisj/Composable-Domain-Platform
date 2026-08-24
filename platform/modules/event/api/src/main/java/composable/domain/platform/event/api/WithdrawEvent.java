package composable.domain.platform.event.api;

import composable.domain.platform.core.execution.ExecutionContext;

public interface WithdrawEvent {

    /**
     * Withdraws an existing published Event.
     *
     * @throws EventNotFoundException if the Event does not exist
     * @throws EventNotPublishedException if the Event is still unpublished
     * @throws EventWithdrawnException if the Event is already terminal withdrawn
     */
    EventView withdraw(ExecutionContext context, String eventId);
}
