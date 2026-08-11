package composable.domain.platform.composition.eventregistration;

import composable.domain.platform.core.execution.ExecutionContext;
import java.util.Optional;

public interface FindEventRegistration {

    Optional<EventRegistrationView> findById(
            ExecutionContext context,
            String registrationId);
}
