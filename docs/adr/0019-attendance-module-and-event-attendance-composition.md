# ADR-0019: Attendance module and Event-Attendance composition

## Status

- Status: Accepted
- Date: 2026-09-02
- Decision: #231

## Context

Accepted scope for Use-case Goal #226 introduces a distinct durable Attendance responsibility associated with one existing Registration. Attendance owns an explicit `attended` or `not_attended` outcome, at-most-one state per Registration, idempotent repeat of the same outcome, rejection of correction to a different outcome, organizer-private retrieval, and restart durability.

That responsibility cannot be placed in an existing owner without changing an accepted boundary. Event explicitly does not own or encode Attendance state. Registration remains domain-neutral and explicitly does not own or encode Attendance state. Compositions and application runtimes cannot own durable module behavior or persistence under the universal module invariant and ADR-0017.

The organizer workflow also depends on facts owned elsewhere: Event ownership and publication lifecycle, Registration target and lifecycle, and Security Authentication/Authorization. Those facts must be coordinated through public APIs without functional module-to-module dependencies. The externally addressable workflow must remain independently selectable under ADR-0016.

## Decision

Attendance is admitted as a new independently owned business module under the universal module invariant.

The Attendance module owns:

- the recorded Attendance outcome associated with one opaque Registration reference;
- explicit `attended` and `not_attended` outcomes;
- at-most-one durable Attendance state per Registration;
- idempotent repeat of the same outcome;
- rejection of a different outcome after one outcome is recorded;
- durable exact retrieval by Registration reference;
- its private PostgreSQL persistence boundary and migrations.

Attendance exposes an explicit public API, hides its implementation and persistence, and has no functional dependency on Event, Registration, or Security. It does not own Event identity, ownership, lifecycle, or Registration availability; Registration target, lifecycle, participant identity, or persistence; Authentication/Authorization; Event-specific Attendance eligibility; organizer disclosure mapping; HTTP transport; correction/history/timestamps/reporting; or runtime wiring.

Event-Attendance is admitted as a non-module composition for the #226 cross-capability workflow. It depends only on public APIs:

`Event-Attendance -> Event API + Registration API + Attendance API + Security API`

For first recording, the composition resolves the authenticated actor, obtains Event ownership and lifecycle, asks Security for the final actor-versus-owner authorization decision, resolves the Registration, verifies that it targets the Event and is currently `active`, then invokes Attendance to record the requested outcome. Organizer-private retrieval remains composition-owned because authorization requires Event ownership while the durable state belongs to Attendance.

Event already exposes lifecycle and organizer ownership through its public API. Registration already exposes Registration identity, target, and lifecycle through its public API. Implementation may add only the smallest narrower public query needed if executable evidence shows the existing surfaces are insufficient; neither Event nor Registration ownership expands. Security remains the existing framework-neutral Authentication/Authorization capability.

The Event-Attendance workflow owns an independently authoritative OpenAPI source unit and matching selectable inbound adapter under ADR-0016. The full Platform Application selects Attendance, Event-Attendance, its contract/interface, and Attendance migrations. The Event-only application does not select them.

Attendance reuses the accepted PostgreSQL, Flyway, JVM, Spring, Gradle, and static application-composition mechanisms. One-per-Registration uniqueness is enforced durably inside the Attendance persistence boundary. No new database, service, broker, worker, scheduler, event-sourcing mechanism, infrastructure, or generic relation abstraction is introduced.

## Rationale

A separate module is the smallest construct that can own the accepted durable Attendance responsibility without violating Event or Registration non-ownership or turning a composition/runtime into a state owner.

Keeping organizer eligibility and authorization in Event-Attendance preserves dependency direction: participating modules stay unaware of one another while the actor journey is coordinated explicitly through public contracts.

Independent external contract ownership preserves static selectability and prevents Attendance transport behavior from being folded into Event or Event-Registration surfaces that own different externally addressable behavior.

## Alternatives considered

- Store Attendance in Event — rejected because Event explicitly does not own or encode Attendance state.
- Store Attendance in Registration — rejected because Registration remains domain-neutral and explicitly does not own or encode Attendance state.
- Persist Attendance in Event-Attendance or an application runtime — rejected because compositions/runtimes coordinate or wire behavior but do not own durable module state or persistence.
- Extend Event-Registration to own Attendance — rejected because Attendance is a separate durable organizer capability with independently selectable external behavior.
- Introduce a generic participation/status abstraction — rejected as speculative beyond the accepted #226 outcome.

## Consequences

Implementation of #226 requires a new Attendance public/private module boundary, Attendance-owned schema/migrations, an Event-Attendance composition, an independently selectable Event-Attendance contract/interface, and full-Platform runtime wiring.

The Current architecture model remains unchanged until those constructs are implemented. The implementation change must then update `docs/architecture/workspace.dsl` and the new Attendance module responsibility document to match executable truth.

Exact Java operations, HTTP paths/status/privacy mapping, SQL shape, runtime wiring, and test implementation remain readiness/implementation details constrained by this decision and existing authorities rather than architectural rationale owned by this ADR.
