# Project Scope

## Authority

This document is the authoritative source for the current accepted project scope of Composable Domain Platform.

## Purpose

Composable Domain Platform provides a reliable foundation for composing independently bounded business capabilities. It is not tied to one business domain.

Event management is the first reference capability, but it is not the platform core and does not define the platform's general model.

## Accepted implementation baseline

**Event runtime and HTTP interface — completed**

This document retains the most recently accepted implementation scope as the baseline until a later scope decision is accepted into `development`. Current project activity and whether an implementation phase is active are owned by [`project-status.md`](project-status.md).

No later implementation phase is currently accepted. Until a later scope decision is accepted, capabilities and technologies excluded below remain outside implementation authority.

The repository, architecture, executable Gradle build, project workflow, continuous-integration foundation, executable architecture verification, Event reference module, and Event-owned durable PostgreSQL persistence have been accepted.

The completed phase established the first externally callable vertical slice through the existing Event capability without adding new Event lifecycle behavior or another bounded context.

## Concrete requirement

A platform operator must be able to start one platform application process, define an Event through a versioned HTTP contract, and retrieve the same Event later through HTTP from the existing durable PostgreSQL state.

The HTTP boundary must expose only the already accepted Event define and retrieve behavior:

- `POST /api/v1/events` defines an Event.
- `GET /api/v1/events/{eventId}` retrieves an Event by identity.
- Successful definition returns HTTP `201`.
- Successful retrieval returns HTTP `200`.
- Duplicate Event identity returns HTTP `409` without changing the persisted Event.
- Unknown Event identity returns HTTP `404`.
- Structurally or domain-invalid client input returns HTTP `400`.
- Unexpected internal failures return HTTP `500` through a contract-defined server-error representation without exposing stack traces, persistence records, SQL, jOOQ exceptions, or implementation types.

The HTTP representation must preserve the currently accepted Event fields: `eventId`, `name`, `slug`, `startsAt`, `endsAt`, and `timezone`.

The OpenAPI document is the authoritative external HTTP contract. Transport models are adapter-layer types and must not become Event domain or application models.

Domain-invalid Event definitions must be represented by an explicit, transport-independent Event public application failure/result. The HTTP adapter must not infer domain-invalid input by catching generic implementation exceptions or by duplicating Event business validation.

Every HTTP response must carry an `X-Correlation-Id` header. If a request supplies a correlation identifier, the boundary preserves it; otherwise the boundary creates one. The correlation identifier is opaque and business-neutral and must be propagated explicitly into the Event application boundary through the minimum shared execution-context primitive required by the already accepted traceability architecture.

The accepted Event runtime and HTTP phase did not add Event update/delete lifecycle behavior, authentication/authorization, another bounded context, messaging, frontend work, deployment, or external-provider integration.

## Runtime and HTTP admission

### Concrete use case

Start the platform application and define or retrieve the existing durable Event capability through a stable external HTTP contract.

### Why the current baseline is insufficient

The Event bounded context can define and retrieve durable Event state, but only through in-process Java application contracts. The repository has no application composition root, executable server runtime, HTTP interface, external contract, runtime database configuration, or external-entry correlation propagation.

Therefore the first reference capability cannot yet be exercised as a running platform boundary.

### HTTP interface owns

- The versioned external Event HTTP contract.
- HTTP request/response transport types.
- Mapping between HTTP transport types and Event public application contracts.
- HTTP status and transport-error mapping.
- Establishing or accepting the HTTP correlation identifier and returning it to the caller.
- HTTP-specific structural validation.

### Application runtime owns

- The executable Spring Boot composition root.
- Wiring the HTTP interface to the existing Event application implementation.
- Constructing the Event PostgreSQL persistence adapter with runtime configuration.
- Applying the existing Event-owned Flyway migrations before the application accepts requests.
- Minimal externalized database configuration required to start the application.
- Runtime propagation of the correlation context across the HTTP-to-Event boundary.

### Platform core owns

- Only the smallest business-neutral execution-context type required to carry the Correlation ID explicitly across in-process module boundaries.

The accepted Event runtime and HTTP phase did not authorize a general utilities library, logging framework abstraction, security context, messaging envelope, distributed tracing API, or other speculative core mechanism.

### Event continues to own

- Event business rules and state.
- `DefineEvent`, `FindEvent`, and Event application result semantics.
- Event persistence port, PostgreSQL schema, Flyway migration history, and jOOQ persistence adapter.

The HTTP interface and application runtime must not duplicate or reinterpret Event business rules.

## Technology decision

### Problem

The accepted Event implementation has no executable application host or external transport. A new runtime must compose the existing ports/adapters, expose HTTP without contaminating Event domain/application code with runtime types, and make the external contract executable and version controlled.

The first external entry point must also satisfy the existing correlation-propagation architecture.

### Requirement

The implementation needs:

- one executable Java application runtime;
- dependency injection and HTTP server bootstrap;
- a version-controlled authoritative HTTP contract;
- generated or otherwise build-verified transport interfaces/models derived from that contract;
- explicit transport-to-application mapping;
- minimal externalized PostgreSQL runtime configuration;
- startup execution of the existing Event Flyway migrations;
- real end-to-end HTTP validation against PostgreSQL;
- explicit Correlation ID establishment and propagation.

### Alternatives considered

- **Spring Boot + Spring Web + OpenAPI + OpenAPI Generator** — follows the existing accepted technology directions, provides a focused composition/runtime mechanism, and keeps generated transport types at the interface boundary.
- **JDK HTTP server with manually maintained JSON and contract mapping** — can expose HTTP with fewer dependencies but creates bespoke server/bootstrap and contract-drift mechanisms that the accepted technology direction already intends to avoid.
- **A different lightweight Java HTTP framework** — technically viable, but introduces an additional runtime direction without a demonstrated advantage over the already accepted Spring Boot direction.
- **Place Spring HTTP controllers inside `event-impl`** — reduces project count but couples the Event bounded context to a transport/runtime framework and weakens the existing module boundary.
- **Expose only the in-process Event Java API** — preserves the current architecture but does not satisfy the external runtime use case.

### Decision

Use Spring Boot as the application runtime and Spring Web for the HTTP adapter.

Create one HTTP interface Gradle project under `interfaces/http` and one executable composition-root Gradle project under `apps/platform`.

The HTTP interface depends on the Event public API and the minimum shared execution-context API, not on `event-impl` or Event persistence.

The application composition root may depend on the private Event implementation only for explicit wiring. It must not contain Event business rules.

Store the authoritative versioned Event OpenAPI contract under `contracts/http/`. Use OpenAPI Generator during the build for the server-side transport interface/model surface required by the HTTP adapter. Generated sources belong to the build output and are not an independently edited source of truth.

Use Jakarta Validation only at the HTTP transport boundary when required to enforce structural constraints expressed by the OpenAPI contract.

Keep manual mapping for the small current transport surface; MapStruct is not authorized by this phase because the current mapping requirement does not justify it.

Use the existing PostgreSQL, Flyway, jOOQ, and Testcontainers decisions. Runtime wiring may add the PostgreSQL connectivity and Flyway dependencies required to start against the existing Event-owned database schema.

Spring Boot, Spring Web, OpenAPI Generator, and any newly introduced runtime dependencies must be pinned through the repository's established dependency/version mechanisms during implementation.

Spring Data, Hibernate/JPA, Spring Modulith, Spring Security, and an observability stack are not authorized by this phase.

## Accepted scope for the completed phase

- Create the minimum business-neutral `core` Gradle project required for an explicit execution context containing the Correlation ID.
- Allow `event-api` to depend on that core execution-context contract and extend the existing Event public use-case signatures only as required to carry the execution context explicitly.
- Preserve existing Event business semantics while adding execution-context propagation.
- Add the smallest explicit Event public application failure/result required to represent an invalid Event definition without exposing domain implementation exception types.
- Create `contracts/http/` and add the authoritative versioned OpenAPI contract for the current Event define/retrieve HTTP surface.
- Define `POST /api/v1/events` and `GET /api/v1/events/{eventId}` only.
- Define transport representations for the currently accepted Event fields only.
- Define contract-stable HTTP success and error responses for `201`, `200`, `400`, `404`, `409`, and `500`.
- Map the explicit Event invalid-definition application failure to HTTP `400` without duplicating Event business validation or treating generic implementation exceptions as client errors.
- Define `X-Correlation-Id` request/response behavior and propagate the resulting Correlation ID explicitly into Event application calls.
- Create the `interfaces/http` Gradle project as an inbound adapter.
- Keep the HTTP interface dependent on Event public contracts rather than Event implementation or persistence.
- Use Spring Web only in the HTTP/interface boundary required for this slice.
- Generate the server transport interface/model surface from the authoritative OpenAPI contract during the build.
- Keep generated OpenAPI types and Jakarta Validation annotations out of Event domain and application implementation.
- Create the `apps/platform` executable Gradle project as the Spring Boot composition root.
- Wire the existing Event define/retrieve application services and `JooqEventRepository` in the composition root.
- Make only the minimum implementation-visibility changes required for composition; do not move Event implementation or persistence types into `event-api`.
- Configure a PostgreSQL `DataSource` from minimal externalized runtime properties.
- Apply the existing Event-owned Flyway migrations before the HTTP server accepts application traffic.
- Keep Event schema ownership and migration files inside the Event implementation.
- Add integration validation that starts the HTTP application against real PostgreSQL through Testcontainers.
- Prove through HTTP that an Event can be defined and then retrieved with all accepted fields preserved exactly.
- Prove through HTTP that duplicate identity returns `409` and leaves the existing Event unchanged.
- Prove through HTTP that an unknown identity returns `404`.
- Prove through HTTP that invalid client input returns `400` without infrastructure leakage.
- Prove Correlation ID preservation when supplied and generation when absent.
- Extend executable architecture verification for the new core/interface/runtime dependency boundaries.
- Keep root `./gradlew --no-daemon check` as the authoritative validation gate, including the end-to-end HTTP/PostgreSQL validation.
- Keep the existing GitHub Actions workflow and required `validate` status as the merge gate.
- Update README, module/runtime/interface documentation, architecture documentation/model, and project status when the implementation is accepted.
- Record the runtime/HTTP architecture rationale in ADR-0006.

## Acceptance criteria for the completed phase

The phase is complete when:

1. A documented repository command starts one executable platform application using Java 21 and Spring Boot.
2. The authoritative OpenAPI contract defines only the accepted Event define/retrieve HTTP surface for this phase.
3. `POST /api/v1/events` maps transport input to the existing Event definition use case and returns `201` with the accepted Event representation on success.
4. `GET /api/v1/events/{eventId}` maps to the existing Event retrieval use case and returns `200` with the accepted Event representation when found.
5. The HTTP round trip preserves `eventId`, `name`, `slug`, `startsAt`, `endsAt`, and `timezone` exactly according to the contract.
6. Duplicate Event identity returns `409`, leaves the previously persisted Event unchanged, and exposes no persistence-specific detail.
7. Unknown Event identity returns `404` through a contract-defined transport result.
8. Structurally invalid transport input or an explicit Event invalid-definition application failure returns `400` through a contract-defined transport result without duplicating Event business rules in the HTTP adapter.
9. Unexpected internal failures return `500` through a contract-defined transport result and do not expose stack traces, SQL, persistence records, jOOQ exceptions, or implementation types.
10. Every HTTP response carries `X-Correlation-Id`; a supplied value is preserved and an absent value is generated.
11. The resulting Correlation ID is carried explicitly through the shared execution context into the Event application boundary.
12. The HTTP interface depends on Event public contracts and does not depend on `event-impl`, jOOQ, Flyway, PostgreSQL driver APIs, or Event persistence records.
13. Event domain and application implementation remain independent of Spring Boot, Spring Web, generated OpenAPI transport types, Jakarta Validation, and HTTP concepts.
14. The application composition root contains wiring/configuration only and does not implement Event business rules.
15. Application startup constructs the Event persistence adapter from externalized PostgreSQL configuration and applies the existing Event Flyway migrations before serving Event requests.
16. End-to-end tests exercise the running HTTP boundary against real PostgreSQL through Testcontainers.
17. Existing Event unit, persistence, and architecture tests continue to pass.
18. Architecture verification enforces the accepted dependency direction for core, Event, HTTP interface, and application runtime.
19. Root `./gradlew --no-daemon check` succeeds with the required end-to-end validation.
20. The required GitHub `validate` check succeeds for the compliant implementation.
21. No Event update/delete/status/visibility/publication behavior, authentication/authorization, messaging, frontend, additional bounded context, deployment, or external-provider integration is introduced.
22. Current-state documentation and the authoritative architecture model reflect the accepted runtime and HTTP implementation after completion.

## Explicitly out of scope

The following were intentionally excluded from the accepted Event runtime and HTTP phase and remain outside accepted implementation scope until an explicit later scope decision:

- Event update, delete, publication, status, visibility, registration-opening, or lifecycle behavior beyond define and retrieve.
- Additional Event business fields or business invariants not required by the existing define/retrieve contract.
- Additional HTTP resources or endpoints beyond the two accepted Event endpoints.
- GraphQL, gRPC, WebSocket, or other external protocols.
- Authentication, authorization, user/role management, Spring Security, OAuth2, OIDC, or identity-provider integration.
- Spring Data, Hibernate, or JPA.
- Spring Modulith configuration or verification.
- MapStruct.
- Distributed tracing, OpenTelemetry, metrics backends, dashboards, or production observability infrastructure.
- A general-purpose shared `common`, `utils`, framework abstraction, or logging abstraction.
- Causation-ID behavior for asynchronous work; no asynchronous behavior enters this phase.
- Event publication or messaging infrastructure.
- A separate Event persistence Gradle project or shared repository framework.
- Shared business database schemas, cross-context joins, or direct cross-context persistence access.
- Production-grade secrets management.
- Production connection-pool tuning or database-operations infrastructure.
- Backup, replication, failover, or high-availability database concerns.
- TLS termination, reverse proxy, ingress, API gateway, rate limiting, or network-policy configuration.
- Registration, ticketing, booking, membership, speaker/program, content, payment, accounting, notification, or other business capabilities.
- Frontend implementation.
- Docker image builds or registry publication.
- Deployment automation or hosting-provider configuration.
- Kubernetes or other orchestration.
- Release automation or automatic version/tag creation.
- Artifact/package publication.
- Multi-platform or multi-JDK CI matrices.
- Code coverage services, quality dashboards, broad static-analysis suites, or external CI services.
- Dependency-update automation.
- External provider integrations.
- Redis, Kafka, RabbitMQ, or other infrastructure without a demonstrated requirement.
- Multi-model development workflow automation.

These items may enter a later phase only through an explicit scope decision.

## Business capability admission rule

A new business capability may enter active scope only when all of the following can be answered:

1. What concrete use case requires it?
2. Why can the requirement not be satisfied within the currently accepted scope?
3. What does the proposed bounded context own?
4. What does it explicitly not own?

## Technology admission rule

A new technology or infrastructure component may enter active scope only when:

1. A concrete accepted requirement exists.
2. The current baseline cannot satisfy that requirement adequately.
3. Reasonable alternatives have been considered.
4. The operational and architectural consequences are understood.

Technology must solve an accepted requirement; the project must not invent requirements to justify a technology.

## Scope change rule

Changes to this document are project decisions and must be made through a topic branch and pull request.

A pull request that introduces functionality outside the accepted scope must either remove the out-of-scope change or explicitly update this document and justify the scope change.

Hidden scope expansion inside implementation pull requests is not accepted.

## Deferred ideas

Potential future capabilities may be recorded as deferred ideas, but a deferred idea is not planned scope and must not create implementation, module, infrastructure, or API commitments.

Examples currently include content management, registration, ticketing, booking, membership, surveys, payment integrations, and accounting integrations.

Their eventual bounded-context boundaries must be determined from real use cases rather than assumed in advance.
