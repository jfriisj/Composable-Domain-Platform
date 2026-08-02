# Architectural Decision Records

Architectural Decision Records (ADRs) capture significant decisions and, most importantly, why they were made.

## When an ADR is required

Create an ADR when a decision materially changes architectural boundaries, module relationships, persistence ownership, contract strategy, deployment architecture, or introduces a significant technology/platform constraint.

Minor implementation details do not require ADRs.

## Naming

Use sequential identifiers:

```text
0001-short-decision-title.md
0002-next-decision.md
```

## Status

Use one of:

- Proposed
- Accepted
- Superseded
- Rejected

Accepted ADRs are not silently rewritten to change historical rationale. A later decision supersedes an earlier ADR and links to it.

## Template

```markdown
# ADR-NNNN: Decision title

- Status: Proposed
- Date: YYYY-MM-DD

## Context

What problem or constraint requires a decision?

## Decision

What has been decided?

## Alternatives considered

What reasonable alternatives were considered?

## Consequences

What becomes easier, harder, constrained, or intentionally deferred?
```

## Authority

ADRs own architectural rationale. Current architecture is represented by the accepted repository state and the authoritative Structurizr model; ADRs explain why significant decisions were made.
