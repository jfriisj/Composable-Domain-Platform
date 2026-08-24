package composable.domain.platform.registration.application;

import composable.domain.platform.registration.domain.Registration;
import composable.domain.platform.registration.domain.TargetReference;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository {

    boolean addIfAbsent(Registration registration);

    Optional<Registration> findById(String registrationId);

    List<Registration> findByTarget(TargetReference targetReference);

    void updateLifecycle(Registration registration);
}
