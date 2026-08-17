# Project Status

## Authority

This document is the authoritative concise statement of where the project currently stands. It describes status, not scope; accepted scope is defined in [`scope.md`](scope.md).

## Current phase

**Selectable application composition — static architecture accepted; implementation #131 next**

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
- A versioned OpenAPI contract under `platform/contracts/http/v1/event.yaml` defines the accepted external Event definition and retrieval surface.
- The HTTP interface maps transport contracts to Event public application contracts without depending on Event implementation or persistence.
- The executable Spring Boot platform application composes the HTTP interface, Event implementation, PostgreSQL runtime configuration, and Event-owned Flyway startup migration.
- HTTP responses establish or preserve `X-Correlation-Id` and propagate the resulting identifier through `ExecutionContext` into the Event application boundary.
- Running HTTP-to-Event-to-PostgreSQL end-to-end tests validate success, duplicate, unknown, invalid-input, internal-failure, durability, and correlation behavior against real PostgreSQL through Testcontainers.
- Executable ArchUnit verification covers the accepted core, Event, HTTP interface, and application-runtime dependency boundaries.
- The authoritative architecture model reflects the current core, contract, HTTP interface, runtime, Event API/implementation, and Event persistence boundaries.
- GitHub Actions registers the `validate` job for pull requests targeting both `development` and `production`, while hosted validation executes only for `production`.
- For pull requests targeting `development`, `validate` is skipped before runner allocation; implementation and build-affecting changes use the mandatory local `./gradlew --no-daemon check` integration gate, and the active `development` ruleset does not require `validate`.
- For pull requests targeting `production`, GitHub Actions executes `./gradlew --no-daemon check` with JDK 21; `validate` acts as the independent release gate and remains required by the active `production` ruleset.
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
- Registration lifecycle implementation #67 was accepted into `development` through PR #68, adding Registration-owned `active` / `cancelled` lifecycle state, transport-neutral generic idempotent cancellation, durable lifecycle persistence and retrieval, and preservation of registrant-target uniqueness across cancellation.
- Event publication/discovery implementation #74 added Event-owned `unpublished` / `published` state, one-way publication, transport-neutral discovery of published Events, durable V1-to-V2 migration/backfill, and real-PostgreSQL validation while keeping known-id retrieval publication-independent.
- Participant-private Event-Registration orchestration #79 was accepted into `development` through PR #80, adding the transport-neutral actor-bound create/retrieve/cancel path, Event-Registration-owned participant authorization, Registration lifecycle exposure, and cancellation delegation after authorization while preserving the legacy HTTP-facing create/find compatibility path unchanged.
- Research issue #83 determined the minimum external participant authentication boundary candidates and established that caller-controlled identity is not authentication, no durable identity mapping is demonstrated as necessary, and Spring Security is the least-bespoke in-process candidate for the existing Spring Boot runtime.
- Decision issue #84 selected Spring Security with stateless HTTP Basic for the minimum non-browser participant-authentication proof, externally supplied encoded runtime credential verifiers, direct opaque stable platform principal pseudonyms as `AuthenticatedActorReference`, and Event-Registration-owned participant authorization.
- Documentation issue #85 was accepted through PR #86 as ADR-0012, recording the selected authentication boundary without changing the architecture model or admitting implementation by itself.
- Scope issue #87 was accepted through PR #88, admitting Spring Security with stateless HTTP Basic for the bounded Goal #57 participant-authentication proof, externally configured encoded password verifiers, direct opaque stable platform-principal adaptation, and no new modeled architecture relationship.
- Implementation issue #91 replaces the transitional caller-owned Event-registration HTTP path with the actor-bound participant-private create/retrieve/cancel path, adds the admitted Spring Security stateless HTTP Basic runtime boundary, derives the opaque actor directly from the authenticated stable platform principal, exposes Registration lifecycle/cancellation, conceals authenticated non-owner existence, preserves correlation/privacy behavior, and validates restart durability against real PostgreSQL.
- Research #93 identified that the accepted module model was weaker than the required project-wide independent-module architecture.
- Decision #94 selected the universal independent-module invariant: every module is independently owned, selectable in application composition, exposes its own public API, hides its private implementation, collaborates through public contracts/adapters, and is never owned or implemented by the runtime, another module, or a composition.
- Documentation #95 records that decision as ADR-0013 and synchronizes module, architecture, governance, and status truth without changing executable module structure or accepted product behavior.
- Scope #97 admits the corrective planned Security module boundary required by ADR-0013: Authentication + Authorization belong to an independently owned `security-api` / `security-impl` module, while the application runtime becomes wiring-only for Security and Event-Registration remains a non-module workflow composition.
- Decision #99 selects the minimum Security public boundary: Security-owned `AuthenticatedActorReference` / `AuthenticatedActorProvider`, opaque `ResourceOwnerReference`, `AuthorizationDecision`, and `AuthorizeResourceOwnership`; Event-Registration retains workflow/domain-fact translation, create uses Authentication only, and retrieve/cancel use the same ownership decision.
- Documentation #100 records that boundary as Accepted ADR-0014 and synchronizes Planned module/scope/architecture/status truth without changing executable source/build state.
- Implementation #102 was accepted into `development` through PR #103, establishing separate `security-api` / `security-impl` Gradle projects, moving actor/provider and Spring Security mechanism ownership to Security, delegating final Event-Registration ownership decisions through `AuthorizeResourceOwnership`, rewiring HTTP/runtime through public Security contracts, and adding executable architecture enforcement while preserving participant-private behavior.
- Implementation #108 exposes Event-owned publication and published-only discovery over HTTP through `GET /api/v1/events` and `POST /api/v1/events/{eventId}/publication`, preserves correlation, maps unknown publication to `event_not_found` and repeated publication to `event_already_published`, wires the existing Event services without changing Event ownership or persistence, and validates publication/discovery durability plus the discovered-Event participant registration/retrieve/cancel lifecycle against real PostgreSQL.
- Goal #57 objective acceptance is satisfied in accepted `development`: the minimum usable adult Event Registration lifecycle is proven through published Event discovery, participant-authenticated registration, participant-private retrieval, cancellation to `cancelled`, explicit failure semantics, correlation, application restart against the same real PostgreSQL database, and accepted ownership and architecture boundaries.
- Research #117 completed the minimum reproducible developer-environment technology and trust-boundary analysis for Goal #116.
- Decision #118 selected Docker Engine + Docker Compose on the admitted Linux host boundary, host-Docker/Testcontainers sibling access, a digest-pinned Temurin JDK 21 developer image, disposable Docker-managed Gradle cache state, and optional PostgreSQL 18.4 for manual development while preserving the executable-JAR runtime boundary.
- Scope #119 was accepted through PR #120, admitting that bounded developer-tooling direction in `docs/scope.md` and `docs/tech-stack.md` without admitting application/runtime/deployment containerization.
- Defect #122 corrected the active `development` ruleset so the intentionally skipped `validate` job is no longer required there; the `production` ruleset continues to require hosted `validate`.
- Implementation #125 establishes the repository-controlled Docker Compose developer environment with digest-pinned Temurin JDK 21 and optional PostgreSQL 18.4, repository-Wrapper validation through host-Docker/Testcontainers, non-root host ownership, disposable persistent Gradle/PostgreSQL state, successful fresh-checkout validation, and manual `bootRun` readiness while preserving the executable-JAR runtime boundary and excluding application OCI/deployment packaging.
- Goal #116 objective acceptance is satisfied in accepted `development`: a fresh checkout on the documented Linux host can enter the repository-controlled developer environment, use its supplied Java 21 and repository Gradle Wrapper, pass authoritative root validation with Testcontainers-owned real PostgreSQL, run the existing application against optional persistent development PostgreSQL to readiness, preserve usable host ownership and disposable development state, and retain the executable-JAR runtime boundary without application OCI/deployment scope.

- Decision #130 selects the minimum static selectable-composition mechanism for Goal #114: an Event-only executable composition, explicit Gradle project boundaries, and minimum physical separation of participant-private Event-registration HTTP adaptation. ADR-0015 records that decision as Accepted architecture rationale while the selected new allocation remains Planned until implementation #131.

## In progress

- Goal #114 `[Goal] Prove selectable application composition` is the current `priority: now` workstream.
- Scope #115 admits the minimum developer-facing selectable-composition proof.
- ADR-0015 records decision #130 as the accepted static architecture direction. Implementation #131 is the next executable subgoal after the required post-merge re-read updates its baseline/readiness.

## Known gaps

- `platform/compositions/event-registration` is currently one Gradle project and remains a non-module composition under the accepted ADR-0013 classification; no split is required by #97/#99/ADR-0014.
- Security has no persistence, provider-specific identity mapping, Person/Account capability, or role/policy expansion; those remain outside the accepted correction.
- No durable provider-to-platform identity-mapping store, Person/Account capability, or external identity provider is accepted. HMAC derivation remains deferred unless a future raw provider subject creates that need.
- Production TLS termination, secrets-management products, deployment/infrastructure, credential enrollment/reset/recovery/admin APIs, and production database operations remain outside the current accepted proof.
- No artifact/package publication process has been accepted.

## Next priority

After this documentation transition is accepted, re-read remote `development`, Goal #114, #132, and implementation #131; update #131 to the resulting accepted baseline and proceed only if no new blocker is exposed.

Then implement #131 as the bounded static Event-only composition proof governed by scope #115 and ADR-0015. Do not expand it into a new capability, ownership change, external contract, persistence change, dynamic composition mechanism, or new technology.

Event-Registration remains a non-module composition. Security remains the Current independent Authentication + Authorization module accepted through #102/PR #103.

Person/Account, participant profiles, capacity/waitlists, re-registration/reactivation, payment/ticketing, notifications/messaging, frontend, deployment/infrastructure expansion, roles/permissions, generic policy-engine work, and other exclusions in `docs/scope.md` remain outside accepted scope.
