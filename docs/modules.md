# Module Model

## Purpose

This document defines what `module` means project-wide. It owns universal module semantics and admission rules, not current module inventories or module-local API/persistence detail.

Implemented module-specific responsibility is documented in `platform/modules/*/module.md`; concrete APIs, build dependencies, migrations, source/tests, contracts, and architecture relationships remain authoritative in their narrower sources.

## Universal module invariant

Every construct classified as a module:

- is independently owned;
- can be selected into or out of a valid platform application composition;
- exposes an explicit public API;
- hides private implementation behind that API;
- exposes its public API as a collaboration surface for compositions and interfaces, without functional dependencies on other modules;
- does not depend on another module's private implementation or persistence;
- is not owned or implemented by an application runtime, another module, or a composition.

The rule applies equally to business, security, technical, workflow, or future module classifications. A construct that should not satisfy it must be classified explicitly as something else rather than weakening the word `module`.

Selectable composition means application assembly chooses participating modules explicitly and unrelated modules remain buildable/testable without an omitted module. It does not mean a workflow remains valid after removing a capability that it explicitly requires.

## Public and private boundary

The standard current physical module shape is:

```text
platform/modules/<name>/
├── module.md
├── api/
└── impl/
```

Equivalent future layouts may be accepted only if the public/private boundary remains explicit and mechanically enforceable where practical.

The public API contains only deliberate collaboration contracts: identifiers, commands/queries, views/results, policies/events, and minimum cross-boundary context needed by owned capabilities.

The private implementation owns domain/application implementation and private adapters. When a module owns persistence, its persistence ports/adapters/migrations/schema are private module concerns.

Functional consumers use the public API only. The application runtime may reference private implementation types for construction/wiring; that technical reference transfers no ownership.

Each implemented module has a concise local `module.md` for purpose, owns/does-not-own, public/private boundary, allowed dependencies, and authority links. It does not duplicate concrete Java signatures, schema/migrations, Gradle truth, external contracts, or architecture relationships.

## Dependency rules

Module public APIs are collaboration surfaces consumed by compositions and adapters. Cross-module workflows are composition-owned; participating modules remain unaware of each other and have no functional dependencies on other modules.

Forbidden:

- functional dependencies on other modules;
- direct module-to-module collaboration in cross-capability use cases;
- another module's private implementation/persistence as a functional dependency;
- cross-module repository/table access;
- shared internal DTOs as collaboration contracts;
- moving missing module behavior into runtime/composition/adapter code;
- provider/framework types in module public/domain boundaries unless explicitly part of the accepted public contract.

Inside a module, dependencies point inward from adapters to application/domain; outbound mechanisms implement module-owned ports.

A composition coordinates cross-module workflows and depends only on required public module APIs and `core` when needed. Inbound interfaces adapt public module or composition contracts. Runtime assembly selects and wires implementations.

Exact build dependency truth is in Gradle; architecture relationships are in `docs/architecture/workspace.dsl`.

## Construct classification

**Domain module** — independently owned bounded business capability with meaningful rules/invariants/lifecycle and, when needed, its own persistence.

**Security module** — an independently owned platform capability under the same invariant; framework/provider mechanisms remain private.

**Composition** — cross-module workflow coordinator, not automatically a module. A composition classified as a module must itself have public/private separation and satisfy the full invariant.

**Interface** — inbound protocol/user adapter, not automatically a module.

**Integration** — outbound/provider adapter, not automatically a module.

**Application runtime** — executable technical composition root. It selects, constructs, configures, migrates, wires, and starts; it owns no module behavior.

**Shared foundation** — small business-neutral cross-boundary mechanism such as accepted execution context. It is not automatically a module and must not become a shared business implementation sink.

**Contract** — OpenAPI/schema/event contract. It is not a module; contract ownership does not determine module classification.

## Module admission

Before accepting a new module, establish:

1. a concrete accepted use case;
2. independent ownership and explicit non-ownership;
3. the smallest required public API;
4. a private implementation boundary;
5. required foundation dependencies (no functional dependencies on other modules);
6. selectable application-composition semantics;
7. adapter boundaries required for external protocols/providers;
8. objective independent build/test evidence.

A new business module additionally requires meaningful business rules, invariants, policy, or independently evolving lifecycle sufficient to justify a bounded context.

Do not create a module merely because a technical concept can be extracted. Once accepted as a module, do not place its implementation in runtime, another module, or composition.

Architecture/module admission does not authorize product scope by itself; follow `docs/scope.md`, `docs/governance.md`, and `docs/workflow.md`.
