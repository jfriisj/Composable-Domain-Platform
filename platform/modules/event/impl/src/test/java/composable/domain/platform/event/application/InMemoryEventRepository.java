package composable.domain.platform.event.application;

import composable.domain.platform.event.domain.Event;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class InMemoryEventRepository implements EventRepository {

    private final Map<String, Event> events = new HashMap<>();

    @Override
    public boolean addIfAbsent(Event event) {
        return events.putIfAbsent(event.id(), event) == null;
    }

    @Override
    public Optional<Event> findById(String eventId) {
        return Optional.ofNullable(events.get(eventId));
    }
}
