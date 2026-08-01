# Composable Domain Platform

Composable Domain Platform is a modular application platform for composing independently bounded business capabilities through explicit contracts, integrations, and compositions.

The platform is not tied to a single business domain. Event management is the first reference capability, but it is not the center of the platform.

## Principles

- Domain-Driven Design with explicit bounded contexts.
- Hexagonal Architecture inside business modules.
- Hard module boundaries; no cross-module implementation or database access.
- Composition over implementation coupling.
- External HTTP contracts defined with OpenAPI.
- Architecture diagrams, scope, module ownership, and ADRs are version-controlled authoritative artifacts.
- Design for extension, implement only accepted requirements.

## Authoritative project sources

- [`docs/scope.md`](docs/scope.md) — current accepted scope and explicit exclusions.
- [`docs/project-status.md`](docs/project-status.md) — current project state and next priority.
- [`docs/governance.md`](docs/governance.md) — governance, branching, change control, and sources of truth.
- [`docs/architecture.md`](docs/architecture.md) — architectural principles and hard boundaries.
- [`docs/modules.md`](docs/modules.md) — allowed module types and ownership rules.
- [`docs/tech-stack.md`](docs/tech-stack.md) — accepted technology directions and candidates.
- [`docs/architecture/workspace.dsl`](docs/architecture/workspace.dsl) — authoritative architecture model.
- [`docs/adr/`](docs/adr/) — architectural decision records.

## Current state

The repository, architecture, and executable Gradle build foundations are established. Event is the first implemented reference bounded context, with separate public API and private implementation Gradle projects. No application runtime, persistence, or external HTTP contract is implemented.
