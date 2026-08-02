# Project Status

## Authority

This document is the authoritative concise statement of where the project currently stands. It describes status, not scope; accepted scope is defined in [`scope.md`](scope.md).

## Current phase

**Event runtime and HTTP interface — complete**

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
- Event runtime and HTTP interface scope accepted into `development` through PR #19.
- Event runtime and HTTP interface implementation accepted into `development` through PR #20.
- Authoritative scope, status, governance, workflow, architecture, module-model, and technology-direction documents established.
- Structurizr DSL established as the authoritative architecture model.
- ADR process established.
- Initial architecture decisions accepted for modular-monolith bounded contexts, Gradle multi-project boundaries, architecture-as-code, correlation/causation traceability, Event-owned PostgreSQL persistence, and the Spring Boot/OpenAPI runtime boundary.
- Gradle Wrapper, Kotlin DSL, Java 21 toolchain convention, Version Catalog foundation, `build-logic`, and root `./gradlew check` established.
- Event reference module established with separate public API and private implementation Gradle projects.
- Event ownership, application contract, domain invariants, and reference-module tests established.
- Event public API supports definition and retrieval by identity without exposing persistence types.
- Event public application calls carry the minimum business-neutral execution context required for explicit Correlation ID propagation.
- Event application services use an application-owned persistence port.
- Event durable state is stored in an Event-owned PostgreSQL schema defined by Flyway migrations and accessed through a private jOOQ adapter.
- Duplicate Event identity is rejected without replacing existing durable state.
- Event persistence integration is validated against real PostgreSQL through Testcontainers.
- A versioned OpenAPI contract under `contracts/http/v1/event.yaml` defines the accepted external Event definition and retrieval surface.
- The HTTP interface maps transport contracts to Event public application contracts without depending on Event implementation or persistence.
- The executable Spring Boot platform application composes the HTTP interface, Event implementation, PostgreSQL runtime configuration, and Event-owned Flyway startup migration.
- HTTP responses establish or preserve `X-Correlation-Id` and propagate the resulting identifier through `ExecutionContext` into the Event application boundary.
- Running HTTP-to-Event-to-PostgreSQL end-to-end tests validate success, duplicate, unknown, invalid-input, internal-failure, durability, and correlation behavior against real PostgreSQL through Testcontainers.
- Executable ArchUnit verification covers the accepted core, Event, HTTP interface, and application-runtime dependency boundaries.
- The authoritative architecture model reflects the current core, contract, HTTP interface, runtime, Event API/implementation, and Event persistence boundaries.
- GitHub Actions runs `./gradlew --no-daemon check` with JDK 21 for pull requests targeting `development` and `production`.
- The `validate` GitHub Actions check is required by the active rulesets for both permanent branches.
- The CI trigger and required check have been verified successfully for pull requests targeting both `development` and `production`.
- Controlled negative CI verification through draft PR #14 confirmed that a failing root `./gradlew --no-daemon check` produces a failing `validate` GitHub status; the validation PR was closed without merge.
- PR #20 passed the required `validate` check before the Event runtime and HTTP implementation was accepted into `development`.
- Event domain production classes are prevented from depending on Event application implementation classes, the public Event API, persistence-adapter classes, database infrastructure, HTTP, or Spring runtime concepts.
- Event application implementation remains independent of the persistence adapter, database technologies, HTTP, and Spring runtime concepts.

## In progress

- No implementation work is currently in progress.
- The next implementation phase requires a new explicit scope decision before code, contracts, technologies, or infrastructure are introduced.

## Known gaps

- No authentication or authorization exists; those concerns were intentionally outside the completed Event runtime and HTTP phase.
- No production deployment, TLS, secrets-management, or production database-operations baseline has been accepted.
- No release has been produced from `production`.

## Next priority

Make the next explicit scope decision based on a concrete use case and the admission rules in `docs/scope.md` and `docs/governance.md`.

Until that decision is accepted, do not introduce new Event lifecycle behavior, additional bounded contexts, authentication/authorization, messaging, frontend, deployment, external integrations, or unrelated infrastructure.
