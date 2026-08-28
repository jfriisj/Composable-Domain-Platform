# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, and Security modules; Event-Management and Event-Registration compositions; independently selectable Event-only and full Platform applications; independently authoritative Event and Event-Registration HTTP source contracts with static application aggregation; PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Authoritative documentation is bounded by `docs/templates/README.md` and deterministically enforced through root `check`. Routine build-affecting developer validation uses `./dev/dev.sh check`, which runs that root check inside the repository-controlled Docker/JDK 21 developer environment.

Routine GitHub issue and pull-request forms capture change-specific evidence without duplicating governance attestations; `docs/governance.md` remains the authority for governance and Definition of Done.

## Active goal

Product Goal #184, **Manage Event waitlist participation**, is the selected next product outcome. Use-case Goal #206, **Participant joins and retrieves own Event waitlist participation**, is the current readiness path.

## In progress

Post-scope readiness for Use-case Goal #206 is the current work.

## Current gaps

The bounded participant waitlist journey is accepted in scope through #207, but physical capability/module ownership, ADR need, contract/persistence impact, and implementation validation remain unresolved. No waitlist implementation is implementation-ready yet.

## Next action

Re-read only the directly relevant architecture/module boundaries and executable Event/Registration/Security truth, then resolve module admission, ADR need, contract/persistence impact, and implementation readiness for #206.
