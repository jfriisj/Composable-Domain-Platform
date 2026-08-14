# Module Model

## Purpose

This document defines what **module** means in Composable Domain Platform and the ownership rules every module must obey.

It also distinguishes modules from other architectural constructs such as the application runtime, compositions, interfaces, integrations, contracts, and shared foundation.

It does not define future capabilities in advance.

## Universal module invariant

Every architectural construct classified as a **module**:

- is independently owned;
- can be selected into or out of a valid platform composition;
- exposes an explicit public API;
- hides its private implementation behind that API;
- collaborates with other modules only through explicit public contracts and adapters;
- does not depend on another module's private implementation or persistence;
- is not owned or implemented by the application runtime;
- is not owned or implemented by another module;
- is not owned or implemented by a composition.

The invariant applies to every module. Domain, technical, security, workflow, or future module classifications do not receive weaker ownership rules.

A construct that should not or cannot satisfy this invariant must be classified as something other than a module. The architecture must not weaken the word "module" to preserve an existing folder or implementation shape.

## Public API and private implementation

A module has an explicit public API and a hidden private implementation.

For current Gradle-based modules the standard physical shape is:

~~~text
platform/modules/<name>/
├── api/
└── impl/
~~~

Equivalent future layouts may be accepted, but the public/private boundary must remain explicit and mechanically enforceable where practical.

The public API contains only concepts deliberately intended for collaboration, such as identifiers, commands, queries, views, published events, policies explicitly exposed as contracts, and the minimum shared execution context required at application boundaries.

The private implementation owns the module's behavior and internal adapters. Where the module owns persistence, its persistence ports, adapters, migrations, and schema remain private implementation concerns.

Other modules, compositions, interfaces, integrations, and runtimes collaborate with a module through its public API. They do not use its private implementation as a functional collaboration surface.

The application runtime may reference private implementation types only to construct and wire the module. That technical construction dependency does not transfer ownership to the runtime.

## Selectable composition

A platform application is assembled from an explicit set of modules and other architectural constructs.

A module can be selected into or out of a valid platform composition. This means:

- application assembly chooses participating modules explicitly;
- unrelated modules remain buildable and testable without an omitted module;
- an omitted module's private implementation is not a hidden compile/runtime requirement of unrelated modules;
- a composition with declared module dependencies is valid only when those required public capabilities are supplied.

Selectable does not mean that every workflow works without every dependency. It means dependencies are explicit at composition boundaries rather than hidden through shared implementation ownership.

## Domain module

**Responsibility:** one bounded business capability with its own language, rules, lifecycle, ownership, and—when durable state is required—its own persistence boundary.

A domain module obeys the universal module invariant and additionally owns the business rules and invariants that justify its bounded context.

The current conforming domain modules are:

- Event;
- Registration.

Both use separate public API and private implementation Gradle projects.

## Composition

**Responsibility:** coordinate a workflow spanning independent modules/capabilities.

A composition:

- depends only on public module APIs for participating module behavior;
- owns the cross-module workflow;
- does not own participating modules;
- does not use participating modules' private implementations or persistence.

A composition is an architectural construct and is not automatically a module.

If a composition is deliberately classified as a module, it must itself satisfy the universal module invariant, including its own explicit public API and private implementation boundary.

The current `platform/compositions/event-registration` project is an accepted cross-capability composition. It is currently one Gradle project and therefore must not be treated as conforming to a module classification until a later accepted migration provides the required public/private separation.

## Integration

**Responsibility:** adapt an internal public/outbound contract to an external system or provider.

An integration is an adapter boundary and is not automatically a module.

Examples may eventually include payment, accounting, identity, messaging, or storage providers when concrete requirements exist.

Provider-specific models must not leak through module public APIs or into unrelated domain code.

If an integration is deliberately classified as a module, the universal module invariant applies.

## Interface

**Responsibility:** expose accepted platform capabilities through an external protocol or user-facing boundary.

An interface is an inbound adapter boundary and is not automatically a module.

The current `platform/interfaces/http` Gradle project:

- implements the server surface generated from `platform/contracts/http/v1/event.yaml`;
- maps transport contracts to public module/composition contracts;
- owns HTTP status/error mapping and structural transport validation;
- establishes or preserves correlation context at the external boundary.

It must not own business-module behavior merely because it adapts that behavior to HTTP.

If an interface is deliberately classified as a module, the universal module invariant applies.

## Shared platform foundation

The current `platform/core` Gradle project is shared business-neutral platform foundation, not a business module.

It contains the minimum execution-context primitives required across current boundaries: `CorrelationId` and `ExecutionContext`.

Cross-boundary execution metadata may live in foundation only when its semantics apply uniformly across boundaries and a concrete accepted need exists.

Foundation must remain small and must not become a shared dumping ground for business or capability implementation.

If a future decision classifies a foundation construct as a module, the universal module invariant applies.

## Application runtime / composition root

`platform/apps/platform` is the executable technical composition root. It is not a module owner.

The runtime may:

- start the executable process;
- select accepted modules and compositions;
- construct private module implementations;
- provide technical dependency injection;
- supply external runtime configuration;
- wire public contracts to adapters;
- configure shared process-level infrastructure required to assemble the application.

Selection, construction, configuration, and wiring are not ownership.

The runtime must not become the permanent implementation location for a capability/module merely because a framework is configured there.

The current participant authentication/security proof is implemented in the application runtime as accepted executable state from ADR-0012/#91. Under ADR-0013 this is explicit migration debt, not the target module ownership model. Authentication and authorization belong to the Security module, which receives no exception from the universal module invariant.

## Contracts

OpenAPI documents, JSON Schemas, event schemas, and similar artifacts are contracts, not modules or bounded contexts.

The current authoritative HTTP contract is stored at `platform/contracts/http/v1/event.yaml`. Generated sources derived from that contract belong to build output.

## Module admission

Before admitting a new module, establish:

1. a concrete use case;
2. the capability or responsibility it independently owns;
3. explicit non-ownership;
4. the smallest required public API;
5. the private implementation boundary;
6. required public dependencies on other modules;
7. selectable composition semantics;
8. objective independent build/test evidence appropriate to the module;
9. adapter boundaries required for external protocols/providers/mechanisms.

A new domain module additionally requires meaningful business rules, invariants, policy, or independently evolving lifecycle sufficient to justify a bounded context.

Do not create a module merely because a technical concept can be extracted. Conversely, once a capability is accepted as a module, do not place its implementation in the runtime, another module, or a composition.

## Current conformance and migration debt

Current accepted executable state must be distinguished from the accepted target invariant.

Conforming/near-conforming current module boundaries:

- Event — separate `api` and `impl`;
- Registration — separate `api` and `impl`.

Known architecture migration debt:

- participant authentication/security behavior currently resides in `platform/apps/platform`;
- Event-Registration is currently one composition Gradle project and therefore is not a conforming module if classified as one;
- existing terminology for interfaces, integrations, compositions, and foundation must not call a construct a module unless it satisfies the universal invariant.

Corrective source/build changes require explicit accepted scope. ADR-0013 and this document establish the rule but do not authorize migration by themselves.
