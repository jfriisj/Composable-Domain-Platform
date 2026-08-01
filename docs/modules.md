# Module Model

## Purpose

This document defines the allowed architectural module categories and the ownership rules they must follow.

It does not define future business capabilities in advance.

## 1. Core

**Responsibility:** platform mechanisms required for modules to participate in the runtime.

Core must remain business-domain neutral and must not become a shared dumping ground.

Cross-boundary execution metadata such as Correlation ID and Causation ID may be represented by small core primitives because their semantics apply uniformly across module boundaries. Business modules must not place business meaning in those identifiers.

## 2. Domain module

**Responsibility:** one bounded business capability with its own language, rules, lifecycle, ownership, and persistence boundary.

The standard Gradle shape for a domain module is:

```text
modules/<name>/
├── api/
└── impl/
```

The public API may expose only concepts deliberately intended for collaboration, such as identifiers, commands, queries, views, and published events.

The implementation owns domain, application services, outbound ports, persistence, and internal adapters.

Other modules may depend on the public API but not on the implementation.

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

Examples may eventually include public HTTP APIs, administrative APIs, or other delivery mechanisms.

Interface modules translate transport contracts into application contracts and must not contain business rules. They are responsible for establishing or accepting correlation context at external entry points and propagating it into the platform execution context.

## Contracts are not bounded contexts

OpenAPI documents, JSON Schemas, event schemas, and similar artifacts are contracts. They may be stored under `contracts/` later but are not business modules by themselves.

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
