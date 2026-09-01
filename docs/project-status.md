# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, Waitlist, and Security modules; Event-Management, Event-Registration, and Event-Waitlist compositions; independently selectable Event-only and full Platform applications; independently authoritative Event, Event-Registration, and Event-Waitlist HTTP source contracts with static application aggregation; module-owned PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Registration owns identity-preserving `active -> cancelled` and `cancelled -> active` lifecycle transitions with atomic expected-state persistence updates. Event-Registration admits participant re-participation through the same durable Registration only for participant-owned cancelled Event Registrations whose Event is published and whose Registration availability is open; already-active reactivation is idempotent and does not re-evaluate Event eligibility. The full Platform application exposes this workflow through `PUT /api/v1/event-registrations/{registrationId}` while the Event-only application continues to exclude Event-Registration.

Authoritative documentation is bounded by `docs/templates/README.md` and deterministically enforced through root `check`. Routine build-affecting developer validation uses `./dev/dev.sh check`, which runs that root check inside the repository-controlled Docker/JDK 21 developer environment.

Routine GitHub issue and pull-request forms capture change-specific evidence without duplicating governance attestations; `docs/governance.md` remains the authority for governance and Definition of Done.

## Active goal

Product Goal #185, **Support participant re-registration lifecycle**, is `priority: next`, with Use-case Goal #220, **Participant regains Event participation after cancellation**, selected for bounded participant re-participation.

Accepted scope and executable implementation now support same-Registration `cancelled -> active` re-participation under the bounded Event Registration eligibility rules.

## In progress

Acceptance and Goal-state reconciliation for Use-case Goal #220 after the coherent implementation authorized by #223.

## Current gaps

No implementation-readiness gap remains for #220. Acceptance must verify the merged `development` state against the Use-case Goal journey and reconcile #223, #220, and parent Product Goal #185 only where the accepted evidence changes their state.

The remaining open roadmap Product Goals stay deliberately deferred until separately promoted.

## Next action

Merge the validated #223 implementation to `development`, re-read accepted remote state, and perform bounded acceptance/reconciliation for Use-case Goal #220 and Product Goal #185.
