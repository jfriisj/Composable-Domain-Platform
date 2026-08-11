package composable.domain.platform.core.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ExecutionContextTest {

    @Test
    void preservesOpaqueCorrelationIdentifier() {
        CorrelationId correlationId = new CorrelationId("corr-01HZX-opaque");
        ExecutionContext context = new ExecutionContext(correlationId);

        assertEquals("corr-01HZX-opaque", context.correlationId().value());
    }

    @Test
    void rejectsBlankCorrelationIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> new CorrelationId(" "));
    }

    @Test
    void rejectsMissingCorrelationIdentifier() {
        assertThrows(NullPointerException.class, () -> new ExecutionContext(null));
    }
}
