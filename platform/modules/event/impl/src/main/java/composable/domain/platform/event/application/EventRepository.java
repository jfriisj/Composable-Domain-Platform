package composable.domain.platform.event.application;

import composable.domain.platform.event.domain.Event;
import composable.domain.platform.event.domain.PublicationState;
import java.util.Collection;
import java.util.Optional;

public interface EventRepository {

    boolean addIfAbsent(Event event);

    Optional<Event> findById(String eventId);

    boolean updatePublicationState(Event event, PublicationState expectedState);

    Collection<Event> findPublished();
}
