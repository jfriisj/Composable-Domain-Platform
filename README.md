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
PLATFORM_SECURITY_PARTICIPANTS_0_PRINCIPAL='opaque-participant-a' \
PLATFORM_SECURITY_PARTICIPANTS_0_PASSWORDVERIFIER='{bcrypt}<externally-supplied-verifier-a>' \
PLATFORM_SECURITY_PARTICIPANTS_1_PRINCIPAL='opaque-participant-b' \
PLATFORM_SECURITY_PARTICIPANTS_1_PASSWORDVERIFIER='{bcrypt}<externally-supplied-verifier-b>' \
SERVER_PORT='8080' \
java -jar platform.jar
~~~

The runtime host must provide a compatible Java runtime, reachable PostgreSQL, the three database settings above, externally supplied participant credential entries, network reachability, and an available HTTP port. Participant entries contain only an opaque stable platform principal and a supported encoded password verifier; missing or structurally invalid participant authentication configuration fails servlet startup closed. Production credential values are not committed to the repository. The runtime host does not require the repository, an IDE, or Gradle at runtime.

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
- `DELETE /api/v1/event-registrations/{registrationId}`

The authoritative business wire contract is [`platform/contracts/http/v1/event.yaml`](platform/contracts/http/v1/event.yaml). Generated OpenAPI Java sources are derived build output and are not edited independently.

Event definition/retrieval and readiness remain public. Event-registration create/retrieve/cancel require HTTP Basic authentication. The authenticated opaque stable platform principal is adapted directly to the participant actor reference; participant ownership authorization remains in Event-Registration composition. HTTP Basic requires secure transport across untrusted networks; production TLS termination remains an external deployment concern.

## Reproducible developer environment

Goal #116 provides a repository-controlled developer environment for the
initially supported host boundary: Linux with Git, Docker Engine, the Docker
Compose plugin, and permission to control the local Docker daemon. A host
project JDK, Gradle installation, and PostgreSQL development installation are
not required.

The developer image is built from the official
`eclipse-temurin:21-jdk-noble@sha256:a871f3e3caddad75608fd4531ed8bbca5cc42a27dc1da3ea3a2e554772b0ee15` input. The optional
manual-development database uses
`postgres:18.4@sha256:a02db8cac496f15b094798a38254f14d6e00741f709360e5e00bb6668ea31636`. Both are pinned by immutable
registry digest. The repository Gradle Wrapper remains the only Gradle
authority.

The developer workflow deliberately bind-mounts the checkout at the same
absolute path inside the developer container. Testcontainers therefore sees
Docker-host-valid repository paths when it creates sibling containers through
the host Docker Engine. Normal work runs with the invoking host UID/GID, while
the Docker socket group is added separately so socket access does not rely on
the primary group mapping.

**Trust boundary:** `/var/run/docker.sock` gives the developer container, and
repository code executed inside it, authority over the host Docker daemon.
This workflow is for a trusted development host and is not a security-isolation
boundary from that host. Docker-in-Docker is not used.

From a fresh checkout, enter the environment with:

~~~bash
./dev/dev.sh shell
~~~

The first invocation builds the developer image. Inside the shell, normal
repository commands use the bind-mounted checkout and the persistent,
Docker-managed Gradle cache. Generated repository files retain the invoking
host developer's UID/GID.

The authoritative repository validation can be run directly from the host
without a host JDK:

~~~bash
./dev/dev.sh check
~~~

The environment can also prove its supplied Java baseline and repository
Wrapper:

~~~bash
./dev/dev.sh java-version
./dev/dev.sh gradle-version
~~~

Automated validation continues to use Testcontainers-owned ephemeral
PostgreSQL through the host Docker Engine. The optional Compose PostgreSQL
service is not a dependency of the developer service and is not started by
`check`.

For manual `bootRun` development, start PostgreSQL explicitly:

~~~bash
./dev/dev.sh postgres-up
~~~

Then run the existing application development workflow against the Compose
service:

~~~bash
./dev/dev.sh boot-run
~~~

`boot-run` uses the existing database configuration contract with
`jdbc:postgresql://postgres:5432/platform` and one deterministic encoded
development-only participant verifier. It does not change application runtime
configuration semantics and must not be treated as production credential
configuration.

While `boot-run` is running, verify the existing readiness contract from
another host terminal:

~~~bash
test "$(curl -sS -o /dev/null -w '%{http_code}'   http://127.0.0.1:8080/internal/readiness)" = "204"   && echo "PASS: platform readiness"
~~~

Stop the environment while retaining the disposable Gradle cache and
development database data:

~~~bash
./dev/dev.sh down
~~~

Delete both Docker-managed convenience volumes when a clean reconstruction is
required:

~~~bash
./dev/dev.sh reset
~~~

The Gradle cache and manual-development PostgreSQL volume are non-authoritative
and disposable. Deleting them must not change project truth.

The initial support contract does not claim macOS, Windows, Docker Desktop,
Podman, Colima, Rancher Desktop, remote Docker, rootless Docker, or another
Docker-compatible engine. Linux `amd64` and `arm64` remain design-compatible
where the pinned official images provide native variants, but an architecture
is only claimed as validated after the complete Goal #116 gate has actually
succeeded on that architecture.

This developer tooling does not containerize the application runtime. The
accepted operational artifact remains the executable Spring Boot/JVM JAR.

## Development run

For repository-local development, the same externally configured PostgreSQL boundary can be used with Gradle `bootRun`:

~~~bash
PLATFORM_DATABASE_URL='jdbc:postgresql://localhost:5432/platform' \
PLATFORM_DATABASE_USERNAME='platform' \
PLATFORM_DATABASE_PASSWORD='platform' \
PLATFORM_SECURITY_PARTICIPANTS_0_PRINCIPAL='opaque-participant-a' \
PLATFORM_SECURITY_PARTICIPANTS_0_PASSWORDVERIFIER='{bcrypt}<externally-supplied-verifier-a>' \
./gradlew --no-daemon :platform-app:bootRun
~~~

`bootRun` is a development workflow; it is not the accepted operational runtime boundary.

Production secrets-management products, infrastructure provisioning, deployment automation, TLS termination, external identity-provider integration, credential enrollment/reset/recovery/admin flows, and production database operations remain outside the current phase.

## Validate

The authoritative repository validation gate is:

~~~bash
./gradlew --no-daemon check
~~~

It includes Event unit and persistence tests, architecture verification, platform runtime tests, and HTTP-to-Event-to-PostgreSQL end-to-end validation.
