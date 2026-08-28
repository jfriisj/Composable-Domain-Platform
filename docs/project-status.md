# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, and Security modules; Event-Management and Event-Registration compositions; independently selectable Event-only and full Platform applications; independently authoritative Event and Event-Registration HTTP source contracts with static application aggregation; PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Authoritative documentation is bounded by `docs/templates/README.md` and deterministically enforced through root `check`. Routine build-affecting developer validation uses `./dev/dev.sh check`, which runs that root check inside the repository-controlled Docker/JDK 21 developer environment.

Routine GitHub issue and pull-request forms capture change-specific evidence without duplicating governance attestations; `docs/governance.md` remains the authority for governance and Definition of Done.

## Active goal

Single-level developer Goal #208, **Enable isolated parallel developer workspaces**, is complete after implementation #213. Product Goal #184, **Manage Event waitlist participation**, and Use-case Goal #206 remain the selected `priority: next` roadmap readiness path.

## In progress

None.

## Current gaps

The bounded participant waitlist journey has accepted architecture direction through #211 / ADR-0018 but remains implementation-unready pending exact API, contract/failure, persistence, runtime, and validation readiness.

## Next action

Resume post-scope readiness for Use-case Goal #206 by resolving exact API, contract/failure, persistence, runtime, and validation readiness from accepted `development`.
