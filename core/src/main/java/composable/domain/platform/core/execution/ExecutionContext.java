package composable.domain.platform.core.execution;

import java.util.Objects;

public record ExecutionContext(CorrelationId correlationId) {

    public ExecutionContext {
        Objects.requireNonNull(correlationId, "correlationId must not be null");
    }
}
