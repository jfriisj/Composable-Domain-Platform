# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, Waitlist, and Security modules; Event-Management, Event-Registration, and Event-Waitlist compositions; independently selectable Event-only and full Platform applications; independently authoritative Event, Event-Registration, and Event-Waitlist HTTP source contracts with static application aggregation; module-owned PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Registration owns identity-preserving `active -> cancelled` and `cancelled -> active` lifecycle transitions with atomic expected-state persistence updates. Event-Registration admits participant re-participation through the same durable Registration only for participant-owned cancelled Event Registrations whose Event is published and whose Registration availability is open; already-active reactivation is idempotent and does not re-evaluate Event eligibility. The full Platform application exposes this workflow through `PUT /api/v1/event-registrations/{registrationId}` while the Event-only application continues to exclude Event-Registration.

Authoritative documentation is bounded by `docs/templates/README.md` and deterministically enforced through root `check`. Routine build-affecting developer validation uses `./dev/dev.sh check`, which runs that root check inside the repository-controlled Docker/JDK 21 developer environment.

Routine GitHub issue and pull-request forms capture change-specific evidence without duplicating governance attestations; `docs/governance.md` remains the authority for governance and Definition of Done.

## Active goal

None. Product Goal #185, **Support participant re-registration lifecycle**, and Use-case Goal #220, **Participant regains Event participation after cancellation**, are complete after implementation #223 / PR #224.

## In progress

None.

## Current gaps

No additional Registration lifecycle or participation policy is admitted by the completed bounded participant re-participation journey beyond same-Registration `cancelled -> active` under the accepted Event Registration eligibility rules.

The remaining open roadmap Product Goals are deliberately deferred and do not authorize implementation until promoted through the normal scope/readiness flow.

## Next action

Select the next Product Goal through normal governance and readiness. No Product Goal or executable implementation is currently `priority: now` or `priority: next`.
