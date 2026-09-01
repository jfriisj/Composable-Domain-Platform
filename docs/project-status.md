# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, Waitlist, and Security modules; Event-Management, Event-Registration, and Event-Waitlist compositions; independently selectable Event-only and full Platform applications; independently authoritative Event, Event-Registration, and Event-Waitlist HTTP source contracts with static application aggregation; module-owned PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Authoritative documentation is bounded by `docs/templates/README.md` and deterministically enforced through root `check`. Routine build-affecting developer validation uses `./dev/dev.sh check`, which runs that root check inside the repository-controlled Docker/JDK 21 developer environment.

Routine GitHub issue and pull-request forms capture change-specific evidence without duplicating governance attestations; `docs/governance.md` remains the authority for governance and Definition of Done.

## Active goal

Product Goal #185, **Support participant re-registration lifecycle**, is `priority: next`, with Use-case Goal #220, **Participant regains Event participation after cancellation**, selected for bounded participant re-participation.

The Goal does not authorize implementation. Accepted scope now admits only same-Registration `cancelled -> active` re-participation under normal Event Registration eligibility.

## In progress

Post-scope readiness for Use-case Goal #220. No executable implementation issue is currently authorized.

## Current gaps

The semantic scope is resolved. Remaining readiness must verify the smallest Registration public capability, Event-Registration orchestration, external failure/disclosure semantics, persistence and concurrency behavior, Waitlist interaction without cross-lifecycle mutation, restart proof, and whether any significant architecture rationale requires an ADR.

The remaining open roadmap Product Goals stay deliberately deferred until separately promoted.

## Next action

Re-read the directly relevant Registration, Event, Event-Registration, contract, persistence, source/test, and architecture authorities for #220; resolve readiness and create one coherent implementation issue only if no separate decision or ADR is required.
