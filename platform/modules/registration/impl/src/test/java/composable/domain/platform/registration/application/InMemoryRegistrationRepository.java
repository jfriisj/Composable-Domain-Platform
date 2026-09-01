package composable.domain.platform.registration.application;

import composable.domain.platform.registration.domain.RegistrantReference;
import composable.domain.platform.registration.domain.Registration;
import composable.domain.platform.registration.domain.RegistrationLifecycle;
import composable.domain.platform.registration.domain.TargetReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class InMemoryRegistrationRepository
        implements RegistrationRepository {

    private final Map<String, Registration> registrations =
            new HashMap<>();
    private final Set<ReferencePair> referencePairs =
            new HashSet<>();

    @Override
    public synchronized boolean addIfAbsent(
            Registration registration) {
        ReferencePair pair = ReferencePair.from(registration);

        if (registrations.containsKey(registration.id())
                || referencePairs.contains(pair)) {
            return false;
        }

        registrations.put(registration.id(), registration);
        referencePairs.add(pair);
        return true;
    }

    @Override
    public synchronized Optional<Registration> findById(
            String registrationId) {
        return Optional.ofNullable(registrations.get(registrationId));
    }

    @Override
    public synchronized Optional<Registration> findByRegistrantAndTarget(
            RegistrantReference registrantReference,
            TargetReference targetReference) {
        Objects.requireNonNull(
                registrantReference,
                "registrantReference must not be null");
        Objects.requireNonNull(
                targetReference,
                "targetReference must not be null");

        return registrations.values().stream()
                .filter(registration ->
                        registration.registrantReference().namespace()
                                        .equals(registrantReference.namespace())
                                && registration.registrantReference().reference()
                                        .equals(registrantReference.reference())
                                && registration.targetReference().namespace()
                                        .equals(targetReference.namespace())
                                && registration.targetReference().reference()
                                        .equals(targetReference.reference()))
                .findFirst();
    }

    @Override
    public synchronized List<Registration> findByTarget(
            TargetReference targetReference) {
        Objects.requireNonNull(
                targetReference,
                "targetReference must not be null");
        return registrations.values().stream()
                .filter(registration ->
                        registration.targetReference().namespace()
                                        .equals(targetReference.namespace())
                                && registration.targetReference().reference()
                                        .equals(targetReference.reference()))
                .toList();
    }

    @Override
    public synchronized boolean updateLifecycle(
            Registration registration,
            RegistrationLifecycle expectedLifecycle) {
        Registration existing = registrations.get(registration.id());

        if (existing == null) {
            return false;
        }

        if (!ReferencePair.from(existing)
                .equals(ReferencePair.from(registration))) {
            throw new IllegalArgumentException(
                    "Registration lifecycle update must preserve registrant and target references");
        }

        if (existing.lifecycle() != expectedLifecycle) {
            return false;
        }

        registrations.put(registration.id(), registration);
        return true;
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
