package composable.domain.platform.composition.eventregistration;

import composable.domain.platform.core.execution.ExecutionContext;
import java.util.Optional;

public interface FindParticipantEventRegistration {

    Optional<ParticipantEventRegistrationView> findById(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            String registrationId);
}
