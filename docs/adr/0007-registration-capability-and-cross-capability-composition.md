# ADR-0007: Registration capability and cross-capability composition

## Status
- Status: Superseded
- Date: 2026-08-03
- Superseded by: [ADR-0008](0008-domain-neutral-registration-and-event-registration-composition.md)

## Context

The first released platform slice proves one independently bounded Event capability, Event-owned persistence, an HTTP adapter, and an executable runtime.

Post-v0.1.0 product research identified a concrete next workflow: a participant registers participation in an Event that already exists and later retrieves the resulting Registration.

Participant-specific Registration state and uniqueness rules are not Event invariants. Making Event own registrations merely because Registration references an Event would couple independently evolving business concepts.

At the same time, Registration cannot determine whether an Event exists without consuming Event-owned truth.

The platform therefore needs its first concrete decision about ownership across two bounded business capabilities, cross-capability workflow composition, dependency direction, and persistence isolation.

## Decision

Registration is a separate bounded business capability.

Registration owns Registration identity, the Event identity reference stored as `eventId`, `participantReference`, Registration uniqueness rules, Registration persistence, and Registration retrieval.

Event continues to own Event existence and all Event business state.

Registration does not depend on Event API, Event implementation, or Event persistence.

A separate Event-Registration composition owns the workflow that:

1. resolves Event existence through the Event public API;
2. rejects an unknown Event;
3. invokes the Registration public API when the Event exists.

The composition depends only on public capability APIs and the business-neutral execution context. Neither Event nor Registration depends on the other capability.

Registration owns a separate PostgreSQL schema and migration history. Registration persistence has no foreign key to Event persistence and performs no cross-schema Event lookup. Event existence is validated through the Event public application contract.

The complete workflow is exposed through a separate versioned Registration HTTP contract rather than extending the Event HTTP contract.

## Rationale
The decision is retained for the constraints recorded in Context and the trade-offs recorded in Alternatives considered and Consequences; this migration changes document structure only.

## Alternatives considered

### Put Registration inside Event

Rejected because Registration has participant-specific state and uniqueness rules that are not Event invariants and can evolve independently from Event definition and scheduling.

### Let Registration depend directly on Event API

Rejected because that would make one bounded capability orchestrate another capability and establish a directional business dependency where the architecture already provides a composition category for cross-capability workflows.

### Validate Event existence through Registration persistence

Rejected because a foreign key, cross-schema join, or direct Event-table lookup would couple Registration persistence to Event-owned storage and make database structure an implicit cross-capability contract.

### Duplicate Event state into Registration

Rejected because the minimum workflow requires only an Event identity reference. Copying Event fields would introduce synchronization semantics without a demonstrated requirement.

### Add messaging or asynchronous integration

Rejected for this phase because the workflow is synchronous and no accepted requirement justifies asynchronous messaging infrastructure.

## Consequences

The platform gains its first concrete proof of two independently bounded business capabilities cooperating through explicit public contracts.

A new composition project is required for the cross-capability workflow.

Registration requires its own API/implementation boundary, persistence port, PostgreSQL schema, Flyway migrations, and private persistence adapter.

The HTTP interface will depend on the composition for Registration creation and may use Registration API directly for retrieval.

The application runtime may depend on Registration implementation only for technical construction and wiring, consistent with the existing runtime exception.

Executable architecture verification must prevent Event-to-Registration dependencies, Registration-to-Event dependencies, composition dependencies on capability implementations or persistence, cross-capability persistence access, and HTTP dependencies on private capability implementations.

Because Registration persistence intentionally has no Event foreign key, Event existence is protected by the application-level composition rather than the database.

Event deletion and resulting Registration consistency are intentionally deferred because Event deletion is outside accepted scope.

Authentication, participant profiles, capacity, cancellation, ticketing, payment, notification, messaging, frontend, and deployment concerns remain outside this decision.
