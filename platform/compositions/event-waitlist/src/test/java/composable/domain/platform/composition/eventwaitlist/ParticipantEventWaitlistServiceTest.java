package composable.domain.platform.composition.eventwaitlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventRegistrationAvailability;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.registration.api.FindRegistrationByRegistrantAndTarget;
import composable.domain.platform.registration.api.RegistrantReference;
import composable.domain.platform.registration.api.RegistrationView;
import composable.domain.platform.registration.api.TargetReference;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import composable.domain.platform.waitlist.api.FindWaitlistParticipation;
import composable.domain.platform.waitlist.api.JoinWaitlist;
import composable.domain.platform.waitlist.api.WaitlistEventReference;
import composable.domain.platform.waitlist.api.WaitlistParticipantReference;
import composable.domain.platform.waitlist.api.WaitlistParticipationView;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ParticipantEventWaitlistServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("event-waitlist-test"));
    private static final AuthenticatedActorReference ACTOR =
            new AuthenticatedActorReference("participant-a");

    @Test
    void joinsPublishedClosedEligibleEventIdempotently() {
        Map<Pair, WaitlistParticipationView> participations =
                new HashMap<>();
        ParticipantEventWaitlistService service = service(
                Optional.of(event(
                        EventPublicationState.PUBLISHED,
                        EventRegistrationAvailability.CLOSED)),
                Optional.empty(),
                participations);

        ParticipantEventWaitlistView first =
                service.join(CONTEXT, ACTOR, "event-a");
        ParticipantEventWaitlistView repeated =
                service.join(CONTEXT, ACTOR, "event-a");

        assertEquals(first, repeated);
        assertEquals(1, participations.size());
    }

    @Test
    void rejectsUnknownIneligibleAndRegistrationOccupiedJoin() {
        assertThrows(
                UnknownEventForWaitlistException.class,
                () -> service(
                                Optional.empty(),
                                Optional.empty(),
                                new HashMap<>())
                        .join(CONTEXT, ACTOR, "event-a"));

        assertThrows(
                EventNotPublishedForWaitlistException.class,
                () -> service(
                                Optional.of(event(
                                        EventPublicationState.UNPUBLISHED,
                                        EventRegistrationAvailability.CLOSED)),
                                Optional.empty(),
                                new HashMap<>())
                        .join(CONTEXT, ACTOR, "event-a"));

        assertThrows(
                EventWaitlistUnavailableException.class,
                () -> service(
                                Optional.of(event(
                                        EventPublicationState.PUBLISHED,
                                        EventRegistrationAvailability.OPEN)),
                                Optional.empty(),
                                new HashMap<>())
                        .join(CONTEXT, ACTOR, "event-a"));

        RegistrationView occupied = new RegistrationView(
                "registration-a",
                new RegistrantReference(
                        "participant",
                        ACTOR.reference()),
                new TargetReference("event", "event-a"));

        assertThrows(
                EventRegistrationExistsForWaitlistException.class,
                () -> service(
                                Optional.of(event(
                                        EventPublicationState.PUBLISHED,
                                        EventRegistrationAvailability.CLOSED)),
                                Optional.of(occupied),
                                new HashMap<>())
                        .join(CONTEXT, ACTOR, "event-a"));
    }

    @Test
    void privateRetrievalUsesAuthenticatedParticipantPairOnly() {
        Map<Pair, WaitlistParticipationView> participations =
                new HashMap<>();
        participations.put(
                new Pair(ACTOR.reference(), "event-a"),
                new WaitlistParticipationView(
                        "waitlist-a",
                        new WaitlistParticipantReference(
                                ACTOR.reference()),
                        new WaitlistEventReference("event-a")));

        ParticipantEventWaitlistService service = service(
                Optional.empty(),
                Optional.empty(),
                participations);

        assertEquals(
                "waitlist-a",
                service.findByEventId(
                                CONTEXT,
                                ACTOR,
                                "event-a")
                        .orElseThrow()
                        .waitlistParticipationId());

        assertTrue(service.findByEventId(
                        CONTEXT,
                        new AuthenticatedActorReference("participant-b"),
                        "event-a")
                .isEmpty());
    }

    private static ParticipantEventWaitlistService service(
            Optional<EventView> event,
            Optional<RegistrationView> registration,
            Map<Pair, WaitlistParticipationView> participations) {
        FindEvent findEvent = (context, eventId) -> event;
        FindRegistrationByRegistrantAndTarget findRegistration =
                (context, registrantReference, targetReference) ->
                        registration;
        AtomicInteger sequence = new AtomicInteger();
        JoinWaitlist join = (context, participantReference, eventReference) ->
                participations.computeIfAbsent(
                        new Pair(
                                participantReference.reference(),
                                eventReference.reference()),
                        ignored -> new WaitlistParticipationView(
                                "waitlist-" + sequence.incrementAndGet(),
                                participantReference,
                                eventReference));
        FindWaitlistParticipation find =
                (context, participantReference, eventReference) ->
                        Optional.ofNullable(participations.get(
                                new Pair(
                                        participantReference.reference(),
                                        eventReference.reference())));

        return new ParticipantEventWaitlistService(
                findEvent,
                findRegistration,
                join,
                find);
    }

    private static EventView event(
            EventPublicationState publicationState,
            EventRegistrationAvailability registrationAvailability) {
        return new EventView(
                "event-a",
                "Event",
                "event-a",
                Instant.parse("2026-09-01T08:00:00Z"),
                Instant.parse("2026-09-01T10:00:00Z"),
                ZoneId.of("Europe/Copenhagen"),
                publicationState,
                registrationAvailability);
    }

    private record Pair(
            String participantReference,
            String eventReference) {
    }
}
