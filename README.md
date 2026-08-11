# Composable Domain Platform

Composable Domain Platform is a modular application platform for composing independently bounded business capabilities through explicit contracts, integrations, and compositions.

The platform is not tied to a single business domain. Event management is the first reference capability, but it is not the center of the platform.

## Principles

- Domain-Driven Design with explicit bounded contexts.
- Hexagonal Architecture inside business modules.
- Hard module boundaries; no cross-module implementation or database access.
- Composition over implementation coupling.
- External HTTP contracts defined with OpenAPI.
- Architecture diagrams, scope, module ownership, and ADRs are version-controlled authoritative artifacts.
- Design for extension, implement only accepted requirements.

## Authoritative project sources

- [`docs/scope.md`](docs/scope.md) — current accepted scope and explicit exclusions.
- [`docs/project-status.md`](docs/project-status.md) — current project state and next priority.
- [`docs/governance.md`](docs/governance.md) — governance, branching, change control, and sources of truth.
- [`docs/workflow.md`](docs/workflow.md) — operational development workflow from accepted scope to merge and next scope gate.
- [`docs/architecture.md`](docs/architecture.md) — architectural principles and hard boundaries.
- [`docs/modules.md`](docs/modules.md) — allowed module types and ownership rules.
- [`docs/tech-stack.md`](docs/tech-stack.md) — accepted technology directions and candidates.
- [`docs/architecture/workspace.dsl`](docs/architecture/workspace.dsl) — authoritative architecture model.
- [`docs/adr/`](docs/adr/) — architectural decision records.

## Current state

Event is the first implemented reference bounded context. It has separate public API and private implementation Gradle projects, Event-owned durable PostgreSQL persistence through a private jOOQ adapter and Flyway migrations, and executable architecture verification.

The repository now also contains the first executable platform runtime and external interface: a Spring Boot composition root under `platform/apps/platform`, an HTTP inbound adapter under `platform/interfaces/http`, a versioned authoritative OpenAPI contract under `platform/contracts/http/v1/event.yaml`, and a minimal business-neutral execution context under `platform/core`. End-to-end tests exercise the running HTTP boundary against real PostgreSQL through Testcontainers.

## Build and run the operational artifact

The accepted operational runtime is the executable Spring Boot/JVM artifact produced by `bootJar`. Build it from an accepted repository checkout:

~~~bash
./gradlew --no-daemon :platform-app:bootJar
~~~

The executable JAR is written under `platform/apps/platform/build/libs/`. A repeatable proof can identify it, copy it outside the repository checkout, and run only that copied artifact:

~~~bash
JAR="$(find platform/apps/platform/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)"
test -n "$JAR"

RUNTIME_DIR="$(mktemp -d)"
cp "$JAR" "$RUNTIME_DIR/platform.jar"
cd "$RUNTIME_DIR"

PLATFORM_DATABASE_URL='jdbc:postgresql://localhost:5432/platform' \
PLATFORM_DATABASE_USERNAME='platform' \
PLATFORM_DATABASE_PASSWORD='platform' \
SERVER_PORT='8080' \
java -jar platform.jar
~~~

The runtime host must provide a compatible Java runtime, reachable PostgreSQL, the three database settings above, network reachability, and an available HTTP port. It does not require the repository, an IDE, or Gradle at runtime.

Machine-checkable readiness is available at:

~~~text
GET /internal/readiness
~~~

The readiness endpoint is operational and is not part of the business OpenAPI contract. It returns `204 No Content` when PostgreSQL is usable and the application has completed startup, including the Event and Registration Flyway migrations. If PostgreSQL becomes unavailable while the process remains running, it returns `503 Service Unavailable`. Both responses have no diagnostic payload.

For example, an operator can read only the readiness status code with:

~~~bash
curl -sS -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/internal/readiness
~~~

After readiness, the accepted external business surface is:

- `POST /api/v1/events`
- `GET /api/v1/events/{eventId}`
- `POST /api/v1/event-registrations`
- `GET /api/v1/event-registrations/{registrationId}`

The authoritative business wire contract is [`platform/contracts/http/v1/event.yaml`](platform/contracts/http/v1/event.yaml). Generated OpenAPI Java sources are derived build output and are not edited independently.

## Development run

For repository-local development, the same externally configured PostgreSQL boundary can be used with Gradle `bootRun`:

~~~bash
PLATFORM_DATABASE_URL='jdbc:postgresql://localhost:5432/platform' PLATFORM_DATABASE_USERNAME='platform' PLATFORM_DATABASE_PASSWORD='platform' ./gradlew --no-daemon :platform-app:bootRun
~~~

`bootRun` is a development workflow; it is not the accepted operational runtime boundary.

Production secrets management, infrastructure provisioning, deployment automation, TLS, authentication, authorization, and production database operations remain outside the current phase.

## Validate

The authoritative repository validation gate is:

~~~bash
./gradlew --no-daemon check
~~~

It includes Event unit and persistence tests, architecture verification, platform runtime tests, and HTTP-to-Event-to-PostgreSQL end-to-end validation.
