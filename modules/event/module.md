# Event Module

## Status

Current reference bounded context.

## Purpose

Own the smallest Event definition lifecycle required by the current reference use case.

A platform operator can define an Event with explicit identity, name, slug, scheduled start/end, and timezone and receive the resulting Event state.

## Owns

- Event identity.
- Event name.
- Event slug.
- Scheduled start and end.
- Event timezone.
- Invariants required to define a valid Event.

## Does not own

- Registration.
- Ticketing.
- Booking.
- Membership.
- Speakers or program management.
- Content management.
- Payments or accounting.
- Notifications.
- Identity-provider concerns.

## Public API

The `event-api` Gradle project publishes only the current application-level contract:

- `DefineEvent`
- `DefineEventCommand`
- `EventView`

The public API does not expose Event domain implementation types.

## Implementation

The `event-impl` Gradle project contains the Event domain model and the application implementation of the public contract.

Domain and application implementation types are not part of the published Event API.

## Allowed dependencies

`event-api`:

- Java standard library only.

`event-impl`:

- `event-api`.
- Java standard library.
- JUnit 5 for tests.
- ArchUnit for architecture tests.

No dependency on another business module is currently allowed.

## Explicitly absent

The current Event module has no:

- Spring runtime dependency.
- persistence adapter or database schema.
- HTTP/OpenAPI adapter.
- event publication or messaging infrastructure.
- external provider integration.
