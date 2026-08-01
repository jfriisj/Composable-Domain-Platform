# Project Status

## Authority

This document is the authoritative concise statement of where the project currently stands. It describes status, not scope; accepted scope is defined in [`scope.md`](scope.md).

## Current phase

**Continuous integration foundation**

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

- Define the next project phase through an explicit scope decision.

## Known gaps

- No automated architecture checks beyond Gradle project boundaries exist yet.
- No application runtime exists yet.
- No durable persistence exists yet.
- No external HTTP contract exists yet.
- No release has been produced from `production`.

## Next priority

Define the next project phase through a dedicated scope pull request.

No further implementation is authorized until that scope change is accepted.
