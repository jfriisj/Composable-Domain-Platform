# Project Status

## Authority

This document is the authoritative concise statement of where the project currently stands. It describes status, not scope; accepted scope is defined in [`scope.md`](scope.md).

## Current phase

**Repository and architecture foundation**

## Completed

- Public GitHub repository created.
- `development` established as the default integration branch.
- `production` established as the stable/release branch.
- `docs/repository-foundation` established as the first topic branch.
- Initial repository bootstrap commit created.
- Initial architectural direction agreed: bounded contexts, Hexagonal Architecture, hard module boundaries, and composition over implementation coupling.

## In progress

- Establish authoritative project documentation.
- Establish architecture-as-code foundation.
- Establish scope and governance rules.

## Known gaps

- Foundation documentation has not yet been accepted into `development`.
- No Gradle build exists yet.
- No automated architecture or CI checks exist yet.
- No business module has been implemented.
- No release has been produced from `production`.

## Next priority

Complete and review the repository foundation on `docs/repository-foundation`, then merge it into `development` through a pull request.

Only after that acceptance should the next scoped branch be selected.
