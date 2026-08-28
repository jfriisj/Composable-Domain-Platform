# Technology Stack

## Authority

This document records accepted technology directions and explicitly provisional candidates for the platform. A technology appearing here does not imply that it is implemented, in current scope, or authorized for introduction by a pull request.

Exact versions are selected and pinned when the corresponding implementation enters scope.

Status meanings:

- **Accepted direction** — selected architectural direction, but implementation still requires relevant scope.
- **Candidate** — plausible choice that must be confirmed when a concrete use case enters scope.
- **Conditional** — introduced only if an accepted runtime requirement demonstrates the need.

| Area | Technology | Status | Intended responsibility |
| --- | --- | --- | --- |
| Backend language | Java 21+ | Accepted direction | Domain, application, ports, and backend adapters. |
| Build | Gradle with Kotlin DSL | Accepted direction | Multi-project build, dependency boundaries, testing, and automation. |
| Build conventions | Gradle convention plugins / `build-logic` | Accepted direction | Centralize build and architecture rules without copy/paste configuration. |
| Module dependency boundary | Gradle `java-library` | Accepted direction | Separate public `api` dependencies from internal `implementation` dependencies. |
| Continuous integration | GitHub Actions | Accepted direction | Execute repository validation gates and expose required pull request status checks. |
| Application runtime | Spring Boot | Accepted direction | Bootstrap, dependency injection, HTTP/runtime configuration, and technical adapters. |
| Security implementation | Spring Security | Accepted direction | Private Security-module implementation/adapter technology for the admitted participant-private proof; stateless HTTP Basic remains the bounded Goal #57 mechanism. The application runtime constructs/configures/wires Security but does not own Authentication/Authorization behavior. |
| Modular verification | Spring Modulith | Accepted direction | Verify application-module boundaries and cycles. |
| Architecture verification | ArchUnit | Accepted direction | Enforce Hexagonal Architecture and forbidden dependency rules. |
| External HTTP contract | OpenAPI | Accepted direction | Authoritative HTTP contract for external clients and interfaces. |
| Contract generation | OpenAPI Generator | Accepted direction | Generate transport interfaces/models and typed clients from OpenAPI. |
| Contract composition/validation | Swagger Parser | Accepted direction | Build-time parsing, reference validation, and deterministic static application OpenAPI aggregation selected by decision #146; not a runtime mechanism. |
| Mapping | MapStruct | Accepted direction | Compile-time mechanical mapping between transport, application, persistence, and provider types. |
| Validation | Jakarta Validation | Accepted direction | Validate transport-level structural constraints at system boundaries. |
| Database | PostgreSQL | Accepted direction | Relational persistence with ownership aligned to bounded contexts. |
| Database migration | Flyway | Accepted direction | Version and reproduce owned database schemas. |
| SQL access | jOOQ | Accepted direction | Explicit, type-safe SQL inside persistence adapters. |
| Unit/integration testing | JUnit 5 | Accepted direction | Test domain, application, adapters, and architecture. |
| Real infrastructure testing | Testcontainers | Accepted direction | Run integration tests against real PostgreSQL and other required services. |
| External HTTP simulation | WireMock | Accepted direction | Test provider adapters, failures, callbacks, and webhooks. |
| Architecture diagrams | Structurizr DSL + C4 | Accepted direction | Version-control the authoritative architecture model and derived stakeholder views. |
| Frontend language | TypeScript | Candidate | Typed frontend clients and UI implementation when frontend work enters scope. |
| Frontend framework | Next.js | Candidate | Possible baseline for administrative and public web interfaces when required. |
| Developer environment | Docker Engine + Docker Compose | Accepted direction | Reproducible repository developer tooling on the admitted Linux host boundary, including host-Docker Testcontainers access and optional manual-development PostgreSQL 18.4; not application/runtime/deployment packaging. |
| Browser developer editor | OpenVSCode Server | Accepted direction | Loopback-only browser presentation of an assigned WSL Git worktree inside the repository-controlled developer environment; not application runtime and not a root-validation dependency. |
| Containerization | Docker | Candidate | Reproducible application and infrastructure runtime packaging when deployment enters scope. |
| Observability | OpenTelemetry | Conditional | Standardize traces, metrics, and logs when an accepted runtime requirement requires observability. |

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

They may be adopted later only through the technology admission rule in `governance.md`.

## General rule

Prefer compile-time and build-time enforcement over runtime convention where practical. Tools may remove mechanical boilerplate or verify boundaries, but they must not hide domain decisions or replace explicit business logic.

This document records technology direction; `docs/scope.md` remains authoritative for whether implementation of a technology is currently permitted.
