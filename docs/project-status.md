# Project Status

## Authority

This document is the authoritative concise statement of where the project currently stands. It describes status, not scope; accepted scope is defined in [`scope.md`](scope.md).

## Current phase

**Build foundation**

## Completed

- Public GitHub repository created.
- `development` established as the default integration branch.
- `production` established as the stable/release branch.
- Repository and architecture foundation accepted into `development` through PR #1.
- Authoritative scope, status, governance, architecture, module-model, and technology-direction documents established.
- Structurizr DSL established as the authoritative architecture model.
- ADR process established.
- Initial architecture decisions accepted for modular-monolith bounded contexts, Gradle multi-project boundaries, architecture-as-code, and correlation/causation traceability.

## In progress

- Complete review and acceptance of the Build Foundation implementation.

## Known gaps

- No automated architecture checks exist yet.
- No CI automation exists yet.
- No business module has been implemented.
- No release has been produced from `production`.

## Next priority

After the Build Foundation is accepted into `development`, define the next project phase through an explicit scope decision.

Do not begin business-module, Spring runtime, persistence, external contract, deployment, or other currently excluded implementation until the corresponding scope change has been accepted.
