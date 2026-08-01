# Technology Stack

## Authority

This document records the approved baseline technologies for the platform. A technology appearing here does not imply that it is already implemented.

Exact versions are selected and pinned when the corresponding implementation enters scope.

| Area | Technology | Intended responsibility |
| --- | --- | --- |
| Backend language | Java 21+ | Domain, application, ports, and backend adapters. |
| Build | Gradle with Kotlin DSL | Multi-project build, dependency boundaries, testing, and automation. |
| Build conventions | Gradle convention plugins / `build-logic` | Centralize build and architecture rules without copy/paste configuration. |
| Module dependency boundary | Gradle `java-library` | Separate public `api` dependencies from internal `implementation` dependencies. |
| Application runtime | Spring Boot | Bootstrap, dependency injection, HTTP/runtime configuration, and technical adapters. |
| Modular verification | Spring Modulith | Verify application-module boundaries and cycles. |
| Architecture verification | ArchUnit | Enforce Hexagonal Architecture and forbidden dependency rules. |
| External HTTP contract | OpenAPI | Authoritative HTTP contract for external clients and interfaces. |
| Contract generation | OpenAPI Generator | Generate transport interfaces/models and typed clients from OpenAPI. |
| Mapping | MapStruct | Compile-time mechanical mapping between transport, application, persistence, and provider types. |
| Validation | Jakarta Validation | Validate transport-level structural constraints at system boundaries. |
| Database | PostgreSQL | Relational persistence with ownership aligned to bounded contexts. |
| Database migration | Flyway | Version and reproduce owned database schemas. |
| SQL access | jOOQ | Explicit, type-safe SQL inside persistence adapters. |
| Unit/integration testing | JUnit 5 | Test domain, application, adapters, and architecture. |
| Real infrastructure testing | Testcontainers | Run integration tests against real PostgreSQL and other required services. |
| External HTTP simulation | WireMock | Test provider adapters, failures, callbacks, and webhooks. |
| Architecture diagrams | Structurizr DSL + C4 | Version-control the authoritative architecture model and derived stakeholder views. |
| Frontend language | TypeScript | Typed frontend clients and UI implementation when frontend work enters scope. |
| Frontend framework | Next.js | Candidate baseline for administrative and public web interfaces when required. |
| Containerization | Docker | Reproducible application and infrastructure runtime packaging. |
| Observability | OpenTelemetry | Standardize traces, metrics, and logs when runtime implementation requires observability. |

## Deferred until a demonstrated requirement exists

The following technologies are deliberately not part of the mandatory initial runtime:

- Redis.
- Kafka.
- RabbitMQ.
- Kubernetes.
- Resilience4j.
- Keycloak as a mandatory identity provider.
- Prometheus/Grafana deployment.
- S3/MinIO deployment.

They may be adopted later through the technology admission rule in `governance.md`.

## General rule

Prefer compile-time and build-time enforcement over runtime convention where practical. Tools may remove mechanical boilerplate or verify boundaries, but they must not hide domain decisions or replace explicit business logic.
