package composable.domain.platform.waitlist.application;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.waitlist.api.JoinWaitlist;
import composable.domain.platform.waitlist.api.WaitlistEventReference;
import composable.domain.platform.waitlist.api.WaitlistParticipantReference;
import composable.domain.platform.waitlist.api.WaitlistParticipationView;
import composable.domain.platform.waitlist.domain.WaitlistParticipation;
import java.util.Objects;
import java.util.UUID;

public final class JoinWaitlistService implements JoinWaitlist {

    private final WaitlistParticipationRepository repository;

    public JoinWaitlistService(WaitlistParticipationRepository repository) {
        this.repository =
                Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public WaitlistParticipationView join(
            ExecutionContext context,
            WaitlistParticipantReference participantReference,
            WaitlistEventReference eventReference) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(
                participantReference,
                "participantReference must not be null");
        Objects.requireNonNull(
                eventReference,
                "eventReference must not be null");

        WaitlistParticipation candidate = new WaitlistParticipation(
                UUID.randomUUID().toString(),
                participantReference.reference(),
                eventReference.reference());

        if (repository.addIfAbsent(candidate)) {
            return toView(candidate);
        }

        return repository.findByParticipantAndEvent(
                        participantReference.reference(),
                        eventReference.reference())
                .map(JoinWaitlistService::toView)
                .orElseThrow(() -> new IllegalStateException(
                        "Expected existing Waitlist participation after uniqueness conflict"));
    }

    static WaitlistParticipationView toView(
            WaitlistParticipation participation) {
        return new WaitlistParticipationView(
                participation.id(),
                new WaitlistParticipantReference(
                        participation.participantReference()),
                new WaitlistEventReference(participation.eventReference()));
    }
}
