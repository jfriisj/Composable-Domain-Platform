package composable.domain.platform.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.http.generated.model.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;

class EventHttpErrorHandlerTest {

    private final EventHttpErrorHandler handler = new EventHttpErrorHandler();

    @Test
    void rendersKnownEventFailureWithItsCorrelationAndContractError() {
        ExecutionContext context =
                new ExecutionContext(new CorrelationId("corr-known"));

        ResponseEntity<ErrorResponse> response =
                handler.handleEventFailure(EventHttpException.notFound(context));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("corr-known", correlation(response));

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(ErrorResponse.CodeEnum.EVENT_NOT_FOUND, body.getCode());
        assertEquals("Event was not found", body.getMessage());
    }

    @Test
    void rendersInvalidTransportFailureWithSuppliedCorrelation() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpCorrelation.HEADER_NAME, "corr-invalid");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidRequest(
                new HttpMessageNotReadableException(
                        "invalid request",
                        new MockHttpInputMessage(new byte[0])),
                request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("corr-invalid", correlation(response));

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(ErrorResponse.CodeEnum.INVALID_REQUEST, body.getCode());
        assertEquals("Request is invalid", body.getMessage());
    }

    @Test
    void generatesCorrelationForInvalidTransportFailureWhenHeaderIsAbsent() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidRequest(
                new HttpMessageNotReadableException(
                        "invalid request",
                        new MockHttpInputMessage(new byte[0])),
                new MockHttpServletRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        String correlation = correlation(response);
        assertNotNull(correlation);
        assertEquals(false, correlation.isBlank());
    }

    @Test
    void sanitizesUnexpectedFailureAndPreservesSuppliedCorrelation() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpCorrelation.HEADER_NAME, "corr-internal");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(
                new IllegalStateException("SQL select * from event.events"),
                request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("corr-internal", correlation(response));

        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(ErrorResponse.CodeEnum.INTERNAL_ERROR, body.getCode());
        assertEquals("Internal server error", body.getMessage());
    }

    private static String correlation(ResponseEntity<?> response) {
        return response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME);
    }
}
