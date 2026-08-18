# Project Workflow

## Authority

This document is the authoritative operational workflow for development of Composable Domain Platform.

It explains how accepted project intent moves from scope and status into a topic branch, validation, pull request, accepted `development` state, and eventually a release.

Governance rules are defined in [`governance.md`](governance.md). This document applies those rules as a repeatable working sequence. If this document conflicts with an authoritative concern-specific source, the concern-specific source wins.

## Core principle

Work proceeds from accepted repository state, not from assumptions, chat history, local notes, or deferred ideas.

The normal flow is:

~~~text
accepted development state
        |
        v
read scope + status
        |
        v
scope decision required?
   |             |
  yes            no
   |             |
docs scope PR    |
   |             |
   +-------> topic branch
                  |
                  v
              implement
                  |
                  v
               validate
                  |
                  v
             pull request
                  |
                  v
                review
                  |
                  v
          squash to development
                  |
                  v
           sync local state
                  |
                  v
           next ready action
~~~

No implementation begins merely because a future direction appears reasonable.

## Goal hierarchy and executable work

Goal Issues use the `type: goal` label and are planning containers, not executable work. They do not authorize implementation, change accepted scope, accept a bounded context, admit a technology, or replace an authoritative repository artifact. `docs/scope.md` remains the authority for accepted scope.

For product delivery, use at most three planning/execution levels:

`Product Goal -> Use-case Goal -> executable issues`

### Product Goal

A Product Goal represents a bounded product experience or release-level outcome with an explicit completion boundary. It must identify the accepted baseline, aggregate outcome, explicit non-goals, required Use-case Goals, and objective completion evidence.

### Use-case Goal

A Use-case Goal is a direct `type: goal` child of a Product Goal and represents one observable actor journey or independently meaningful use-case outcome.

A Use-case Goal must define:

- the actor and observable outcome;
- the accepted repository baseline;
- explicit non-goals;
- objective end-to-end acceptance evidence;
- the executable issues currently required;
- explicit `Blocked by #...` and `Blocks #...` dependencies where ordering matters.

Goal nesting stops here. A Use-case Goal cannot contain another Goal.

Non-product stakeholder, operator, or developer outcomes may remain single-level Goals when the Product Goal/Use-case Goal hierarchy adds no useful planning structure.

### Executable issues and dependencies

Executable work uses the existing issue types: decision, scope, research, implementation, defect, and documentation. No new executable issue type is introduced.

Each child records its direct planning parent with `Goal: #...`. Planning hierarchy is separate from execution dependency:

- `Goal: #...` identifies the direct planning parent;
- `Blocked by #...` identifies an unresolved execution dependency;
- `Blocks #...` records the reverse execution dependency.

Creating a Goal or executable issue records planned work only. Capability names, architecture alternatives, technologies, and implementation approaches mentioned in planning remain exploratory until the applicable scope, decision, architecture, and technology controls accept them.

### Goal priority and decomposition

There should normally be only one active top-level Product Goal or single-level Goal with `priority: now`. Multiple Use-case Goals or executable issues may carry `priority: now` when they belong to that coherent workstream and are independently ready.

Goal detail increases only as execution approaches:

- `priority: later` — keep the Product Goal or single-level Goal outcome-level; do not pre-design bounded contexts or implementation.
- `priority: next` — identify required Use-case Goals and only the unresolved research, decisions, dependencies, and scope needed for readiness.
- `priority: now` — execute only ready Use-case Goals and executable issues.

Technical implementation stages are not automatically separate issues.

### Proportional delivery

For an accepted and ready Use-case Goal, the normal path is:

~~~text
scope/readiness
      |
      v
vertical implementation
      |
      v
acceptance
~~~

Research, decision, scope, ADR, and documentation work are not mandatory lifecycle stages. Create them separately only when the concern is genuinely unresolved, independently reviewable as its own change, or required by another governance rule.

When readiness is already resolved, one coherent implementation issue and pull request may deliver the complete accepted vertical slice, including as applicable:

- authoritative OpenAPI contract changes;
- generated inbound transport shape;
- required module public APIs and private implementations;
- persistence and migrations;
- required cross-module composition;
- HTTP adapter and runtime wiring;
- focused, integration, architecture, and end-to-end evidence;
- directly affected authoritative documentation.

The coherent purpose is the accepted use-case slice, not an individual technical layer.

### Parallel readiness

An executable issue is parallel-ready only when:

- its explicit prerequisites are resolved;
- applicable accepted scope authorizes the work;
- ownership and non-ownership are explicit;
- it does not depend on an unresolved sibling result;
- it has an independently verifiable outcome;
- concurrent work will not make uncontrolled competing changes to the same authoritative truth.

Parallel work is an execution property, not a reason to merge scope or ownership decisions prematurely.

### Change-local re-read after progress

After accepted progress merges:

1. re-read remote `development` and verify the merged result;
2. re-read only directly changed authoritative artifacts;
3. re-read directly dependent issues and the direct parent Goal chain;
4. update readiness, dependencies, or acceptance where repository evidence changed them;
5. only then select the next ready action.

Do not perform a broad project audit unless the accepted change provides evidence that wider authority is affected.

An implementation issue is ready only when its concrete outcome, accepted scope, ownership and non-ownership, exclusions, validation, and dependencies are resolved.

### Goal completion

A Use-case Goal is complete only when its required executable work is complete, objective end-to-end acceptance is satisfied in accepted `development`, and no dependency required for that use case remains unresolved.

A Product Goal is complete only when all required Use-case Goals are complete, its aggregate completion boundary is satisfied in accepted `development`, `docs/project-status.md` records the resulting state when applicable, and no dependency required for the Product Goal remains unresolved.

A single-level non-product Goal follows the same applicable completion rules.

## 1. Start from authoritative state

Before planning or changing code, inspect at minimum:

- [`scope.md`](scope.md) for accepted scope and exclusions.
- [`project-status.md`](project-status.md) for current state and next priority.
- [`governance.md`](governance.md) for change-control rules.
- Applicable language-engineering standard registered by `governance.md`; for Java work, [`engineering/java.md`](engineering/java.md).
- Relevant architecture, module, ADR, technology, contract, build, and source files for the concern being changed.

Repository state on `development` is the accepted next-state baseline.

Repository state on `production` is the accepted stable/released baseline.

Topic branches and open pull requests are proposals, not accepted truth.

External conversations, AI output, issue discussion, local notes, and generated artifacts are not authoritative unless their result is accepted into the repository through the normal change process.

## 2. Classify the proposed work

Before creating a branch, decide which category the work belongs to.

### Work inside accepted scope

Proceed to a topic branch when the requested outcome is already authorized by `docs/scope.md`.

Examples include:

- Implementing an acceptance criterion already in scope.
- Fixing a defect in accepted behavior.
- Refactoring without changing external behavior, ownership, or accepted architecture.
- Adding tests for accepted behavior.
- Updating documentation to match an already accepted implementation.

### Scope change required

A dedicated scope decision is required before implementation when the work would:

- Add a new business capability.
- Add a new use case not covered by current scope.
- Introduce a currently excluded technology or infrastructure component.
- Introduce persistence, external contracts, deployment, integration, or runtime concerns that are not already authorized.
- Change bounded-context ownership or introduce a new bounded context.
- Expand acceptance criteria beyond the currently accepted phase.

The scope decision must update `docs/scope.md` and normally `docs/project-status.md`. It must be accepted into `development` before the implementation branch is created.

### Architecture decision required

Create or update an ADR when a change makes a significant architecture decision that needs durable rationale, especially when reasonable alternatives exist or an existing decision is changed.

An ADR does not replace a required scope change.

## 3. Define one coherent outcome

Each topic branch and pull request must have one primary outcome.

Before implementation, state:

1. What exact outcome is being delivered?
2. Why is it inside accepted scope?
3. Which authoritative sources are affected?
4. What is deliberately out of scope?
5. How will the change be validated?

Do not bundle unrelated cleanup, speculative abstractions, future infrastructure, or neighboring capabilities.

Prefer the smallest change that proves the accepted requirement.

## 4. Synchronize the base branch

Normal work starts from the current remote `development` state.

Before creating a topic branch, ensure there is no uncommitted work that would be lost.

Typical synchronization:

~~~bash
git switch development
git fetch origin --tags
git reset --hard origin/development
git status
~~~

`git reset --hard` is appropriate here only when the working tree has already been verified safe and local `development` is intended to mirror `origin/development`.

Do not merge an already squash-merged topic branch back into local `development`. After a squash merge, synchronize local `development` to `origin/development`.

## 5. Create a topic branch

Branch from the synchronized `development` branch.

Allowed prefixes:

- `feat/` — new accepted behavior.
- `fix/` — defect correction.
- `docs/` — documentation or scope/governance decisions.
- `chore/` — build or repository maintenance.
- `refactor/` — behavior-preserving restructuring.
- `test/` — test-only changes.
- `hotfix/` — emergency fix based on `production`.

Examples:

~~~text
docs/next-phase-scope
docs/project-workflow
feat/event-reference-module
fix/event-validation
chore/gradle-foundation
~~~

Use a branch name that describes the outcome, not the implementation detail of the moment.

## 6. Implement within the accepted boundary

Implementation follows a **contract-first, capability-driven, module-complete, composition-last** sequence for the accepted use case.

The implementation sequence is an execution order, not a reversal of the architectural dependency rule. Hexagonal dependencies still point inward, module ownership remains authoritative, and transport/infrastructure concerns must not leak into domain code.

For an accepted use case with an external HTTP surface, work normally proceeds in this order:

~~~text
accepted use case
        |
        v
external OpenAPI contract
        |
        v
generated HTTP API shape + inbound adapter boundary
        |
        v
identify required capabilities and ownership
        |
        v
complete required module public APIs + private implementations
        |
        v
cross-module composition, when required
        |
        v
complete HTTP delegation
        |
        v
runtime assembly
        |
        v
integration and end-to-end proof
~~~

### 6.1 Complete the accepted external contract first

When the use case changes or adds HTTP behavior, define the complete accepted contract area in the authoritative OpenAPI document before implementing business behavior behind it.

Complete the contract for the accepted use case, including applicable:

- operations and paths;
- request and response schemas;
- status and error semantics;
- correlation headers;
- authentication requirements;
- externally visible privacy behavior.

Generated OpenAPI sources are derived transport artifacts. They may establish the inbound adapter shape, but generated types and HTTP handlers do not own business behavior.

Do not expand the contract for speculative future use cases merely because adjacent operations are easy to imagine.

An authoritative Current contract must not be merged into `development` in a state that falsely claims executable behavior which the accepted runtime does not provide. Contract-first describes implementation order inside the coherent workstream; merge boundaries must still leave `development` truthful and usable.

### 6.2 Use the contract to identify required capabilities

After the contract shape is explicit, derive the capabilities required to satisfy it and assign each behavior to its accepted owner.

For every required capability, determine:

- which module owns the behavior;
- whether an existing public module API already exposes the required capability;
- whether the module public API needs an accepted extension;
- which behavior remains private implementation detail;
- whether persistence or another outbound adapter is required;
- whether more than one module must collaborate.

Do not create services, modules, abstractions, or dependencies merely to mirror HTTP endpoints. Module APIs describe owned capabilities, not transport structure.

### 6.3 Complete required modules before cross-module composition

Implement each required module as an independently coherent capability behind its public API.

A module stage includes, as applicable:

- the smallest public API required by the accepted capability;
- domain rules and invariants;
- application/use-case implementation;
- outbound ports;
- private persistence or other outbound adapters;
- module-level tests and architecture enforcement.

Finish one owned capability area before moving its missing behavior into a composition or transport adapter.

Module APIs should remain useful to later accepted contracts and compositions because they express module-owned capabilities, but they must not pre-design hypothetical future behavior.

Independent required modules may be implemented in parallel only when normal readiness and ownership rules allow it.

### 6.4 Add compositions after participating module capabilities exist

When the use case spans modules, implement the composition after the participating public module capabilities are available.

A composition:

- depends only on participating public module APIs;
- owns only the cross-module workflow;
- does not implement missing module behavior;
- does not access another module's private implementation or persistence;
- does not absorb transport-specific HTTP behavior.

If composition work reveals that a participating module lacks an owned capability, return to that module stage instead of implementing the capability inside the composition.

### 6.5 Complete the inbound adapter and runtime assembly last

After required module capabilities and compositions exist, complete the HTTP adapter by delegating generated transport operations to the appropriate public module API or composition.

The HTTP interface owns transport adaptation only, including structural request validation, transport mapping, HTTP status/error mapping, correlation boundary behavior, and accepted external privacy mapping.

The application runtime then selects, constructs, configures, and wires the required modules, compositions, and adapters. Runtime wiring must not become business ownership.

### 6.6 Finish with integrated proof

The final implementation stage proves that the complete accepted contract is backed by the assembled capabilities.

Add the applicable:

- focused module tests;
- composition tests;
- HTTP adapter tests;
- architecture/dependency checks;
- persistence integration tests;
- real-dependency end-to-end tests;
- restart/durability evidence;
- correlation, authentication, authorization, and privacy evidence required by accepted scope.

The root `./gradlew --no-daemon check` remains the final build gate for build-affecting work.

### 6.7 Keep vertical implementation coherent

Sections 6.1 through 6.6 define execution order inside a ready use-case workstream. They are not automatic issue or pull-request boundaries.

By default, keep the complete accepted vertical slice in one coherent implementation issue and pull request when it can remain reviewable, truthful, and independently valid. Track progress inside that work item using stages such as:

1. accepted external contract;
2. required module capability or capabilities;
3. required cross-module composition;
4. inbound-adapter/runtime completion;
5. integrated end-to-end acceptance.

Split a stage into separate executable work only when a real dependency, unresolved scope/decision/technology concern, independently accepted intermediate outcome, or another governance rule requires the boundary. Do not split work merely because execution moves from contract to module, composition, adapter, runtime, testing, or documentation.

Directly affected authoritative documentation should normally be updated with the change that makes the previous documentation stale. A separate documentation issue is required only when documentation itself has an independently reviewable outcome or cannot truthfully move with the implementation.

Research and decision issues are likewise conditional tools for unresolved questions, not mandatory steps before every implementation.

During all stages:

- Keep domain ownership explicit.
- Respect public API/private implementation boundaries.
- Do not introduce dependencies on another module's implementation.
- Keep domain code free of infrastructure technologies unless explicitly authorized.
- Add only dependencies required by the accepted change.
- Update the authoritative source that owns any changed truth.
- Update the Structurizr model when current architecture changes.
- Add or update an ADR when architectural rationale changes materially.
- Add tests for introduced or changed behavior and invariants.

If implementation reveals that accepted scope is insufficient, stop implementation and return to the scope-decision step. Do not hide the expansion inside the implementation pull request.

## 7. Apply patches safely

When a prepared patch is used, validate it before changing the working tree:

~~~bash
git apply --check path/to/change.patch
git apply path/to/change.patch
~~~

Do not apply a patch that fails `git apply --check`.

After applying a patch, inspect the actual diff rather than assuming the patch produced the intended result.

## 8. Validate locally

Validation is fail-fast. A failed required gate blocks commit or merge until the cause is understood and corrected.

### Minimum repository hygiene

Run:

~~~bash
git diff --check
git status --short
~~~

Before committing, inspect the staged change:

~~~bash
git diff --cached --check
git diff --cached --stat
git diff --cached --name-only
git status --short
~~~

Remember that normal `git diff` does not show untracked files. Stage intended new files before treating the cached diff as the final review surface.

### Executable build gate

For implementation or build-affecting work:

~~~bash
./gradlew --no-daemon check
~~~

The root `check` task is the final repository build gate unless a later accepted process explicitly replaces or extends it.

For implementation or build-affecting work targeting `development`, this validation is performed locally before merge. The GitHub Actions `validate` job is registered for pull requests targeting `development` but is skipped there before runner allocation. The active `development` ruleset does not require `validate`; the mandatory local root check is the operative validation gate for those pull requests.

For pull requests targeting `production`, the GitHub Actions `validate` job executes `./gradlew --no-daemon check` with JDK 21, acts as the independent release validation gate, and remains required by the active `production` ruleset.

Run targeted tests or dependency reports when they provide stronger evidence for the change.

Examples:

~~~bash
./gradlew :event-impl:test
./gradlew :event-api:dependencies --configuration compileClasspath
./gradlew :event-impl:dependencies --configuration compileClasspath
~~~

Targeted checks supplement root `./gradlew check`; they do not replace it.

### Documentation-only changes

Documentation-only changes do not need artificial implementation tests, but they must still:

- pass `git diff --check`;
- remain internally consistent;
- update all directly affected authoritative references;
- avoid claiming proposed state as already accepted or implemented.

If documentation changes executable build files, code, generated contracts, or other executable sources, the applicable executable gates still apply.

## 9. Stage only the intended change

Stage explicit paths when practical.

Before commit, verify that:

- every staged file belongs to the branch purpose;
- no editor, IDE, temporary, generated, or local-environment file is included accidentally;
- no intended new file remains untracked;
- no unrelated working-tree change is being silently omitted from review.

Local-only exclusions may be placed in `.git/info/exclude` when they are specific to one developer environment and should not become repository policy.

## 10. Commit discipline

Commits on topic branches should describe the coherent outcome.

Typical subjects:

~~~text
docs: define next project phase
docs: establish project workflow
feat: establish event reference module
fix: reject invalid event schedule
chore: establish gradle build foundation
~~~

A topic branch may contain multiple development commits while work is in progress, but topic branches are normally squash-merged into `development`.

If a commit is amended after the branch has been pushed, update only the topic branch and use force-with-lease rather than an unrestricted force push:

~~~bash
git push --force-with-lease
~~~

Never force-push `development` or `production`.

## 11. Open the pull request

Every pull request targets `development` for normal work.

The PR must address the sections defined by the repository pull request template and explain:

- one clear purpose;
- whether the change is inside scope or intentionally changes scope;
- architecture impact;
- module ownership impact;
- deliberately excluded related work;
- validation performed.

The pull request itself is still a proposal. Documentation on the topic branch must distinguish proposed state from accepted state where that distinction matters.

## 12. Review the actual remote change

Review the GitHub PR state, not only the local working tree.

Before merge, verify:

- base branch is correct;
- head branch is correct;
- the commit/file set matches the intended change;
- the branch is not unexpectedly behind `development`;
- GitHub reports the PR as mergeable;
- required local or automated checks are green;
- authoritative documentation matches the implementation;
- scope has not expanded silently;
- architecture and dependency direction remain valid;
- tests cover new or changed behavior and invariants;
- all review conversations are resolved.

If the PR head changes after review, review the new head again before merging.

## 13. Merge normal work

Normal topic branches are squash-merged into `development`.

Typical command:

~~~bash
gh pr merge <PR_NUMBER> --squash --delete-branch
~~~

The resulting commit on `development` is the accepted repository state for that change.

Do not treat the topic branch commit SHA as the accepted integration commit after a squash merge.

## 14. Synchronize after merge

After the PR is confirmed merged, synchronize local `development` to the remote accepted state.

~~~bash
git switch development
git fetch origin --tags
git reset --hard origin/development
git status
~~~

Delete a remaining local topic branch only after confirming that the PR was merged and that no unique work still needs to be preserved.

Because squash merge does not preserve the topic commit as an ancestor, normal `git branch -d` may refuse deletion. Inspect first; use forced local deletion only after confirming the work is safely represented on `development`.

## 15. Stop at the next scope gate

Completing an implementation phase does not automatically authorize the next implementation.

When `docs/project-status.md` says the next priority is to define a new phase, the next branch must be a scope/documentation branch.

The sequence is:

~~~text
completed implementation
        |
        v
update accepted status
        |
        v
explicit scope decision
        |
        v
scope PR accepted into development
        |
        v
next implementation branch
~~~

Do not start Spring, persistence, OpenAPI, deployment, another business capability, or any other deferred concern merely because the previous phase completed.

## 16. Release workflow

`production` represents stable/released state.

A release starts only from accepted `development` state and uses a pull request from `development` to `production`.

Release flow:

~~~text
development
    |
    v
release pull request
    |
    v
production
    |
    v
tag vX.Y.Z
~~~

Release PRs use a merge commit rather than squash merge so the release relationship between the permanent branches remains explicit.

Before release:

- the intended `development` state must be complete and reviewable;
- applicable local validation on `development` must already have succeeded;
- the production-targeting GitHub Actions `validate` job must execute and succeed;
- release documentation/versioning required by the accepted release process must be updated;
- no unrelated future work should be bundled into the release PR.

### Permanent-branch ancestry after a release

A normal release pull request is merged into `production` with a merge commit. That release merge commit belongs to the stable/release history and is not copied back into `development`.

After normal work subsequently advances `development`, GitHub may therefore report `development` and `production` as diverged: `production` contains the release merge commit while `development` contains later accepted integration commits. This is expected and is not by itself a reconciliation defect.

Do not merge `production` back into `development` merely to make permanent-branch ancestry appear linear. The next normal release pull request merges the accepted `development` state into the existing `production` history.

Reconciliation from `production` back into `development` is required for a hotfix because the hotfix contains unique released work that must not be lost from future development. Ordinary release merge commits do not require that reconciliation.

## 17. Hotfix workflow

Emergency fixes to released state may branch from `production` using `hotfix/`.

A hotfix must:

1. Address a real production defect.
2. Stay narrowly focused.
3. Be validated against the released baseline.
4. Merge to `production`.
5. Be reconciled back into `development` so the branches do not permanently diverge.

A hotfix is not a shortcut for normal feature delivery.

## 18. Tool and AI usage

Development tools and AI assistants may help analyze, draft, review, or generate changes, but they do not own project truth.

Rules:

- Inspect the repository when exact current state matters.
- Treat generated code, patches, documentation, and recommendations as proposals until reviewed.
- Validate generated patches before applying them.
- Validate generated implementation with the same tests and build gates as manually written implementation.
- Do not accept a technology, capability, dependency, or architectural pattern solely because a tool recommends it.
- Never use chat history or tool memory as a substitute for accepted repository documentation.

The repository remains the authority.

## 19. Fail-fast conditions

Stop the current flow and resolve the issue before continuing when:

- the working tree contains unexplained changes;
- a patch does not apply cleanly;
- the branch is based on unexpected repository state;
- `git diff --check` fails;
- required tests fail;
- `./gradlew check` fails;
- dependency direction differs from the accepted architecture;
- the implementation needs something explicitly out of scope;
- authoritative documentation and implementation disagree;
- the PR contains unrelated files;
- GitHub reports a real merge conflict;
- review reveals that the branch purpose is no longer coherent.

Do not work around a failed gate merely to reach merge.

## 20. Issue backlog and prioritization

GitHub Issues are a non-authoritative planning and decision queue. They record candidate problems, dependencies, decisions, deferred work, defects, and work that may later become ready for implementation.

An open issue does not change accepted scope, architecture, project status, contracts, or implementation authority. If an issue conflicts with an authoritative repository source, the authoritative source wins.

Substantive planned work should normally have an issue so ordering, dependencies, and prior decisions remain visible across sessions. Trivial corrections do not require an issue solely for process ceremony.

### Issue categories

Use the repository issue forms according to the maturity of the work:

- **Decision / scope candidate** — the problem or use case is understood enough to investigate, but implementation is not yet authorized. This also covers research and possible scope changes.
- **Ready work** — the outcome is already authorized by accepted repository state and satisfies the Definition of Ready below.
- **Defect** — accepted behavior differs from the implementation or executable evidence.

Use one primary type label where practical:

- `type: decision`
- `type: scope`
- `type: implementation`
- `type: defect`
- `type: documentation`
- `type: research`

A decision, research result, or issue discussion is still not authoritative. A scope change becomes accepted only when the applicable authoritative documents are changed through a pull request and merged into `development`.

### Priority

Use one priority label for open planned work:

- `priority: now` — the current coherent workstream. Keep this deliberately small; normally only one primary workstream should be active.
- `priority: next` — the strongest candidate after current work, subject to all normal scope and decision gates.
- `priority: later` — deliberately deferred work with no current implementation commitment.

Use `state: blocked` in addition to the priority label when an unresolved dependency prevents progress.

Priority expresses ordering, not authorization. `priority: now` must never be used to bypass an unresolved scope, architecture, ownership, or technology decision.

### Dependencies

When ordering matters, record dependencies explicitly using issue references such as `Blocked by #123` and `Blocks #456`.

Do not rely on chat history, issue comments, or memory as the only record of a dependency that affects execution order.

Downstream implementation must not begin while a required decision, scope change, or other blocking issue remains unresolved.

### Definition of Ready

Implementation work is ready only when all applicable statements are true:

1. The concrete use case or exact outcome is understood.
2. An accepted repository source already authorizes the outcome.
3. Ownership and non-ownership are understood.
4. Contract, persistence, integration, runtime, and architecture impact are understood where applicable.
5. Any required ADR or scope decision has already been accepted.
6. Deliberately excluded adjacent work is explicit.
7. Validation evidence is known before implementation starts.

If these conditions are not satisfied, keep the work classified as decision, scope, or research rather than treating it as implementation-ready.

### Issue lifecycle

The normal planning sequence is:

1. Record the concrete problem, use case, defect, or candidate decision.
2. Classify and prioritize the issue.
3. Resolve prerequisite research, ownership, architecture, or scope decisions.
4. If scope changes, accept the scope/documentation pull request into `development` first.
5. Only then treat downstream implementation work as ready.
6. Implement through the normal topic-branch and pull-request workflow.
7. Close the issue when its intended outcome is accepted or deliberately rejected/deferred.

Closing an issue records backlog state; it does not itself change accepted project truth.

### Milestones and project boards

Use milestones only for an accepted phase or release whose contents are sufficiently understood. Do not place speculative ideas into a milestone merely to make them appear planned.

A GitHub Project board is not required while labels, issues, dependencies, milestones, and pull requests provide sufficient visibility. If a project board is introduced later, it remains a derived planning view rather than an authoritative project source.

### Backlog review

Review `priority: now`, `priority: next`, and blocked issues after significant merges, releases, and before selecting a new scope phase.

During review:

- remove or close stale issues;
- demote work whose prerequisite no longer exists;
- expose hidden dependencies;
- ensure implementation issues still satisfy the Definition of Ready;
- ensure `docs/project-status.md` and accepted scope remain the authority for what may happen next.

## 21. Working checklist

Use this checklist for normal topic work.

### Before implementation

- [ ] `development` is synchronized with `origin/development`.
- [ ] Working tree is clean or all local work is understood and protected.
- [ ] `docs/scope.md` authorizes the change.
- [ ] `docs/project-status.md` supports the next action.
- [ ] Branch purpose is one coherent outcome.
- [ ] A scope or ADR decision has been made first if required.
- [ ] If the work is issue-tracked, its dependencies are current and it satisfies the applicable Definition of Ready.

### During implementation

- [ ] Work remains inside accepted scope.
- [ ] Ownership and dependency boundaries are respected.
- [ ] Only required dependencies and abstractions are introduced.
- [ ] Tests cover new or changed behavior and invariants.
- [ ] Relevant authoritative documentation is updated.

### Before commit

- [ ] Targeted tests/checks pass.
- [ ] `./gradlew --no-daemon check` passes when applicable.
- [ ] `git diff --check` passes.
- [ ] Intended files are staged.
- [ ] `git diff --cached --check` passes.
- [ ] Staged stat and filenames match the branch purpose.
- [ ] No unrelated or local-environment files are staged.

### Before merge

- [ ] PR base/head are correct.
- [ ] Remote PR diff matches the reviewed change.
- [ ] PR is mergeable.
- [ ] Scope and architecture impact are explicit.
- [ ] Validation evidence is recorded.
- [ ] Review conversations are resolved.
- [ ] Latest PR head has been reviewed.

### After merge

- [ ] PR is confirmed merged.
- [ ] Local `development` is reset to `origin/development`.
- [ ] Working tree is clean.
- [ ] Obsolete topic branch is removed safely.
- [ ] `docs/project-status.md` is checked for the next authorized action.
- [ ] No new implementation begins before any required next scope decision.
