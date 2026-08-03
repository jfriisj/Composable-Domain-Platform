package composable.domain.platform.registration.domain;

import java.util.Objects;

public record Registration(
        String id,
        RegistrantReference registrantReference,
        TargetReference targetReference) {

    public Registration {
        id = requireText(id, "id");
        Objects.requireNonNull(registrantReference, "registrantReference must not be null");
        Objects.requireNonNull(targetReference, "targetReference must not be null");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
