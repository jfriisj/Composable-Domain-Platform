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

Attendance remains outside accepted scope until scope transition #227 is accepted into `development`.

## In progress

Scope transition #227, **Admit organizer-recorded Event attendance**, proposes the minimum semantic Attendance responsibility and organizer recording/retrieval boundary. No implementation is authorized.

## Current gaps

Implementation is blocked pending acceptance of #227 and post-merge readiness. Physical module placement, cross-capability collaboration, persistence ownership, external contract behavior, deterministic failure mapping, runtime wiring, and any required ADR remain deliberately unresolved until the scope transition is accepted.

The other open roadmap Product Goals remain deliberately deferred.

## Next action

Accept or reject scope transition #227 through one documentation-only PR. After acceptance, re-read the directly relevant architecture/module and executable Event/Registration/Security boundaries and resolve post-scope readiness before creating implementation work.
