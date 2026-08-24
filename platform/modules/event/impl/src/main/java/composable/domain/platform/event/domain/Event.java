package composable.domain.platform.event.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

public record Event(
        String id,
        String name,
        String slug,
        Instant startsAt,
        Instant endsAt,
        ZoneId timezone,
        PublicationState publicationState,
        Optional<String> owner) {

    public Event {
        id = requireText(id, "id");
        name = requireText(name, "name");
        slug = requireText(slug, "slug");
        Objects.requireNonNull(startsAt, "startsAt must not be null");
        Objects.requireNonNull(endsAt, "endsAt must not be null");
        Objects.requireNonNull(timezone, "timezone must not be null");
        Objects.requireNonNull(publicationState, "publicationState must not be null");
        Objects.requireNonNull(owner, "owner must not be null");

        if (owner.isPresent() && owner.get().isBlank()) {
            throw new IllegalArgumentException("owner must not be blank");
        }

        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
    }

    public Event(
            String id,
            String name,
            String slug,
            Instant startsAt,
            Instant endsAt,
            ZoneId timezone,
            String owner) {
        this(
                id,
                name,
                slug,
                startsAt,
                endsAt,
                timezone,
                PublicationState.UNPUBLISHED,
                Optional.of(requireText(owner, "owner")));
    }

    public Event publish() {
        if (publicationState == PublicationState.PUBLISHED) {
            throw new IllegalStateException("Event is already published");
        }
        if (publicationState == PublicationState.WITHDRAWN) {
            throw new IllegalStateException("Event is already withdrawn");
        }

        return new Event(
                id,
                name,
                slug,
                startsAt,
                endsAt,
                timezone,
                PublicationState.PUBLISHED,
                owner);
    }

    public Event withdraw() {
        if (publicationState == PublicationState.UNPUBLISHED) {
            throw new IllegalStateException("Event is not published");
        }
        if (publicationState == PublicationState.WITHDRAWN) {
            throw new IllegalStateException("Event is already withdrawn");
        }

        return new Event(
                id,
                name,
                slug,
                startsAt,
                endsAt,
                timezone,
                PublicationState.WITHDRAWN,
                owner);
    }

    public Event updateDefinition(
            String newName,
            String newSlug,
            Instant newStartsAt,
            Instant newEndsAt,
            ZoneId newTimezone) {
        if (publicationState == PublicationState.PUBLISHED) {
            throw new IllegalStateException("Event is already published");
        }
        if (publicationState == PublicationState.WITHDRAWN) {
            throw new IllegalStateException("Event is already withdrawn");
        }

        return new Event(
                id,
                newName,
                newSlug,
                newStartsAt,
                newEndsAt,
                newTimezone,
                publicationState,
                owner);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
