package composable.domain.platform.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import composable.domain.platform.composition.eventregistration.AuthenticatedActorReference;
import composable.domain.platform.composition.eventregistration.CancelParticipantEventRegistration;
import composable.domain.platform.composition.eventregistration.CreateParticipantEventRegistration;
import composable.domain.platform.composition.eventregistration.CreateParticipantEventRegistrationCommand;
import composable.domain.platform.composition.eventregistration.EventRegistrationAuthorizationDeniedException;
import composable.domain.platform.composition.eventregistration.EventRegistrationLifecycle;
import composable.domain.platform.composition.eventregistration.EventRegistrationUniquenessConflictException;
import composable.domain.platform.composition.eventregistration.FindParticipantEventRegistration;
import composable.domain.platform.composition.eventregistration.InvalidEventRegistrationDefinitionException;
import composable.domain.platform.composition.eventregistration.ParticipantEventRegistrationView;
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
    void createsForAuthenticatedActorWithoutCallerOwnedParticipantReference() {
        AtomicReference<ExecutionContext> capturedContext = new AtomicReference<>();
        AtomicReference<AuthenticatedActorReference> capturedActor =
                new AtomicReference<>();
        AtomicReference<CreateParticipantEventRegistrationCommand> capturedCommand =
                new AtomicReference<>();

        CreateParticipantEventRegistration create =
                (context, actorReference, command) -> {
                    capturedContext.set(context);
                    capturedActor.set(actorReference);
                    capturedCommand.set(command);
                    return new ParticipantEventRegistrationView(
                            command.registrationId(),
                            command.eventId(),
                            EventRegistrationLifecycle.ACTIVE);
                };

        ResponseEntity<EventRegistrationResponse> response =
                adapter(create, unusedFind(), unusedCancel(), "opaque-actor-a")
                        .createEventRegistration(
                                new CreateEventRegistrationRequest(
                                        "registration-1",
                                        "event-1"),
                                "corr-registration");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(
                "corr-registration",
                response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
        assertEquals(
                "corr-registration",
                capturedContext.get().correlationId().value());
        assertEquals(
                new AuthenticatedActorReference("opaque-actor-a"),
                capturedActor.get());
        assertEquals(
                new CreateParticipantEventRegistrationCommand(
                        "registration-1",
                        "event-1"),
                capturedCommand.get());

        EventRegistrationResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("registration-1", body.getRegistrationId());
        assertEquals("event-1", body.getEventId());
        assertEquals("active", body.getLifecycle().toString());
    }

    @Test
    void mapsParticipantAuthorizationDenialToExternalNotFound() {
        FindParticipantEventRegistration denied =
                (context, actorReference, registrationId) -> {
                    throw new EventRegistrationAuthorizationDeniedException();
                };

        EventRegistrationHttpException exception = assertThrows(
                EventRegistrationHttpException.class,
                () -> adapter(unusedCreate(), denied, unusedCancel(), "opaque-actor-b")
                        .findEventRegistrationById(
                                "registration-private",
                                "corr-private"));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        assertEquals(
                ErrorResponse.CodeEnum.EVENT_REGISTRATION_NOT_FOUND,
                exception.code());
    }

    @Test
    void retrievesOwnedParticipantRegistrationWithLifecycle() {
        FindParticipantEventRegistration find =
                (context, actorReference, registrationId) ->
                        Optional.of(new ParticipantEventRegistrationView(
                                registrationId,
                                "event-1",
                                EventRegistrationLifecycle.CANCELLED));

        ResponseEntity<EventRegistrationResponse> response =
                adapter(unusedCreate(), find, unusedCancel(), "opaque-actor-a")
                        .findEventRegistrationById(
                                "registration-1",
                                "corr-find");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("cancelled", response.getBody().getLifecycle().toString());
        assertEquals(
                "corr-find",
                response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
    }

    @Test
    void cancelsOwnedParticipantRegistration() {
        CancelParticipantEventRegistration cancel =
                (context, actorReference, registrationId) ->
                        Optional.of(new ParticipantEventRegistrationView(
                                registrationId,
                                "event-1",
                                EventRegistrationLifecycle.CANCELLED));

        ResponseEntity<EventRegistrationResponse> response =
                adapter(unusedCreate(), unusedFind(), cancel, "opaque-actor-a")
                        .cancelEventRegistration(
                                "registration-1",
                                "corr-cancel");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("cancelled", response.getBody().getLifecycle().toString());
        assertEquals(
                "corr-cancel",
                response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME));
    }

    @Test
    void mapsUnknownCancellationToNotFound() {
        CancelParticipantEventRegistration cancel =
                (context, actorReference, registrationId) -> Optional.empty();

        EventRegistrationHttpException exception = assertThrows(
                EventRegistrationHttpException.class,
                () -> adapter(unusedCreate(), unusedFind(), cancel, "opaque-actor-a")
                        .cancelEventRegistration(
                                "registration-missing",
                                "corr-cancel-missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        assertEquals(
                ErrorResponse.CodeEnum.EVENT_REGISTRATION_NOT_FOUND,
                exception.code());
    }

    @Test
    void preservesCreateFailureMappings() {
        assertCreateFailure(
                (context, actorReference, command) -> {
                    throw new InvalidEventRegistrationDefinitionException();
                },
                HttpStatus.BAD_REQUEST,
                ErrorResponse.CodeEnum.INVALID_REQUEST);

        assertCreateFailure(
                (context, actorReference, command) -> {
                    throw new UnknownEventForRegistrationException();
                },
                HttpStatus.NOT_FOUND,
                ErrorResponse.CodeEnum.EVENT_NOT_FOUND);

        assertCreateFailure(
                (context, actorReference, command) -> {
                    throw new EventRegistrationUniquenessConflictException();
                },
                HttpStatus.CONFLICT,
                ErrorResponse.CodeEnum.REGISTRATION_CONFLICT);
    }

    @Test
    void createsCorrelationWhenHeaderIsAbsent() {
        AtomicReference<ExecutionContext> capturedContext = new AtomicReference<>();

        CreateParticipantEventRegistration create =
                (context, actorReference, command) -> {
                    capturedContext.set(context);
                    return new ParticipantEventRegistrationView(
                            command.registrationId(),
                            command.eventId(),
                            EventRegistrationLifecycle.ACTIVE);
                };

        ResponseEntity<EventRegistrationResponse> response =
                adapter(create, unusedFind(), unusedCancel(), "opaque-actor-a")
                        .createEventRegistration(
                                new CreateEventRegistrationRequest(
                                        "registration-1",
                                        "event-1"),
                                null);

        String correlation =
                response.getHeaders().getFirst(HttpCorrelation.HEADER_NAME);
        assertNotNull(correlation);
        assertTrue(!correlation.isBlank());
        assertEquals(
                correlation,
                capturedContext.get().correlationId().value());
    }

    private static void assertCreateFailure(
            CreateParticipantEventRegistration create,
            HttpStatus expectedStatus,
            ErrorResponse.CodeEnum expectedCode) {
        EventRegistrationHttpException exception = assertThrows(
                EventRegistrationHttpException.class,
                () -> adapter(create, unusedFind(), unusedCancel(), "opaque-actor-a")
                        .createEventRegistration(
                                new CreateEventRegistrationRequest(
                                        "registration-1",
                                        "event-1"),
                                "corr-failure"));

        assertEquals(expectedStatus, exception.status());
        assertEquals(expectedCode, exception.code());
    }

    private static EventRegistrationHttpAdapter adapter(
            CreateParticipantEventRegistration create,
            FindParticipantEventRegistration find,
            CancelParticipantEventRegistration cancel,
            String actorReference) {
        return new EventRegistrationHttpAdapter(
                create,
                find,
                cancel,
                () -> new AuthenticatedActorReference(actorReference));
    }

    private static CreateParticipantEventRegistration unusedCreate() {
        return (context, actorReference, command) -> {
            throw new AssertionError(
                    "CreateParticipantEventRegistration must not be called");
        };
    }

    private static FindParticipantEventRegistration unusedFind() {
        return (context, actorReference, registrationId) -> {
            throw new AssertionError(
                    "FindParticipantEventRegistration must not be called");
        };
    }

    private static CancelParticipantEventRegistration unusedCancel() {
        return (context, actorReference, registrationId) -> {
            throw new AssertionError(
                    "CancelParticipantEventRegistration must not be called");
        };
    }
}
