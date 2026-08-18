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

## Current state

The authoritative current project state and next priority are maintained in [`docs/project-status.md`](docs/project-status.md); accepted scope and exclusions are maintained in [`docs/scope.md`](docs/scope.md).

Current accepted architecture includes independently owned Event, Registration, and Security modules with explicit public APIs and private implementations, plus the non-module Event-Registration workflow composition. The Event HTTP adapter lives under `platform/interfaces/http`, while participant-private Event-registration HTTP adaptation is physically separated under `platform/interfaces/event-registration-http`.

Two executable application compositions currently prove the platform model: the full Platform Application under `platform/apps/platform` and an Event-only application under `platform/apps/event` that deliberately omits Registration, Security, and Event-Registration from its functional dependency graph. The authoritative architecture model is [`docs/architecture/workspace.dsl`](docs/architecture/workspace.dsl).

Operational and development references:

- **External HTTP source contracts:** Event [`platform/contracts/http/v1/event.yaml`](platform/contracts/http/v1/event.yaml) and Event-Registration [`platform/contracts/http/v1/event-registration.yaml`](platform/contracts/http/v1/event-registration.yaml); application contracts are derived statically during the build.
- **Operational runtime:** executable Spring Boot/JVM JAR; see [ADR-0010](docs/adr/0010-executable-jvm-operational-runtime-boundary.md). Runtime readiness is exposed at `GET /internal/readiness`.
- **Developer environment:** repository-controlled Linux/Docker workflow through [`dev/dev.sh`](dev/dev.sh); accepted technology constraints are recorded in [`docs/tech-stack.md`](docs/tech-stack.md).
- **Development workflow:** canonical commands, Git/PR flow, and validation rules are defined in [`docs/workflow.md`](docs/workflow.md).
- **Validation:** `./gradlew --no-daemon check`; on the accepted Linux developer environment, `./dev/dev.sh check` provides the repository-controlled equivalent.

## Authoritative project sources

- **Scope and status:** [`docs/scope.md`](docs/scope.md), [`docs/project-status.md`](docs/project-status.md).
- **Governance and workflow:** [`docs/governance.md`](docs/governance.md), [`docs/workflow.md`](docs/workflow.md).
- **Architecture and ownership:** [`docs/architecture/workspace.dsl`](docs/architecture/workspace.dsl), [`docs/architecture.md`](docs/architecture.md), [`docs/modules.md`](docs/modules.md).
- **Architecture rationale:** [`docs/adr/`](docs/adr/).
- **Technology direction:** [`docs/tech-stack.md`](docs/tech-stack.md).
