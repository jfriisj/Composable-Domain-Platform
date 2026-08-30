package composable.domain.platform.http;

import composable.domain.platform.composition.eventwaitlist.EventNotPublishedForWaitlistException;
import composable.domain.platform.composition.eventwaitlist.EventRegistrationExistsForWaitlistException;
import composable.domain.platform.composition.eventwaitlist.EventWaitlistUnavailableException;
import composable.domain.platform.composition.eventwaitlist.FindParticipantEventWaitlist;
import composable.domain.platform.composition.eventwaitlist.InvalidEventWaitlistRequestException;
import composable.domain.platform.composition.eventwaitlist.JoinParticipantEventWaitlist;
import composable.domain.platform.composition.eventwaitlist.ParticipantEventWaitlistView;
import composable.domain.platform.composition.eventwaitlist.UnknownEventForWaitlistException;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.http.eventwaitlist.generated.api.EventWaitlistApi;
import composable.domain.platform.http.eventwaitlist.generated.model.EventWaitlistParticipationResponse;
import composable.domain.platform.security.api.AuthenticatedActorProvider;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventWaitlistHttpAdapter implements EventWaitlistApi {

    private final JoinParticipantEventWaitlist joinEventWaitlist;
    private final FindParticipantEventWaitlist findEventWaitlist;
    private final AuthenticatedActorProvider authenticatedActorProvider;

    public EventWaitlistHttpAdapter(
            JoinParticipantEventWaitlist joinEventWaitlist,
            FindParticipantEventWaitlist findEventWaitlist,
            AuthenticatedActorProvider authenticatedActorProvider) {
        this.joinEventWaitlist =
                Objects.requireNonNull(
                        joinEventWaitlist,
                        "joinEventWaitlist must not be null");
        this.findEventWaitlist =
                Objects.requireNonNull(
                        findEventWaitlist,
                        "findEventWaitlist must not be null");
        this.authenticatedActorProvider =
                Objects.requireNonNull(
                        authenticatedActorProvider,
                        "authenticatedActorProvider must not be null");
    }

    @Override
    public ResponseEntity<EventWaitlistParticipationResponse> joinEventWaitlist(
            String eventId,
            String suppliedCorrelationId) {
        ExecutionContext context =
                EventWaitlistHttpCorrelation.establish(suppliedCorrelationId);

        try {
            AuthenticatedActorReference actorReference =
                    authenticatedActorProvider.authenticatedActor();
            ParticipantEventWaitlistView participation =
                    joinEventWaitlist.join(context, actorReference, eventId);
            return response(HttpStatus.OK, context, participation);
        } catch (InvalidEventWaitlistRequestException exception) {
            throw EventWaitlistHttpException.invalidRequest(context);
        } catch (UnknownEventForWaitlistException exception) {
            throw EventWaitlistHttpException.eventNotFound(context);
        } catch (EventNotPublishedForWaitlistException exception) {
            throw EventWaitlistHttpException.eventNotPublished(context);
        } catch (EventWaitlistUnavailableException exception) {
            throw EventWaitlistHttpException.waitlistUnavailable(context);
        } catch (EventRegistrationExistsForWaitlistException exception) {
            throw EventWaitlistHttpException.registrationExists(context);
        } catch (RuntimeException exception) {
            throw EventWaitlistHttpException.internal(context, exception);
        }
    }

    @Override
    public ResponseEntity<EventWaitlistParticipationResponse>
            findEventWaitlistParticipation(
                    String eventId,
                    String suppliedCorrelationId) {
        ExecutionContext context =
                EventWaitlistHttpCorrelation.establish(suppliedCorrelationId);

        try {
            AuthenticatedActorReference actorReference =
                    authenticatedActorProvider.authenticatedActor();
            ParticipantEventWaitlistView participation =
                    findEventWaitlist.findByEventId(
                                    context,
                                    actorReference,
                                    eventId)
                            .orElseThrow(
                                    () -> EventWaitlistHttpException
                                            .participationNotFound(context));
            return response(HttpStatus.OK, context, participation);
        } catch (InvalidEventWaitlistRequestException exception) {
            throw EventWaitlistHttpException.invalidRequest(context);
        } catch (EventWaitlistHttpException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw EventWaitlistHttpException.internal(context, exception);
        }
    }

    private static ResponseEntity<EventWaitlistParticipationResponse> response(
            HttpStatus status,
            ExecutionContext context,
            ParticipantEventWaitlistView participation) {
        return ResponseEntity.status(status)
                .header(
                        EventWaitlistHttpCorrelation.HEADER_NAME,
                        EventWaitlistHttpCorrelation.value(context))
                .body(new EventWaitlistParticipationResponse(
                        participation.waitlistParticipationId(),
                        participation.eventId()));
    }
}
