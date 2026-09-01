package composable.domain.platform.composition.eventwaitlist;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventRegistrationAvailability;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.registration.api.FindRegistrationByRegistrantAndTarget;
import composable.domain.platform.registration.api.RegistrantReference;
import composable.domain.platform.registration.api.TargetReference;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import composable.domain.platform.waitlist.api.FindWaitlistParticipation;
import composable.domain.platform.waitlist.api.JoinWaitlist;
import composable.domain.platform.waitlist.api.WaitlistEventReference;
import composable.domain.platform.waitlist.api.WaitlistParticipantReference;
import composable.domain.platform.waitlist.api.WaitlistParticipationView;
import java.util.Objects;
import java.util.Optional;

public final class ParticipantEventWaitlistService
        implements JoinParticipantEventWaitlist,
                FindParticipantEventWaitlist {

    static final String PARTICIPANT_NAMESPACE = "participant";
    static final String EVENT_NAMESPACE = "event";

    private final FindEvent findEvent;
    private final FindRegistrationByRegistrantAndTarget
            findRegistrationByRegistrantAndTarget;
    private final JoinWaitlist joinWaitlist;
    private final FindWaitlistParticipation findWaitlistParticipation;

    public ParticipantEventWaitlistService(
            FindEvent findEvent,
            FindRegistrationByRegistrantAndTarget
                    findRegistrationByRegistrantAndTarget,
            JoinWaitlist joinWaitlist,
            FindWaitlistParticipation findWaitlistParticipation) {
        this.findEvent =
                Objects.requireNonNull(
                        findEvent,
                        "findEvent must not be null");
        this.findRegistrationByRegistrantAndTarget =
                Objects.requireNonNull(
                        findRegistrationByRegistrantAndTarget,
                        "findRegistrationByRegistrantAndTarget must not be null");
        this.joinWaitlist =
                Objects.requireNonNull(
                        joinWaitlist,
                        "joinWaitlist must not be null");
        this.findWaitlistParticipation =
                Objects.requireNonNull(
                        findWaitlistParticipation,
                        "findWaitlistParticipation must not be null");
    }

    @Override
    public ParticipantEventWaitlistView join(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            String eventId) {
        Objects.requireNonNull(context, "context must not be null");
        requireActor(actorReference);
        requireEventId(eventId);

        EventView event = findEvent.findById(context, eventId)
                .orElseThrow(UnknownEventForWaitlistException::new);

        if (event.publicationState()
                != EventPublicationState.PUBLISHED) {
            throw new EventNotPublishedForWaitlistException();
        }
        if (event.registrationAvailability()
                != EventRegistrationAvailability.CLOSED) {
            throw new EventWaitlistUnavailableException();
        }

        RegistrantReference registrantReference =
                new RegistrantReference(
                        PARTICIPANT_NAMESPACE,
                        actorReference.reference());
        TargetReference targetReference =
                new TargetReference(EVENT_NAMESPACE, eventId);

        if (findRegistrationByRegistrantAndTarget
                .findByRegistrantAndTarget(
                        context,
                        registrantReference,
                        targetReference)
                .isPresent()) {
            throw new EventRegistrationExistsForWaitlistException();
        }

        return toView(joinWaitlist.join(
                context,
                participantReference(actorReference),
                new WaitlistEventReference(eventId)));
    }

    @Override
    public Optional<ParticipantEventWaitlistView> findByEventId(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            String eventId) {
        Objects.requireNonNull(context, "context must not be null");
        requireActor(actorReference);
        requireEventId(eventId);

        return findWaitlistParticipation.findByParticipantAndEvent(
                        context,
                        participantReference(actorReference),
                        new WaitlistEventReference(eventId))
                .map(ParticipantEventWaitlistService::toView);
    }

    private static WaitlistParticipantReference participantReference(
            AuthenticatedActorReference actorReference) {
        return new WaitlistParticipantReference(
                actorReference.reference());
    }

    private static void requireActor(
            AuthenticatedActorReference actorReference) {
        if (actorReference == null
                || actorReference.reference() == null
                || actorReference.reference().isBlank()) {
            throw new InvalidEventWaitlistRequestException();
        }
    }

    private static void requireEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new InvalidEventWaitlistRequestException();
        }
    }

    private static ParticipantEventWaitlistView toView(
            WaitlistParticipationView participation) {
        return new ParticipantEventWaitlistView(
                participation.waitlistParticipationId(),
                participation.eventReference().reference());
    }
}
