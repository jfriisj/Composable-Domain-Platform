package composable.domain.platform.event.api;

import composable.domain.platform.core.execution.ExecutionContext;
import java.util.Collection;

public interface DiscoverEvents {

    /**
     * Returns currently published Events.
     *
     * <p>Iteration order is not a business contract.
     */
    Collection<EventView> discover(ExecutionContext context);
}
