package composable.domain.platform.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import composable.domain.platform.composition.eventwaitlist.EventNotPublishedForWaitlistException;
import composable.domain.platform.composition.eventwaitlist.EventRegistrationExistsForWaitlistException;
import composable.domain.platform.composition.eventwaitlist.EventWaitlistUnavailableException;
import composable.domain.platform.composition.eventwaitlist.FindParticipantEventWaitlist;
import composable.domain.platform.composition.eventwaitlist.JoinParticipantEventWaitlist;
import composable.domain.platform.composition.eventwaitlist.ParticipantEventWaitlistView;
import composable.domain.platform.composition.eventwaitlist.UnknownEventForWaitlistException;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EventWaitlistHttpAdapterTest {

    private static final AuthenticatedActorReference ACTOR =
            new AuthenticatedActorReference("participant-a");

    @Test
    void mapsJoinBusinessFailuresToAcceptedStatusAndCode() {
        assertFailure(
                new composable.domain.platform.composition.eventwaitlist.InvalidEventWaitlistRequestException(),
                400,
                "INVALID_REQUEST");
        assertFailure(
                new UnknownEventForWaitlistException(),
                404,
                "EVENT_NOT_FOUND");
        assertFailure(
                new EventNotPublishedForWaitlistException(),
                409,
                "EVENT_NOT_PUBLISHED");
        assertFailure(
                new EventWaitlistUnavailableException(),
                409,
                "EVENT_WAITLIST_UNAVAILABLE");
        assertFailure(
                new EventRegistrationExistsForWaitlistException(),
                409,
                "EVENT_REGISTRATION_EXISTS");
    }

    @Test
    void privateAbsenceMapsToAcceptedNotFound() {
        FindParticipantEventWaitlist find =
                (context, actorReference, eventId) -> Optional.empty();
        EventWaitlistHttpAdapter adapter =
                new EventWaitlistHttpAdapter(
                        successfulJoin(),
                        find,
                        () -> ACTOR);

        try {
            adapter.findEventWaitlistParticipation(
                    "event-a",
                    "corr-private");
        } catch (EventWaitlistHttpException exception) {
            assertEquals(404, exception.status().value());
            assertEquals(
                    "WAITLIST_PARTICIPATION_NOT_FOUND",
                    exception.code().name());
            return;
        }

        throw new AssertionError("Expected private not-found mapping");
    }

    private static void assertFailure(
            RuntimeException failure,
            int status,
            String code) {
        JoinParticipantEventWaitlist join =
                (context, actorReference, eventId) -> {
                    throw failure;
                };
        FindParticipantEventWaitlist find =
                (context, actorReference, eventId) -> Optional.empty();
        EventWaitlistHttpAdapter adapter =
                new EventWaitlistHttpAdapter(join, find, () -> ACTOR);

        try {
            adapter.joinEventWaitlist(
                    "event-a",
                    "corr-failure");
        } catch (EventWaitlistHttpException exception) {
            assertEquals(status, exception.status().value());
            assertEquals(code, exception.code().name());
            return;
        }

        throw new AssertionError("Expected mapped Event-Waitlist failure");
    }

    private static JoinParticipantEventWaitlist successfulJoin() {
        return (context, actorReference, eventId) ->
                new ParticipantEventWaitlistView(
                        "waitlist-a",
                        eventId);
    }
}
