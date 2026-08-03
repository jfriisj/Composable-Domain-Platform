# Project Status

## Authority

This document is the authoritative concise statement of where the project currently stands. It describes status, not scope; accepted scope is defined in [`scope.md`](scope.md).

## Current phase

**Registration composition proof — domain-neutral Registration scope accepted**

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
- First release preparation accepted into `development` through PR #22.
- First release promoted from `development` to `production` through release PR #23 using a merge commit.
- Annotated repository release tag `v0.1.0` created on accepted `production` commit `5427dabe5eb0d00c25cd7470d345016f7cf77404`.
- Issue planning and prioritization workflow accepted into `development` through PR #25.
- CI resource policy accepted into `development` through PR #26, keeping `validate` registered on both permanent-branch PR targets while executing the GitHub-hosted validation runner only for `production`.
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
- GitHub Actions registers the `validate` job for pull requests targeting both `development` and `production` so the existing required check remains present on both permanent branches.
- For pull requests targeting `development`, `validate` is skipped before runner allocation; implementation and build-affecting changes use the mandatory local `./gradlew --no-daemon check` integration gate.
- For pull requests targeting `production`, GitHub Actions executes `./gradlew --no-daemon check` with JDK 21 and `validate` acts as the independent release gate.
- The `validate` GitHub Actions check remains required by the active rulesets for both permanent branches.
- Controlled negative CI verification through draft PR #14 confirmed that a failing root `./gradlew --no-daemon check` produces a failing `validate` GitHub status; the validation PR was closed without merge.
- PR #20 passed the required `validate` check before the Event runtime and HTTP implementation was accepted into `development`.
- Release PR #23 passed the required `validate` check before the `v0.1.0` state was accepted into `production`.
- Event domain production classes are prevented from depending on Event application implementation classes, the public Event API, persistence-adapter classes, database infrastructure, HTTP, or Spring runtime concepts.
- Event application implementation remains independent of the persistence adapter, database technologies, HTTP, and Spring runtime concepts.

## In progress

- The Registration composition proof is the accepted next implementation phase.
- Implementation has not started.
- Registration is planned as a domain-neutral capability owning namespaced opaque `RegistrantReference` and `TargetReference` values rather than Event-specific state.
- The Event-Registration composition is planned as the owner of Event-specific existence validation and translation into Registration references.
- The planned external contract is Event-specific at `contracts/http/v1/event-registration.yaml`; no generic Registration HTTP dispatcher is accepted.
- Issue #32 records the original scope decision, and issue #35 records the domain-boundary correction that supersedes the Event-specific Registration state model.

## Known gaps

- No authentication or authorization exists; those concerns were intentionally outside the completed Event runtime and HTTP phase.
- No production deployment, TLS, secrets-management, or production database-operations baseline has been accepted.
- No artifact/package publication process has been accepted.

## Next priority

Update and execute the blocked Registration implementation work against the accepted domain-neutral Registration model without expanding beyond `docs/scope.md`.

Implementation must remain limited to the accepted Registration capability, Event-Registration composition, Event-specific registration HTTP surface, Registration-owned persistence, runtime wiring, architecture verification, and required tests.

Authentication/authorization, Person capability implementation, payment, capacity, ticketing, notifications, messaging, frontend, deployment, external integrations, unrelated Event lifecycle behavior, and other excluded concerns remain outside accepted scope.
