# Project Workflow

## Authority

This document owns the operational sequence for moving accepted intent through local work, validation, pull request, accepted `development`, and release. Governance rules live in [`governance.md`](governance.md); concern-specific authorities win for their concerns.

Repository state, not chat history or notes, determines truth.

## Bootstrap

Start from the smallest authoritative context that governs the concrete change:

1. Verify remote `development`; it is the accepted next-state baseline.
2. If work is issue-tracked, verify the active Goal/issue, direct dependencies, labels/priority, and accepted baseline.
3. Use the authority map in `governance.md` to select only directly applicable authorities.
4. Read exact concern-specific docs/contracts/migrations/source/tests/build truth needed for readiness.
5. Expand the authority set only when repository evidence shows plausible wider impact.
6. When local execution matters, verify branch, commit, and working tree.

Conditional reads:

- read `scope.md` when capability/use case/exclusion/boundary may change;
- read `project-status.md` when selecting work or synchronizing current state;
- read architecture/module/ADR sources when ownership, relationship, dependency direction, or significant rationale may change;
- read a language engineering standard only when changing/reviewing that language's source or rules;
- read contracts, migrations, build files, source, and tests only when the change can affect their truth.

`production` is released/stable. Topic branches and open PRs are proposals. External conversations, generated output, and local notes are not authoritative until accepted through the repository process.

## Readiness

Classify the proposed outcome before implementation.

Work may proceed when accepted scope already authorizes the outcome and the issue, if any, has resolved:

- concrete outcome/use case;
- ownership and non-ownership;
- deliberate exclusions;
- direct dependencies;
- contract/persistence/integration/runtime/architecture impact where applicable;
- required decision/ADR/technology admission;
- objective validation evidence.

A new capability/use case or currently excluded responsibility requires accepted scope change first. Significant architecture rationale requires an ADR. New technology requires a demonstrated problem and admission through governance.

If readiness is resolved, prefer one coherent vertical implementation rather than issue-per-layer decomposition. For an HTTP use case, the normal execution order is:

`OpenAPI -> inbound boundary -> module capabilities -> composition -> HTTP/runtime -> integration/E2E -> affected docs`

This is execution order, not automatic issue/PR boundaries. Split only for a real dependency, unresolved authority, independently accepted intermediate outcome, or governance requirement.

If implementation reveals missing authority, stop implementation and perform only the necessary readiness transition.

## Branch and pull request flow

Normal topic work:

```bash
git switch development
git fetch origin --tags
git reset --hard origin/development
git status
git switch -c <topic-branch>
```

Use `git reset --hard` only after verifying the working tree is safe and local `development` is intended to mirror remote.

Allowed topic prefixes: `feat/`, `fix/`, `docs/`, `chore/`, `refactor/`, `test/`, `hotfix/`.

Keep one coherent outcome. Do not bundle unrelated cleanup, speculative abstraction, future infrastructure, or neighboring capability work.

When applying a prepared patch:

```bash
git apply --check --whitespace=error-all <patch>
git apply --whitespace=error-all <patch>
```

Never apply a patch that fails the check.

Stage only intended paths. Before commit:

```bash
git diff --cached --check
git diff --cached --stat
git diff --cached --name-only
git status --short
```

Topic commits describe the outcome. Topic branches are normally squash-merged into `development`. If an already-pushed topic commit is amended, use `git push --force-with-lease`; never force-push permanent branches.

Every normal PR targets `development`. Link the tracked issue/Goal when applicable, state the coherent purpose, and record only actual scope/architecture/module impact, affected authorities, material exclusions, and validation. Do not restate governance rules as checkbox attestations.

Before merge verify remote PR base/head, commit/file set, mergeability, latest head, required validation, authoritative consistency, scope, architecture/dependency direction, and resolved review conversations.

Normal merge is squash to `development`. The resulting integration commit—not the topic commit—is accepted truth.

## Validation

Fail closed. A failed required gate blocks commit/merge until understood and corrected.

Repository hygiene:

```bash
git diff --check
git status --short
```

Implementation/build-affecting final developer gate:

```bash
bash -lc 'set -euo pipefail; ./dev/dev.sh check; git diff --check; echo "PASS: full repository gate in developer environment"'
```

`./dev/dev.sh check` is the canonical developer command. It runs the repository Gradle Wrapper root `./gradlew --no-daemon check` inside the repository-controlled Docker/JDK 21 developer environment. The wrapper root `check` remains the underlying build truth and the production-targeting hosted validation command.

Targeted tests/dependency checks may add evidence but do not replace root `check`.

For PRs to `development`, hosted `validate` is skipped; the local developer-environment root gate is operative for build-affecting work. For PRs to `production`, hosted `validate` runs root `check` with JDK 21 and must pass.

Documentation-only changes require the relevant deterministic documentation/structure checks plus `git diff --check`. Do not run Gradle solely because documentation references Java/build concerns; run it when executable build truth changes.

Before commit always run:

```bash
git diff --cached --check
```

Missing evidence is not success. Print `PASS` only after the complete gate succeeds.

## Post-merge

After accepted progress:

1. re-read remote `development` and verify the merge result;
2. verify affected issue/Goal state;
3. re-read only directly changed authoritative artifacts;
4. re-read directly dependent issues and the direct parent Goal chain;
5. update readiness/dependencies/acceptance only where repository evidence changed them;
6. select the next ready action.

Do not perform a broad project audit without evidence of broader impact.

Synchronize local state only after confirming the merge:

```bash
git switch development
git fetch origin --tags
git reset --hard origin/development
git status
```

Because squash merge does not preserve the topic commit as an ancestor, delete a local topic branch only after confirming its work is represented in accepted `development`.

## Release

A normal release is a PR from accepted `development` to `production`.

Before merge, the intended `development` state must be complete, local applicable validation must already be green, and the production-targeting hosted `validate` gate must pass with JDK 21/root `check`.

Release PRs normally use a merge commit so permanent-branch release ancestry is explicit. After release, tag the accepted `production` state according to the release process.

Do not merge `production` back into `development` merely to make ancestry linear. Normal release merge commits need no reconciliation.

Urgent released defects may use `hotfix/` from `production`; validate narrowly, merge to `production`, then reconcile the unique hotfix work back into `development`.
