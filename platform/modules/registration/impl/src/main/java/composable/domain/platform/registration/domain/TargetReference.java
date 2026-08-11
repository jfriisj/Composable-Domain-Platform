package composable.domain.platform.registration.domain;

public record TargetReference(
        String namespace,
        String reference) {

    public TargetReference {
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
