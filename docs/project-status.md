# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, and Security modules; Event-Registration composition; independently selectable Event-only and full Platform applications; independently authoritative Event and Event-Registration HTTP source contracts with static application aggregation; PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Authoritative documentation is bounded by `docs/templates/README.md` and deterministically enforced through root `check`. Routine build-affecting developer validation uses `./dev/dev.sh check`, which runs that root check inside the repository-controlled Docker/JDK 21 developer environment.

Routine GitHub issue and pull-request forms capture change-specific evidence without duplicating governance attestations; `docs/governance.md` remains the authority for governance and Definition of Done.

## Active goal

Product Goal #167, **Deliver Event experience v1**, is the active product outcome.

Use-case Goal #168, **Organizer manages own Event**, is the current readiness path. Later Use-case Goals #169 (*Participant registers for published Event*) and #170 (*Organizer views Event registrations*) remain scheduled for subsequent delivery.

## In progress

Organizer-owned Event management scope (#171) and composition-owned delivery architecture (ADR-0017 / #173) are accepted for Use-case Goal #168.

No organizer-management implementation is yet accepted. Implementation proceeds once post-decision readiness is confirmed for the ready vertical slice.

## Current gaps

Accepted scope now includes authenticated Event creation with durable organizer ownership, owner-authorized unpublished Event modification, and owner-authorized publication.

These behaviors are not yet implemented in executable contracts, adapters, Event behavior, persistence, Security collaboration, compositions, or runtime wiring.

## Next action

Re-read accepted `development` after #173 and evaluate implementation readiness for Use-case Goal #168.

If no new semantic requirement or significant architecture uncertainty remains, create one coherent vertical implementation issue covering the ready slice. If an unresolved decision remains, create only the minimum required readiness transition first.
