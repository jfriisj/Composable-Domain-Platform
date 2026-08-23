# Project Governance

## Purpose

Governance keeps accepted scope, architecture, implementation, and project state aligned. Repository truth is concern-specific; no planning artifact, chat history, or tool output overrides the source that owns a concern.

## Authority map

| Concern | Authoritative source |
| --- | --- |
| Accepted scope | `docs/scope.md` |
| Current project state | `docs/project-status.md` |
| Documentation structure/responsibility/size | `docs/templates/README.md` |
| Operational development workflow | `docs/workflow.md` |
| Work tracking, Goals, dependencies, priority | GitHub Issues |
| Architecture model/relationships | `docs/architecture/workspace.dsl` |
| Architecture principles/boundaries | `docs/architecture.md` |
| Universal module semantics | `docs/modules.md` |
| Implemented module responsibility | `platform/modules/*/module.md` |
| Significant architecture rationale | `docs/adr/` |
| Accepted technology direction | `docs/tech-stack.md` |
| Java engineering quality | `docs/engineering/java.md` |
| External HTTP behavior | versioned OpenAPI sources under `platform/contracts/http/` |
| Durable schema | owning Flyway migrations |
| Build dependencies/wiring | Gradle wrapper/build logic/build files/version catalog |
| Executable behavior | source code and automated tests |

A fact is fully defined by its most-specific authority. Other documents reference that authority instead of maintaining a competing copy.

Registered authoritative Markdown follows `docs/templates/README.md`: required structure, responsibility boundaries, line budgets, final newline, and governed exceptions. History belongs in Git/issues/PRs/ADRs rather than current-state documents. Semantic ownership remains a review responsibility; deterministic checks must not infer meaning with heuristic/AI analysis.

## Change control

`development` is accepted next-state integration truth. `production` is stable/released truth. Topic branches and open pull requests are proposals.

Normal work follows `development -> topic branch -> pull request -> development`; topic PRs are normally squash-merged. Releases use `development -> production` and normally a merge commit. Urgent production defects may use a `hotfix/` branch from `production` and must be reconciled back into `development`.

No normal work is performed directly on permanent branches.

Every pull request must have one coherent purpose, identify scope/architecture/module impact, name affected authorities, state deliberate exclusions, record applicable validation, avoid unrelated cleanup/speculative work, and resolve review conversations before merge.

A useful idea is not accepted scope. A new semantic capability, use case, durable responsibility, or currently excluded product responsibility requires an accepted `docs/scope.md` change before implementation. Ordinary implementation surfaces (APIs, contracts, migrations, adapters, compositions, wiring) required to realize an already accepted use case do not require separate scope transitions. Significant architecture decisions require an ADR. New technologies require a demonstrated problem and accepted requirement. Infrastructure is not introduced for hypothetical future need.

The operational commands and sequencing that apply these rules are owned by `docs/workflow.md`.

## Goal hierarchy

Goals are planning/tracking containers and never authorize implementation, scope, architecture, or technology.

Product delivery uses at most:

`Product Goal -> Use-case Goal -> executable issues`

A Product Goal owns a bounded product/release outcome and completion boundary. A Use-case Goal is its direct `type: goal` child for one observable actor journey/use-case outcome; Goal nesting stops there. Non-product stakeholder/operator/developer outcomes may use a single-level Goal when extra hierarchy adds no value.

Executable work uses `decision`, `scope`, `research`, `implementation`, `defect`, or `documentation`. `Goal: #...` records planning parent; `Blocked by #...` / `Blocks #...` record execution dependencies.

Priority controls scheduling, not authority:

- `priority: later` — outcome-level only; do not pre-design.
- `priority: next` — identify Use-case Goals and unresolved readiness work.
- `priority: now` — execute only ready work.

Normally only one top-level Product Goal or single-level Goal is `priority: now`.

Use proportional delivery. The normal path for ready product work is:

`scope/readiness -> vertical implementation -> acceptance`

Research, decision, scope, ADR, or documentation work is separate only when genuinely unresolved, independently reviewable, or required by governance. Technical implementation layers are not automatic issue/PR boundaries; one coherent implementation issue/PR may deliver the full accepted vertical slice when readiness is resolved.

A Use-case Goal is complete only when required executable work and objective E2E acceptance are complete in accepted `development`. A Product Goal additionally requires all required Use-case Goals and its aggregate completion boundary. A single-level Goal follows the applicable same rule.

## Architecture and module control

`docs/architecture/workspace.dsl` owns current architecture relationships. `docs/architecture.md` owns durable architectural semantics. Architecture changes update model/narrative when affected; significant rationale requires an ADR. Planned/exploratory architecture must not be represented as Current.

Every construct classified as a module must satisfy the universal invariant in `docs/modules.md`, ADR-0013, and ADR-0017: independent ownership, explicit public API, private implementation, selectable application composition, public API collaboration surfaces, no functional dependency on other modules, and no dependence on another module's private implementation/persistence.

Modules do not call other modules directly as part of cross-module workflows. Compositions coordinate cross-module workflows through public module APIs. Application runtimes own technical selection, construction, configuration, and wiring only. Interfaces/integrations are adapters unless deliberately admitted as modules. Shared foundation remains small and business-neutral.

Before admitting a module, establish a concrete use case, ownership/non-ownership, smallest public API, private boundary, required foundation dependencies (no functional dependencies on other modules), selectable composition semantics, and objective validation. A business module additionally needs meaningful independently owned rules/invariants/lifecycle.

## Definition of done

An accepted change must:

- be inside accepted scope or explicitly change scope;
- preserve concern ownership and the applicable architecture/module invariants;
- update directly affected authoritative sources without duplicating narrower truth;
- update the architecture model/narrative when relationships or durable architecture semantics change;
- add/update an ADR when significant architectural rationale changes;
- include behavior/invariant tests for implementation changes;
- pass the applicable validation in `docs/workflow.md`;
- introduce no unrelated feature, technology, infrastructure, cleanup, or speculative abstraction;
- remain reviewable as one coherent change;
- resolve review conversations before merge.

For build-affecting changes the canonical repository gate is the Gradle Wrapper root `check`; for documentation-only changes use the relevant deterministic documentation/structure gates plus `git diff --check`.
