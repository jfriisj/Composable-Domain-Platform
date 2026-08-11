package composable.domain.platform.registration.domain;

public record RegistrantReference(
        String namespace,
        String reference) {

    public RegistrantReference {
        namespace = requireText(namespace, "namespace");
        reference = requireText(reference, "reference");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
