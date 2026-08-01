package composable.domain.platform.event.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import composable.domain.platform.event.api.DefineEventCommand;
import composable.domain.platform.event.api.EventView;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class DefineEventServiceTest {

    @Test
    void definesEventAndReturnsResultingState() {
        Instant startsAt = Instant.parse("2026-09-01T08:00:00Z");
        Instant endsAt = Instant.parse("2026-09-01T10:00:00Z");
        ZoneId timezone = ZoneId.of("Europe/Copenhagen");

        DefineEventCommand command = new DefineEventCommand(
                "event-1",
                "Platform Day",
                "platform-day",
                startsAt,
                endsAt,
                timezone);

        EventView result = new DefineEventService().define(command);

        assertEquals(
                new EventView(
                        "event-1",
                        "Platform Day",
                        "platform-day",
                        startsAt,
                        endsAt,
                        timezone),
                result);
    }
}
