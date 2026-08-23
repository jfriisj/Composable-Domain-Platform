package composable.domain.platform.composition.eventmanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import composable.domain.platform.core.execution.CorrelationId;
import composable.domain.platform.core.execution.ExecutionContext;
import composable.domain.platform.event.api.DefineEvent;
import composable.domain.platform.event.api.DefineEventCommand;
import composable.domain.platform.event.api.EventAlreadyPublishedException;
import composable.domain.platform.event.api.EventNotFoundException;
import composable.domain.platform.event.api.EventOwnerReference;
import composable.domain.platform.event.api.EventPublicationState;
import composable.domain.platform.event.api.EventView;
import composable.domain.platform.event.api.FindEvent;
import composable.domain.platform.event.api.PublishEvent;
import composable.domain.platform.event.api.UpdateEvent;
import composable.domain.platform.event.api.UpdateEventCommand;
import composable.domain.platform.security.api.AuthenticatedActorReference;
import composable.domain.platform.security.api.AuthorizationDecision;
import composable.domain.platform.security.api.AuthorizeResourceOwnership;
import composable.domain.platform.security.api.ResourceOwnerReference;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizerEventManagementServiceTest {

    private static final ExecutionContext CONTEXT =
            new ExecutionContext(new CorrelationId("test-correlation"));
    private static final AuthenticatedActorReference ACTOR_A =
            new AuthenticatedActorReference("organizer-alpha");
    private static final AuthenticatedActorReference ACTOR_B =
            new AuthenticatedActorReference("organizer-beta");

    private static final Instant START = Instant.parse("2026-09-01T08:00:00Z");
    private static final Instant END = Instant.parse("2026-09-01T10:00:00Z");
    private static final ZoneId TIMEZONE = ZoneId.of("Europe/Copenhagen");

    private Map<String, EventView> eventStore;
    private OrganizerEventManagementService service;

    @BeforeEach
    void setUp() {
        eventStore = new HashMap<>();

        DefineEvent defineEvent = (ctx, cmd) -> {
            EventView view = new EventView(
                    cmd.eventId(),
                    cmd.name(),
                    cmd.slug(),
                    cmd.startsAt(),
                    cmd.endsAt(),
                    cmd.timezone(),
                    EventPublicationState.UNPUBLISHED,
                    cmd.owner());
            eventStore.put(cmd.eventId(), view);
            return view;
        };

        FindEvent findEvent = (ctx, eventId) -> Optional.ofNullable(eventStore.get(eventId));

        UpdateEvent updateEvent = (ctx, cmd) -> {
            EventView existing = eventStore.get(cmd.eventId());
            if (existing == null) {
                throw new EventNotFoundException(cmd.eventId());
            }
            if (existing.publicationState() == EventPublicationState.PUBLISHED) {
                throw new EventAlreadyPublishedException(cmd.eventId());
            }
            EventView updated = new EventView(
                    cmd.eventId(),
                    cmd.name(),
                    cmd.slug(),
                    cmd.startsAt(),
                    cmd.endsAt(),
                    cmd.timezone(),
                    existing.publicationState(),
                    existing.owner());
            eventStore.put(cmd.eventId(), updated);
            return updated;
        };

        PublishEvent publishEvent = (ctx, eventId) -> {
            EventView existing = eventStore.get(eventId);
            if (existing == null) {
                throw new EventNotFoundException(eventId);
            }
            if (existing.publicationState() == EventPublicationState.PUBLISHED) {
                throw new EventAlreadyPublishedException(eventId);
            }
            EventView published = new EventView(
                    existing.eventId(),
                    existing.name(),
                    existing.slug(),
                    existing.startsAt(),
                    existing.endsAt(),
                    existing.timezone(),
                    EventPublicationState.PUBLISHED,
                    existing.owner());
            eventStore.put(eventId, published);
            return published;
        };

        AuthorizeResourceOwnership authorizeResourceOwnership = (actor, owner) ->
                actor.reference().equals(owner.reference())
                        ? AuthorizationDecision.ALLOWED
                        : AuthorizationDecision.DENIED;

        service = new OrganizerEventManagementService(
                defineEvent,
                updateEvent,
                publishEvent,
                findEvent,
                authorizeResourceOwnership);
    }

    @Test
    void definesEventWithOwnerDerivedFromAuthenticatedActor() {
        DefineOrganizerEventCommand command = new DefineOrganizerEventCommand(
                "evt-1",
                "Platform Day",
                "platform-day",
                START,
                END,
                TIMEZONE);

        EventView created = service.define(CONTEXT, ACTOR_A, command);

        assertEquals("evt-1", created.eventId());
        assertEquals("Platform Day", created.name());
        assertEquals(EventPublicationState.UNPUBLISHED, created.publicationState());
        assertEquals(Optional.of(new EventOwnerReference("organizer-alpha")), created.owner());
    }

    @Test
    void updatesUnpublishedEventWhenActorIsOwner() {
        service.define(
                CONTEXT,
                ACTOR_A,
                new DefineOrganizerEventCommand("evt-1", "Original", "original", START, END, TIMEZONE));

        Instant newStart = Instant.parse("2026-10-01T09:00:00Z");
        Instant newEnd = Instant.parse("2026-10-01T11:00:00Z");
        ZoneId newTz = ZoneId.of("Europe/Oslo");

        EventView updated = service.update(
                CONTEXT,
                ACTOR_A,
                new UpdateOrganizerEventCommand("evt-1", "Updated", "updated", newStart, newEnd, newTz));

        assertEquals("Updated", updated.name());
        assertEquals("updated", updated.slug());
        assertEquals(newStart, updated.startsAt());
        assertEquals(newEnd, updated.endsAt());
        assertEquals(newTz, updated.timezone());
        assertEquals(Optional.of(new EventOwnerReference("organizer-alpha")), updated.owner());
    }

    @Test
    void updateFailsWhenActorIsNotOwner() {
        service.define(
                CONTEXT,
                ACTOR_A,
                new DefineOrganizerEventCommand("evt-1", "Original", "original", START, END, TIMEZONE));

        assertThrows(
                EventManagementAuthorizationDeniedException.class,
                () -> service.update(
                        CONTEXT,
                        ACTOR_B,
                        new UpdateOrganizerEventCommand("evt-1", "Updated", "updated", START, END, TIMEZONE)));
    }

    @Test
    void updateFailsWhenEventHasNoOwner() {
        eventStore.put(
                "legacy-evt",
                new EventView(
                        "legacy-evt",
                        "Legacy",
                        "legacy",
                        START,
                        END,
                        TIMEZONE,
                        EventPublicationState.UNPUBLISHED,
                        Optional.empty()));

        assertThrows(
                EventManagementAuthorizationDeniedException.class,
                () -> service.update(
                        CONTEXT,
                        ACTOR_A,
                        new UpdateOrganizerEventCommand("legacy-evt", "Updated", "updated", START, END, TIMEZONE)));
    }

    @Test
    void updateFailsWhenEventNotFound() {
        assertThrows(
                EventNotFoundException.class,
                () -> service.update(
                        CONTEXT,
                        ACTOR_A,
                        new UpdateOrganizerEventCommand("non-existent", "Updated", "updated", START, END, TIMEZONE)));
    }

    @Test
    void publishesEventWhenActorIsOwner() {
        service.define(
                CONTEXT,
                ACTOR_A,
                new DefineOrganizerEventCommand("evt-1", "Original", "original", START, END, TIMEZONE));

        EventView published = service.publish(CONTEXT, ACTOR_A, "evt-1");

        assertEquals(EventPublicationState.PUBLISHED, published.publicationState());
    }

    @Test
    void publishFailsWhenActorIsNotOwner() {
        service.define(
                CONTEXT,
                ACTOR_A,
                new DefineOrganizerEventCommand("evt-1", "Original", "original", START, END, TIMEZONE));

        assertThrows(
                EventManagementAuthorizationDeniedException.class,
                () -> service.publish(CONTEXT, ACTOR_B, "evt-1"));
    }

    @Test
    void publishFailsWhenEventHasNoOwner() {
        eventStore.put(
                "legacy-evt",
                new EventView(
                        "legacy-evt",
                        "Legacy",
                        "legacy",
                        START,
                        END,
                        TIMEZONE,
                        EventPublicationState.UNPUBLISHED,
                        Optional.empty()));

        assertThrows(
                EventManagementAuthorizationDeniedException.class,
                () -> service.publish(CONTEXT, ACTOR_A, "legacy-evt"));
    }

    @Test
    void publishFailsWhenEventNotFound() {
        assertThrows(
                EventNotFoundException.class,
                () -> service.publish(CONTEXT, ACTOR_A, "non-existent"));
    }
}
