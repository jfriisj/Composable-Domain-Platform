package composable.domain.platform.event.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EventTest {

    private static final Instant START = Instant.parse("2026-09-01T08:00:00Z");
    private static final Instant END = Instant.parse("2026-09-01T10:00:00Z");
    private static final ZoneId TIMEZONE = ZoneId.of("Europe/Copenhagen");

    @Test
    void newEventIsUnpublished() {
        Event event = event();

        assertEquals(PublicationState.UNPUBLISHED, event.publicationState());
        assertEquals(Optional.of("organizer-1"), event.owner());
    }

    @Test
    void publishingPreservesDefinitionAndProducesPublishedState() {
        Event original = event();

        Event published = original.publish();

        assertNotSame(original, published);
        assertEquals(original.id(), published.id());
        assertEquals(original.name(), published.name());
        assertEquals(original.slug(), published.slug());
        assertEquals(original.startsAt(), published.startsAt());
        assertEquals(original.endsAt(), published.endsAt());
        assertEquals(original.timezone(), published.timezone());
        assertEquals(original.owner(), published.owner());
        assertEquals(PublicationState.UNPUBLISHED, original.publicationState());
        assertEquals(PublicationState.PUBLISHED, published.publicationState());
    }

    @Test
    void alreadyPublishedEventCannotPerformPublicationTransitionAgain() {
        Event published = event().publish();

        assertThrows(IllegalStateException.class, published::publish);
    }

    @Test
    void updatingDefinitionReplacesMutableFieldsPreservingIdOwnerAndPublicationState() {
        Event original = new Event(
                "event-1",
                "Platform Day",
                "platform-day",
                START,
                END,
                TIMEZONE,
                "organizer-1");

        Instant newStart = Instant.parse("2026-10-01T09:00:00Z");
        Instant newEnd = Instant.parse("2026-10-01T11:00:00Z");
        ZoneId newTz = ZoneId.of("Europe/Oslo");

        Event updated = original.updateDefinition(
                "Updated Platform Day",
                "updated-platform-day",
                newStart,
                newEnd,
                newTz);

        assertNotSame(original, updated);
        assertEquals("event-1", updated.id());
        assertEquals("Updated Platform Day", updated.name());
        assertEquals("updated-platform-day", updated.slug());
        assertEquals(newStart, updated.startsAt());
        assertEquals(newEnd, updated.endsAt());
        assertEquals(newTz, updated.timezone());
        assertEquals(PublicationState.UNPUBLISHED, updated.publicationState());
        assertEquals(Optional.of("organizer-1"), updated.owner());
    }

    @Test
    void publishedEventCannotBeUpdated() {
        Event published = new Event(
                "event-1",
                "Platform Day",
                "platform-day",
                START,
                END,
                TIMEZONE,
                "organizer-1").publish();

        assertThrows(
                IllegalStateException.class,
                () -> published.updateDefinition(
                        "Updated",
                        "updated",
                        START,
                        END,
                        TIMEZONE));
    }

    @Test
    void withdrawingPublishedEventPreservesDefinitionAndProducesWithdrawnState() {
        Event published = event().publish();

        Event withdrawn = published.withdraw();

        assertNotSame(published, withdrawn);
        assertEquals(published.id(), withdrawn.id());
        assertEquals(published.name(), withdrawn.name());
        assertEquals(published.slug(), withdrawn.slug());
        assertEquals(published.startsAt(), withdrawn.startsAt());
        assertEquals(published.endsAt(), withdrawn.endsAt());
        assertEquals(published.timezone(), withdrawn.timezone());
        assertEquals(published.owner(), withdrawn.owner());
        assertEquals(PublicationState.PUBLISHED, published.publicationState());
        assertEquals(PublicationState.WITHDRAWN, withdrawn.publicationState());
    }

    @Test
    void unpublishedEventCannotBeWithdrawn() {
        Event unpublished = event();

        assertThrows(IllegalStateException.class, unpublished::withdraw);
    }

    @Test
    void alreadyWithdrawnEventCannotBeWithdrawnAgain() {
        Event withdrawn = event().publish().withdraw();

        assertThrows(IllegalStateException.class, withdrawn::withdraw);
    }

    @Test
    void withdrawnEventCannotBePublished() {
        Event withdrawn = event().publish().withdraw();

        assertThrows(IllegalStateException.class, withdrawn::publish);
    }

    @Test
    void withdrawnEventCannotBeUpdated() {
        Event withdrawn = event().publish().withdraw();

        assertThrows(
                IllegalStateException.class,
                () -> withdrawn.updateDefinition(
                        "Updated",
                        "updated",
                        START,
                        END,
                        TIMEZONE));
    }

    @Test
    void restoresLegacyRowWithoutOwnerReference() {
        Event legacy = new Event(
                "event-legacy",
                "Legacy Event",
                "legacy-event",
                START,
                END,
                TIMEZONE,
                PublicationState.UNPUBLISHED,
                Optional.empty());

        assertTrue(legacy.owner().isEmpty());
        assertEquals("event-legacy", legacy.id());
        assertEquals(PublicationState.UNPUBLISHED, legacy.publicationState());
    }

    @Test
    void registrationAvailabilityStartsOpenAndChangesOnlyWhilePublished() {
        Event unpublished = event();

        assertEquals(RegistrationAvailability.OPEN, unpublished.registrationAvailability());
        assertThrows(
                IllegalStateException.class,
                () -> unpublished.setRegistrationAvailability(
                        RegistrationAvailability.CLOSED));

        Event published = unpublished.publish();
        Event closed = published.setRegistrationAvailability(
                RegistrationAvailability.CLOSED);
        Event repeatedClosed = closed.setRegistrationAvailability(
                RegistrationAvailability.CLOSED);
        Event reopened = closed.setRegistrationAvailability(
                RegistrationAvailability.OPEN);

        assertEquals(RegistrationAvailability.OPEN, published.registrationAvailability());
        assertEquals(RegistrationAvailability.CLOSED, closed.registrationAvailability());
        assertSame(closed, repeatedClosed);
        assertEquals(RegistrationAvailability.OPEN, reopened.registrationAvailability());

        Event withdrawn = closed.withdraw();
        assertThrows(
                IllegalStateException.class,
                () -> withdrawn.setRegistrationAvailability(
                        RegistrationAvailability.OPEN));
    }

    @Test
    void rejectsBlankRequiredTextFields() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event(" ", "Platform Day", "platform-day", START, END, TIMEZONE, "organizer-1")),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event("event-1", " ", "platform-day", START, END, TIMEZONE, "organizer-1")),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event("event-1", "Platform Day", " ", START, END, TIMEZONE, "organizer-1")),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event("event-1", "Platform Day", "platform-day", START, END, TIMEZONE, " ")));
    }

    @Test
    void rejectsNullRequiredFields() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event(null, "Platform Day", "platform-day", START, END, TIMEZONE, "organizer-1")),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event("event-1", null, "platform-day", START, END, TIMEZONE, "organizer-1")),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event("event-1", "Platform Day", null, START, END, TIMEZONE, "organizer-1")),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new Event("event-1", "Platform Day", "platform-day", null, END, TIMEZONE, "organizer-1")),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new Event("event-1", "Platform Day", "platform-day", START, null, TIMEZONE, "organizer-1")),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new Event("event-1", "Platform Day", "platform-day", START, END, null, "organizer-1")),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event("event-1", "Platform Day", "platform-day", START, END, TIMEZONE, (String) null)),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new Event("event-1", "Platform Day", "platform-day", START, END, TIMEZONE, PublicationState.UNPUBLISHED, null)));
    }

    @Test
    void rejectsEndThatIsNotAfterStart() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event("event-1", "Platform Day", "platform-day", START, START, TIMEZONE, "organizer-1")),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event(
                                "event-1",
                                "Platform Day",
                                "platform-day",
                                START,
                                START.minusSeconds(1),
                                TIMEZONE,
                                "organizer-1")));
    }

    private static Event event() {
        return new Event(
                "event-1",
                "Platform Day",
                "platform-day",
                START,
                END,
                TIMEZONE,
                "organizer-1");
    }
}
