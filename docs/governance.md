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
| Work tracking, Goal/Subgoal decomposition, and dependencies | GitHub issues |
| Architecture model and diagrams | `docs/architecture/workspace.dsl` |
| Architecture principles and boundary rules | `docs/architecture.md` |
| Module responsibilities | `docs/modules.md` and each implemented module's `module.md` |
| Architecture rationale | `docs/adr/` |
| Approved baseline technologies | `docs/tech-stack.md` |
| Java engineering quality | `docs/engineering/java.md` |
| External HTTP contract | Versioned OpenAPI contracts under `platform/contracts/http/` |
| Database schema | Bounded-context-owned Flyway migrations; currently Event migrations under `platform/modules/event/impl/src/main/resources/db/migration/event/` |
| Build dependencies and module wiring | Gradle build files |
| Implementation behavior | Source code and automated tests |

Other documents may reference these sources but must not redefine competing versions of the same truth.

Language-specific engineering standards live under `docs/engineering/` only when an accepted language requires durable project rules. Each language standard must be registered explicitly in the source-of-truth table above; the directory itself is not a blanket authority and does not authorize standards for hypothetical languages.

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

## Goal hierarchy and proportional delivery

Goal Issues are planning and tracking containers above the existing executable issue types. They use the `type: goal` label and are not executable work. A Goal does not authorize implementation, change accepted scope, accept a bounded context, admit a technology, or replace an authoritative repository artifact.

For product delivery, the maximum planning hierarchy is:

`Product Goal -> Use-case Goal -> executable issues`

### Product Goals

A Product Goal represents a bounded product experience or release-level outcome with an explicit completion boundary. It groups the required Use-case Goals and remains outcome-oriented rather than prescribing modules, technologies, or implementation structure.

### Use-case Goals

A Use-case Goal is a direct `type: goal` child of a Product Goal. It represents one observable actor journey or independently meaningful use-case outcome and defines its actor/outcome, accepted baseline, objective end-to-end acceptance, explicit non-goals, and required executable work.

Goal nesting stops at the Use-case Goal level. A Use-case Goal cannot contain another Goal.

Non-product stakeholder, operator, or developer outcomes may remain single-level Goals when a Product Goal/Use-case Goal hierarchy adds no planning value.

### Executable issues and dependencies

Only the existing executable issue types perform work: decision, scope, research, implementation, defect, and documentation.

Each child records its direct planning parent with `Goal: #...`. Planning hierarchy remains separate from execution dependencies recorded through `Blocked by #...` and `Blocks #...`.

Goal and executable issues are not authoritative scope. Creating them may record exploratory capability hypotheses or alternatives, but those hypotheses remain unaccepted until the applicable scope, architecture, technology, and decision controls are satisfied.

### Priority and decomposition

There should normally be only one active top-level Product Goal or single-level Goal with `priority: now`. Multiple Use-case Goals or executable issues may be active when they belong to that coherent workstream, have resolved prerequisites, independent ownership and validation, and do not make uncontrolled competing changes to the same authoritative truth.

Goal decomposition is progressive:

- `priority: later` — keep the Product Goal or single-level Goal outcome-level; do not pre-design bounded contexts or implementation.
- `priority: next` — identify the required Use-case Goals and only the unresolved research, decisions, scope, and dependencies needed for readiness.
- `priority: now` — execute only ready Use-case Goals and executable issues.

A Use-case Goal is decomposed only enough to expose genuinely required executable work and dependencies. Technical implementation stages are not automatically separate issues.

### Proportional process

For an accepted, ready Use-case Goal, the normal executable path is the smallest coherent path that preserves repository truth:

`scope/readiness -> vertical implementation -> acceptance`

Research, decision, scope, ADR, or documentation work is created separately only when that concern is genuinely unresolved, independently reviewable as its own change, or required by another governance rule. Lifecycle stages do not require separate persistent artifacts by default.

A single coherent implementation issue or pull request may deliver the complete accepted vertical slice, including directly affected authoritative documentation, when ownership, scope, dependencies, and validation are already resolved.

### Change-local re-evaluation

After accepted progress merges:

1. re-read remote `development` and verify the merged result;
2. re-read only the directly changed authoritative artifacts;
3. re-read directly dependent issues and the direct parent Goal chain;
4. update readiness, dependencies, or Goal acceptance only where the accepted change affects them;
5. then select the next ready action.

Do not turn routine post-merge verification into a broad project audit without evidence that wider authority is affected.

### Goal completion

A Use-case Goal is complete only when its required executable work is complete, its objective end-to-end acceptance is proven in accepted `development`, and no dependency required for that use case remains unresolved.

A Product Goal is complete only when its required Use-case Goals are complete, its aggregate completion boundary is satisfied in accepted `development`, applicable project status is synchronized, and no dependency required for the Product Goal remains unresolved.

A single-level non-product Goal follows the same applicable completion rules. Closing any Goal does not retroactively authorize work that bypassed accepted scope or governance.

## Architecture control

Architecture changes must update the authoritative Structurizr model when they change system relationships, bounded contexts, containers, or significant flows.

Significant architecture decisions require an ADR.

Architecture diagrams have three conceptual states:

- **Current** — accepted architecture represented by accepted repository state.
- **Planned** — accepted future work not yet implemented.
- **Exploratory** — discussion material with no commitment.

Current views must never visually imply that planned or exploratory capabilities are already implemented.

## Universal module control

Every architectural construct classified as a module must satisfy ADR-0013 and `docs/modules.md`.

A module is independently owned, selectable in application composition, exposes an explicit public API, hides its private implementation, and collaborates through public contracts/adapters.

The application runtime may select, construct, configure, and wire modules but must not own or implement them. Another module or a composition must not own or implement a module.

A construct that does not satisfy the universal module invariant must be classified explicitly as a non-module architectural construct rather than receiving a weaker module rule.

## Module admission

Before admitting a new module, establish a concrete use case, independent ownership and non-ownership, the smallest public API, the private implementation boundary, explicit public dependencies, selectable composition semantics, and objective independent validation.

A new bounded business context additionally requires meaningful business rules, invariants, policy, or independently evolving lifecycle.

A module is not created merely because a concept can be separated technically. Once accepted as a module, its implementation is not placed in the application runtime, another module, or a composition.

## Technology admission

New technologies require a demonstrated problem and an accepted requirement. Prefer the simplest solution that preserves established boundaries.

Do not introduce infrastructure in anticipation of hypothetical scale or future integration needs.

## Definition of done

Every accepted change must satisfy the applicable definition of done:

- The change is inside accepted scope or explicitly changes scope.
- Ownership is placed in the correct architectural area.
- Every affected module preserves the universal independent-module invariant from ADR-0013.
- Relevant authoritative documentation is updated.
- Architecture diagrams are updated when architecture changes.
- Automated tests cover behavior and invariants introduced or changed by implementation work.
- `./gradlew check` succeeds for implementation changes and other changes that affect the executable build.
- No unrelated feature or technology is introduced.
- The pull request is reviewable as one coherent decision.
- Review conversations are resolved before merge.

The detailed validation and merge sequence is defined in [`workflow.md`](workflow.md).
