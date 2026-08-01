# Project Scope

## Authority

This document is the authoritative source for the current accepted project scope of Composable Domain Platform.

## Purpose

Composable Domain Platform provides a reliable foundation for composing independently bounded business capabilities. It is not tied to one business domain.

Event management is expected to become the first reference capability, but it is not the platform core and does not define the platform's general model.

## Current phase

**Build foundation**

The repository and architecture foundation has been accepted. The current phase establishes the smallest executable build foundation required to enforce future module boundaries consistently.

## In scope

- Add the Gradle Wrapper.
- Use Gradle Kotlin DSL for project build configuration.
- Establish a Java 21+ toolchain policy.
- Establish a Gradle Version Catalog for centrally managed dependency/plugin coordinates.
- Establish `build-logic` as an included build for convention plugins.
- Establish only the minimum convention-plugin infrastructure needed by later module types.
- Establish a deterministic root `./gradlew check` command that succeeds on the build foundation.
- Keep build configuration compatible with the accepted modular-monolith and hard-boundary architecture.
- Update authoritative documentation when the implemented build shape differs from current architectural intent.

## Acceptance criteria

The phase is complete when:

1. A fresh checkout can run the committed Gradle Wrapper without relying on a globally installed Gradle version.
2. The project uses Kotlin DSL.
3. Java toolchain configuration targets the accepted Java baseline.
4. Version Catalog and convention-plugin infrastructure are present and buildable.
5. `./gradlew check` succeeds from the repository root.
6. No business module, framework runtime, persistence, external contract, or deployment concern is introduced.

## Explicitly out of scope

The following remain intentionally excluded from the current phase:

- Business-domain implementation.
- Event or any other reference capability implementation.
- Spring Boot application bootstrap.
- Spring Modulith configuration.
- ArchUnit architecture rules.
- PostgreSQL schemas and Flyway migrations.
- jOOQ configuration.
- OpenAPI contracts or generation.
- Frontend implementation.
- Docker or deployment configuration.
- GitHub Actions or other CI/CD automation.
- External integrations.
- Kafka, RabbitMQ, Redis, Kubernetes, or other infrastructure without a demonstrated requirement.
- Multi-model development workflow automation.

These items may enter a later phase only through an explicit scope decision.

## Business capability admission rule

A new business capability may enter active scope only when all of the following can be answered:

1. What concrete use case requires it?
2. Why can the requirement not be satisfied within the currently accepted scope?
3. What does the proposed bounded context own?
4. What does it explicitly not own?

## Technology admission rule

A new technology or infrastructure component may enter active scope only when:

1. A concrete accepted requirement exists.
2. The current baseline cannot satisfy that requirement adequately.
3. Reasonable alternatives have been considered.
4. The operational and architectural consequences are understood.

Technology must solve an accepted requirement; the project must not invent requirements to justify a technology.

## Scope change rule

Changes to this document are project decisions and must be made through a topic branch and pull request.

A pull request that introduces functionality outside the accepted scope must either remove the out-of-scope change or explicitly update this document and justify the scope change.

Hidden scope expansion inside implementation pull requests is not accepted.

## Deferred ideas

Potential future capabilities may be recorded as deferred ideas, but a deferred idea is not planned scope and must not create implementation, module, infrastructure, or API commitments.

Examples currently include event management, content management, registration, ticketing, booking, membership, surveys, payment integrations, and accounting integrations.

Their eventual bounded-context boundaries must be determined from real use cases rather than assumed in advance.
