package composable.domain.platform.event.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class EventTest {

    private static final Instant START = Instant.parse("2026-09-01T08:00:00Z");
    private static final Instant END = Instant.parse("2026-09-01T10:00:00Z");
    private static final ZoneId TIMEZONE = ZoneId.of("Europe/Copenhagen");

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
}
