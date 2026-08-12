# Event Module

## Status

Current reference bounded context.

## Purpose

Own the minimum Event definition, durable retrieval, publication, and participant-discovery lifecycle required by the current reference use case.

A platform operator can define an Event with explicit identity, name, slug, scheduled start/end, and timezone, persist that state durably, retrieve it later by Event identity, publish it once, and discover published Events through Event-owned state.

## Owns

- Event identity.
- Event name.
- Event slug.
- Scheduled start and end.
- Event timezone.
- Event publication state.
- Initial `unpublished` state.
- The one-way `unpublished -> published` transition.
- Participant discovery over published Event state.
- Invariants required to define a valid Event.
- Event application result semantics, including duplicate identity, invalid definition, unknown publication target, and already-published transition failure.
- The Event application persistence port.
- Event-owned PostgreSQL schema and Flyway migration history.
- Mapping between Event domain state and private persistence records.

## Does not own

- HTTP transport contracts or status/error mapping.
- Application runtime/bootstrap or runtime database configuration.
- Registration.
- Registration eligibility, opening/closing periods, capacity, quotas, or waitlists.
- Participant identity, authentication, or authorization.
- Ticketing.
- Booking.
- Membership.
- Speakers or program management.
- Content management.
- Payments or accounting.
- Notifications.
- Identity-provider concerns.
- Shared business schemas or persistence for another bounded context.

## Public API

The `event-api` Gradle project publishes the current application-level contract:

- `DefineEvent`
- `DefineEventCommand`
- `FindEvent`
- `PublishEvent`
- `DiscoverEvents`
- `EventView`
- `EventPublicationState`
- `EventAlreadyDefinedException`
- `InvalidEventDefinitionException`
- `EventNotFoundException`
- `EventAlreadyPublishedException`

Event public application operations accept the business-neutral `ExecutionContext` from `core` so correlation can be propagated explicitly across the application boundary without introducing HTTP or participant-identity types into Event.

`FindEvent` returns `Optional<EventView>` for retrieval by Event identity. An unknown identity returns an empty result. Known-id retrieval is independent of publication state.

`PublishEvent` publishes an existing unpublished Event. Unknown Event identity and an already-published Event are explicit application failures. No unpublish/withdraw or idempotent republish lifecycle is exposed.

`DiscoverEvents` returns only published Events. Discovery depends only on Event-owned state, requires no participant identity, and defines no business-significant ordering.

Defining an Event whose identity already exists is rejected with `EventAlreadyDefinedException`; the existing persisted Event remains unchanged. A domain-invalid definition is represented at the public application boundary by `InvalidEventDefinitionException`.

The public API does not expose Event domain, persistence implementation, Spring, HTTP, Registration, security, or generated OpenAPI types.

## Implementation

The `event-impl` Gradle project contains:

- the Event domain model and publication lifecycle;
- application implementations and the application-owned `EventRepository` outbound port;
- the private jOOQ PostgreSQL persistence adapter;
- Event-owned Flyway migrations;
- unit, architecture, and PostgreSQL integration tests.

The persistence adapter uses atomic insert-if-absent semantics for Event identity, conditionally persists the publication transition from the expected prior state, reconstructs publication state on retrieval, and discovers published Events from Event-owned persistence.

Domain, application implementation, persistence implementation, and persistence-record details are not part of the published Event API.

## Allowed dependencies

`event-api`:

- `core` execution-context contract.
- Java standard library.

`event-impl` production:

- `event-api`.
- `core` execution-context contract through the public application boundary.
- Java standard library.
- jOOQ inside the persistence adapter.

`event-impl` tests:

- JUnit 5.
- ArchUnit.
- Flyway.
- PostgreSQL JDBC driver.
- Testcontainers PostgreSQL.

No dependency on another business module is currently allowed.

## Persistence

Event owns schema `event`.

Flyway migration `V1__create_event_schema.sql` defines the Event table and identity primary key. Migration `V2__add_event_publication_state.sql` adds durable publication state, backfills all pre-publication Event rows as `unpublished`, and constrains persisted state to `unpublished` or `published`.

The persistence representation preserves the full Java `Instant` value as epoch-second plus nanosecond components and stores the timezone identifier explicitly.

Integration tests execute the migration history and persistence adapter against real PostgreSQL through Testcontainers, including V1-to-V2 backfill, publication persistence, known-id retrieval in both states, discovery, and persistence-boundary state validation.

## Explicitly absent

The Event module itself has no:

- Spring runtime dependency.
- Spring Data, Hibernate, or JPA dependency.
- HTTP/OpenAPI publication/discovery adapter.
- Event messaging infrastructure.
- External provider integration.
- Application runtime/bootstrap or production database configuration.
- Participant authentication or authorization behavior.

Those runtime and HTTP responsibilities live outside the bounded context in `platform/apps/platform` and `platform/interfaces/http`. Cross-capability participant orchestration remains outside Event in the Event-Registration composition.
