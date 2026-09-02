---
name: code-review
description: Review a bounded branch or PR on two independent axes: repository Standards and accepted Spec/intent. Use before merge or when asked to review changes from a fixed point.
---

# Code review

## Precedence and scope

Review the bounded diff, not the whole repository. Repository authorities override generic code-smell heuristics. Do not turn a review into unrelated cleanup, redesign, or speculative architecture work.

For a topic branch/PR, use the merge-base with the accepted `development` baseline as the default fixed point when repository state makes that unambiguous. If the user explicitly supplies another fixed point, use it. Fail closed if the fixed point cannot be resolved or the intended diff cannot be identified.

Keep the two review axes separate so correctness against one axis cannot hide failure against the other.

## Axis 1: Standards

Check the changed code against only the directly applicable repository standards and executable truths, including as relevant:

- `docs/governance.md` and `docs/workflow.md`;
- architecture/model/module authorities;
- `docs/engineering/java.md` for Java changes;
- contracts, migrations, Gradle/build truth, source, and tests for the affected concern.

Typical hard findings include broken ownership, wrong dependency direction, access to another module's private implementation/persistence, framework leakage into domain, cross-module workflow inside a module, duplicated narrower truth, missing required authority updates, or missing required validation.

Use general smells only as secondary judgement calls, never as project law. Useful signals include speculative generality, pass-through middle layers, shotgun surgery, data clumps, feature envy, repeated conditionals, message chains, primitive/domain-model confusion, and abstractions that do not earn their surface area. Suppress a heuristic when repository authority deliberately endorses the design.

## Axis 2: Spec

Identify the originating accepted intent from the executable issue and its direct accepted authorities. For product implementation this commonly includes the parent Goal chain, accepted scope, relevant ADRs, and authoritative external contract where already selected.

Look for:

- required behavior that is missing or only partial;
- behavior that contradicts the accepted requirement;
- extra behavior, infrastructure, abstraction, cleanup, or policy not authorized by the issue/scope;
- violations of explicit exclusions/non-goals;
- incorrect failure, privacy, authorization, lifecycle, idempotence, persistence, restart, or acceptance behavior;
- tests that appear green but do not actually prove the requirement.

A Goal by itself is never implementation authority.

## Findings format

Report findings first. For each finding provide:

- axis: `Standards` or `Spec`;
- severity: blocking or non-blocking;
- exact file/hunk or concrete changed surface;
- the authoritative rule/requirement it conflicts with;
- concise explanation of impact and the smallest correction.

Distinguish hard repository violations from heuristic design concerns. If there are no findings on an axis, say so explicitly. Do not manufacture suggestions to make the review look busy.

After findings, summarize actual validation evidence and unresolved review conversations separately. A clean review does not replace required repository gates or GitHub branch policy.
