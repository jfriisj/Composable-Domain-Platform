package composable.domain.platform.event.application;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.FindEvent;
import java.util.Objects;
import java.util.Optional;

public final class FindEventService implements FindEvent {

    private final EventRepository repository;

    public FindEventService(EventRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Optional<EventView> findById(ExecutionContext context, String eventId) {
        Objects.requireNonNull(context, "context must not be null");

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }

        return repository.findById(eventId).map(EventViews::from);
    }
}
