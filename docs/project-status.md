# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, and Security modules; Event-Management and Event-Registration compositions; independently selectable Event-only and full Platform applications; independently authoritative Event and Event-Registration HTTP source contracts with static application aggregation; PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Authoritative documentation is bounded by `docs/templates/README.md` and deterministically enforced through root `check`. Routine build-affecting developer validation uses `./dev/dev.sh check`, which runs that root check inside the repository-controlled Docker/JDK 21 developer environment.

Routine GitHub issue and pull-request forms capture change-specific evidence without duplicating governance attestations; `docs/governance.md` remains the authority for governance and Definition of Done.

## Active goal

Product Goal #167, **Deliver Event experience v1**, is the active product outcome.

Use-case Goal #168, **Organizer manages own Event**, and Use-case Goal #169, **Participant registers for published Event**, are implemented. Use-case Goal #170, **Organizer views Event registrations**, is the next readiness path.

## In progress

No executable #170 implementation is currently scheduled. Scope #181 admits the organizer read-only Event-registration view; post-scope readiness is the current planning transition.

## Current gaps

Organizer viewing of Event registrations remains to be delivered under Use-case Goal #170.

## Next action

Evaluate post-scope implementation readiness for Use-case Goal #170: the minimum domain-neutral Registration target-query capability, organizer-facing contract and failure/disclosure semantics, and focused/end-to-end validation. If resolved without a significant architecture choice, create one coherent vertical implementation issue.
