package composable.domain.platform.waitlist.application;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.waitlist.api.FindWaitlistParticipation;
import composable.domain.platform.waitlist.api.WaitlistEventReference;
import composable.domain.platform.waitlist.api.WaitlistParticipantReference;
import composable.domain.platform.waitlist.api.WaitlistParticipationView;
import java.util.Objects;
import java.util.Optional;

public final class FindWaitlistParticipationService
        implements FindWaitlistParticipation {

    private final WaitlistParticipationRepository repository;

    public FindWaitlistParticipationService(
            WaitlistParticipationRepository repository) {
        this.repository =
                Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Optional<WaitlistParticipationView> findByParticipantAndEvent(
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

        return repository.findByParticipantAndEvent(
                        participantReference.reference(),
                        eventReference.reference())
                .map(JoinWaitlistService::toView);
    }
}
