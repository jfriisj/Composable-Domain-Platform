# Project Governance

## Purpose

Governance exists to keep project scope, architecture, implementation, and stakeholder understanding aligned as the platform evolves.

## Source-of-truth ownership

Each type of information has one authoritative owner:

| Concern | Authoritative source |
| --- | --- |
| Accepted scope | `docs/scope.md` |
| Current project state | `docs/project-status.md` |
| Development workflow | `docs/workflow.md` |
| Architecture model and diagrams | `docs/architecture/workspace.dsl` |
| Architecture principles and boundary rules | `docs/architecture.md` |
| Module responsibilities | `docs/modules.md` and each implemented module's `module.md` |
| Architecture rationale | `docs/adr/` |
| Approved baseline technologies | `docs/tech-stack.md` |
| External HTTP contract | Versioned OpenAPI contracts under `contracts/http/` |
| Database schema | Bounded-context-owned Flyway migrations; currently Event migrations under `modules/event/impl/src/main/resources/db/migration/event/` |
| Build dependencies and module wiring | Gradle build files |
| Implementation behavior | Source code and automated tests |

Other documents may reference these sources but must not redefine competing versions of the same truth.

The operational sequence for applying these governance rules is defined in [`workflow.md`](workflow.md).

## Git branches

Two permanent branches are used:

- `production` — stable, working, release-ready state.
- `development` — accepted integration state and base for normal work.

Normal work branches from `development` and returns through a pull request.

Allowed topic prefixes are:

- `feat/`
- `fix/`
- `docs/`
- `chore/`
- `refactor/`
- `test/`
- `hotfix/`

Emergency production fixes may use `hotfix/`, branch from `production`, merge to `production`, and then be reconciled back into `development`.

No normal work is performed directly on `development` or `production`.

## Pull requests

A pull request is the change-control boundary for accepted project state.

Every pull request must:

- Have one clear purpose.
- Identify its scope impact.
- Identify architecture impact.
- Identify affected authoritative artifacts.
- Avoid unrelated cleanup or speculative improvements.
- Resolve review conversations before merge.

Topic branches merge into `development`. Releases merge from `development` into `production`.

## Scope control

A useful idea is not automatically accepted scope.

If a change is outside the current scope, it must either be deferred or proposed explicitly as a scope change. Scope changes must update `docs/scope.md` in the same decision flow.

No future capability is implemented solely because the architecture could support it.

## Architecture control

Architecture changes must update the authoritative Structurizr model when they change system relationships, bounded contexts, containers, or significant flows.

Significant architecture decisions require an ADR.

Architecture diagrams have three conceptual states:

- **Current** — accepted architecture represented by accepted repository state.
- **Planned** — accepted future work not yet implemented.
- **Exploratory** — discussion material with no commitment.

Current views must never visually imply that planned or exploratory capabilities are already implemented.

## Module admission

A new bounded context must have a concrete use case, clear ownership, clear non-ownership, meaningful business rules or lifecycle, and an explicit public contract.

A module is not created merely because a concept can be separated technically.

## Technology admission

New technologies require a demonstrated problem and an accepted requirement. Prefer the simplest solution that preserves established boundaries.

Do not introduce infrastructure in anticipation of hypothetical scale or future integration needs.

## Definition of done

Every accepted change must satisfy the applicable definition of done:

- The change is inside accepted scope or explicitly changes scope.
- Ownership is placed in the correct architectural area.
- Relevant authoritative documentation is updated.
- Architecture diagrams are updated when architecture changes.
- Automated tests cover behavior and invariants introduced or changed by implementation work.
- `./gradlew check` succeeds for implementation changes and other changes that affect the executable build.
- No unrelated feature or technology is introduced.
- The pull request is reviewable as one coherent decision.
- Review conversations are resolved before merge.

The detailed validation and merge sequence is defined in [`workflow.md`](workflow.md).
