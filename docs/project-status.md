# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, and Security modules; Event-Management and Event-Registration compositions; independently selectable Event-only and full Platform applications; independently authoritative Event and Event-Registration HTTP source contracts with static application aggregation; PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Authoritative documentation is bounded by `docs/templates/README.md` and deterministically enforced through root `check`. Routine build-affecting developer validation uses `./dev/dev.sh check`, which runs that root check inside the repository-controlled Docker/JDK 21 developer environment.

Routine GitHub issue and pull-request forms capture change-specific evidence without duplicating governance attestations; `docs/governance.md` remains the authority for governance and Definition of Done.

## Active goal

Single-level developer Goal #208, **Enable isolated parallel developer workspaces**, is the active `priority: now` workstream. Product Goal #184, **Manage Event waitlist participation**, and Use-case Goal #206 remain the selected `priority: next` roadmap readiness path.

## In progress

Post-scope implementation readiness for Goal #208 is the current work after #209 accepts the isolated local workspace capability and browser-editor technology direction.

## Current gaps

The developer outcome is accepted, but the minimum worktree-aware `dev.sh`/Compose lifecycle, browser-service packaging and pinning, local port allocation, persistent editor state, and executable isolation proof remain unresolved. No developer-environment implementation is implementation-ready yet. Separately, the bounded participant waitlist journey is accepted through #207 but remains implementation-unready pending its post-scope readiness.

## Next action

Re-read only `dev/Dockerfile`, `dev/compose.yaml`, `dev/dev.sh`, and `dev/README.md`, resolve the minimum implementation surface and validation for Goal #208, and create one coherent implementation issue only if readiness is complete; #184/#206 remain the next product readiness path.
