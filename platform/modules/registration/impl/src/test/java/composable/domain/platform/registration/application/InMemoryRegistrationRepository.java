package composable.domain.platform.registration.application;

import composable.domain.platform.registration.domain.Registration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class InMemoryRegistrationRepository implements RegistrationRepository {

    private final Map<String, Registration> registrations = new HashMap<>();
    private final Set<ReferencePair> referencePairs = new HashSet<>();

    @Override
    public synchronized boolean addIfAbsent(Registration registration) {
        ReferencePair pair = ReferencePair.from(registration);

        if (registrations.containsKey(registration.id()) || referencePairs.contains(pair)) {
            return false;
        }

        registrations.put(registration.id(), registration);
        referencePairs.add(pair);
        return true;
    }

    @Override
    public synchronized Optional<Registration> findById(String registrationId) {
        return Optional.ofNullable(registrations.get(registrationId));
    }

    @Override
    public synchronized void updateLifecycle(Registration registration) {
        Registration existing = registrations.get(registration.id());

        if (existing == null) {
            throw new IllegalStateException(
                    "Expected Registration to exist before lifecycle update");
        }

        if (!ReferencePair.from(existing).equals(ReferencePair.from(registration))) {
            throw new IllegalArgumentException(
                    "Registration lifecycle update must preserve registrant and target references");
        }

        registrations.put(registration.id(), registration);
    }

    private record ReferencePair(
            String registrantNamespace,
            String registrantReference,
            String targetNamespace,
            String targetReference) {

        static ReferencePair from(Registration registration) {
            return new ReferencePair(
                    registration.registrantReference().namespace(),
                    registration.registrantReference().reference(),
                    registration.targetReference().namespace(),
                    registration.targetReference().reference());
        }
    }
}
