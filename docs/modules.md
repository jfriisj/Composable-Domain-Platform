# Module Model

## Purpose

This document defines the allowed architectural module categories and the ownership rules they must follow.

It does not define future business capabilities in advance.

## 1. Core

**Responsibility:** platform mechanisms required for modules to participate in the runtime.

Core must remain business-domain neutral and must not become a shared dumping ground.

The current `core` Gradle project contains only the minimum execution-context primitives required by the first external entry point: `CorrelationId` and `ExecutionContext`.

Cross-boundary execution metadata such as Correlation ID and Causation ID may be represented by small core primitives because their semantics apply uniformly across module boundaries. Business modules must not place business meaning in those identifiers.

## 2. Domain module

**Responsibility:** one bounded business capability with its own language, rules, lifecycle, ownership, and persistence boundary.

The standard Gradle shape for a domain module is:

~~~text
modules/<name>/
├── api/
└── impl/
~~~

The public API may expose only concepts deliberately intended for collaboration, such as identifiers, commands, queries, views, published events, and the minimum shared execution context required at the application boundary.

The implementation owns domain, application services, outbound ports, persistence, and internal adapters.

Business modules, compositions, integrations, and interfaces may depend on a domain module's public API but not on its implementation. The executable application composition root is the only current exception: it may depend on a private implementation when explicit scope requires that dependency solely to construct and wire runtime adapters and services.

The Event reference module is the first implemented instance of this shape.

## 3. Composition module

**Responsibility:** coordinate a workflow spanning multiple independent capabilities.

A composition may depend on the public APIs of participating domain modules. It must not depend on their implementation modules or persistence.

Composition exists to avoid forcing one bounded context to understand another bounded context's internals.

## 4. Integration module

**Responsibility:** adapt an internal outbound contract to an external system or provider.

Examples may eventually include payment, accounting, identity, messaging, or storage providers when concrete requirements exist.

An integration must not leak provider-specific models into domain code.

## 5. Interface module

**Responsibility:** expose platform capabilities through an external protocol or user-facing boundary.

Interface modules translate transport contracts into application contracts and must not contain business rules. They are responsible for establishing or accepting correlation context at external entry points and propagating it into the platform execution context.

The current `interfaces/http` Gradle project is the first implemented interface module. It:

- implements the server surface generated from `contracts/http/v1/event.yaml`;
- depends on `event-api` and `core`, not on `event-impl` or Event persistence;
- maps HTTP transport types manually to Event public application contracts;
- owns HTTP status/error mapping and structural transport validation;
- preserves a supplied `X-Correlation-Id` or creates one when absent and propagates it through `ExecutionContext`.

Generated OpenAPI types remain transport-layer build output and are not Event domain or application models.

## 6. Application runtime

**Responsibility:** provide the executable technical composition root.

The current `apps/platform` Gradle project is the first application runtime. It owns Spring Boot startup, technical dependency injection, minimal externalized PostgreSQL configuration, Event Flyway startup migration, and construction of the Event persistence and application adapters.

The application runtime may depend on private implementation types only where required for explicit technical wiring. It must not contain Event business rules, reinterpret Event failures, or become a general shared implementation module.

## Contracts are not bounded contexts

OpenAPI documents, JSON Schemas, event schemas, and similar artifacts are contracts, not business modules.

The current authoritative HTTP contract is stored at `contracts/http/v1/event.yaml`. Generated sources derived from that contract belong to build output rather than `contracts/`.

## Module admission rule

Before creating a new domain module, establish:

1. A concrete use case.
2. A clear business capability.
3. Ownership of concepts and lifecycle.
4. Explicit non-ownership.
5. At least one meaningful invariant, policy, or independently evolving lifecycle.
6. The smallest required public contract.

Do not create a bounded context merely because a technical concept can be extracted.

## Independence target

A domain module should eventually be capable of being built and tested without other feature implementations, owning its own schema/migrations, exposing only explicit contracts, and being disabled without breaking unrelated business capabilities.
