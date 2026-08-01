package composable.domain.platform.event.application;

import composable.domain.platform.event.api.DefineEvent;
import composable.domain.platform.event.api.DefineEventCommand;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.domain.Event;
import java.util.Objects;

final class DefineEventService implements DefineEvent {

    @Override
    public EventView define(DefineEventCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Event event = new Event(
                command.eventId(),
                command.name(),
                command.slug(),
                command.startsAt(),
                command.endsAt(),
                command.timezone());

        return new EventView(
                event.id(),
                event.name(),
                event.slug(),
                event.startsAt(),
                event.endsAt(),
                event.timezone());
    }
}
