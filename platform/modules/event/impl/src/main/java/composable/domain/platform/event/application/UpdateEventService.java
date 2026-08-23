package composable.domain.platform.event.application;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventAlreadyPublishedException;
import composable.domain.platform.event.api.EventNotFoundException;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.InvalidEventDefinitionException;
import composable.domain.platform.event.api.UpdateEvent;
import composable.domain.platform.event.api.UpdateEventCommand;
import composable.domain.platform.event.domain.Event;
import composable.domain.platform.event.domain.PublicationState;
import java.util.Objects;

public final class UpdateEventService implements UpdateEvent {

    private final EventRepository repository;

    public UpdateEventService(EventRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public EventView update(ExecutionContext context, UpdateEventCommand command) {
        Objects.requireNonNull(context, "context must not be null");

        if (command == null || command.eventId() == null || command.eventId().isBlank()) {
            throw new InvalidEventDefinitionException();
        }

        Event existing = repository.findById(command.eventId())
                .orElseThrow(() -> new EventNotFoundException(command.eventId()));

        if (existing.publicationState() == PublicationState.PUBLISHED) {
            throw new EventAlreadyPublishedException(command.eventId());
        }

        Event updated;
        try {
            updated = existing.updateDefinition(
                    command.name(),
                    command.slug(),
                    command.startsAt(),
                    command.endsAt(),
                    command.timezone());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidEventDefinitionException();
        }

        if (!repository.updateDefinition(updated)) {
            throw new EventAlreadyPublishedException(command.eventId());
        }

        return EventViews.from(updated);
    }
}
