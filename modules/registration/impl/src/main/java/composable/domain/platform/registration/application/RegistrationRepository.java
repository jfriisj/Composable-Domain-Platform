package composable.domain.platform.registration.application;

import composable.domain.platform.registration.domain.Registration;
import java.util.Optional;

public interface RegistrationRepository {

    boolean addIfAbsent(Registration registration);

    Optional<Registration> findById(String registrationId);
}
