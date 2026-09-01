package composable.domain.platform.registration.application;

import composable.domain.platform.registration.domain.RegistrantReference;
import composable.domain.platform.registration.domain.Registration;
import composable.domain.platform.registration.domain.RegistrationLifecycle;
import composable.domain.platform.registration.domain.TargetReference;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository {

    boolean addIfAbsent(Registration registration);

    Optional<Registration> findById(String registrationId);

    Optional<Registration> findByRegistrantAndTarget(
            RegistrantReference registrantReference,
            TargetReference targetReference);

    List<Registration> findByTarget(TargetReference targetReference);

    boolean updateLifecycle(
            Registration registration,
            RegistrationLifecycle expectedLifecycle);
}
