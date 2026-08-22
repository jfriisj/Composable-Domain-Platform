# Java Engineering Standard

**Version:** 0.3
**Status:** Accepted project standard
**Scope:** Java engineering quality for Composable Domain Platform
**Applies to:** production Java, test Java, generated-code boundaries, and Java-facing integration layers
**Adoption decision:** GitHub issue #75

## Purpose

This standard defines durable Java engineering rules for correctness, clarity, maintainability, testability, security where applicable, and economical change.

It applies only within Java concerns. It does not authorize product scope, architecture, module ownership, persistence, external contract, dependency, or workflow changes. Those concerns remain owned by the sources registered in `docs/governance.md`.

Normative terms:

- **MUST / MUST NOT** — mandatory unless an explicit governed deviation applies.
- **SHOULD / SHOULD NOT** — default expectation; deviation requires a concrete reason.
- **MAY** — optional.
- Examples are illustrative unless explicitly declared normative.

Prefer the simplest implementation that preserves correctness, explicit contracts/ownership, accepted architecture, testability, and minimum accidental complexity.

## Authority

Concern-specific authority applies:

- Java language/JVM/API semantics follow the Java specifications for the accepted Java baseline.
- Project scope, architecture, module ownership, contracts, migrations/schema, build truth, and workflow override this standard for their own concerns.
- This document owns Java engineering quality only.
- Exact admitted dependencies/plugins/versions are owned by Gradle build files/version catalog unless a Java-language baseline is explicitly recorded here.

If a Java rule conflicts with accepted architecture or contracts, stop and resolve the authority conflict instead of redesigning implicitly.

External style/engineering references are review inputs, not automatic project rules.

Adoption is incremental: new Java MUST follow the standard; materially changed Java SHOULD conform within the smallest affected change surface; untouched historical code does not enter scope merely because this standard exists.

## Core rules

### Scope and design

- Implement only the accepted outcome and minimum enabling work.
- Do not bundle unrelated cleanup, speculative abstractions, future infrastructure, or hypothetical reuse.
- Prefer explicit code over clever/generalized code when both satisfy the requirement.
- Add an abstraction only when it represents a real boundary/policy/concept or removes demonstrated duplication without hiding ownership.
- Keep classes/methods focused; split when distinct responsibilities or change reasons are present.
- Keep public visibility minimal.
- Prefer composition/delegation to inheritance unless subtype semantics are genuine and stable.
- Do not add mutable global/static state for convenience.

### Types and invariants

- Use types to represent meaningful concepts and invalid-state prevention where the value justifies it.
- Constructors/factories MUST establish required invariants.
- Preserve invariants through all observable state transitions.
- Prefer immutable value/state objects. Mutable state requires explicit ownership and lifecycle.
- Records MAY represent transparent immutable data carriers when record semantics fit.
- Enums SHOULD represent a fixed closed set of meaningful states rather than ad-hoc strings/integers.
- `equals`/`hashCode` MUST agree; ordering MUST be consistent with equality when callers rely on both.
- Avoid exposing mutable internal collections. Return immutable/unmodifiable snapshots where ownership must remain internal.

### Nullness and absence

- Nullness is part of a contract, not an implementation accident.
- Reject invalid nulls at the owning boundary.
- Do not return `null` from APIs when the accepted contract uses an explicit result/absence model.
- `Optional` MAY express return-value absence; do not use it mechanically for fields/parameters/collections.
- Empty collection and absent collection are different only when the domain/API explicitly says so.
- Do not add a nullness annotation/checker dependency without accepted tooling admission.

### Naming and documentation

- Names communicate domain/technical intent without encoding obsolete implementation detail.
- Avoid vague names such as `data`, `manager`, `helper`, `util`, or `handler` when a precise responsibility exists.
- Boolean names SHOULD read as predicates.
- Comments/Javadoc explain intent, constraints, ownership, non-obvious trade-offs, or contract obligations—not syntax.
- Remove stale/commented-out code. TODOs that represent real work belong in the accepted tracking mechanism when they affect delivery.
- Public API documentation SHOULD state meaningful preconditions, failure/absence semantics, and ownership where code signatures cannot do so clearly.

### Language usage

- Use the accepted Java release only; preview features require explicit admission.
- Prefer standard-library facilities over custom equivalents when semantics fit.
- Use modern language features when they improve clarity and remain inside the accepted baseline.
- Avoid reflection/dynamic access when ordinary typed APIs suffice.
- Generic APIs SHOULD use the narrowest useful type bounds and avoid unchecked operations. Any unavoidable unchecked suppression must be local and justified.

## Source and API

### Source conventions

- Source MUST compile cleanly under the accepted Gradle/JDK configuration.
- Keep package names stable, lowercase, and aligned with owned component boundaries.
- One public top-level type per source file unless a narrower arrangement materially improves clarity.
- Imports SHOULD be explicit and readable; avoid wildcard imports in hand-written production code.
- Avoid formatting-only churn outside the active change. Until a formatter is accepted, follow this standard and surrounding source consistently.
- Generated sources are build output: do not hand-edit them or use generated transport types as domain/application models.

### Public APIs

- Minimize public surface; expose only contracts required for intentional collaboration.
- Public APIs SHOULD use domain/application concepts owned by the providing component, not private adapters/framework/provider types.
- Inputs/outputs MUST make failure and absence semantics explicit when callers need to distinguish them.
- Prefer dedicated result types/values for expected business/application outcomes over throwing generic exceptions.
- Do not expose implementation exceptions as stable public failure contracts.
- Compatibility impact MUST be considered before changing an accepted public/cross-component contract.

### Interfaces, constructors, factories

- Introduce an interface for a real substitution/boundary/port, not by default for every class.
- Prefer constructor injection/explicit dependencies for required collaborators.
- Constructors SHOULD establish a valid usable object; factories MAY be used when construction needs naming, validation, selection, or controlled variants.
- Do not hide mandatory dependencies behind service locators/global state.

### Exceptions

- Use exceptions for exceptional failure, not routine expected control flow when an explicit result is clearer.
- Catch only when adding context, translating at an owning boundary, recovering, or guaranteeing cleanup.
- Never swallow a failure silently.
- Preserve the original cause when translating unexpected technical exceptions.
- Exception messages/logging MUST avoid credentials, secrets, participant/private identifiers, SQL records, or other sensitive implementation data.
- Do not catch `Throwable`/broad `Exception` merely to continue.

## Boundaries

Java code MUST preserve the architecture registered in `docs/architecture.md`, `docs/modules.md`, local `module.md` files, and `workspace.dsl`.

### Domain

- Domain code owns business concepts/rules/invariants.
- Domain MUST NOT depend on Spring, HTTP/Servlet, generated OpenAPI types, database frameworks, provider SDKs, or infrastructure implementation.
- Domain behavior SHOULD be testable without infrastructure.

### Application

- Application code orchestrates owned use cases and declares required outbound ports.
- It MUST NOT embed HTTP mapping, SQL/database mechanics, provider SDK behavior, or runtime wiring.
- Expected application outcomes SHOULD be transport-neutral.

### Public module/API boundaries

- Functional collaboration crosses module public APIs only.
- A module MUST NOT depend on another module's private implementation/persistence.
- Do not leak private domain/adapter/persistence types into public contracts.
- `core` remains business-neutral; do not move identity/business concepts there for reuse convenience.

### Persistence

- Persistence adapters implement ports owned by the module/application layer.
- Database/jOOQ/Flyway types remain inside the owning private persistence boundary.
- Schema/migration semantics are owned by migrations, not Java DTOs.
- No Java repository/adapter may bypass accepted cross-module persistence ownership.

### Transport/adapters

- HTTP/provider adapters translate external representations/mechanisms to public application/module contracts.
- Structural transport validation belongs at the adapter; business validation remains with its owner.
- Generated OpenAPI types remain transport-layer build artifacts.
- Sanitize external/internal failure mapping according to the authoritative contract.

### Runtime/composition

- Runtime code selects, constructs, configures, starts, and wires.
- Runtime MUST NOT become the implementation owner of module/business behavior.
- Cross-module compositions own only workflow and depend on public module APIs.

## Errors and resources

### Interruption and cleanup

- Preserve thread interruption: when catching `InterruptedException`, either propagate it or restore interruption before returning/throwing another failure unless ownership explicitly requires consuming it.
- Close owned `AutoCloseable` resources reliably, normally with try-with-resources.
- Resource ownership MUST be explicit; do not close resources owned by callers/containers unless the contract says so.

### Concurrency

- Introduce concurrency only for a concrete accepted need.
- Define ownership, thread-safety, ordering, cancellation, failure propagation, and shutdown semantics before adding async/executor behavior.
- Prefer immutable state and confinement over shared synchronization.
- Executors/threads require explicit lifecycle/shutdown ownership.
- Do not block indefinitely without an accepted reason and observable cancellation/timeout behavior where needed.

### Time, numbers, locale

- Use `java.time` APIs; make timezone/clock ownership explicit where behavior depends on time.
- Inject/control time in tests for time-sensitive behavior rather than relying on wall-clock sleeps.
- Use decimal types/rounding rules appropriate to accepted domain semantics; do not use binary floating point for exact monetary semantics.
- Use explicit locale/charset where platform defaults could change behavior.

### Logging

- Log operational facts at the boundary that owns them.
- Do not log passwords, credential verifiers/tokens, authorization headers, private actor/participant references, or other prohibited sensitive values.
- Correlation/causation metadata follows architecture; it MUST NOT become business identity.
- Avoid duplicate logging of the same failure at every layer.

## Testing

Tests provide evidence for changed behavior/invariants; they do not justify unaccepted production design.

- Tests MUST be deterministic, isolated from ordering, and readable as behavioral evidence.
- Test observable behavior/contract rather than private implementation structure unless the private structure itself is the invariant under test.
- Unit tests cover local behavior/rules cheaply.
- Integration tests cover real adapter/infrastructure semantics where substitutes would hide relevant behavior.
- Architecture tests enforce dependency/ownership rules that are mechanically expressible.
- End-to-end tests prove accepted assembled workflows only when cross-boundary evidence is required.
- Real PostgreSQL tests use the repository's accepted Testcontainers approach where persistence behavior requires it.
- Avoid sleeps/races; control clocks, synchronization, or eventual conditions explicitly.
- Test names SHOULD identify condition and expected outcome.
- Failure paths, boundary validation, uniqueness/idempotency, authorization/privacy, and resource cleanup SHOULD be tested when affected.
- Mocks/fakes MUST not redefine an external/database contract more permissively than production.
- Generated code normally is validated through generation/build/adapter behavior rather than duplicating generated implementation tests.

Test scope remains proportional: targeted tests supplement, but do not replace, required repository validation.

## Security

Apply security rules only where the active Java change touches a relevant trust/security boundary, but apply them rigorously when it does.

- Treat external input as untrusted until validated by the owning boundary.
- Never construct SQL by concatenating untrusted values; use accepted typed/bound parameter mechanisms.
- Do not deserialize untrusted Java native object streams.
- Avoid reflection/dynamic class loading from untrusted input.
- Keep credentials, secrets, tokens, verifiers, authorization headers, and private participant identifiers out of logs/errors/test fixtures unless an accepted test-only mechanism explicitly requires a non-secret deterministic value.
- Do not hard-code production secrets.
- Use standard cryptographic/security APIs and accepted algorithms/configuration; do not invent cryptography.
- Use secure randomness where security semantics require unpredictability.
- Authentication/Authorization framework types remain in their accepted private adapter/implementation boundary; public/domain contracts remain framework-neutral.
- Authorization decisions belong to the accepted owner; transport adapters enforce only accepted external mapping/privacy behavior.
- Validate file/path/URL/process inputs appropriately when Java code crosses those trust boundaries.
- Dependency additions with security/runtime impact require normal technology/change control.

This standard does not admit a new authentication mechanism, security product, analyzer, or dependency.

## Validation

Required validation is owned operationally by `docs/workflow.md`.

For Java/build-affecting changes, the final repository gate is:

```bash
./gradlew --no-daemon check
```

Before completion also run the applicable focused tests/checks and:

```bash
git diff --check
git diff --cached --check
```

Current Java adoption intentionally does **not** require:

- a deterministic Java formatter;
- Error Prone;
- JSpecify/nullness checker;
- Checkstyle, SpotBugs, PMD, Sonar, or another analyzer merely because this standard names the concern.

Introducing such tooling requires separate accepted change, pinned/reproducible configuration, handling of existing code, and integration into the normal repository gate.

Suppressions are exceptions:

- keep them narrow/local;
- justify the exact rule/risk;
- never use them to hide an unexplained correctness failure;
- broad package/module suppressions require explicit project justification.

Review-only Java concerns include intent/naming, domain invariant placement, API minimality, scope creep, and semantic ownership when deterministic tooling cannot judge them reliably.

Documentation-only work does not run Gradle solely because this standard exists; follow the docs-only gates in `workflow.md`.

## Project profile

- **Java/JDK release:** 21.
- **Preview features:** not accepted.
- **Build authority:** repository Gradle Wrapper and accepted Java convention/build logic.
- **Formatter:** none accepted; avoid whole-repository formatting churn.
- **Additional compiler/analyzer gate:** none introduced by Java-standard adoption.
- **Nullness tooling:** no JSpecify/nullness checker adopted.
- **Architecture verification:** existing ArchUnit tests; exact version/configuration is build truth.
- **Test framework:** existing JUnit configuration; exact version is build truth.
- **Real infrastructure:** existing Testcontainers configuration where required; exact version/image truth is build/test configuration.
- **Persistence stack where already owned:** PostgreSQL + Flyway + jOOQ; exact versions are build/migration truth and this profile does not authorize persistence for new owners.
- **Security:** accepted scope/architecture/ADRs own Security behavior; this standard adds no mechanism.
- **Dependency/version truth:** Gradle build files/version catalog.
- **Adoption:** changed-code/incremental brownfield; no whole-repository Java cleanup is authorized by issue #75.
- **Mechanical deviations:** formatter, Error Prone, and JSpecify remain deliberately unadopted until separately accepted.

For project-specific ownership, contract, schema, module, runtime, and validation detail, follow the most-specific authorities instead of expanding this profile.
