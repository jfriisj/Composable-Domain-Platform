# ADR-0018: Waitlist module and Event-Waitlist composition

## Status

- Status: Accepted
- Date: 2026-08-28
- Decision: #211

## Context

Accepted scope for Use-case Goal #206 introduces a distinct durable Waitlist participation responsibility with its own identity, opaque participant/Event references, participant/Event uniqueness, idempotent join semantics, private retrieval, and restart durability.

That responsibility cannot be placed in an existing owner without changing an accepted boundary. Event explicitly does not own waitlists. Registration remains a domain-neutral registrant-to-target capability and does not own or encode Waitlist participation. ADR-0017 also prohibits compositions and application runtimes from owning module behavior, state, or persistence.

The participant join workflow needs facts from Event lifecycle/Registration availability and Registration occupancy before creating Waitlist participation. Those facts must be coordinated without functional module-to-module dependencies. The externally addressable workflow must also remain independently selectable under ADR-0016.

## Decision

Waitlist is admitted as a new independently owned business module under the universal module invariant.

The Waitlist module owns:

- Waitlist participation identity;
- opaque participant and Event references;
- at-most-one durable participation per participant/Event pair;
- idempotent repeat of the same participant/Event join intent;
- durable retrieval of Waitlist participation;
- its private PostgreSQL persistence boundary and migrations.

Waitlist exposes an explicit public API, hides its implementation and persistence, and has no functional dependency on Event, Registration, or Security. It does not own Event lifecycle/availability, Registration occupancy, Authentication/Authorization, HTTP mapping, ordering, capacity, promotion, cancellation/removal, notifications, or runtime wiring.

Event-Waitlist is admitted as a non-module composition for the #206 cross-capability workflow. It depends only on public APIs:

`Event-Waitlist -> Event API + Registration API + Waitlist API + Security API`

For participant join, the composition resolves the authenticated participant, verifies Event existence plus `published` lifecycle plus closed new-Registration availability, verifies that the participant/Event pair is not occupied by Registration, and then invokes the Waitlist public join capability. Participant-private retrieval likewise remains composition-owned where cross-capability authentication/authorization or external privacy mapping is required.

Event already exposes lifecycle and Registration availability, so this decision adds no Event ownership. Registration remains domain-neutral; implementation may add the smallest public exact participant/target occupancy query required by the workflow rather than exposing persistence or broadening Registration semantics. Security remains the existing framework-neutral Authentication/Authorization capability.

The Event-Waitlist workflow owns an independently authoritative OpenAPI source unit and matching selectable inbound adapter under ADR-0016. The full Platform Application selects Waitlist, Event-Waitlist, its contract/interface, and Waitlist migrations. The Event-only application does not select them.

Waitlist reuses the accepted PostgreSQL, Flyway, JVM, Spring, Gradle, and static application-composition mechanisms. No new database, service, broker, worker, scheduler, infrastructure, or generic participation abstraction is introduced.

## Rationale

A separate module is the smallest construct that can own the accepted durable responsibility without violating Event or Registration boundaries or making a composition/runtime an ownership sink.

Keeping eligibility in Event-Waitlist preserves ADR-0017 dependency direction: modules stay unaware of one another while the cross-capability actor journey is coordinated explicitly through public contracts.

Independent external contract ownership preserves ADR-0016 selectability and avoids coupling Waitlist transport to the Event or Event-Registration surfaces.

## Alternatives considered

- Store Waitlist participation in Event — rejected because Event explicitly does not own waitlists and participant/Event participation is not Event aggregate state.
- Encode Waitlist participation as Registration state or type — rejected because accepted scope makes the responsibility distinct and Registration must remain domain-neutral with its accepted lifecycle.
- Persist Waitlist state in a composition or application runtime — rejected because ADR-0013 and ADR-0017 prohibit those constructs from owning module state/persistence.
- Extend Event-Registration to own the Waitlist workflow and external contract — rejected because it would blur a separate durable capability and independently selectable behavior surface into a Registration-specific construct.
- Introduce a generic participation/relation abstraction — rejected as speculative beyond the accepted #206 outcome.

## Consequences

Implementation of #206 requires a new Waitlist public/private module boundary, Waitlist-owned schema/migrations, an Event-Waitlist composition, an independently selectable Event-Waitlist contract/interface, and full-Platform runtime wiring.

The Current architecture model remains unchanged until those constructs are implemented. The implementation change must then update `docs/architecture/workspace.dsl` and the new module responsibility document to match executable truth.

Exact Java operations, HTTP paths/status mappings, SQL shape, and test implementation remain readiness/implementation details constrained by this decision and existing authorities rather than architectural rationale owned by this ADR.
