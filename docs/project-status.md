# Project Status

## Authority

This document is the authoritative concise statement of where the project currently stands. It describes status, not scope; accepted scope is defined in [`scope.md`](scope.md).

## Current phase

**Minimum usable adult Event Registration lifecycle — scope accepted**

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
- Domain-neutral Registration API, implementation, Registration-owned PostgreSQL persistence, migration, tests, and architecture verification were accepted into `development` through PR #37.
- Event-Registration composition, Event-facing HTTP operations in the unified `event.yaml`, runtime wiring, Registration startup migration, architecture enforcement, and real PostgreSQL end-to-end validation were accepted into `development` through PR #41, completing issue #38.
- Operational-runtime research completed in issue #30, establishing the minimum operator use case, reproducibility requirements, externally supplied dependencies, readiness evidence, solution alternatives, and the separate Terraform/OpenTofu/IaC provisioning boundary.
- The minimum operational-runtime contract was selected in issue #45: executable JVM artifact, externally supplied Java/PostgreSQL/host/network, machine-checkable readiness, and no infrastructure provisioning or Terraform/OpenTofu/IaC in the minimum proof.
- The minimum operational-runtime proof was implemented through PR #51, completing issue #49 with an executable Spring Boot/JVM artifact run path, runtime-owned machine-checkable readiness, repeatable operator documentation, restart/durability evidence, and PostgreSQL-loss readiness validation.
- Goal/Subgoal planning was accepted through PR #55, completing decision issue #54 with `type: goal` planning semantics, explicit `Goal: #...` parent relationships, separate execution dependencies, progressive decomposition, parallel-readiness rules, and end-to-end Goal completion criteria.
- Decision issue #53 selected the minimum usable adult Event Registration lifecycle as the next product-driven proof: an adult participant can discover an intentionally available Event, register, later retrieve private Event-registration state, and cancel that registration. The decision does not authorize implementation or technology.
- Decision issue #59 accepted explicit Event-owned `unpublished` / `published` publication state and public discovery of published Events while keeping publication separate from Registration eligibility.
- Decision issue #60 accepted a transport-neutral authenticated actor reference with Event-Registration-owned participant authorization for participant-private create/retrieve/cancel behavior while keeping Registration security-neutral.
- Decision issue #61 accepted Registration-owned generic `active` / `cancelled` lifecycle semantics with idempotent cancellation and uniqueness preserved across cancellation.
- Documentation issue #62 recorded the Registration lifecycle ownership extension as Accepted ADR-0011 without changing Event/Registration dependency relationships.
- Decision issue #64 accepted the minimum participant-data/privacy boundary: platform-facing opaque actor identity, no raw provider subject as Registration durable state, external non-owner existence concealment, identity-free normal logging/correlation, and no extra retention workflow.
- Scope issue #65 accepted the minimum usable adult Event Registration lifecycle for implementation planning while leaving concrete authentication technology, actor-reference derivation, and any new identity-mapping/security architecture subject to later readiness and change-control gates.

## In progress

- Goal issue #57 remains active with `priority: now`; its minimum participant lifecycle scope is accepted and ready for progressive implementation decomposition.
- No executable implementation subgoal is active yet. Each candidate child must still pass its own dependency, ownership, architecture, technology, and validation readiness checks before execution.

## Known gaps

- No authentication or authorization implementation exists yet. The accepted lifecycle requires an external/security authentication boundary and Event-Registration-owned participant authorization, but concrete authentication technology and actor-reference derivation remain unselected.
- No durable provider-to-platform identity-mapping store or new security component is accepted; any concrete need for one requires separate decision/architecture control.
- No production deployment, TLS, secrets-management, or production database-operations baseline has been accepted.
- No artifact/package publication process has been accepted.

## Next priority

Progressively decompose the accepted Goal #57 scope into the smallest independently verifiable executable subgoals for Event publication/discovery, Registration lifecycle, Event-Registration participant authorization/orchestration, external contract/security-boundary adaptation, and complete end-to-end evidence.

Before any authentication/security-boundary implementation becomes ready, verify whether its concrete design can use already accepted technology and existing architectural relationships. Any new security technology, durable identity mapping, persistence owner, component, or significant relationship requires the applicable decision/ADR/architecture gate first.

Person/Account, participant profiles, capacity/waitlists, re-registration/reactivation, payment/ticketing, notifications/messaging, frontend, deployment/infrastructure expansion, and other exclusions in `docs/scope.md` remain outside accepted scope.
