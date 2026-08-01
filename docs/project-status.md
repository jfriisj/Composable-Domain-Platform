# Project Status

## Authority

This document is the authoritative concise statement of where the project currently stands. It describes status, not scope; accepted scope is defined in [`scope.md`](scope.md).

## Current phase

**Event reference module foundation**

## Completed

- Public GitHub repository created.
- `development` established as the default integration branch.
- `production` established as the stable/release branch.
- Repository and architecture foundation accepted into `development` through PR #1.
- Build Foundation accepted into `development` through PR #3.
- Authoritative scope, status, governance, architecture, module-model, and technology-direction documents established.
- Structurizr DSL established as the authoritative architecture model.
- ADR process established.
- Initial architecture decisions accepted for modular-monolith bounded contexts, Gradle multi-project boundaries, architecture-as-code, and correlation/causation traceability.
- Gradle Wrapper, Kotlin DSL, Java 21 toolchain convention, Version Catalog foundation, `build-logic`, and root `./gradlew check` established.

## In progress

- Prepare the first concrete Event bounded-context implementation within the accepted reference-module scope.

## Known gaps

- No business module has been implemented yet.
- No automated architecture checks beyond Gradle project boundaries exist yet.
- No application runtime exists yet.
- No durable persistence exists yet.
- No external HTTP contract exists yet.
- No CI automation exists yet.
- No release has been produced from `production`.

## Next priority

Implement the smallest Event reference slice using separate `api` and `impl` Gradle projects, explicit module ownership, pure Java domain/application code, and tests.

Do not introduce Spring, persistence, HTTP/OpenAPI, external integrations, deployment, or additional business capabilities in this phase.
