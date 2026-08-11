package composable.domain.platform.event.application;

import composable.domain.platform.event.domain.Event;
import java.util.Optional;

public interface EventRepository {

    boolean addIfAbsent(Event event);

    Optional<Event> findById(String eventId);
}
