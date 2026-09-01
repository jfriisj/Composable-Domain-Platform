package composable.domain.platform.waitlist.api;

import composable.domain.platform.core.execution.ExecutionContext;

@FunctionalInterface
public interface JoinWaitlist {

    WaitlistParticipationView join(
            ExecutionContext context,
            WaitlistParticipantReference participantReference,
            WaitlistEventReference eventReference);
}
