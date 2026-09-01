package composable.domain.platform.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import composable.domain.platform.composition.eventregistration.CancelParticipantEventRegistration;
import composable.domain.platform.composition.eventregistration.CreateParticipantEventRegistration;
import composable.domain.platform.composition.eventregistration.EventNotPublishedForRegistrationException;
import composable.domain.platform.composition.eventregistration.EventRegistrationAuthorizationDeniedException;
import composable.domain.platform.composition.eventregistration.EventRegistrationClosedException;
import composable.domain.platform.composition.eventregistration.EventRegistrationLifecycle;
import composable.domain.platform.composition.eventregistration.FindOrganizerEventRegistrations;
import composable.domain.platform.composition.eventregistration.FindParticipantEventRegistration;
import composable.domain.platform.composition.eventregistration.ParticipantEventRegistrationView;
import composable.domain.platform.composition.eventregistration.ReactivateParticipantEventRegistration;
import composable.domain.platform.composition.eventregistration.UnknownEventForRegistrationException;
import composable.domain.platform.http.eventregistration.generated.model.EventRegistrationErrorResponse;
import composable.domain.platform.http.eventregistration.generated.model.EventRegistrationResponse;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class EventRegistrationHttpReactivationAdapterTest {

    @Test
    void reactivatesOwnedRegistrationAndPreservesCorrelation() {
        ReactivateParticipantEventRegistration reactivate =
                (context, actorReference, registrationId) ->
                        Optional.of(new ParticipantEventRegistrationView(
                                registrationId,
                                "event-1",
                                EventRegistrationLifecycle.ACTIVE));

        ResponseEntity<EventRegistrationResponse> response =
                adapter(reactivate, "opaque-actor-a")
                        .reactivateEventRegistration(
                                "registration-1",
                                "corr-reactivate");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "corr-reactivate",
                response.getHeaders().getFirst(
                        EventRegistrationHttpCorrelation.HEADER_NAME));
        EventRegistrationResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("registration-1", body.getRegistrationId());
        assertEquals("event-1", body.getEventId());
        assertEquals("active", body.getLifecycle().toString());
    }

    @Test
    void mapsUnknownOrPrivateRegistrationToPrivacyPreservingNotFound() {
        EventRegistrationHttpException unknown = assertThrows(
                EventRegistrationHttpException.class,
                () -> adapter(
                                (context, actorReference, registrationId) -> Optional.empty(),
                                "opaque-actor-a")
                        .reactivateEventRegistration(
                                "missing",
                                "corr-missing"));

        assertEquals(HttpStatus.NOT_FOUND, unknown.status());
        assertEquals(
                EventRegistrationErrorResponse.CodeEnum.EVENT_REGISTRATION_NOT_FOUND,
                unknown.code());

        EventRegistrationHttpException denied = assertThrows(
                EventRegistrationHttpException.class,
                () -> adapter(
                                (context, actorReference, registrationId) -> {
                                    throw new EventRegistrationAuthorizationDeniedException();
                                },
                                "opaque-actor-a")
                        .reactivateEventRegistration(
                                "private",
                                "corr-private"));

        assertEquals(HttpStatus.NOT_FOUND, denied.status());
        assertEquals(
                EventRegistrationErrorResponse.CodeEnum.EVENT_REGISTRATION_NOT_FOUND,
                denied.code());
    }

    @Test
    void mapsEventEligibilityFailures() {
        assertFailure(
                (context, actorReference, registrationId) -> {
                    throw new UnknownEventForRegistrationException();
                },
                HttpStatus.NOT_FOUND,
                EventRegistrationErrorResponse.CodeEnum.EVENT_NOT_FOUND);

        assertFailure(
                (context, actorReference, registrationId) -> {
                    throw new EventNotPublishedForRegistrationException();
                },
                HttpStatus.CONFLICT,
                EventRegistrationErrorResponse.CodeEnum.EVENT_NOT_PUBLISHED);

        assertFailure(
                (context, actorReference, registrationId) -> {
                    throw new EventRegistrationClosedException();
                },
                HttpStatus.CONFLICT,
                EventRegistrationErrorResponse.CodeEnum.EVENT_REGISTRATION_CLOSED);
    }

    private static void assertFailure(
            ReactivateParticipantEventRegistration reactivate,
            HttpStatus expectedStatus,
            EventRegistrationErrorResponse.CodeEnum expectedCode) {
        EventRegistrationHttpException exception = assertThrows(
                EventRegistrationHttpException.class,
                () -> adapter(reactivate, "opaque-actor-a")
                        .reactivateEventRegistration(
                                "registration-1",
                                "corr-failure"));

        assertEquals(expectedStatus, exception.status());
        assertEquals(expectedCode, exception.code());
    }

    private static EventRegistrationHttpAdapter adapter(
            ReactivateParticipantEventRegistration reactivate,
            String actorReference) {
        CreateParticipantEventRegistration create = (context, actor, command) -> {
            throw new AssertionError("Creation must not be invoked");
        };
        FindParticipantEventRegistration find = (context, actor, registrationId) -> {
            throw new AssertionError("Find must not be invoked");
        };
        CancelParticipantEventRegistration cancel = (context, actor, registrationId) -> {
            throw new AssertionError("Cancellation must not be invoked");
        };
        FindOrganizerEventRegistrations findOrganizer = (context, actor, eventId) -> {
            throw new AssertionError("Organizer find must not be invoked");
        };

        return new EventRegistrationHttpAdapter(
                create,
                find,
                cancel,
                reactivate,
                findOrganizer,
                () -> new AuthenticatedActorReference(actorReference));
    }
}
