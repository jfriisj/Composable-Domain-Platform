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
           next scope gate
~~~

No implementation begins merely because a future direction appears reasonable.

## Goal and subgoal planning

A Goal Issue is a planning and tracking container for one observable stakeholder, operator, or developer outcome that may require multiple existing issue types to complete.

Goal Issues use the `type: goal` label. `type: goal` identifies a planning container; it is not executable work and does not authorize implementation, change accepted scope, accept a bounded context, admit a technology, or replace an authoritative repository artifact. `docs/scope.md` remains the authority for accepted scope.

A Goal Issue must define:

- one concrete use case or measurable outcome;
- the accepted repository baseline from which the goal starts;
- explicit non-goals;
- objective end-to-end acceptance evidence;
- a `Subgoals` checklist containing the child issues currently known to be required;
- explicit `Blocked by #...` and `Blocks #...` dependencies where ordering matters;
- which subgoals are independent enough to proceed in parallel.

Each subgoal records its planning parent as `Goal: #...`. Goal membership is separate from execution dependency:

- `Goal: #...` identifies the planning hierarchy;
- `Blocked by #...` identifies an unresolved execution dependency;
- `Blocks #...` records the reverse execution dependency.

Subgoals use the existing executable issue types: decision, scope, research, implementation, defect, and documentation. Each subgoal keeps its own admission, readiness, validation, and change-control requirements.

Creating a Goal or subgoal issue records planned work only. Capability names, architecture alternatives, technologies, and implementation approaches mentioned in planning remain exploratory hypotheses until the applicable decision and scope flow accepts them.

### Goal priority and decomposition

There should normally be only one active `type: goal` with `priority: now`. Multiple child issues may carry `priority: now` when they belong to the same active Goal or coherent workstream and are independently ready.

Goal detail increases only as the Goal approaches execution:

- `priority: later` — keep the Goal outcome-level; do not pre-design bounded contexts or implementation.
- `priority: next` — re-read the Goal against current `development`, identify the research/decisions needed to make ownership and scope explicit, and decompose only enough to expose meaningful dependencies and parallel work.
- `priority: now` — execute only subgoals that satisfy their normal readiness rules.

### Parallel readiness

A subgoal is parallel-ready only when:

- its explicit prerequisites are resolved;
- applicable accepted scope authorizes the work;
- ownership and non-ownership are explicit;
- it does not depend on an unresolved sibling result;
- it has an independently verifiable outcome;
- concurrent work will not make uncontrolled competing changes to the same authoritative truth.

Parallel work is an execution property, not a reason to merge scope or ownership decisions prematurely.

### Re-read after subgoal progress

After a prerequisite or subgoal merges:

1. re-read remote `development`;
2. verify the merged result;
3. re-read directly dependent issues;
4. re-read the parent Goal;
5. update readiness, dependencies, or the Goal subgoal checklist when repository evidence changed the plan;
6. only then select the next ready action.

An implementation subgoal is ready only when its concrete outcome, accepted scope, ownership and non-ownership, exclusions, validation, and dependencies are resolved.

### Goal completion

A Goal is complete only when:

- all required subgoals are complete;
- objective end-to-end acceptance evidence is satisfied;
- the complete accepted result exists in `development`;
- `docs/project-status.md` records the resulting project state when the Goal changes current status;
- no unresolved dependency required for the Goal outcome remains.

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

During implementation:

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

For implementation or build-affecting work targeting `development`, this validation is performed locally before merge. The GitHub Actions `validate` job is registered for pull requests targeting `development` but is skipped there before runner allocation so the existing required check remains compatible with the branch ruleset without consuming a validation runner.

For pull requests targeting `production`, the GitHub Actions `validate` job executes `./gradlew --no-daemon check` with JDK 21 and acts as the independent release validation gate.

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
