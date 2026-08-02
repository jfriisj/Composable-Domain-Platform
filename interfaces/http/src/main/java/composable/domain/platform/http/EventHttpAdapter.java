package composable.domain.platform.http;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.DefineEvent;
import composable.domain.platform.event.api.DefineEventCommand;
import composable.domain.platform.event.api.EventAlreadyDefinedException;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.event.api.InvalidEventDefinitionException;
import composable.domain.platform.http.generated.api.EventApi;
import composable.domain.platform.http.generated.model.DefineEventRequest;
import composable.domain.platform.http.generated.model.EventResponse;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventHttpAdapter implements EventApi {

    private final DefineEvent defineEvent;
    private final FindEvent findEvent;

    public EventHttpAdapter(DefineEvent defineEvent, FindEvent findEvent) {
        this.defineEvent = Objects.requireNonNull(defineEvent, "defineEvent must not be null");
        this.findEvent = Objects.requireNonNull(findEvent, "findEvent must not be null");
    }

    @Override
    public ResponseEntity<EventResponse> defineEvent(
            DefineEventRequest request,
            String suppliedCorrelationId) {
        ExecutionContext context = HttpCorrelation.establish(suppliedCorrelationId);

        DefineEventCommand command;
        try {
            command = toCommand(request);
        } catch (DateTimeException exception) {
            throw EventHttpException.invalidRequest(context);
        }

        try {
            EventView event = defineEvent.define(context, command);
            return response(HttpStatus.CREATED, context, toResponse(event));
        } catch (InvalidEventDefinitionException exception) {
            throw EventHttpException.invalidDefinition(context);
        } catch (EventAlreadyDefinedException exception) {
            throw EventHttpException.alreadyDefined(context);
        } catch (RuntimeException exception) {
            throw EventHttpException.internal(context, exception);
        }
    }

    @Override
    public ResponseEntity<EventResponse> findEventById(
            String eventId,
            String suppliedCorrelationId) {
        ExecutionContext context = HttpCorrelation.establish(suppliedCorrelationId);

        try {
            EventView event = findEvent.findById(context, eventId)
                    .orElseThrow(() -> EventHttpException.notFound(context));
            return response(HttpStatus.OK, context, toResponse(event));
        } catch (EventHttpException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw EventHttpException.internal(context, exception);
        }
    }

    private static DefineEventCommand toCommand(DefineEventRequest request) {
        return new DefineEventCommand(
                request.getEventId(),
                request.getName(),
                request.getSlug(),
                request.getStartsAt().toInstant(),
                request.getEndsAt().toInstant(),
                ZoneId.of(request.getTimezone()));
    }

    private static EventResponse toResponse(EventView event) {
        return new EventResponse(
                event.eventId(),
                event.name(),
                event.slug(),
                event.startsAt().atOffset(ZoneOffset.UTC),
                event.endsAt().atOffset(ZoneOffset.UTC),
                event.timezone().getId());
    }

    private static ResponseEntity<EventResponse> response(
            HttpStatus status,
            ExecutionContext context,
            EventResponse body) {
        return ResponseEntity.status(status)
                .header(HttpCorrelation.HEADER_NAME, HttpCorrelation.value(context))
                .body(body);
    }
}
