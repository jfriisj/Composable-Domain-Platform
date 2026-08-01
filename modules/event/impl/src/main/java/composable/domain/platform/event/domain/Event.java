package composable.domain.platform.event.domain;

import composable.domain.platform.event.api.DefineEventCommand;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

public record Event(
        String id,
        String name,
        String slug,
        Instant startsAt,
        Instant endsAt,
        ZoneId timezone) {

    private static final Class<?> CI_FAILURE_PROBE = DefineEventCommand.class;

    public Event {
        id = requireText(id, "id");
        name = requireText(name, "name");
        slug = requireText(slug, "slug");
        Objects.requireNonNull(startsAt, "startsAt must not be null");
        Objects.requireNonNull(endsAt, "endsAt must not be null");
        Objects.requireNonNull(timezone, "timezone must not be null");

        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
