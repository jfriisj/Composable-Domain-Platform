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

## Records

- [ADR-0001: Modular monolith with bounded contexts](0001-modular-monolith-with-bounded-contexts.md)
- [ADR-0002: Gradle multi-project boundaries](0002-gradle-multi-project-boundaries.md)
- [ADR-0003: Architecture model as code](0003-architecture-model-as-code.md)
- [ADR-0004: Correlation and causation traceability](0004-correlation-and-causation-traceability.md)
- [ADR-0005: Event-owned PostgreSQL persistence](0005-event-owned-postgresql-persistence.md)
- [ADR-0006: Spring Boot and OpenAPI runtime boundary](0006-spring-boot-openapi-runtime-boundary.md)
- [ADR-0007: Registration capability and cross-capability composition](0007-registration-capability-and-cross-capability-composition.md) — Superseded by ADR-0008
- [ADR-0008: Domain-neutral Registration and Event-registration composition](0008-domain-neutral-registration-and-event-registration-composition.md)
- [ADR-0009: Unified Event-facing OpenAPI contract](0009-unified-event-facing-openapi-contract.md)
- [ADR-0010: Executable JVM operational-runtime boundary](0010-executable-jvm-operational-runtime-boundary.md)
- [ADR-0011: Registration-owned cancellation lifecycle](0011-registration-owned-cancellation-lifecycle.md)
- [ADR-0012: Minimum participant authentication boundary](0012-minimum-participant-authentication-boundary.md)
- [ADR-0013: Universal independent module invariant](0013-universal-independent-module-invariant.md)
- [ADR-0014: Security public authentication and authorization boundary](0014-security-public-authentication-authorization-boundary.md)
- [ADR-0015: Static selectable application composition](0015-static-selectable-application-composition.md)
- [ADR-0016: Selectable external contracts with static application aggregation](0016-selectable-external-contracts-static-application-aggregation.md)

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
