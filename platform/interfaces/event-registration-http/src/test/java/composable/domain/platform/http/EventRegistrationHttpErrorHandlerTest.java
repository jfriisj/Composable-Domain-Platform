package composable.domain.platform.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.http.eventregistration.generated.model.EventRegistrationErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class EventRegistrationHttpErrorHandlerTest {

    private final EventRegistrationHttpErrorHandler handler =
            new EventRegistrationHttpErrorHandler();

    @Test
    void rendersRegistrationFailureWithItsCorrelationAndContractError() {
        ExecutionContext context =
                new ExecutionContext(new CorrelationId("corr-registration-error"));

        ResponseEntity<EventRegistrationErrorResponse> response =
                handler.handleEventRegistrationFailure(
                        EventRegistrationHttpException.registrationNotFound(context));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(
                "corr-registration-error",
                response.getHeaders().getFirst(EventRegistrationHttpCorrelation.HEADER_NAME));

        EventRegistrationErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(
                EventRegistrationErrorResponse.CodeEnum.EVENT_REGISTRATION_NOT_FOUND,
                body.getCode());
        assertEquals("Event registration was not found", body.getMessage());
    }
}
