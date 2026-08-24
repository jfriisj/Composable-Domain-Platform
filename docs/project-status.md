# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, and Security modules; Event-Management and Event-Registration compositions; independently selectable Event-only and full Platform applications; independently authoritative Event and Event-Registration HTTP source contracts with static application aggregation; PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Authoritative documentation is bounded by `docs/templates/README.md` and deterministically enforced through root `check`. Routine build-affecting developer validation uses `./dev/dev.sh check`, which runs that root check inside the repository-controlled Docker/JDK 21 developer environment.

Routine GitHub issue and pull-request forms capture change-specific evidence without duplicating governance attestations; `docs/governance.md` remains the authority for governance and Definition of Done.

## Active goal

Product Goal #183, **Control Event registration availability**, with selected Use-case Goal #198, **Organizer closes and reopens Event registration**.

## In progress

Scope transition #199 admits manual organizer-controlled new-Registration availability for a published Event while preserving current dependency direction and the existing Registration and Security non-ownership boundaries.

## Current gaps

The accepted scope now defines the #198 outcome and semantic ownership, but implementation readiness is not yet resolved.

Post-scope readiness must determine the minimum Event public capability/state and persistence impact, Event-Management authorization flow, Event-Registration eligibility semantics, external contract/failure behavior, validation proof, and whether the Event responsibility expansion is significant enough to require an ADR.

No implementation issue is ready yet.

## Next action

Re-read the accepted Event module and architecture boundaries after #199, decide whether an ADR is required, and resolve the remaining #198 implementation readiness. Create one coherent vertical implementation issue only if readiness is fully resolved.
