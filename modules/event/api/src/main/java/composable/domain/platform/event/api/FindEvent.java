package composable.domain.platform.event.api;

import java.util.Optional;

public interface FindEvent {

    Optional<EventView> findById(String eventId);
}
