# Project Status

## Authority

This document owns current project state only. Accepted scope is in [`scope.md`](scope.md); work planning and dependencies are in GitHub Issues; history is in Git, pull requests, issues, and ADRs.

## Current state

The accepted executable platform contains the Event, Registration, and Security modules; Event-Registration composition; independently selectable Event-only and full Platform applications; independently authoritative Event and Event-Registration HTTP source contracts with static application aggregation; PostgreSQL persistence; the executable JVM runtime boundary; and the repository-controlled Docker developer environment.

Issue #158 established the bounded authoritative-documentation model, templates, line budgets, reference-not-repeat rule, and change-local authority resolution.

The documentation migration represented by #160 brings the currently registered authoritative documents into that accepted structure and budget without changing product, architecture, contract, persistence, build, or executable semantics.

## Active goal

Goal #157, **Reduce documentation and workflow execution overhead**, remains the active single-level non-product Goal.

After the documentation migration, its remaining outcome is deterministic enforcement of the accepted documentation model and removal of the temporary Goal #157 transition allowance.

## In progress

No build-affecting enforcement work is yet accepted or in progress.

Issue #160 is satisfied by the documentation migration represented by this state; after it is accepted into `development`, the next executable work is the minimum deterministic checker integration required by `docs/templates/README.md`.

## Current gaps

- Deterministic template/structure/size enforcement is specified but is not yet integrated into normal repository validation.
- The Goal #157 transition allowance remains until the enforcement follow-up is accepted.
- Goal #157 is therefore not complete solely from the documentation migration.

No product/runtime gap is inferred from this documentation work.

## Next action

After #160 is accepted into `development`, create and execute only the minimum build-affecting enforcement issue required to integrate the deterministic documentation checker into normal repository validation, remove the Goal #157 transition allowance, and prove the Goal completion boundary.
