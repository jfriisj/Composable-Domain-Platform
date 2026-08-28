package composable.domain.platform.event.application;

import composable.domain.platform.event.api.EventOwnerReference;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventRegistrationAvailability;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.domain.Event;

final class EventViews {

    private EventViews() {
    }

    static EventView from(Event event) {
        return new EventView(
                event.id(),
                event.name(),
                event.slug(),
                event.startsAt(),
                event.endsAt(),
                event.timezone(),
                switch (event.publicationState()) {
                    case UNPUBLISHED -> EventPublicationState.UNPUBLISHED;
                    case PUBLISHED -> EventPublicationState.PUBLISHED;
                    case WITHDRAWN -> EventPublicationState.WITHDRAWN;
                },
                switch (event.registrationAvailability()) {
                    case OPEN -> EventRegistrationAvailability.OPEN;
                    case CLOSED -> EventRegistrationAvailability.CLOSED;
                },
                event.owner().map(EventOwnerReference::new));
    }
}
