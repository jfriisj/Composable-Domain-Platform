# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, Waitlist, and Security modules; Event-Management, Event-Registration, and Event-Waitlist compositions; independently selectable Event-only and full Platform applications; independently authoritative Event, Event-Registration, and Event-Waitlist HTTP source contracts with static application aggregation; module-owned PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Registration owns identity-preserving `active -> cancelled` and `cancelled -> active` lifecycle transitions with atomic expected-state persistence updates. Event-Registration admits participant re-participation through the same durable Registration only for participant-owned cancelled Event Registrations whose Event is published and whose Registration availability is open; already-active reactivation is idempotent and does not re-evaluate Event eligibility. The full Platform application exposes this workflow through `PUT /api/v1/event-registrations/{registrationId}` while the Event-only application continues to exclude Event-Registration.

Authoritative documentation is bounded by `docs/templates/README.md` and deterministically enforced through root `check`. Routine build-affecting developer validation uses `./dev/dev.sh check`, which runs that root check inside the repository-controlled Docker/JDK 21 developer environment.

Routine GitHub issue and pull-request forms capture change-specific evidence without duplicating governance attestations; `docs/governance.md` remains the authority for governance and Definition of Done.

## Active goal

Product Goal #186, **Deliver Event attendance experience**, is `priority: next`, with Use-case Goal #226, **Organizer records and retrieves Event attendance**, selected for the bounded organizer attendance journey.

The bounded organizer-recorded Attendance semantics are accepted through scope transition #227 / PR #228.

## In progress

Post-scope readiness for Use-case Goal #226. No Attendance implementation issue is authorized or in progress yet.

## Current gaps

Attendance semantic scope is accepted. Implementation remains blocked until post-scope readiness resolves physical module placement, cross-capability collaboration through public APIs, persistence ownership and one-per-Registration uniqueness, organizer-private authorization/disclosure and deterministic failure semantics, external contract shape, runtime wiring/restart proof, and whether the durable responsibility requires an ADR.

The other open roadmap Product Goals remain deliberately deferred.

## Next action

Re-read the directly relevant architecture/module boundaries and executable Event/Registration/Security contract, source, and persistence truth. Decide whether Attendance requires a distinct module and ADR, then create one coherent implementation issue only if readiness is fully resolved.
