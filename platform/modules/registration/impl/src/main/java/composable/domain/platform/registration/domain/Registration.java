package composable.domain.platform.registration.domain;

import java.util.Objects;

public record Registration(
        String id,
        RegistrantReference registrantReference,
        TargetReference targetReference,
        RegistrationLifecycle lifecycle) {

    public Registration {
        id = requireText(id, "id");
        Objects.requireNonNull(registrantReference, "registrantReference must not be null");
        Objects.requireNonNull(targetReference, "targetReference must not be null");
        Objects.requireNonNull(lifecycle, "lifecycle must not be null");
    }

    public Registration(
            String id,
            RegistrantReference registrantReference,
            TargetReference targetReference) {
        this(id, registrantReference, targetReference, RegistrationLifecycle.ACTIVE);
    }

    public Registration cancel() {
        if (lifecycle == RegistrationLifecycle.CANCELLED) {
            return this;
        }

        return new Registration(
                id,
                registrantReference,
                targetReference,
                RegistrationLifecycle.CANCELLED);
    }

    public Registration reactivate() {
        if (lifecycle == RegistrationLifecycle.ACTIVE) {
            return this;
        }

        return new Registration(
                id,
                registrantReference,
                targetReference,
                RegistrationLifecycle.ACTIVE);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
