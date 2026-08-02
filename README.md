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
- [`docs/workflow.md`](docs/workflow.md) — operational development workflow from accepted scope to merge and next scope gate.
- [`docs/architecture.md`](docs/architecture.md) — architectural principles and hard boundaries.
- [`docs/modules.md`](docs/modules.md) — allowed module types and ownership rules.
- [`docs/tech-stack.md`](docs/tech-stack.md) — accepted technology directions and candidates.
- [`docs/architecture/workspace.dsl`](docs/architecture/workspace.dsl) — authoritative architecture model.
- [`docs/adr/`](docs/adr/) — architectural decision records.

## Current state

Event is the first implemented reference bounded context. It has separate public API and private implementation Gradle projects, Event-owned durable PostgreSQL persistence through a private jOOQ adapter and Flyway migrations, and executable architecture verification.

The repository now also contains the first executable platform runtime and external interface: a Spring Boot composition root under `apps/platform`, an HTTP inbound adapter under `interfaces/http`, a versioned authoritative OpenAPI contract under `contracts/http/v1/event.yaml`, and a minimal business-neutral execution context under `core`. End-to-end tests exercise the running HTTP boundary against real PostgreSQL through Testcontainers.

## Run the platform application

The runtime requires an externally configured PostgreSQL database. Supply the three runtime properties through environment variables and start the Spring Boot application from the repository root:

~~~bash
PLATFORM_DATABASE_URL='jdbc:postgresql://localhost:5432/platform' PLATFORM_DATABASE_USERNAME='platform' PLATFORM_DATABASE_PASSWORD='platform' ./gradlew --no-daemon :platform-app:bootRun
~~~

The default HTTP port is `8080` unless standard Spring Boot server configuration overrides it.

The currently accepted external surface is:

- `POST /api/v1/events`
- `GET /api/v1/events/{eventId}`

The authoritative wire contract is [`contracts/http/v1/event.yaml`](contracts/http/v1/event.yaml). Generated OpenAPI Java sources are derived build output and are not edited independently.

Production secrets management, deployment, TLS, authentication, authorization, and operational database configuration remain outside the current phase.

## Validate

The authoritative repository validation gate is:

~~~bash
./gradlew --no-daemon check
~~~

It includes Event unit and persistence tests, architecture verification, platform runtime tests, and HTTP-to-Event-to-PostgreSQL end-to-end validation.
