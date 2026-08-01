# ADR-0002: Gradle multi-project build for physical module boundaries

- Status: Accepted
- Date: 2026-08-01

## Context

Package conventions alone do not provide sufficiently hard boundaries between independently bounded business capabilities.

The build should prevent accidental dependencies on another module's implementation.

## Decision

Use Gradle with Kotlin DSL as a multi-project build.

Business domain modules are intended to expose a small public `api` project and keep domain/application/adapters in a private `impl` project. Gradle `java-library` API/implementation semantics and convention plugins will be used to standardize dependency rules when build implementation enters scope.

## Alternatives considered

- Maven multi-module build.
- One Gradle project with package-only boundaries.
- Separate repositories for every capability.

## Consequences

- Illegal cross-module dependencies can fail at build time.
- Build logic requires deliberate convention management.
- The repository can remain a monorepo while preserving physical module boundaries.
- Exact Gradle structure will be implemented in a later scoped change; this ADR defines the direction, not current implementation state.
