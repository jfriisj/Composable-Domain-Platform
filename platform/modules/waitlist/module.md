# Waitlist Module

## Purpose

Own the durable participant-to-Event waitlist participation capability admitted for the bounded participant Event waitlist journey.

## Owns

- Waitlist participation identity.
- Opaque participant and Event references.
- At-most-one durable participation for one participant/Event pair.
- Idempotent repeated join for the same participant/Event pair.
- Participant/Event exact retrieval of durable participation.
- Waitlist-owned PostgreSQL persistence, schema, and migrations.

## Does not own

- Event existence, lifecycle, publication, or Registration availability.
- Registration occupancy, lifecycle, or persistence.
- Authentication or Authorization.
- Event-Waitlist eligibility workflow.
- HTTP transport mapping.
- Queue ordering, ranking, capacity, promotion, cancellation/removal, notifications, or Registration creation.

## Public boundary

`api/` is the Waitlist application-level collaboration boundary. It exposes only opaque Waitlist-owned references, idempotent join, exact participant/Event retrieval, and the resulting participation view.

Consumers use this boundary without depending on Waitlist implementation or persistence internals.

## Private implementation boundary

`impl/` owns Waitlist domain/application implementation, persistence port/adapter, migrations, schema, and implementation tests.

## Dependencies

The public boundary may depend on the business-neutral `core` execution context.

The private implementation depends inward on the Waitlist public boundary and has no functional dependency on Event, Registration, Security, HTTP, Spring, or another module's private implementation/persistence.

## Related authorities

- `docs/scope.md` owns the admitted participant waitlist semantics and durable exclusions.
- ADR-0018 owns the Waitlist module and Event-Waitlist composition decision.
- `docs/modules.md` owns the universal module invariant.
- `platform/modules/waitlist/api/` owns concrete public Java contract truth.
- `platform/modules/waitlist/impl/` owns implementation and Waitlist persistence truth.
- `docs/architecture/workspace.dsl` owns Current architecture relationships.
