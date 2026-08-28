package composable.domain.platform.event.application;

import composable.domain.platform.event.domain.Event;
import composable.domain.platform.event.domain.PublicationState;
import java.util.Collection;
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

    @Override
    public boolean updatePublicationState(Event event, PublicationState expectedState) {
        Event current = events.get(event.id());
        if (current == null || current.publicationState() != expectedState) {
            return false;
        }

        events.put(event.id(), event);
        return true;
    }

    @Override
    public boolean updateRegistrationAvailability(Event event) {
        Event current = events.get(event.id());
        if (current == null || current.publicationState() != PublicationState.PUBLISHED) {
            return false;
        }

        events.put(event.id(), event);
        return true;
    }

    @Override
    public boolean updateDefinition(Event event) {
        Event current = events.get(event.id());
        if (current == null || current.publicationState() != PublicationState.UNPUBLISHED) {
            return false;
        }

        events.put(event.id(), event);
        return true;
    }

    @Override
    public Collection<Event> findPublished() {
        return events.values().stream()
                .filter(event -> event.publicationState() == PublicationState.PUBLISHED)
                .toList();
    }
}
