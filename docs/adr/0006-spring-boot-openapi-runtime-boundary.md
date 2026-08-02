# ADR-0006: Spring Boot composition root and OpenAPI HTTP boundary

- Status: Accepted
- Date: 2026-08-02

## Context

The Event bounded context now supports durable definition and retrieval through its public Java application API and Event-owned PostgreSQL persistence.

The next accepted use case requires a platform operator to start one application process and exercise those existing Event capabilities through a versioned external HTTP contract.

The architecture already separates external interfaces from bounded-context implementations and requires external entry points to establish or accept correlation context and propagate it across module boundaries. The technology directions already identify Spring Boot as the application runtime, OpenAPI as the authoritative external HTTP contract, OpenAPI Generator for contract-derived transport surfaces, and Jakarta Validation for transport-level structural constraints.

Those technology directions do not authorize implementation until a concrete requirement enters scope. This runtime/HTTP use case provides that requirement.

## Decision

Use Spring Boot as the executable application runtime and Spring Web for the first HTTP interface.

Create:

- a minimum business-neutral `core` Gradle project containing only the execution-context/Correlation ID contract required by this external entry point;
- an HTTP interface Gradle project under `interfaces/http`;
- an executable Spring Boot composition-root Gradle project under `apps/platform`;
- the authoritative versioned Event OpenAPI contract under `contracts/http/`.

The HTTP interface depends on Event public application contracts and the shared execution-context contract. It must not depend on `event-impl`, Event persistence, jOOQ, Flyway, or PostgreSQL implementation types.

The application composition root may depend on `event-impl` only to construct and wire the existing Event services and persistence adapter. It contains technical composition and runtime configuration, not Event business rules.

Use OpenAPI Generator during the build to derive the server-side transport interface/model surface from the authoritative contract. Generated sources are build output and are not independently edited source-of-truth files.

Use Jakarta Validation only at the HTTP boundary for structural constraints represented by the OpenAPI contract. Keep the current small transport/application mapping explicit and manual; MapStruct is not introduced by this decision.

Where invalid Event definition would otherwise escape only as a domain or implementation exception, expose the smallest transport-independent Event public application failure/result required to represent that outcome. The HTTP adapter maps only that explicit application failure to `400`; it does not duplicate Event business validation or treat generic implementation exceptions as client errors. Unexpected internal failures map to `500`.

The runtime configures PostgreSQL from externalized properties and applies the existing Event-owned Flyway migrations before accepting Event HTTP requests. Event retains ownership of its schema, migration files, persistence port, and jOOQ adapter.

Every HTTP response carries `X-Correlation-Id`. The HTTP boundary preserves a supplied correlation identifier or creates one when absent, then carries it explicitly through the shared execution context into the Event public application boundary.

Spring Boot and HTTP/OpenAPI types remain outside Event domain and application implementation.

Spring Data, Hibernate/JPA, Spring Modulith, Spring Security, authentication/authorization, messaging, deployment, and observability infrastructure are not introduced by this decision.

## Alternatives considered

- Use the JDK HTTP server and manually maintain JSON parsing, routing, and contract conformance.
- Select another lightweight Java HTTP framework instead of the already accepted Spring Boot direction.
- Place Spring HTTP controllers directly inside `event-impl`.
- Expose the Event Java API only and defer an external interface.
- Add a broader shared runtime/framework abstraction before another concrete use case requires one.

## Consequences

- The repository gains its first executable application process and external protocol boundary.
- Event remains a bounded business capability rather than becoming the application runtime.
- The external HTTP contract becomes version controlled and build connected.
- The interface module can evolve transport concerns without exposing generated HTTP types to Event domain/application code.
- The application composition root is the explicit place where private implementations are wired.
- Correlation semantics become executable at the first external entry point without introducing asynchronous causation behavior.
- Runtime validation now exercises HTTP, application contracts, Event persistence, Flyway, jOOQ, and real PostgreSQL in one vertical slice.
- Starting the application requires PostgreSQL connection configuration.
- The phase deliberately does not make the runtime production-deployment ready; security, deployment, TLS, operational observability, HA, and provider-specific infrastructure remain future scope decisions.
