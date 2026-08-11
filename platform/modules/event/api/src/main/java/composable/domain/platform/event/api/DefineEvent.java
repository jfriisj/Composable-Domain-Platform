package composable.domain.platform.event.api;

import composable.domain.platform.core.execution.ExecutionContext;

public interface DefineEvent {

    EventView define(ExecutionContext context, DefineEventCommand command);
}
