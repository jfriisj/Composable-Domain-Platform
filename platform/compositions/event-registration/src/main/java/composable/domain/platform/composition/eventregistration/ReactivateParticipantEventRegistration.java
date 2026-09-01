package composable.domain.platform.composition.eventregistration;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import java.util.Optional;

public interface ReactivateParticipantEventRegistration {

    Optional<ParticipantEventRegistrationView> reactivate(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            String registrationId);
}
