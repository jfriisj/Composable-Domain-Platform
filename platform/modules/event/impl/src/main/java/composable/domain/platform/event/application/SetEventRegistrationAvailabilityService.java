package composable.domain.platform.event.application;

import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.EventNotFoundException;
import composable.domain.platform.event.api.EventNotPublishedException;
import composable.domain.platform.event.api.EventRegistrationAvailability;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.EventWithdrawnException;
import composable.domain.platform.event.api.SetEventRegistrationAvailability;
import composable.domain.platform.event.domain.Event;
import composable.domain.platform.event.domain.PublicationState;
import composable.domain.platform.event.domain.RegistrationAvailability;
import java.util.Objects;

public final class SetEventRegistrationAvailabilityService
        implements SetEventRegistrationAvailability {

    private final EventRepository repository;

    public SetEventRegistrationAvailabilityService(EventRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public EventView setRegistrationAvailability(
            ExecutionContext context,
            String eventId,
            EventRegistrationAvailability availability) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(availability, "availability must not be null");

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }

        Event existing = repository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        if (existing.publicationState() == PublicationState.UNPUBLISHED) {
            throw new EventNotPublishedException(eventId);
        }
        if (existing.publicationState() == PublicationState.WITHDRAWN) {
            throw new EventWithdrawnException(eventId);
        }

        RegistrationAvailability desired = switch (availability) {
            case OPEN -> RegistrationAvailability.OPEN;
            case CLOSED -> RegistrationAvailability.CLOSED;
        };

        if (existing.registrationAvailability() == desired) {
            return EventViews.from(existing);
        }

        Event updated = existing.setRegistrationAvailability(desired);
        if (!repository.updateRegistrationAvailability(updated)) {
            Event current = repository.findById(eventId)
                    .orElseThrow(() -> new EventNotFoundException(eventId));
            if (current.publicationState() == PublicationState.WITHDRAWN) {
                throw new EventWithdrawnException(eventId);
            }
            if (current.publicationState() != PublicationState.PUBLISHED) {
                throw new EventNotPublishedException(eventId);
            }
            throw new IllegalStateException(
                    "Event registration availability update was not persisted");
        }

        return EventViews.from(updated);
    }
}
