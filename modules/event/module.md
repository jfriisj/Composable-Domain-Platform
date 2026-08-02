# Event Module

## Status

Current reference bounded context.

## Purpose

Own the smallest Event definition and durable retrieval lifecycle required by the current reference use case.

A platform operator can define an Event with explicit identity, name, slug, scheduled start/end, and timezone, persist that state durably, and retrieve it later by Event identity.

## Owns

- Event identity.
- Event name.
- Event slug.
- Scheduled start and end.
- Event timezone.
- Invariants required to define a valid Event.
- Event application result semantics, including duplicate identity and invalid definition.
- The Event application persistence port.
- Event-owned PostgreSQL schema and Flyway migration history.
- Mapping between Event domain state and private persistence records.

## Does not own

- HTTP transport contracts or status/error mapping.
- Application runtime/bootstrap or runtime database configuration.
- Registration.
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

The `event-api` Gradle project publishes only the current application-level contract:

- `DefineEvent`
- `DefineEventCommand`
- `FindEvent`
- `EventView`
- `EventAlreadyDefinedException`
- `InvalidEventDefinitionException`

`DefineEvent` and `FindEvent` accept the business-neutral `ExecutionContext` from `core` so correlation can be propagated explicitly across the application boundary without introducing HTTP types into Event.

`FindEvent` returns `Optional<EventView>` for retrieval by Event identity. An unknown identity returns an empty result.

Defining an Event whose identity already exists is rejected with `EventAlreadyDefinedException`; the existing persisted Event remains unchanged. A domain-invalid definition is represented at the public application boundary by `InvalidEventDefinitionException`.

The public API does not expose Event domain, persistence implementation, Spring, HTTP, or generated OpenAPI types.

## Implementation

The `event-impl` Gradle project contains:

- the Event domain model;
- application implementations and the application-owned `EventRepository` outbound port;
- the private jOOQ PostgreSQL persistence adapter;
- Event-owned Flyway migrations;
- unit, architecture, and PostgreSQL integration tests.

The persistence adapter uses atomic insert-if-absent semantics for Event identity and reconstructs the existing Event domain state on retrieval.

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

Flyway migration `V1__create_event_schema.sql` defines the current Event table. Event identity is the primary key.

The persistence representation preserves the full Java `Instant` value as epoch-second plus nanosecond components and stores the timezone identifier explicitly.

Integration tests execute the migration and persistence adapter against real PostgreSQL through Testcontainers.

## Explicitly absent

The Event module itself has no:

- Spring runtime dependency.
- Spring Data, Hibernate, or JPA dependency.
- HTTP/OpenAPI adapter.
- event publication or messaging infrastructure.
- external provider integration.
- application runtime/bootstrap or production database configuration.

Those runtime and HTTP responsibilities live outside the bounded context in `apps/platform` and `interfaces/http`.
