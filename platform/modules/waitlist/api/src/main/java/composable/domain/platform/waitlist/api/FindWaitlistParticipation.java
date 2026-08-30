package composable.domain.platform.waitlist.api;

import composable.domain.platform.core.execution.ExecutionContext;
import java.util.Optional;

@FunctionalInterface
public interface FindWaitlistParticipation {

    Optional<WaitlistParticipationView> findByParticipantAndEvent(
            ExecutionContext context,
            WaitlistParticipantReference participantReference,
            WaitlistEventReference eventReference);
}
