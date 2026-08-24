# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, and Security modules; Event-Management and Event-Registration compositions; independently selectable Event-only and full Platform applications; independently authoritative Event and Event-Registration HTTP source contracts with static application aggregation; PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Authoritative documentation is bounded by `docs/templates/README.md` and deterministically enforced through root `check`. Routine build-affecting developer validation uses `./dev/dev.sh check`, which runs that root check inside the repository-controlled Docker/JDK 21 developer environment.

Routine GitHub issue and pull-request forms capture change-specific evidence without duplicating governance attestations; `docs/governance.md` remains the authority for governance and Definition of Done.

## Active goal

Product Goal #182, **Extend published Event lifecycle**, is the active product outcome.

Use-case Goal #193, **Organizer withdraws published Event**, is the selected observable journey. Scope transition #194 admits the terminal withdrawn Event lifecycle and its visitor/participant effects.

## In progress

Post-scope implementation readiness for Use-case Goal #193.

## Current gaps

Executable withdrawal behavior is not yet implemented. Readiness must resolve only the minimum organizer-facing contract/failure semantics, Event lifecycle public capability and persistence impact, Event-Registration eligibility behavior for withdrawn Events, and focused/end-to-end validation.

## Next action

Evaluate post-scope implementation readiness for Use-case Goal #193. If ownership, contract, persistence, dependency, and validation details are resolved without a significant architecture decision, create one coherent vertical implementation issue.
