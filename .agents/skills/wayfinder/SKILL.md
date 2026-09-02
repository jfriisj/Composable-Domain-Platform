---
name: wayfinder
description: Map a genuinely large or ambiguous governed effort into repository-native readiness questions and executable decision/research work. Use when the destination spans multiple sessions and the route is not yet clear; skip when the current Goal/readiness state already yields one coherent next action.
---

# Wayfinder

## Precedence and trigger

This skill is an advisory planning technique. It does not create a second planning system.

Use it only when all of these are true:

- the destination is materially larger than one coherent agent session;
- important decisions or evidence are unresolved;
- the next executable path cannot yet be stated without guessing;
- mapping the uncertainty will reduce speculative implementation or documentation.

Do not invoke Wayfinder merely because a task has several implementation steps. If accepted scope/readiness already yields one coherent next action, follow `docs/workflow.md` instead.

Repository authority and hierarchy remain unchanged: accepted `development`, `docs/scope.md`, `docs/project-status.md`, `docs/governance.md`, `docs/workflow.md`, concern-specific authorities, and GitHub Issues win for their concerns.

## Repository-native map

Do not create a `wayfinder:map`, `wayfinder:*` labels, a separate ticket hierarchy, or a parallel tracker convention.

Use the existing Goal hierarchy as the shared map:

- the Product Goal or single-level Goal owns the destination at outcome level;
- a Use-case Goal, when applicable, owns one observable actor journey and its unresolved readiness questions;
- executable child issues use only the repository's existing types: `decision`, `scope`, `research`, `implementation`, `defect`, or `documentation`;
- `Goal: #...`, `Blocked by #...`, `Blocks #...`, and priority express parentage, dependencies, and scheduling;
- closed executable issues and accepted repository changes are resolved evidence, not duplicated prose in a second map document.

The Goal remains an index/planning container, not implementation authority.

## Destination, frontier, and fog

**Destination** is the already-governed Goal outcome/completion boundary. Tighten it only through the repository's normal Goal/scope process; Wayfinder cannot silently redraw scope.

**Frontier** is the smallest set of open, sharp, dependency-unblocked readiness issues that can be worked now. Prefer one current coherent issue at a time. Parallel frontier work is justified only when issues are genuinely independent and repository ownership/worktree rules are preserved.

**Fog** is in-scope uncertainty that is visible but not yet precise enough for an executable issue. Keep it as bounded readiness questions on the applicable Goal until it becomes sharp. Do not pre-slice fog into speculative tickets.

Use this test:

- create an executable issue when the question, expected evidence/result, authority impact, and validation can be stated precisely now;
- keep it as fog when doing so would require guessing about scope, architecture, technology, ownership, or dependencies.

Out-of-scope work stays outside the map. It does not become a future ticket merely because it is imaginable.

## Decision work

Wayfinder resolves uncertainty before implementation. Choose the smallest existing executable issue type that owns the unresolved question:

- `decision` when a concrete choice must be made from available evidence;
- `research` when missing evidence must be gathered before a decision/readiness conclusion;
- `scope` when the required outcome or durable responsibility is outside accepted `docs/scope.md`;
- `documentation` only when the independently reviewable outcome is documentation itself.

A throwaway prototype is a technique, not a new issue type. Use it only to answer a concrete unresolved question inside the owning `research` or `decision` issue. Keep prototype code out of accepted product implementation unless later authorized through normal readiness and implementation.

Do not create an implementation issue while material readiness questions remain unresolved.

## Work the frontier

For each Wayfinder cycle:

1. Verify remote `development`, the active Goal/Use-case Goal, accepted scope, labels, dependencies, and current project status.
2. Orient on the destination and read only authorities directly relevant to the current unresolved question.
3. Select the first coherent unblocked frontier question; do not broaden into a project audit.
4. Resolve only that question or gather the evidence needed to resolve it.
5. Record the durable result in its authoritative place: accepted repository change, ADR/authority when required, or the executable issue's resolution evidence.
6. Re-evaluate direct dependents and the Goal's remaining readiness questions.
7. Promote newly sharp fog into the minimum necessary executable issue(s); leave the rest unexpanded.
8. Stop Wayfinder when the route to implementation is clear.

When readiness becomes complete, hand off to the normal repository flow. Prefer one coherent vertical implementation issue/PR rather than converting every technical layer into separate tickets.

## Completion signal

Wayfinding is done when the accepted outcome has no material unresolved scope, ownership, dependency, contract/persistence/runtime/architecture, technology, validation, or ADR question blocking implementation.

At that point, the next action must be expressible directly under `docs/governance.md` and `docs/workflow.md` without additional speculative planning.
