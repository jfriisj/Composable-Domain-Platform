# ADR-0010: Executable JVM operational-runtime boundary

## Status
- Status: Accepted
- Date: 2026-08-03

## Context

The accepted platform can be built and exercised from the development repository, and the current Spring Boot composition root already wires the Event, Registration, Event-Registration composition, HTTP interface, PostgreSQL runtime configuration, and owned Flyway migrations.

Research issue #30 established a smaller operational question than production deployment: one operator needs to run one accepted platform version on one non-developer host, against externally supplied PostgreSQL, determine objectively when the runtime is ready, and verify durable Event/Registration state across an application-process restart.

Decision issue #45 compared three packaging/run boundaries:

- repository checkout plus Gradle `bootRun`;
- an executable JVM application artifact;
- an OCI/container image.

It also separated application-runtime reproducibility from infrastructure provisioning. Terraform, OpenTofu, and other IaC approaches are relevant only if the platform proof owns provisioning of a host, PostgreSQL, networking, or provider resources.

The architecture therefore needs a minimum build-to-runtime boundary and readiness contract without prematurely admitting container, cloud, or infrastructure technology.

## Decision

Use one executable Spring Boot/JVM application artifact as the operational packaging/run boundary.

An accepted repository version produces the artifact. A non-developer runtime host can run it without repository checkout, IDE state, or Gradle `bootRun`.

The operator supplies:

- a compatible Java runtime;
- one non-developer host or VM;
- a reachable PostgreSQL instance;
- the existing external database URL, username, and password configuration;
- network reachability between application and PostgreSQL and between caller and application;
- an available HTTP listen port.

The platform runtime continues to own application startup and execution of the existing Event and Registration Flyway migrations.

Require a machine-checkable readiness signal distinct from process existence. Readiness remains false until:

1. required runtime configuration has been accepted;
2. PostgreSQL is reachable with the supplied configuration;
3. Event Flyway migrations have completed successfully;
4. Registration Flyway migrations have completed successfully;
5. the HTTP runtime can serve the accepted external contract.

After startup, readiness reports not-ready when PostgreSQL unavailability prevents the accepted HTTP use cases from being serviced.

Readiness is a runtime/operational concern, not a business-domain API. It must not expose credentials, database details, stack traces, or implementation internals.

This ADR selects readiness semantics, not a specific readiness technology. It does not require Spring Boot Actuator, a particular endpoint path, another library, or a separate liveness contract.

Infrastructure provisioning is outside this proof. Host/VM infrastructure, PostgreSQL infrastructure, networking/firewall resources, and cloud/provider resources remain externally supplied.

Docker/OCI packaging, Docker Compose, Kubernetes, Terraform, OpenTofu, infrastructure provisioning, and artifact registry/publication infrastructure are not admitted by this decision.

## Rationale
The decision is retained for the constraints recorded in Context and the trade-offs recorded in Alternatives considered and Consequences; this migration changes document structure only.

## Alternatives considered

### Repository checkout plus Gradle `bootRun`

Rejected as the operational boundary because it leaves source checkout and build-workspace tooling coupled to the runtime host. It remains valid for development.

### OCI/container image

Deferred because it improves runtime-environment reproducibility by packaging more of the runtime environment, but introduces container packaging/runtime concerns not demonstrated by the minimum operator use case. Docker remains a candidate technology rather than an accepted requirement for this phase.

### Include infrastructure provisioning

Rejected for this proof because the operator use case can be satisfied with an externally supplied host, PostgreSQL, and networking. Selecting Terraform, OpenTofu, a provider, or another IaC approach before a concrete provisioning requirement would violate the technology-admission rule.

### No machine-checkable readiness

Rejected because process existence alone does not prove that required configuration is accepted, PostgreSQL is reachable, owned migrations are complete, or the HTTP runtime can serve the accepted contract.

## Consequences

The project gains a distinct build-to-runtime artifact boundary while reusing the accepted Java, Gradle, Spring Boot, PostgreSQL, and Flyway directions.

A later implementation must prove reproducible artifact creation, non-developer-host startup, fail-closed required configuration, migration-before-readiness behavior, accepted HTTP serviceability after readiness, readiness loss when PostgreSQL prevents serviceability, and durable state across application-process restart.

The runtime host is responsible for a compatible Java runtime and externally supplied infrastructure. This proof does not standardize host operating systems, Java distribution, artifact distribution infrastructure, or production process supervision.

Container packaging and infrastructure provisioning remain reversible future decisions. A later requirement for reproducible provisioning must separately compare Terraform, OpenTofu, and reasonable alternatives against a concrete provider/operator requirement.

The authoritative Structurizr model does not gain a new container or relationship because this decision changes packaging/run and readiness semantics of the existing application runtime rather than adding a new architectural participant.
