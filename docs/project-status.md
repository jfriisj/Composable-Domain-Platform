# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, and Security modules; Event-Management and Event-Registration compositions; independently selectable Event-only and full Platform applications; independently authoritative Event and Event-Registration HTTP source contracts with static application aggregation; PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Authoritative documentation is bounded by `docs/templates/README.md` and deterministically enforced through root `check`. Routine build-affecting developer validation uses `./dev/dev.sh check`, which runs that root check inside the repository-controlled Docker/JDK 21 developer environment.

Routine GitHub issue and pull-request forms capture change-specific evidence without duplicating governance attestations; `docs/governance.md` remains the authority for governance and Definition of Done.

## Active goal

Product Goal #167, **Deliver Event experience v1**, is the active product outcome.

Use-case Goal #168, **Organizer manages own Event**, is implemented. Use-case Goal #169, **Participant registers for published Event**, is the current readiness path. Use-case Goal #170 (*Organizer views Event registrations*) remains scheduled for subsequent delivery.

## In progress

Published-Event eligibility scope (#177) is accepted for Use-case Goal #169. No #169 implementation is yet accepted; implementation remains blocked pending post-scope readiness.

## Current gaps

Executable Event-Registration creation still accepts an existing Event without requiring it to be `published`. Use-case Goal #169 must close that gap while preserving accepted Registration privacy, lifecycle, uniqueness, and durability semantics. Organizer viewing of Event registrations remains later under #170.

## Next action

Re-read accepted `development` after #177 and evaluate implementation readiness for Use-case Goal #169. If ownership, failure semantics, contract impact, and validation are resolved without a significant architecture choice, create one coherent vertical implementation issue.
