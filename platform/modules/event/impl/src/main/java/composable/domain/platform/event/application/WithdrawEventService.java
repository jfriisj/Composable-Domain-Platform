package composable.domain.platform.event.application;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventNotFoundException;
import composable.domain.platform.event.api.EventNotPublishedException;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.EventWithdrawnException;
import composable.domain.platform.event.api.WithdrawEvent;
import composable.domain.platform.event.domain.Event;
import composable.domain.platform.event.domain.PublicationState;
import java.util.Objects;

public final class WithdrawEventService implements WithdrawEvent {

    private final EventRepository repository;

    public WithdrawEventService(EventRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public EventView withdraw(ExecutionContext context, String eventId) {
        Objects.requireNonNull(context, "context must not be null");

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }

        Event existing = repository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (existing.publicationState() == PublicationState.UNPUBLISHED) {
            throw new EventNotPublishedException(eventId);
        }
        if (existing.publicationState() == PublicationState.WITHDRAWN) {
            throw new EventWithdrawnException(eventId);
        }

        Event withdrawn = existing.withdraw();

        if (!repository.updatePublicationState(withdrawn, existing.publicationState())) {
            Event current = repository.findById(eventId).orElse(null);
            if (current != null && current.publicationState() == PublicationState.WITHDRAWN) {
                throw new EventWithdrawnException(eventId);
            }
            throw new EventNotPublishedException(eventId);
        }

        return EventViews.from(withdrawn);
    }
}
