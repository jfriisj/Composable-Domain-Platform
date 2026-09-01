# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, Waitlist, and Security modules; Event-Management, Event-Registration, and Event-Waitlist compositions; independently selectable Event-only and full Platform applications; independently authoritative Event, Event-Registration, and Event-Waitlist HTTP source contracts with static application aggregation; module-owned PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Authoritative documentation is bounded by `docs/templates/README.md` and deterministically enforced through root `check`. Routine build-affecting developer validation uses `./dev/dev.sh check`, which runs that root check inside the repository-controlled Docker/JDK 21 developer environment.

Routine GitHub issue and pull-request forms capture change-specific evidence without duplicating governance attestations; `docs/governance.md` remains the authority for governance and Definition of Done.

## Active goal

None. Product Goal #184, **Manage Event waitlist participation**, and Use-case Goal #206, **Participant joins and retrieves own Event waitlist participation**, are complete after implementation #216 / PR #217.

## In progress

None.

## Current gaps

No additional waitlist lifecycle is admitted by the completed bounded participant waitlist journey. Ordered/ranked waitlists, capacity, automatic promotion or Registration creation, organizer waitlist management/view, participant cancellation/removal, notifications, and the other durable exclusions in `docs/scope.md` remain outside accepted scope.

The remaining open roadmap Product Goals are deliberately deferred and do not authorize implementation until promoted through the normal scope/readiness flow.

## Next action

Select the next Product Goal through normal governance and readiness. No new Product Goal or executable implementation is currently `priority: now` or `priority: next`.
