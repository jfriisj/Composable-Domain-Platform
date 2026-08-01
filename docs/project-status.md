# Project Status

## Authority

This document is the authoritative concise statement of where the project currently stands. It describes status, not scope; accepted scope is defined in [`scope.md`](scope.md).

## Current phase

**Architecture verification foundation**

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
- Authoritative scope, status, governance, workflow, architecture, module-model, and technology-direction documents established.
- Structurizr DSL established as the authoritative architecture model.
- ADR process established.
- Initial architecture decisions accepted for modular-monolith bounded contexts, Gradle multi-project boundaries, architecture-as-code, and correlation/causation traceability.
- Gradle Wrapper, Kotlin DSL, Java 21 toolchain convention, Version Catalog foundation, `build-logic`, and root `./gradlew check` established.
- Event reference module established with separate public API and private implementation Gradle projects.
- Event ownership, application contract, domain invariants, and reference-module tests established.
- The authoritative architecture model reflects the Event API/implementation boundary.
- GitHub Actions runs `./gradlew --no-daemon check` with JDK 21 for pull requests targeting `development` and `production`.
- The `validate` GitHub Actions check is required by the active rulesets for both permanent branches.
- The CI trigger and required check have been verified successfully for pull requests targeting both `development` and `production`.

## In progress

- Define executable architecture verification for the current Event domain/application dependency direction.
- Integrate the architecture verification into the existing root validation gate without introducing runtime infrastructure.

## Known gaps

- Event package-level architecture rules are documented but not yet automatically enforced.
- No application runtime exists yet.
- No durable persistence exists yet.
- No external HTTP contract exists yet.
- No release has been produced from `production`.

## Next priority

Implement the minimum ArchUnit verification authorized by `docs/scope.md`, prove that a deliberate dependency-direction violation fails the root validation gate, and keep the compliant implementation under the required CI check.

Do not introduce Spring Boot, Spring Modulith, persistence, HTTP/OpenAPI, external integrations, additional architectural layers, or new business capabilities in this phase.
