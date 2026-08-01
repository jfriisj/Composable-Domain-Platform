# Project Scope

## Authority

This document is the authoritative source for the current accepted project scope of Composable Domain Platform.

## Purpose

Composable Domain Platform provides a reliable foundation for composing independently bounded business capabilities. It is not tied to one business domain.

Event management is the first reference capability, but it is not the platform core and does not define the platform's general model.

## Current phase

**Continuous integration foundation**

The repository, architecture, executable Gradle build, project workflow, and first Event reference module have been accepted.

The current phase makes the existing repository validation gate automatically visible and enforceable on GitHub pull requests before additional runtime, persistence, transport, integration, or business capability work is admitted.

## Concrete requirement

Every pull request targeting `development` or `production` must automatically execute the repository's root Gradle validation gate in a clean CI environment using JDK 21.

The resulting GitHub status check must be suitable for use as a required merge condition so a pull request cannot be accepted when the repository validation gate fails or has not completed successfully.

This phase automates an existing validation rule. It does not add product behavior.

## Technology decision

### Problem

The repository has an accepted local validation gate, `./gradlew check`, but pull request acceptance still depends on locally reported validation because no CI system executes the gate on GitHub.

### Requirement

The accepted root validation gate must run automatically for pull requests and expose a GitHub-native status that can be required by repository rulesets.

### Alternatives considered

- **GitHub Actions** — native to the existing GitHub repository and pull request workflow, with direct status-check integration.
- **External CI service** — capable, but introduces an additional service, integration, credential, and operational surface without a demonstrated need.
- **Local validation only** — already available, but cannot provide an independently executed required pull request status.

### Decision

Use GitHub Actions for the minimum continuous-integration workflow required by this phase.

The decision is limited to repository validation. It does not authorize deployment, release automation, artifact publication, environment management, or broader DevOps infrastructure.

## In scope

- Add the minimum GitHub Actions workflow required to execute the accepted repository validation gate.
- Run the CI workflow for pull requests targeting `development` and `production`.
- Use JDK 21 in CI.
- Use the committed Gradle Wrapper as the build entry point.
- Execute `./gradlew --no-daemon check` as the authoritative CI build/test gate.
- Keep workflow permissions at the minimum required for repository checkout and validation.
- Produce one stable GitHub status check that can be required by repository rulesets.
- Configure the `development` and `production` rulesets to require the successful CI status check before merge.
- Keep the existing pull-request, conversation-resolution, force-push, and deletion protections intact.
- Update authoritative workflow, governance, technology, or status documentation if implementation details make an accepted process statement inaccurate.
- Keep the CI workflow independent of application runtime, persistence, HTTP, deployment, and external providers.

## Acceptance criteria

The phase is complete when:

1. A pull request targeting `development` automatically starts the CI validation.
2. A pull request targeting `production` automatically starts the same CI validation.
3. CI provisions JDK 21 and invokes the committed Gradle Wrapper.
4. CI executes `./gradlew --no-daemon check`.
5. A successful root check produces a successful, stable GitHub status check.
6. A failing root check produces a failing GitHub status check.
7. Repository rulesets require that CI check before merge to both `development` and `production`.
8. Existing repository protections remain in place.
9. No deployment, release automation, publishing, product runtime, persistence, HTTP, external integration, or new business capability is introduced.
10. `docs/project-status.md` reflects completion of the CI foundation after implementation is accepted.

## Explicitly out of scope

The following remain intentionally excluded from the current phase:

- Spring Boot application bootstrap.
- Spring Modulith configuration.
- ArchUnit architecture rules.
- PostgreSQL schemas and Flyway migrations.
- jOOQ configuration.
- OpenAPI contracts or generation.
- HTTP controllers or other external interfaces.
- Durable persistence adapters.
- Event publication or messaging infrastructure.
- Registration, ticketing, booking, membership, speaker/program, content, payment, accounting, notification, or other business capabilities.
- Frontend implementation.
- Deployment automation.
- Release automation or automatic version/tag creation.
- Artifact or package publication.
- Docker image builds or registry publication.
- Multi-platform or multi-JDK CI matrices.
- Code coverage services or quality dashboards.
- Broad static-analysis or security-scanning suites.
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
