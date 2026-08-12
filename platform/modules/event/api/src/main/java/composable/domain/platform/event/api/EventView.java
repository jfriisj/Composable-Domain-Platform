package composable.domain.platform.event.api;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

public record EventView(
        String eventId,
        String name,
        String slug,
        Instant startsAt,
        Instant endsAt,
        ZoneId timezone,
        EventPublicationState publicationState) {

    public EventView {
        Objects.requireNonNull(publicationState, "publicationState must not be null");
    }
}
