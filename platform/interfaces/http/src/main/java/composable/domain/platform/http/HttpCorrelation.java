package composable.domain.platform.http;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import java.util.UUID;

public final class HttpCorrelation {

    public static final String HEADER_NAME = "X-Correlation-Id";

    private HttpCorrelation() {
    }

    public static ExecutionContext establish(String suppliedCorrelationId) {
        String correlationId = suppliedCorrelationId == null || suppliedCorrelationId.isBlank()
                ? UUID.randomUUID().toString()
                : suppliedCorrelationId;

        return new ExecutionContext(new CorrelationId(correlationId));
    }

    public static String value(ExecutionContext context) {
        return context.correlationId().value();
    }
}
