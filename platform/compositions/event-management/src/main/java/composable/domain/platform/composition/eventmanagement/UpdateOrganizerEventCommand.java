package composable.domain.platform.composition.eventmanagement;

import java.time.Instant;
import java.time.ZoneId;

public record UpdateOrganizerEventCommand(
        String eventId,
        String name,
        String slug,
        Instant startsAt,
        Instant endsAt,
        ZoneId timezone) {
}
