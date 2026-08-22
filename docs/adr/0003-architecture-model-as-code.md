# ADR-0003: Architecture diagrams are maintained as code

## Status
- Status: Accepted
- Date: 2026-08-01

## Context

Architecture must be understandable by stakeholders and developers without maintaining separate, conflicting diagrams.

Diagram changes must be reviewable and versioned with the code and architectural decisions they represent.

## Decision

Use the C4 model expressed through Structurizr DSL as the authoritative architecture diagram model.

`docs/architecture/workspace.dsl` is the source of truth. Rendered PNG/SVG views are derived artifacts and are not independently edited.

Views must clearly distinguish current accepted architecture from planned or exploratory ideas.

## Rationale
The decision is retained for the constraints recorded in Context and the trade-offs recorded in Alternatives considered and Consequences; this migration changes document structure only.

## Alternatives considered

- Manually maintained Draw.io diagrams.
- Presentation slides as architecture documentation.
- Mermaid as the primary architecture model.

## Consequences

- Architecture changes can be reviewed through Git diffs and pull requests.
- Multiple stakeholder views can be derived from one model.
- The project must validate Structurizr DSL in CI once CI enters scope.
- Exploratory diagrams must not be presented as current authoritative architecture.
