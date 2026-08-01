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
- Authoritative scope, status, governance, workflow, architecture, module-model, and technology-direction documents established.
- Structurizr DSL established as the authoritative architecture model.
- ADR process established.
- Initial architecture decisions accepted for modular-monolith bounded contexts, Gradle multi-project boundaries, architecture-as-code, and correlation/causation traceability.
- Gradle Wrapper, Kotlin DSL, Java 21 toolchain convention, Version Catalog foundation, `build-logic`, and root `./gradlew check` established.
- Event reference module established with separate public API and private implementation Gradle projects.
- Event ownership, application contract, domain invariants, and reference-module tests established.
- The authoritative architecture model reflects the Event API/implementation boundary.

## In progress

- Establish minimum GitHub Actions continuous integration for the accepted root Gradle validation gate.
- Make the resulting CI status a required merge condition for `development` and `production`.

## Known gaps

- No automated architecture checks beyond Gradle project boundaries exist yet.
- No application runtime exists yet.
- No durable persistence exists yet.
- No external HTTP contract exists yet.
- CI automation is authorized by the current phase but not yet implemented.
- No release has been produced from `production`.

## Next priority

Implement the minimum CI workflow authorized by `docs/scope.md`, validate it on a pull request, and require its successful status in the permanent-branch rulesets.

Do not introduce deployment, release automation, application runtime, persistence, HTTP/OpenAPI, external integrations, or additional business capabilities in this phase.
