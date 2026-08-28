package composable.domain.platform.event.api;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

public record EventView(
        String eventId,
        String name,
        String slug,
        Instant startsAt,
        Instant endsAt,
        ZoneId timezone,
        EventPublicationState publicationState,
        EventRegistrationAvailability registrationAvailability,
        Optional<EventOwnerReference> owner) {

    public EventView {
        Objects.requireNonNull(publicationState, "publicationState must not be null");
        Objects.requireNonNull(
                registrationAvailability,
                "registrationAvailability must not be null");
        Objects.requireNonNull(owner, "owner must not be null");
    }

    public EventView(
            String eventId,
            String name,
            String slug,
            Instant startsAt,
            Instant endsAt,
            ZoneId timezone,
            EventPublicationState publicationState) {
        this(
                eventId,
                name,
                slug,
                startsAt,
                endsAt,
                timezone,
                publicationState,
                EventRegistrationAvailability.OPEN,
                Optional.empty());
    }

    public EventView(
            String eventId,
            String name,
            String slug,
            Instant startsAt,
            Instant endsAt,
            ZoneId timezone,
            EventPublicationState publicationState,
            Optional<EventOwnerReference> owner) {
        this(
                eventId,
                name,
                slug,
                startsAt,
                endsAt,
                timezone,
                publicationState,
                EventRegistrationAvailability.OPEN,
                owner);
    }

    public EventView(
            String eventId,
            String name,
            String slug,
            Instant startsAt,
            Instant endsAt,
            ZoneId timezone,
            EventPublicationState publicationState,
            EventOwnerReference owner) {
        this(
                eventId,
                name,
                slug,
                startsAt,
                endsAt,
                timezone,
                publicationState,
                EventRegistrationAvailability.OPEN,
                Optional.ofNullable(owner));
    }

    public EventView(
            String eventId,
            String name,
            String slug,
            Instant startsAt,
            Instant endsAt,
            ZoneId timezone,
            EventPublicationState publicationState,
            EventRegistrationAvailability registrationAvailability) {
        this(
                eventId,
                name,
                slug,
                startsAt,
                endsAt,
                timezone,
                publicationState,
                registrationAvailability,
                Optional.empty());
    }

    public EventView(
            String eventId,
            String name,
            String slug,
            Instant startsAt,
            Instant endsAt,
            ZoneId timezone,
            EventPublicationState publicationState,
            EventRegistrationAvailability registrationAvailability,
            EventOwnerReference owner) {
        this(
                eventId,
                name,
                slug,
                startsAt,
                endsAt,
                timezone,
                publicationState,
                registrationAvailability,
                Optional.ofNullable(owner));
    }
}
