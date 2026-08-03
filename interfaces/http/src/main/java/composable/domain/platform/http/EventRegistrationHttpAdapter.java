package composable.domain.platform.http;

import composable.domain.platform.composition.eventregistration.CreateEventRegistration;
import composable.domain.platform.composition.eventregistration.CreateEventRegistrationCommand;
import composable.domain.platform.composition.eventregistration.EventRegistrationUniquenessConflictException;
import composable.domain.platform.composition.eventregistration.EventRegistrationView;
import composable.domain.platform.composition.eventregistration.FindEventRegistration;
import composable.domain.platform.composition.eventregistration.InvalidEventRegistrationDefinitionException;
import composable.domain.platform.composition.eventregistration.UnknownEventForRegistrationException;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.http.generated.api.EventRegistrationApi;
import composable.domain.platform.http.generated.model.CreateEventRegistrationRequest;
import composable.domain.platform.http.generated.model.EventRegistrationResponse;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventRegistrationHttpAdapter implements EventRegistrationApi {

    private final CreateEventRegistration createEventRegistration;
    private final FindEventRegistration findEventRegistration;

    public EventRegistrationHttpAdapter(
            CreateEventRegistration createEventRegistration,
            FindEventRegistration findEventRegistration) {
        this.createEventRegistration =
                Objects.requireNonNull(
                        createEventRegistration,
                        "createEventRegistration must not be null");
        this.findEventRegistration =
                Objects.requireNonNull(
                        findEventRegistration,
                        "findEventRegistration must not be null");
    }

    @Override
    public ResponseEntity<EventRegistrationResponse> createEventRegistration(
            CreateEventRegistrationRequest request,
            String suppliedCorrelationId) {
        ExecutionContext context = HttpCorrelation.establish(suppliedCorrelationId);

        try {
            EventRegistrationView registration = createEventRegistration.create(
                    context,
                    new CreateEventRegistrationCommand(
                            request.getRegistrationId(),
                            request.getEventId(),
                            request.getParticipantReference()));
            return response(HttpStatus.CREATED, context, registration);
        } catch (InvalidEventRegistrationDefinitionException exception) {
            throw EventRegistrationHttpException.invalidDefinition(context);
        } catch (UnknownEventForRegistrationException exception) {
            throw EventRegistrationHttpException.eventNotFound(context);
        } catch (EventRegistrationUniquenessConflictException exception) {
            throw EventRegistrationHttpException.conflict(context);
        } catch (RuntimeException exception) {
            throw EventRegistrationHttpException.internal(context, exception);
        }
    }

    @Override
    public ResponseEntity<EventRegistrationResponse> findEventRegistrationById(
            String registrationId,
            String suppliedCorrelationId) {
        ExecutionContext context = HttpCorrelation.establish(suppliedCorrelationId);

        try {
            EventRegistrationView registration =
                    findEventRegistration.findById(context, registrationId)
                            .orElseThrow(
                                    () -> EventRegistrationHttpException.registrationNotFound(context));
            return response(HttpStatus.OK, context, registration);
        } catch (EventRegistrationHttpException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw EventRegistrationHttpException.internal(context, exception);
        }
    }

    private static ResponseEntity<EventRegistrationResponse> response(
            HttpStatus status,
            ExecutionContext context,
            EventRegistrationView registration) {
        return ResponseEntity.status(status)
                .header(HttpCorrelation.HEADER_NAME, HttpCorrelation.value(context))
                .body(new EventRegistrationResponse(
                        registration.registrationId(),
                        registration.eventId(),
                        registration.participantReference()));
    }
}
