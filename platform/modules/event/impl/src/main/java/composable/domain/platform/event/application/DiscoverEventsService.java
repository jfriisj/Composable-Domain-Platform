package composable.domain.platform.event.application;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.DiscoverEvents;
import composable.domain.platform.event.api.EventView;
import java.util.Collection;
import java.util.Objects;

public final class DiscoverEventsService implements DiscoverEvents {

    private final EventRepository repository;

    public DiscoverEventsService(EventRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Collection<EventView> discover(ExecutionContext context) {
        Objects.requireNonNull(context, "context must not be null");

        return repository.findPublished().stream()
                .map(EventViews::from)
                .toList();
    }
}
