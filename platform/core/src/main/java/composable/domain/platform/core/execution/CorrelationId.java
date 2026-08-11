package composable.domain.platform.core.execution;

public record CorrelationId(String value) {

    public CorrelationId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("correlationId must not be blank");
        }
    }
}
