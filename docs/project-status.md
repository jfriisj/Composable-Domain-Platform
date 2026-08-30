# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, Waitlist, and Security modules; Event-Management, Event-Registration, and Event-Waitlist compositions; independently selectable Event-only and full Platform applications; independently authoritative Event, Event-Registration, and Event-Waitlist HTTP source contracts with static application aggregation; module-owned PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Authoritative documentation is bounded by `docs/templates/README.md` and deterministically enforced through root `check`. Routine build-affecting developer validation uses `./dev/dev.sh check`, which runs that root check inside the repository-controlled Docker/JDK 21 developer environment.

Routine GitHub issue and pull-request forms capture change-specific evidence without duplicating governance attestations; `docs/governance.md` remains the authority for governance and Definition of Done.

## Active goal

Use-case Goal #206, **Participant joins and retrieves own Event waitlist participation**, is complete after implementation #216. Product Goal #184, **Manage Event waitlist participation**, remains the roadmap container pending post-merge acceptance review against its remaining completion boundary.

## In progress

None after #216 is accepted into `development`.

## Current gaps

No additional waitlist lifecycle is admitted by #216. Ordered/ranked waitlists, capacity, automatic promotion or Registration creation, organizer waitlist management/view, participant cancellation/removal, notifications, and the other durable exclusions in `docs/scope.md` remain outside accepted scope.

## Next action

After #216 is accepted into `development`, verify #206 completion, reassess Product Goal #184 only against accepted evidence and scope, and then select the next ready roadmap action through the normal governance/readiness flow.
