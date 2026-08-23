package composable.domain.platform.event.api;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

public record DefineEventCommand(
        String eventId,
        String name,
        String slug,
        Instant startsAt,
        Instant endsAt,
        ZoneId timezone,
        EventOwnerReference owner) {

    public DefineEventCommand {
        Objects.requireNonNull(owner, "owner must not be null");
    }
}
