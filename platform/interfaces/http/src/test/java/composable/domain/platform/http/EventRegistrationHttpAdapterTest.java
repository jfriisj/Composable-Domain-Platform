package composable.domain.platform.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.composition.eventregistration.CreateEventRegistration;
import composable.domain.platform.composition.eventregistration.CreateEventRegistrationCommand;
import composable.domain.platform.composition.eventregistration.EventRegistrationUniquenessConflictException;
import composable.domain.platform.composition.eventregistration.EventRegistrationView;
import composable.domain.platform.composition.eventregistration.FindEventRegistration;
import composable.domain.platform.composition.eventregistration.InvalidEventRegistrationDefinitionException;
import composable.domain.platform.composition.eventregistration.UnknownEventForRegistrationException;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.http.generated.model.CreateEventRegistrationRequest;
import composable.domain.platform.http.generated.model.ErrorResponse;
import composable.domain.platform.http.generated.model.EventRegistrationResponse;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class EventRegistrationHttpAdapterTest {

    @Test
    void createsEventRegistrationAndPreservesSuppliedCorrelation() {
        AtomicReference<ExecutionContext> capturedContext = new AtomicReference<>();
        AtomicReference<CreateEventRegistrationCommand> capturedCommand = new AtomicReference<>();

        CreateEventRegistration create = (context, command) -> {
            capturedContext.set(context);
            capturedCommand.set(command);
            return new EventRegistrationView(
                    command.registrationId(),
                    command.eventId(),
                    command.participantReference());
        };

        ResponseEntity<EventRegistrationResponse> response =
                new EventRegistrationHttpAdapter(create, unusedFind())
                        .createEventRegistration(request(), "corr-registration");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(
                "corr-registration",
                response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
        assertEquals(
                "corr-registration",
                capturedContext.get().correlationId().value());
        assertEquals(
                new CreateEventRegistrationCommand(
                        "registration-1",
                        "event-1",
                        "participant-opaque"),
                capturedCommand.get());

        EventRegistrationResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("registration-1", body.getRegistrationId());
        assertEquals("event-1", body.getEventId());
        assertEquals("participant-opaque", body.getParticipantReference());
    }

    @Test
    void createsCorrelationWhenHeaderIsAbsent() {
        AtomicReference<ExecutionContext> capturedContext = new AtomicReference<>();

        CreateEventRegistration create = (context, command) -> {
            capturedContext.set(context);
            return new EventRegistrationView(
                    command.registrationId(),
                    command.eventId(),
                    command.participantReference());
        };

        ResponseEntity<EventRegistrationResponse> response =
                new EventRegistrationHttpAdapter(create, unusedFind())
                        .createEventRegistration(request(), null);

        String correlation = response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME);
        assertNotNull(correlation);
        assertTrue(!correlation.isBlank());
        assertEquals(correlation, capturedContext.get().correlationId().value());
    }

    @Test
    void mapsInvalidDefinitionToBadRequest() {
        CreateEventRegistration create = (context, command) -> {
            throw new InvalidEventRegistrationDefinitionException();
        };

        EventRegistrationHttpException exception = assertThrows(
                EventRegistrationHttpException.class,
                () -> new EventRegistrationHttpAdapter(create, unusedFind())
                        .createEventRegistration(request(), "corr-invalid"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.status());
        assertEquals(
                ErrorResponse.CodeEnum.INVALID_REQUEST,
                exception.code());
    }

    @Test
    void mapsUnknownEventToNotFound() {
        CreateEventRegistration create = (context, command) -> {
            throw new UnknownEventForRegistrationException();
        };

        EventRegistrationHttpException exception = assertThrows(
                EventRegistrationHttpException.class,
                () -> new EventRegistrationHttpAdapter(create, unusedFind())
                        .createEventRegistration(request(), "corr-missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        assertEquals(
                ErrorResponse.CodeEnum.EVENT_NOT_FOUND,
                exception.code());
    }

    @Test
    void mapsUniquenessConflictToConflict() {
        CreateEventRegistration create = (context, command) -> {
            throw new EventRegistrationUniquenessConflictException();
        };

        EventRegistrationHttpException exception = assertThrows(
                EventRegistrationHttpException.class,
                () -> new EventRegistrationHttpAdapter(create, unusedFind())
                        .createEventRegistration(request(), "corr-conflict"));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        assertEquals(
                ErrorResponse.CodeEnum.REGISTRATION_CONFLICT,
                exception.code());
    }

    @Test
    void mapsUnexpectedFailureToSanitizedInternalError() {
        CreateEventRegistration create = (context, command) -> {
            throw new IllegalStateException("database-specific detail");
        };

        EventRegistrationHttpException exception = assertThrows(
                EventRegistrationHttpException.class,
                () -> new EventRegistrationHttpAdapter(create, unusedFind())
                        .createEventRegistration(request(), "corr-internal"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.status());
        assertEquals(
                ErrorResponse.CodeEnum.INTERNAL_ERROR,
                exception.code());
        assertEquals("Internal server error", exception.getMessage());
    }

    @Test
    void retrievesEventRegistration() {
        AtomicReference<ExecutionContext> capturedContext = new AtomicReference<>();

        FindEventRegistration find = (context, registrationId) -> {
            capturedContext.set(context);
            return Optional.of(new EventRegistrationView(
                    registrationId,
                    "event-1",
                    "participant-opaque"));
        };

        ResponseEntity<EventRegistrationResponse> response =
                new EventRegistrationHttpAdapter(unusedCreate(), find)
                        .findEventRegistrationById("registration-1", "corr-find");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "corr-find",
                response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
        assertEquals("corr-find", capturedContext.get().correlationId().value());
        assertEquals("registration-1", response.getBody().getRegistrationId());
    }

    @Test
    void mapsUnknownOrNonEventRegistrationToNotFound() {
        EventRegistrationHttpException exception = assertThrows(
                EventRegistrationHttpException.class,
                () -> new EventRegistrationHttpAdapter(
                                unusedCreate(),
                                (context, registrationId) -> Optional.empty())
                        .findEventRegistrationById(
                                "registration-missing",
                                "corr-find-missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        assertEquals(
                ErrorResponse.CodeEnum.EVENT_REGISTRATION_NOT_FOUND,
                exception.code());
    }

    private static CreateEventRegistrationRequest request() {
        return new CreateEventRegistrationRequest(
                "registration-1",
                "event-1",
                "participant-opaque");
    }

    private static CreateEventRegistration unusedCreate() {
        return (context, command) -> {
            throw new AssertionError("CreateEventRegistration must not be called");
        };
    }

    private static FindEventRegistration unusedFind() {
        return (context, registrationId) -> {
            throw new AssertionError("FindEventRegistration must not be called");
        };
    }
}
