package composable.domain.platform.event.application;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.DefineEvent;
import composable.domain.platform.event.api.DefineEventCommand;
import composable.domain.platform.event.api.EventAlreadyDefinedException;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.InvalidEventDefinitionException;
import composable.domain.platform.event.domain.Event;
import java.util.Objects;

public final class DefineEventService implements DefineEvent {

    private final EventRepository repository;

    public DefineEventService(EventRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public EventView define(ExecutionContext context, DefineEventCommand command) {
        Objects.requireNonNull(context, "context must not be null");

        Event event = createEvent(command);

        if (!repository.addIfAbsent(event)) {
            throw new EventAlreadyDefinedException(event.id());
        }

        return EventViews.from(event);
    }

    private static Event createEvent(DefineEventCommand command) {
        if (command == null || command.owner() == null) {
            throw new InvalidEventDefinitionException();
        }

        try {
            return new Event(
                    command.eventId(),
                    command.name(),
                    command.slug(),
                    command.startsAt(),
                    command.endsAt(),
                    command.timezone(),
                    command.owner().reference());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidEventDefinitionException();
        }
    }
}
