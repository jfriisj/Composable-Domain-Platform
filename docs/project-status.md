# Project Status

## Authority

This document is the authoritative concise statement of where the project currently stands. It describes status, not scope; accepted scope is defined in [`scope.md`](scope.md).

## Current phase

**Event runtime and HTTP interface**

## Completed

- Public GitHub repository created.
- `development` established as the default integration branch.
- `production` established as the stable/release branch.
- Repository and architecture foundation accepted into `development` through PR #1.
- Build Foundation accepted into `development` through PR #3.
- Event reference module accepted into `development` through PR #5.
- Project workflow accepted into `development` through PR #6.
- Continuous Integration Foundation scope accepted into `development` through PR #7.
- Minimum GitHub Actions continuous integration accepted into `development` through PR #8.
- Continuous Integration Foundation completion recorded through PR #10.
- Architecture Verification Foundation scope accepted into `development` through PR #11.
- Minimum ArchUnit architecture verification accepted into `development` through PR #12.
- Architecture Verification Foundation completion recorded through PR #13.
- Event Durable Persistence scope accepted into `development` through PR #17.
- Event durable persistence implementation accepted into `development` through PR #18.
- Authoritative scope, status, governance, workflow, architecture, module-model, and technology-direction documents established.
- Structurizr DSL established as the authoritative architecture model.
- ADR process established.
- Initial architecture decisions accepted for modular-monolith bounded contexts, Gradle multi-project boundaries, architecture-as-code, correlation/causation traceability, and Event-owned PostgreSQL persistence.
- Gradle Wrapper, Kotlin DSL, Java 21 toolchain convention, Version Catalog foundation, `build-logic`, and root `./gradlew check` established.
- Event reference module established with separate public API and private implementation Gradle projects.
- Event ownership, application contract, domain invariants, and reference-module tests established.
- Event public API supports definition and retrieval by identity without exposing persistence types.
- Event application services use an application-owned persistence port.
- Event durable state is stored in an Event-owned PostgreSQL schema defined by Flyway migrations and accessed through a private jOOQ adapter.
- Duplicate Event identity is rejected without replacing existing durable state.
- Event persistence integration is validated against real PostgreSQL through Testcontainers.
- The authoritative architecture model reflects the Event API, implementation, and persistence boundaries.
- GitHub Actions runs `./gradlew --no-daemon check` with JDK 21 for pull requests targeting `development` and `production`.
- The `validate` GitHub Actions check is required by the active rulesets for both permanent branches.
- The CI trigger and required check have been verified successfully for pull requests targeting both `development` and `production`.
- Controlled negative CI verification through draft PR #14 confirmed that a failing root `./gradlew --no-daemon check` produces a failing `validate` GitHub status; the validation PR was closed without merge.
- ArchUnit verifies the accepted Event domain/application/persistence-adapter dependency direction through the existing `event-impl` JUnit test task.
- Event domain production classes are prevented from depending on Event application implementation classes, the public Event API, persistence-adapter classes, or database infrastructure.
- Event application implementation remains independent of the persistence adapter and database technologies.

## In progress

- Establish the minimum business-neutral Correlation ID execution context required by the first external entry point.
- Define the versioned OpenAPI contract for Event definition and retrieval.
- Add the HTTP interface and executable Spring Boot composition root without moving runtime concerns into Event.
- Wire the existing Event durable persistence into runtime startup and apply Event Flyway migrations before serving requests.
- Validate the full HTTP-to-Event-to-PostgreSQL slice through Testcontainers and the existing root validation gate.

## Known gaps

- No executable application runtime exists yet.
- No external HTTP contract or HTTP interface exists yet.
- No authentication or authorization exists; those concerns are intentionally outside the current phase.
- No release has been produced from `production`.

## Next priority

Implement the minimum Event runtime and HTTP interface slice authorized by `docs/scope.md`.

Do not add Event lifecycle behavior, additional bounded contexts, authentication/authorization, messaging, frontend, deployment, or unrelated infrastructure in this phase.
