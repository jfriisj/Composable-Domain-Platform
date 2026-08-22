# Event Module

## Purpose

Own the Event business capability for definition, durable retrieval, publication, and discovery.

## Owns

- Event identity, descriptive state, schedule, and timezone.
- Invariants required for a valid Event.
- The one-way unpublished-to-published lifecycle and discovery of published Events.
- Event-owned durable state, persistence boundary, and retrieval semantics.

## Does not own

- Registration or Event-Registration workflow.
- Participant authentication or authorization.
- Registration eligibility, capacity, waitlists, ticketing, payments, or notifications.
- HTTP transport mapping, application runtime assembly, or persistence for another module.

## Public boundary

`api/` is the Event application-level collaboration boundary. Consumers use this boundary rather than Event domain, implementation, or persistence internals.

The Java source under `api/` is authoritative for the concrete public types and operation semantics.

## Private implementation boundary

`impl/` owns the Event domain and application implementation together with Event-owned outbound ports, persistence adapters, migrations, and implementation tests.

These details remain private to Event and are not a collaboration surface for other modules.

## Dependencies

The public boundary may depend on the business-neutral `core` execution context required by the accepted application contract. The private implementation depends inward on the Event public boundary.

Event has no functional dependency on another business module and does not use another module's private implementation or persistence.

## Related authorities

- `docs/modules.md` and ADR-0013 define the universal module invariant.
- ADR-0005 records Event persistence rationale.
- `platform/modules/event/api/` owns concrete public Java contract truth.
- `platform/modules/event/impl/` owns implementation, tests, and Event persistence truth.
- Event Gradle build files own build dependency truth.
- `platform/contracts/http/v1/event.yaml` owns the external Event HTTP contract.
- `docs/architecture/workspace.dsl` owns current architecture relationships.
