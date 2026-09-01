package composable.domain.platform.composition.eventwaitlist;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import java.util.Optional;

@FunctionalInterface
public interface FindParticipantEventWaitlist {

    Optional<ParticipantEventWaitlistView> findByEventId(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            String eventId);
}
