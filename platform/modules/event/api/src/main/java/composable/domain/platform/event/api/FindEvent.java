package composable.domain.platform.event.api;

import composable.domain.platform.core.execution.ExecutionContext;
import java.util.Optional;

public interface FindEvent {

    Optional<EventView> findById(ExecutionContext context, String eventId);
}
