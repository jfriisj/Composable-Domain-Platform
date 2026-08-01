package composable.domain.platform.event.api;

import java.time.Instant;
import java.time.ZoneId;

public record DefineEventCommand(
        String eventId,
        String name,
        String slug,
        Instant startsAt,
        Instant endsAt,
        ZoneId timezone) {
}
