package composable.domain.platform.composition.eventregistration;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import java.util.Optional;

public interface CancelParticipantEventRegistration {

    Optional<ParticipantEventRegistrationView> cancel(
            ExecutionContext context,
            AuthenticatedActorReference actorReference,
            String registrationId);
}
