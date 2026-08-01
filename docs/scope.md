# Project Scope

## Authority

This document is the authoritative source for the current accepted project scope of Composable Domain Platform.

## Purpose

Composable Domain Platform provides a reliable foundation for composing independently bounded business capabilities. It is not tied to one business domain.

Event management is the first reference capability, but it is not the platform core and does not define the platform's general model.

## Current phase

**Architecture verification foundation**

The repository, architecture, executable Gradle build, project workflow, first Event reference module, and continuous-integration foundation have been accepted.

The current phase makes the already accepted internal architecture rules executable so structural violations fail the same root validation gate that CI requires before merge.

## Concrete requirement

The current Event implementation must have automated build-time verification of its accepted dependency direction.

A pull request must fail `./gradlew check` when production code in the Event domain depends on the Event application implementation or public API, or when Event application implementation code violates the currently accepted application-to-domain/API dependency direction.

The verification must operate on the existing Event package structure and must not require an application runtime.

This phase automates existing architecture rules. It does not add product behavior, new bounded contexts, runtime infrastructure, or new architectural layers.

## Technology decision

### Problem

Gradle currently enforces the physical `event-api` / `event-impl` project boundary, but the accepted Hexagonal Architecture dependency direction inside `event-impl` is documented rather than executable.

CI now executes the root validation gate automatically, but that gate cannot yet detect package-level dependency-direction violations inside the implementation project.

### Requirement

The existing Event domain/application dependency rules must be expressed as automated tests that run under the normal JUnit/Gradle validation path and therefore under the required CI check.

### Alternatives considered

- **ArchUnit** — verifies Java package/class dependencies directly under JUnit without requiring an application runtime and is already an accepted technology direction.
- **Spring Modulith verification** — useful for Spring application-module verification, but the repository has no accepted Spring runtime or application bootstrap and introducing one only for verification would expand scope unnecessarily.
- **Gradle project boundaries only** — continue to protect `api` versus `impl`, but cannot express dependency direction between packages inside `event-impl`.
- **Manual architecture review only** — can identify violations during review but does not make the rule independently executable or part of the required CI gate.

### Decision

Use ArchUnit for the minimum package-level architecture verification required by this phase.

ArchUnit is introduced only as a test dependency for architecture verification of the current Event implementation. This decision does not authorize Spring Boot, Spring Modulith, additional modules, runtime wiring, adapters, persistence, HTTP, or broader static-analysis tooling.

## In scope

- Add ArchUnit as a test-scoped dependency using the repository version catalog.
- Add architecture tests to the existing `event-impl` test suite.
- Verify that Event domain production classes do not depend on Event application implementation classes.
- Verify that Event domain production classes do not depend on the public Event API.
- Verify the current application implementation dependency direction: application implementation may depend on Event domain, Event public API, and Java platform types, but not on a newly invented infrastructure or adapter layer.
- Import and evaluate production classes only; architecture-test fixtures and ordinary test classes must not redefine the production architecture.
- Run the architecture tests through the existing JUnit test task.
- Keep root `./gradlew check` as the single authoritative validation gate.
- Make an intentional dependency-direction violation fail the architecture test and root validation gate during implementation validation.
- Keep the existing GitHub Actions workflow and required `validate` status check unchanged unless an implementation detail proves a documentation correction necessary.
- Update `docs/project-status.md` after the architecture verification is accepted.

## Acceptance criteria

The phase is complete when:

1. ArchUnit is pinned through the version catalog and available only in the relevant test scope.
2. `event-impl` contains automated architecture tests for the accepted Event domain/application dependency direction.
3. Event domain production classes are verified not to depend on Event application implementation classes.
4. Event domain production classes are verified not to depend on the Event public API.
5. The accepted current application implementation dependency direction is executable without introducing new runtime or adapter packages.
6. The architecture tests execute as part of the existing `event-impl` JUnit test task.
7. Root `./gradlew --no-daemon check` executes the architecture verification.
8. A deliberate dependency-direction violation is demonstrated to make the architecture verification and root validation gate fail, and the accepted branch contains no such violation.
9. The existing required `validate` CI check remains the merge gate and succeeds for the compliant implementation.
10. No Spring runtime, Spring Modulith, persistence, HTTP, deployment, external integration, new business capability, or additional architectural layer is introduced.
11. `docs/project-status.md` reflects completion after implementation is accepted.

## Explicitly out of scope

The following remain intentionally excluded from the current phase:

- Spring Boot application bootstrap.
- Spring Modulith configuration or verification.
- PostgreSQL schemas and Flyway migrations.
- jOOQ configuration.
- OpenAPI contracts or generation.
- HTTP controllers or other external interfaces.
- Durable persistence adapters.
- Creation of adapter, infrastructure, interface, composition, integration, core, or application-runtime modules merely to exercise architecture rules.
- Event publication or messaging infrastructure.
- Registration, ticketing, booking, membership, speaker/program, content, payment, accounting, notification, or other business capabilities.
- Frontend implementation.
- Deployment automation.
- Release automation or automatic version/tag creation.
- Artifact or package publication.
- Docker image builds or registry publication.
- Multi-platform or multi-JDK CI matrices.
- Code coverage services or quality dashboards.
- Broad static-analysis or security-scanning suites beyond the scoped ArchUnit rules.
- External CI services.
- Dependency-update automation.
- External provider integrations.
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

Examples currently include content management, registration, ticketing, booking, membership, surveys, payment integrations, and accounting integrations.

Their eventual bounded-context boundaries must be determined from real use cases rather than assumed in advance.
