package composable.domain.platform.event.application;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventAlreadyPublishedException;
import composable.domain.platform.event.api.EventNotFoundException;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.PublishEvent;
import composable.domain.platform.event.domain.Event;
import java.util.Objects;

public final class PublishEventService implements PublishEvent {

    private final EventRepository repository;

    public PublishEventService(EventRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public EventView publish(ExecutionContext context, String eventId) {
        Objects.requireNonNull(context, "context must not be null");

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }

        Event existing = repository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        Event published;
        try {
            published = existing.publish();
        } catch (IllegalStateException exception) {
            throw new EventAlreadyPublishedException(eventId);
        }

        if (!repository.updatePublicationState(published, existing.publicationState())) {
            throw new EventAlreadyPublishedException(eventId);
        }

        return EventViews.from(published);
    }
}
