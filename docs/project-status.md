# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, Waitlist, and Security modules; Event-Management, Event-Registration, and Event-Waitlist compositions; independently selectable Event-only and full Platform applications; independently authoritative Event, Event-Registration, and Event-Waitlist HTTP source contracts with static application aggregation; module-owned PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Authoritative documentation is bounded by `docs/templates/README.md` and deterministically enforced through root `check`. Routine build-affecting developer validation uses `./dev/dev.sh check`, which runs that root check inside the repository-controlled Docker/JDK 21 developer environment.

Routine GitHub issue and pull-request forms capture change-specific evidence without duplicating governance attestations; `docs/governance.md` remains the authority for governance and Definition of Done.

## Active goal

Product Goal #185, **Support participant re-registration lifecycle**, is `priority: next` for readiness preparation.

The Goal does not authorize implementation. Current accepted scope still excludes same-pair Registration re-registration/reactivation.

## In progress

Readiness preparation for Product Goal #185. No executable implementation issue is currently authorized.

## Current gaps

The participant-visible semantics after cancelling an Event Registration are not yet admitted for same-pair re-registration/reactivation. Identity semantics, lifecycle behavior, uniqueness implications, interaction with Event Registration availability, and interaction with waitlist eligibility must be resolved through scope/readiness before implementation.

The remaining open roadmap Product Goals stay deliberately deferred until separately promoted.

## Next action

Define the minimum participant-visible Use-case Goal under #185, then make the required scope/readiness transition for same-pair re-registration/reactivation before any implementation design or executable implementation issue.
