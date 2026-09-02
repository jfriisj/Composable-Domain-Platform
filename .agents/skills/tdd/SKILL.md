---
name: tdd
description: Implement ready features or defects test-first in small vertical behavior slices. Use when executable work is authorized and repository-defined public behavior can drive a red/green feedback loop.
---

# Test-driven development

## Precedence

This skill is an implementation technique, not a readiness mechanism. Do not use it to invent missing scope, ownership, contracts, architecture, persistence policy, or technology decisions.

Before implementation, rely on the repository authorities and executable issue to identify the behavior and valid test seams. If those sources already determine the seam, do not ask the user to reconfirm it. If implementation exposes missing semantic authority or a significant unresolved architecture decision, stop and return to the necessary readiness transition.

## Test seams

Prefer tests through stable externally meaningful or module-public behavior. The applicable seam may be a domain/public module API, composition behavior, HTTP contract boundary, persistence port, or end-to-end runtime boundary depending on what the accepted requirement owns.

Do not test another module through its private implementation or persistence. Direct database assertions are appropriate only when database/persistence behavior itself is the owned subject under test; they are not a substitute for verifying a higher-level public behavior.

## Vertical red/green loop

For each accepted behavior:

1. Select the smallest observable behavior not yet proven.
2. Add one focused test that fails for the intended missing behavior.
3. Run the narrowest relevant test and verify the failure is meaningful.
4. Implement only enough production code to make that behavior pass.
5. Re-run the targeted test and nearby affected tests.
6. Perform only bounded refactoring that preserves the now-green behavior and current architecture rules.
7. Repeat with the next behavior, letting the previous slice inform the next one.

Keep slices vertical. Do not write an entire layer's tests before implementation of any behavior, and do not pre-build abstractions for imagined later tests.

## Test quality

Good tests:

- express accepted behavior or an invariant in repository/domain terminology;
- derive expected results from the issue, accepted scope, contract, ADR, or a concrete example—not by repeating the production algorithm;
- remain stable across private refactoring;
- use fakes/mocks at real seams, not to expose or lock down internal call structure;
- prove negative behavior where privacy, authorization, eligibility, idempotence, uniqueness, or failure mapping is part of the requirement.

Avoid tests that are tautological, implementation-coupled, excessively mocked, or dependent on private call order. Avoid adding test-only production hooks when a correct public seam already exists.

## Validation

Targeted tests provide fast feedback but never replace the repository gate. For implementation/build-affecting work, finish with the canonical developer validation from `docs/workflow.md`:

`./dev/dev.sh check`

Then apply the repository's normal whitespace/staging gates before commit and PR.
