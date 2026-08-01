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
- Architecture Verification Foundation scope accepted into `development` through PR #11.
- Minimum ArchUnit architecture verification accepted into `development` through PR #12.
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
- Controlled negative CI verification through draft PR #14 confirmed that a failing root `./gradlew --no-daemon check` produces a failing `validate` GitHub status; the validation PR was closed without merge.
- ArchUnit verifies the accepted Event domain/application dependency direction through the existing `event-impl` JUnit test task.
- Event domain production classes are prevented from depending on Event application implementation classes or the public Event API.
- Event application implementation dependencies are constrained to the current application, domain, public API, and Java platform packages.
- A deliberate architecture violation has been demonstrated to fail both the architecture test and the root validation gate, while the compliant implementation passes the required CI check.

## In progress

- Define the next project phase through an explicit scope decision.

## Known gaps

- No application runtime exists yet.
- No durable persistence exists yet.
- No external HTTP contract exists yet.
- No release has been produced from `production`.

## Next priority

Define the next project phase through a dedicated scope pull request.

No further implementation is authorized until that scope change is accepted.
