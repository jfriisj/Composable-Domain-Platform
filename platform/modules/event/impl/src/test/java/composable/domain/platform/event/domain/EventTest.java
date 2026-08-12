package composable.domain.platform.event.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class EventTest {

    private static final Instant START = Instant.parse("2026-09-01T08:00:00Z");
    private static final Instant END = Instant.parse("2026-09-01T10:00:00Z");
    private static final ZoneId TIMEZONE = ZoneId.of("Europe/Copenhagen");

    @Test
    void newEventIsUnpublished() {
        Event event = event();

        assertEquals(PublicationState.UNPUBLISHED, event.publicationState());
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
        assertEquals(PublicationState.UNPUBLISHED, original.publicationState());
        assertEquals(PublicationState.PUBLISHED, published.publicationState());
    }

    @Test
    void alreadyPublishedEventCannotPerformPublicationTransitionAgain() {
        Event published = event().publish();

        assertThrows(IllegalStateException.class, published::publish);
    }

    @Test
    void rejectsBlankRequiredTextFields() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event(" ", "Platform Day", "platform-day", START, END, TIMEZONE)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event("event-1", " ", "platform-day", START, END, TIMEZONE)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event("event-1", "Platform Day", " ", START, END, TIMEZONE)));
    }

    @Test
    void rejectsNullRequiredFields() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event(null, "Platform Day", "platform-day", START, END, TIMEZONE)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event("event-1", null, "platform-day", START, END, TIMEZONE)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event("event-1", "Platform Day", null, START, END, TIMEZONE)),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new Event("event-1", "Platform Day", "platform-day", null, END, TIMEZONE)),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new Event("event-1", "Platform Day", "platform-day", START, null, TIMEZONE)),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new Event("event-1", "Platform Day", "platform-day", START, END, null)));
    }

    @Test
    void rejectsEndThatIsNotAfterStart() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event("event-1", "Platform Day", "platform-day", START, START, TIMEZONE)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new Event(
                                "event-1",
                                "Platform Day",
                                "platform-day",
                                START,
                                START.minusSeconds(1),
                                TIMEZONE)));
    }

    private static Event event() {
        return new Event(
                "event-1",
                "Platform Day",
                "platform-day",
                START,
                END,
                TIMEZONE);
    }
}
