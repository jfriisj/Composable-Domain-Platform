# Java Engineering Standard

**Version:** 0.3
**Status:** Accepted project standard
**Scope:** Java engineering quality for Composable Domain Platform
**Applies to:** Production Java, test Java, generated-code boundaries, and Java-facing integration layers
**Adoption decision:** GitHub issue #75
**Companion authorities:** `docs/governance.md`, `docs/workflow.md`, `docs/architecture.md`, `docs/architecture/workspace.dsl`, `docs/tech-stack.md`, `docs/scope.md`

---

## 1. Purpose

This standard defines how Java code should be designed, implemented, reviewed, and mechanically validated so that it remains:

- correct;
- understandable;
- explicit;
- maintainable;
- testable;
- secure where relevant;
- compatible with accepted architecture;
- economical to change.

It covers more than formatting. It defines expectations for source style, type design, APIs, nullness, state, error handling, resources, concurrency, testing, persistence boundaries, transport adapters, runtime configuration, security-sensitive code, and static enforcement.

This standard is intentionally independent of any particular:

- framework;
- database;
- build system;
- repository host;
- application architecture;
- deployment platform;
- domain.

Project-specific choices are recorded in the **Composable Domain Platform Project Profile** in Section 69.

This document applies only to Java concerns. It does not define engineering rules for Kotlin DSL, SQL, YAML, OpenAPI, TypeScript, or other languages/formats unless a rule explicitly addresses a Java-facing boundary.

This document is the normative engineering standard for Java code within its concern. External style guides and engineering guides are sources used to derive, review, and improve this standard; they do not change it automatically.

---

## 2. Non-goals

This standard does not:

- define product requirements;
- authorize architecture changes;
- define domain ownership;
- replace accepted contracts or schemas;
- require a specific framework;
- require a specific persistence technology;
- require every optional analysis tool;
- authorize unrelated cleanup;
- require all legacy code to be rewritten immediately.

A Java rule MUST NOT be used to bypass accepted project scope, architecture, ownership, contracts, persistence truth, or change-control rules.

---

## 3. Normative language

The following terms are normative:

- **MUST** — mandatory unless an explicit governed deviation applies.
- **MUST NOT** — prohibited unless an explicit governed deviation applies.
- **SHOULD** — expected default; deviation requires a concrete reason.
- **SHOULD NOT** — normally avoided; deviation requires a concrete reason.
- **MAY** — optional.

Examples are illustrative unless explicitly marked normative.

---

## 4. Authority and precedence

Authority is **concern-specific**. There is no single global precedence order that makes a source authoritative outside the concern it owns.

The following model applies.

### 4.1 External obligations

Applicable legal, regulatory, contractual, safety, and security obligations take precedence within their applicable concern.

### 4.2 Project authorities

Accepted project governance, scope, architecture, ownership, external contracts, persistence contracts, and workflow define what work is authorized and which system boundaries and behaviors must be preserved.

Examples:

- architecture decides **where responsibility belongs**;
- contracts decide **what external behavior is promised**;
- persistence schema/migrations decide **what durable structure exists**;
- planning/governance decides **what work is authorized**.

This Java standard MUST NOT be used to override those authorities.

### 4.3 Java platform authorities

For Java language, virtual-machine, and platform semantics, the authoritative sources are the specifications applicable to the project's accepted Java baseline, including as relevant:

- the Java Language Specification (JLS);
- the Java Virtual Machine Specification (JVMS);
- the Java SE/JDK API Specification;
- other official Java/JDK specifications for APIs or formats used by the project.

These sources define what the Java platform means and guarantees. They do not define the project's architecture or engineering scope.

If this standard accidentally contradicts the applicable Java specification on Java semantics, the specification wins for that semantic question and the standard SHOULD be corrected.

### 4.4 Java Engineering Standard

Within the boundaries established above, **this Java Engineering Standard is the normative authority for Java engineering quality**.

It owns the project's general Java expectations for:

- source conventions;
- type and API design;
- invariants and mutability;
- nullness;
- exceptions and failure handling;
- resources and concurrency;
- tests;
- layer-specific Java quality;
- mechanical validation expectations;
- Java-specific scope control.

A general external style or engineering guide MUST NOT silently override this standard.

### 4.5 Project Profile

The Project Profile specializes this standard only where this standard permits project-specific choice.

Examples include:

- accepted JDK baseline;
- formatter and exact formatter version;
- static-analysis tools and versions;
- nullness tooling;
- layer/package mapping;
- test commands;
- adoption/migration mode;
- explicit governed deviations.

### 4.6 Executable enforcement

Pinned tools MAY act as executable authorities for the concern they are explicitly assigned.

Examples:

- a pinned formatter defines canonical formatting;
- the compiler defines whether source compiles for the accepted toolchain;
- a configured static analyzer defines whether its accepted checks pass;
- architecture tests define whether encoded accepted architecture rules pass.

A tool is authoritative only for its assigned mechanical concern. It does not become an authority for product scope, architecture, ownership, or design merely because it can report a finding.

### 4.7 External guidance

External guides such as Google Java Style, OpenJDK style guidance, the OpenJDK Developers' Guide, Cornell teaching guidance, OWASP guidance, and tool documentation are **review and derivation sources** unless explicitly incorporated elsewhere.

They MAY:

- motivate a rule;
- reveal a missing concern;
- help review this standard;
- inform future revisions.

They MUST NOT change accepted project rules automatically.

A newer upstream guide, tool release, JDK release, blog post, or recommendation does not become normative merely because it is newer.

An older guide is not rejected merely because it is old; individual guidance is evaluated against current platform semantics, project needs, and this standard.

### 4.8 External-guidance update policy

Changes in external guidance MAY trigger review.

Adopting a changed rule requires one of:

- a new accepted version of this standard;
- an allowed Project Profile specialization;
- an explicit governed deviation.

The rationale and executable tooling baseline SHOULD be updated together where a rule is mechanically enforced.

### 4.9 Conflicts

If two applicable authoritative sources conflict within the same concern, the conflict MUST be surfaced.

Do not silently choose whichever rule is easier to satisfy.

A lower-level Java preference MUST NOT override a higher-level accepted architectural, contractual, or scope decision.

---

## 5. Core engineering principles

Java code MUST preserve required correctness and applicable accepted architecture/contracts.

Within those constraints, code SHOULD optimize for:

- explicit contracts and invariants;
- clear expression of intent;
- testability and diagnosability;
- minimum accidental complexity;
- performance where measured requirements justify it.

These concerns are not a universal total ordering. Their relative weight depends on the accepted outcome, risk, and project context.

The following principles apply throughout this standard:

- prefer simple, direct code over clever code;
- make invalid states difficult to represent where practical;
- make dependencies visible;
- keep ownership explicit;
- keep public APIs small;
- minimize mutable shared state;
- fail close to the cause;
- separate boundary concerns from domain/application concerns;
- automate deterministic rules instead of debating them repeatedly;
- do not introduce abstractions for hypothetical variation;
- do not optimize without evidence when optimization adds complexity;
- prefer current verified platform semantics over remembered or historical Java advice;
- evaluate external recommendations by rationale and applicability, not by popularity, age, or novelty alone.

---

## 6. Scope control and anti-scope-creep

Quality improvement does not authorize unlimited cleanup.

### 6.1 Active-work rule

When working on an accepted change:

- new code MUST follow this standard;
- new work MUST NOT introduce or materially worsen violations in the affected change surface;
- materially changed code SHOULD comply with this standard to the extent required by the accepted outcome and migration mode;
- pre-existing violations outside the minimum affected change surface do not become part of the current work merely because their file is touched;
- required enabling changes MAY be included when they are the minimum necessary consequence of the accepted work;
- adjacent cleanup MUST NOT be absorbed merely because the file is open;
- unrelated existing violations MUST remain separate work.

A Project Profile MUST define how formatter/static-analysis enforcement applies to legacy files when whole-file tools would otherwise create unrelated churn.

### 6.2 Discovery is not authorization

Discovering a:

- style violation;
- design smell;
- missing null contract;
- old API problem;
- dependency issue;
- test weakness;
- security concern;
- architecture concern

does not automatically authorize fixing it in the current work unit.

Classify it according to the project's scope-control rules.

### 6.3 Brownfield adoption

Existing projects MAY adopt this standard incrementally.

A Project Profile SHOULD define one of:

- **full enforcement** — all applicable code must pass;
- **baseline enforcement** — existing violations are recorded, and no new violations are allowed;
- **changed-code enforcement** — new and materially changed code must pass while legacy areas are migrated separately.

Tooling changes that reformat or rewrite large unrelated areas SHOULD be delivered separately from behavior changes.

---

# Part I — Source and language rules

## 7. Java source conventions

The rules in this section are normative. They are informed by established Java practice, including Google, OpenJDK, and other reviewed guidance, but this section—not a live external webpage—defines the accepted engineering requirement.

### 7.1 Canonical formatting

A project SHOULD designate a deterministic Java formatter and pin its exact version in the Project Profile.

The recommended default is a pinned `google-java-format` version because it provides deterministic, low-configuration formatting.

When a formatter is designated:

- its output is canonical for formatting;
- manual formatting preferences MUST NOT override it;
- formatting validation SHOULD run non-mutating in normal validation;
- automatic formatting MAY be available as a separate developer action;
- formatter upgrades are deliberate tooling/standard changes rather than silent changes to style.

Humans and agents decide design. The designated formatter decides only the formatting concern assigned to it.

### 7.2 Braces and control flow

Braces MUST be used for the bodies of:

- `if` / `else`;
- `for`;
- enhanced `for`;
- `while`;
- `do` / `while`.

Avoiding braces to save lines is not an accepted simplification.

### 7.3 Imports

Hand-written Java MUST:

- avoid wildcard imports;
- avoid unused imports;
- keep static and non-static imports in the order required by the designated formatter/project convention;
- use static imports only where they improve clarity.

Import layout SHOULD be machine-enforced when practical.

### 7.4 Exactly one top-level type per ordinary source file

An ordinary hand-written Java source file MUST contain exactly one top-level class, record, enum, interface, or annotation type.

The file name MUST correspond to that top-level type.

`package-info.java` and `module-info.java` follow their special Java source-file structures and are not ordinary class source files.

### 7.5 `@Override`

Use `@Override` whenever a method legally overrides or implements a supertype method.

This makes override intent compiler-checkable and avoids accidental signature drift.

### 7.6 Encoding

Source files MUST use UTF-8.

Projects SHOULD use a consistent line-ending policy suitable for their repository and tooling.

### 7.7 Generated source

Generated Java is governed by Section 59 and is not manually reformatted unless the generator explicitly supports and owns that formatting.

---

## 8. Language feature rule

Use Java language features when they:

- make intent clearer;
- reduce invalid states;
- reduce boilerplate without hiding semantics;
- are supported by the project's accepted Java baseline.

Do not use a feature merely because it is newer.

Modernization by itself is not sufficient justification for a behavior-affecting or broad code change.

Before introducing a newer language feature into existing code, consider:

- whether maintainers can understand it in context;
- whether it changes behavior or compatibility;
- whether the existing tests adequately protect the change;
- whether the simplification is real rather than cosmetic.

Examples:

- records SHOULD be considered for immutable value/data carriers;
- sealed types SHOULD be considered when a hierarchy is intentionally closed;
- pattern matching MAY be used when it makes branching clearer;
- switch expressions SHOULD be preferred over mutable temporary-result patterns when they improve clarity.

Preview features MUST NOT be introduced without explicit project approval.

---

## 9. Naming

Names MUST reveal intent and use consistent domain/project terminology.

### 9.1 Types

Classes, records, interfaces, enums, and annotations SHOULD use nouns or noun phrases that describe the represented concept.

Avoid vague suffixes unless they express a real architectural role:

- `Manager`
- `Helper`
- `Util`
- `Processor`
- `Handler`
- `Data`
- `Info`

These names are acceptable only when the concept itself is genuinely accurate.

### 9.2 Methods

Methods SHOULD use verbs or verb phrases that describe observable action or result.

Boolean-returning methods SHOULD read as predicates where practical.

### 9.3 Variables and fields

Names SHOULD encode meaning rather than type mechanics.

Prefer:

`registrationId`

over:

`strId`

Avoid unexplained abbreviations.

### 9.4 Constants

Constants MUST represent concepts, not merely extracted literals.

Do not create a named constant solely to hide an otherwise obvious value if the name adds no meaning.

---

## 10. Comments and Javadoc

Code SHOULD express what it does directly.

Comments SHOULD explain information that code cannot reasonably express, such as:

- why a non-obvious constraint exists;
- why a workaround is necessary;
- compatibility constraints;
- security reasoning;
- algorithmic reasoning;
- external-system behavior.

Comments MUST NOT:

- repeat obvious code;
- preserve deleted/commented-out code;
- serve as change history;
- compensate for misleading names.

Dead code and commented-out implementation code MUST NOT be retained as a substitute for version control.

`TODO`, `FIXME`, and similar markers MUST NOT be used as a substitute for durable work tracking when the missing work matters to correctness, security, compatibility, or an accepted outcome.

If such markers are permitted by the Project Profile, they SHOULD reference durable tracked work when follow-up is required.

### 10.1 Javadoc

Javadoc MUST be syntactically valid and attached to the declaration it documents.

Public APIs SHOULD document semantics that a caller cannot safely infer from the signature alone, including where relevant:

- nullability;
- units;
- ordering;
- side effects;
- thread-safety;
- lifecycle;
- failure behavior;
- compatibility commitments.

Do not write verbose Javadoc that merely repeats a self-explanatory declaration.

---

# Part II — Correctness and static analysis

## 11. Compiler correctness

All production and test Java MUST compile without errors under the project's accepted Java toolchain.

The project SHOULD enable useful compiler diagnostics.

New actionable compiler warnings SHOULD NOT be introduced.

Warnings MUST NOT be globally suppressed merely to achieve a green build.

Suppressions MUST be:

- narrow;
- locally justified;
- attached to the smallest practical scope.

### 11.1 Java assertions are not contract validation

Java `assert` MUST NOT be used for:

- argument checking on public APIs;
- validation of untrusted input;
- authorization or security decisions;
- side effects required for correct execution.

Assertions may be disabled at runtime. Assertion expressions SHOULD therefore be free of side effects that affect required program behavior.

Assertions MAY be used for internal invariants when failure is genuinely a programming error and correctness does not depend on assertion evaluation.

---

## 12. Error Prone

Projects SHOULD use **Error Prone** or an equivalent compile-time bug detector when compatible with the build.

When Error Prone is adopted:

- default correctness errors MUST pass;
- a check MUST NOT be disabled globally without a documented reason;
- suppressions MUST be narrow;
- style opinions SHOULD NOT be promoted to errors merely because the analyzer supports them;
- experimental checks SHOULD be enabled only when their signal is understood.

Static analysis exists to find likely defects, not to create arbitrary busywork.

---

## 13. Additional analyzers

Projects MAY add tools such as:

- Checkstyle;
- SpotBugs;
- PMD;
- Sonar-based analysis;
- custom compiler plugins;
- custom static rules.

Additional analyzers MUST NOT:

- contradict the canonical formatter;
- duplicate the same concern with conflicting rules;
- become a hidden architecture authority;
- create a requirement to clean unrelated legacy code during every change.

Each enabled rule SHOULD have a clear quality rationale.

---

# Part III — Type and API design

## 14. Type design

Use the type system to make important distinctions explicit.

A separate type SHOULD be considered when it:

- prevents category mistakes;
- carries an invariant;
- communicates a domain concept;
- prevents accidental unit/identifier confusion;
- creates a meaningful API boundary.

Do not create wrapper types that add no semantic value.

### 14.1 Value types

Immutable value concepts SHOULD prefer:

- records;
- final classes with final state;
- validated factory/constructor creation.

A value type SHOULD establish its validity at creation.

### 14.2 Type invariants

A type with invariants MUST establish them during valid construction/factory creation and preserve them after every externally observable operation.

Methods MAY assume an established invariant only when callers cannot legitimately create or supply an invalid instance through the accepted API.

Invariants SHOULD be expressed in code and types where practical. Documentation SHOULD explain non-obvious invariants that cannot be made sufficiently clear through types, names, and validation.

### 14.3 Entity/lifecycle types

Mutable domain state MAY exist where lifecycle behavior requires it.

Mutation MUST remain owned by the responsible concept.

Avoid public setters that allow callers to bypass invariants.

### 14.4 Method design

A method SHOULD express one coherent operation or decision.

There is no universal maximum method length. Consider extraction when it improves:

- naming of a meaningful sub-operation;
- local reasoning;
- variable scope;
- testability;
- control-flow clarity.

Do not split methods merely to satisfy an arbitrary line-count target.

Do not combine operations that have different responsibilities merely because their implementation looks structurally similar.

Method parameters SHOULD NOT be reassigned unless reassignment materially improves clarity and does not obscure the caller-provided value.

---

## 15. Public API minimization

Public visibility is a design decision.

Use the narrowest visibility that satisfies the accepted architecture.

A type or member MUST NOT be public solely for:

- test convenience;
- framework convenience that can be isolated;
- anticipated future use.

Public APIs SHOULD expose domain/application concepts rather than implementation details.

---

## 16. Interfaces and abstractions

Create an interface when there is a concrete reason, such as:

- a stable architectural boundary;
- multiple legitimate implementations;
- test substitution at an accepted port;
- separation from infrastructure;
- an externally consumed contract.

Do not create an interface for every class.

Do not introduce factories, builders, strategies, adapters, repositories, or other patterns without a concrete responsibility.

An abstraction SHOULD remove meaningful coupling or express a real concept.

---

## 17. Constructors and factories

Constructors and factories MUST establish valid object state.

Prefer constructors when creation is simple and unambiguous.

Use static factories when they materially improve:

- naming;
- validation;
- representation hiding;
- subtype choice;
- caching;
- distinction between creation modes.

Use builders when parameter count or optional combinations otherwise make construction difficult to read or misuse-prone.

Builders MUST NOT become a default ceremony for small immutable objects.

---

## 18. Records

Records SHOULD be considered for transparent, value-like carriers whose shallow immutability matches the intended contract.

Java records are **shallowly immutable**: component fields are final, but referenced component objects may still be mutable.

If a record is intended to provide immutable value semantics, mutable components MUST have explicit ownership and SHOULD be defensively copied or replaced with immutable representations where necessary.

A record MUST NOT be used merely to avoid writing a class when:

- identity semantics dominate;
- lifecycle mutation is essential;
- representation must remain hidden;
- constructor semantics become misleading.

Record component names are part of the API and MUST be chosen carefully.

The generated `toString()` includes component names and values. Records containing secrets or sensitive data MUST therefore be designed so that accidental logging/stringification does not expose protected information.

---

## 19. Enums

Enums SHOULD be used for finite, meaningful sets of states or choices.

Persistence and external contracts MUST NOT rely on enum ordinal values.

Persist or transmit stable explicit representations.

Adding or renaming enum constants MUST consider external compatibility where the enum crosses a contract boundary.

---

## 20. Generics

Generics SHOULD improve type safety and reduce casting.

Avoid:

- raw types;
- unchecked casts without narrow justification;
- unnecessarily complex generic hierarchies;
- type parameters that add no constraint or meaning.

Wildcard use SHOULD follow API variance needs rather than stylistic preference.

Suppress unchecked warnings only at the smallest verified boundary.

---

# Part IV — Nullness, absence, and validation

## 21. Nullness is part of the contract

Whether a value may be `null` is part of the API contract.

Nullability MUST NOT be left intentionally ambiguous in public or cross-component APIs.

Projects SHOULD adopt **JSpecify** semantics for explicit nullness.

### 21.1 Preferred model

Where JSpecify is adopted:

- code SHOULD use `@NullMarked` scopes;
- nullable API/type usages for which JSpecify defines explicit nullness SHOULD use `@Nullable`;
- root types of local variables SHOULD follow JSpecify dataflow semantics rather than being mechanically annotated `@Nullable` or `@NonNull`;
- nullable type arguments, array components, bounds, parameters, fields, and return types MUST follow JSpecify's type-use semantics;
- unspecified nullness SHOULD be limited to migration or third-party boundaries;
- nullness analysis SHOULD run in validation.

The Project Profile SHOULD pin the adopted JSpecify specification/tooling baseline when nullness is mechanically enforced.

### 21.2 Runtime checks

Static nullness analysis does not eliminate all runtime boundary validation.

`Objects.requireNonNull` or equivalent checks MAY be appropriate at:

- public API boundaries;
- construction boundaries;
- untrusted integration boundaries.

Programming errors and user/domain validation failures SHOULD remain distinguishable.

---

## 22. Optional

`Optional<T>` MAY represent absence in return contracts.

It SHOULD NOT be used mechanically for every nullable concept.

Avoid `Optional` when:

- absence is not meaningful;
- a collection already naturally represents zero-or-more;
- a domain-specific result type communicates more;
- it would make serialization/framework integration worse without benefit.

An `Optional` reference itself MUST NOT be `null`.

---

## 23. Validation ownership

Validate a rule at the boundary that owns it.

Examples:

- syntax/transport shape → adapter boundary;
- use-case precondition → application boundary;
- domain invariant → domain owner;
- schema constraint → persistence authority plus application/domain behavior where needed.

Do not duplicate the same business rule independently across layers.

Boundary validation MUST NOT replace domain invariants.

---

# Part V — State, mutability, and value semantics

## 24. Immutability

Prefer immutable state by default.

Instance fields SHOULD be `final` unless the owning type requires that field reference/value to change after construction.

State SHOULD be mutable only when mutation represents meaningful lifecycle behavior or materially simpler implementation.

Collections exposed from an object MUST NOT allow callers to mutate internal state accidentally.

Use defensive copies where ownership is not otherwise guaranteed.

Do not copy automatically when the type and ownership contract already guarantees immutability.

---

## 25. Static state

Mutable static state SHOULD be avoided.

If mutable process-wide state is required:

- ownership MUST be explicit;
- lifecycle MUST be explicit;
- concurrency semantics MUST be explicit;
- tests MUST isolate it.

Global state MUST NOT be introduced as a convenience shortcut for dependency wiring.

---

## 26. `equals`, `hashCode`, and ordering

If a type overrides `equals`, it MUST provide a compatible `hashCode`.

Equality MUST be:

- reflexive;
- symmetric;
- transitive;
- consistent;
- stable while used as a hash key.

Mutable fields that participate in hashing require particular care and SHOULD generally be avoided.

### 26.1 Natural ordering

A type's natural ordering through `Comparable` SHOULD be consistent with `equals`.

If `compareTo(a, b) == 0` can intentionally differ from equality semantics, the type MUST document that fact and callers MUST consider the effect on sorted sets/maps.

### 26.2 Purpose-specific comparators

A `Comparator` MAY intentionally order or group objects by only part of their state and therefore need not generally be consistent with `equals`.

When a comparator is used where `compare(a, b) == 0` determines key/element identity, such as sorted sets or sorted maps, the resulting semantics MUST be deliberate and tested/documented when they differ from `equals`.

Arrays MUST use content-aware equality/hash operations when value comparison is intended.

---

# Part VI — Collections, streams, and data processing

## 27. Collections

Use the narrowest useful collection abstraction.

Distinguish **unmodifiable** from **immutable** collections:

- an unmodifiable collection rejects mutation through that reference;
- an unmodifiable view may still reflect changes to its backing collection;
- an immutable collection guarantees that changes to the collection itself are not observable.

Use immutable snapshots when callers require stable value state.

Use unmodifiable views only when shared backing-state semantics are intentional and ownership remains safe.

Do not expose implementation-specific collection types without a reason.

Choose collection implementations from required semantics:

- ordering;
- uniqueness;
- lookup behavior;
- concurrency;
- memory/performance constraints.

Do not choose a collection solely by habit.

---

## 28. Streams

Streams SHOULD be used when they make transformation intent clearer.

Prefer ordinary loops when they are clearer for:

- complex control flow;
- stateful algorithms;
- exception-heavy logic;
- early exits;
- multi-step mutation.

Stream pipelines SHOULD remain understandable without tracing excessive nested lambdas.

Avoid side effects inside stream operations unless clearly controlled and justified.

Parallel streams MUST NOT be introduced without a measured concurrency requirement and understood execution semantics.

---

# Part VII — Errors and failure handling

## 29. Exceptions

Exceptions represent exceptional or failure conditions, not ordinary branching.

Code MUST NOT:

- silently swallow unexpected exceptions;
- use empty catch blocks;
- catch `Throwable` for ordinary application logic;
- use exceptions as a substitute for expected control flow.

### 29.1 Catch narrowly

Catch the most specific exception type that the boundary can meaningfully handle.

Catching `Exception` MAY be appropriate at carefully defined outer boundaries, such as:

- request dispatch;
- job execution;
- process integration;
- error reporting.

Such broad catches MUST NOT hide failures.

### 29.2 Translation

Translate an exception only when crossing a meaningful abstraction boundary.

Translated exceptions SHOULD:

- use vocabulary appropriate to the receiving layer;
- preserve the original cause when diagnostically useful;
- avoid leaking infrastructure details into domain/application contracts.

### 29.3 Messages

Exception messages SHOULD provide useful context without exposing secrets or sensitive data.

### 29.4 Checked versus unchecked

Use checked or unchecked exceptions according to the project's API strategy and recoverability semantics.

Do not force checked exceptions through multiple layers when callers cannot meaningfully recover.

Do not use unchecked exceptions to hide normal expected alternatives.

---

## 30. Interrupted execution

Code that catches `InterruptedException` MUST preserve cancellation semantics.

Normally it should either:

- propagate the exception; or
- restore the interrupted status before returning/throwing another failure.

Never swallow interruption silently.

---

# Part VIII — Resource management

## 31. Closeable resources

Resources implementing `AutoCloseable` SHOULD be managed with try-with-resources when ownership is local.

Resource ownership MUST be explicit.

Code MUST NOT rely on finalization for correctness.

Examples include:

- files;
- streams;
- sockets;
- database resources.

Executor services are governed primarily by Sections 32–34 because `ExecutorService.close()` has task-completion and interruption semantics that require an explicit concurrency/lifecycle decision.

A resource MUST NOT be closed by code that does not own its lifecycle unless the API contract explicitly transfers ownership.

---

# Part IX — Concurrency

## 32. Concurrency admission rule

Concurrency SHOULD be introduced only for a concrete requirement.

Before introducing concurrency, establish:

- what work may execute concurrently;
- who owns mutable state;
- cancellation behavior;
- failure propagation;
- ordering requirements;
- resource limits;
- validation strategy.

Do not add concurrency as speculative optimization.

---

## 33. Thread safety

Thread-safety characteristics SHOULD be clear for types shared across threads.

Prefer, in order:

1. immutability;
2. thread confinement;
3. message/queue ownership;
4. well-defined concurrent data structures;
5. explicit locking.

Shared mutable state MUST have defined synchronization.

Do not synchronize on publicly accessible mutable objects.

Avoid invoking unknown/external callbacks while holding locks.

Lock ordering MUST be consistent where multiple locks are required.

---

## 34. Executors and async work

Executor ownership and lifecycle MUST be explicit.

For an owned `ExecutorService`, shutdown policy MUST be deliberate, including as applicable:

- orderly shutdown;
- forced shutdown;
- interruption handling;
- termination timeout;
- behavior for queued/running tasks.

`ExecutorService.close()` MAY be used when waiting for submitted tasks to complete is the intended lifecycle behavior. It MUST NOT be used mechanically merely because `ExecutorService` implements `AutoCloseable`.

Code SHOULD NOT create ad hoc thread pools deep inside business logic.

Asynchronous APIs SHOULD define:

- execution context;
- cancellation;
- failure behavior;
- timeout semantics where relevant.

Do not block indefinitely on async work without an accepted reason.

---

# Part X — Time, numbers, text, and units

## 35. Time

Use Java time types according to semantics.

Typical defaults:

- `Instant` → machine timestamp;
- `LocalDate` → calendar date without time;
- `LocalTime` → local time without date/zone;
- `LocalDateTime` → local date/time whose zone is intentionally external;
- `OffsetDateTime` → timestamp with offset;
- `ZonedDateTime` → time where named-zone rules matter;
- `Duration` → elapsed amount;
- `Period` → calendar-based amount.

Code MUST NOT rely accidentally on the machine default timezone or locale for business behavior.

When current time affects deterministic business logic or tests, a time source such as `Clock` SHOULD be injectable at the appropriate boundary.

---

## 36. Numeric precision

Use numeric types according to required semantics.

Do not use binary floating point for values requiring exact decimal semantics, such as many monetary calculations.

When `BigDecimal` is used:

- rounding behavior MUST be explicit where division/scale requires it;
- equality/scale semantics MUST be understood;
- conversion from floating-point literals SHOULD be avoided when it introduces representation surprises.

Units SHOULD be explicit in names or types.

---

## 37. Locale and text

Formatting and parsing that depend on locale MUST use an explicit locale when behavior must be stable across environments.

Case conversion for machine identifiers SHOULD use locale-independent semantics.

Character encoding MUST be explicit at external byte/text boundaries unless the API contract fixes it unambiguously.

---

# Part XI — Logging and diagnostics

## 38. Logging

Logging is an operational boundary, not business logic.

Logs SHOULD:

- communicate actionable events;
- include relevant context;
- use stable structured fields where the logging stack supports them;
- preserve correlation/trace context where the architecture defines it.

Logs MUST NOT:

- contain secrets;
- expose credentials or tokens;
- dump sensitive personal data without explicit authorization;
- become a substitute for returned errors or proper metrics.

Avoid logging the same exception repeatedly at multiple layers unless each log serves a distinct operational purpose.

Library/domain code SHOULD NOT write directly to standard output except where stdout/stderr is the accepted interface, such as a CLI.

---

# Part XII — Security-sensitive Java

## 39. Security applicability

Security review is change-local.

Apply security rules when the current Java code:

- accepts untrusted input;
- constructs queries/commands;
- handles authentication/authorization;
- handles secrets;
- performs cryptography;
- performs deserialization;
- interacts with files/network/processes;
- crosses trust boundaries.

This section MUST NOT be used to turn every Java change into a general security audit.

---

## 40. Core security rules

When applicable:

- untrusted values MUST NOT be interpolated directly into executable database query syntax;
- use bind parameters, typed query DSLs, or equivalent APIs that separate values from executable query structure;
- dynamically selected identifiers/query structure MUST be constructed from trusted/validated choices rather than raw untrusted text;
- avoid constructing shell/process commands from untrusted input;
- validate input according to the receiving boundary's contract;
- encode/escape output for its destination context;
- application code MUST NOT implement custom cryptographic primitives;
- if cryptographic implementation is itself the accepted purpose of the project/work, it requires applicable specifications, test vectors, and appropriate expert security review;
- use cryptographically secure randomness for security tokens;
- keep secrets out of source, logs, exception messages, and test fixtures;
- use constant-time/security-specific APIs where the underlying protocol requires them;
- keep security-sensitive dependencies maintained and reviewed;
- fail closed for authorization decisions.

Use the applicable JDK Security Developer's Guide and OWASP guidance for security-sensitive implementations.

---

# Part XIII — Serialization, reflection, and dynamic behavior

## 41. Serialization

External and persistent serialized representations are contracts.

They SHOULD be:

- explicit;
- versionable;
- testable;
- independent of accidental in-memory representation where long-term compatibility matters.

Java native object serialization SHOULD NOT be introduced for new external or persistent contracts without explicit justification and security review.

Deserializing untrusted data MUST be treated as security-sensitive.

---

## 42. Reflection and dynamic access

Reflection, dynamic proxies, bytecode manipulation, and deep framework magic MAY be used when a concrete requirement justifies them.

They SHOULD be isolated behind a small boundary.

Reflective code MUST have tests covering failure modes that the compiler cannot verify.

Do not use reflection to avoid ordinary type design.

---

# Part XIV — Dependencies

## 43. Third-party dependencies

A dependency is an architectural and operational commitment.

New or materially changed dependencies MUST follow the project's dependency-admission process.

Within Java code:

- expose third-party types publicly only when that dependency is intentionally part of the contract;
- otherwise keep third-party details behind appropriate boundaries;
- avoid duplicating a mature library feature with custom code without a reason;
- avoid large dependencies for trivial functionality when maintenance/security cost is disproportionate.

Unused dependencies MUST be removed when discovered within authorized scope.

---

# Part XV — Layer-specific Java rules

The following sections apply only when the corresponding layer exists in the accepted architecture.

They define quality expectations, not mandatory architecture.

## 44. Domain/core layer

Domain/core Java SHOULD:

- express owned concepts and invariants directly;
- remain independent of transport and persistence representation;
- avoid framework dependencies unless the accepted architecture explicitly allows them;
- avoid public setters that bypass invariants;
- keep infrastructure exceptions/types out of domain contracts;
- model meaningful value distinctions with types where useful;
- use deterministic logic whenever possible.

Domain code MUST NOT absorb behavior owned by another bounded context/component merely for convenience.

---

## 45. Application/use-case layer

Application code SHOULD:

- orchestrate accepted use cases;
- depend on domain/application contracts rather than transport details;
- coordinate dependencies explicitly;
- define transaction/use-case boundaries where the architecture assigns them;
- translate failures only when crossing meaningful boundaries;
- keep HTTP, persistence schema, and framework mechanics out of use-case contracts unless intentionally part of the architecture.

Application services SHOULD remain focused on use-case orchestration rather than becoming general-purpose managers.

---

## 46. Ports/contracts/API layer

Java contracts SHOULD:

- expose the minimum capability required;
- use stable, meaningful names;
- avoid leaking implementation-only types;
- define absence/nullness clearly;
- define failure semantics clearly;
- preserve compatibility commitments.

Do not add methods “for future use.”

Contract changes MUST follow project change control when externally or cross-component observable.

---

## 47. Persistence layer

Persistence Java SHOULD:

- isolate database/driver/ORM specifics from domain/application contracts;
- map explicitly between persistence representation and owned model;
- make cardinality assumptions clear;
- make transaction expectations clear;
- avoid leaking database exceptions across inappropriate boundaries;
- avoid relying on implicit database behavior that is semantically important;
- use schema/migrations as persistence authority;
- treat query performance as a requirement-driven concern.

Persistence code MUST NOT silently invent domain rules that belong elsewhere.

Persist enums and identifiers using stable explicit representations.

When queries depend on uniqueness, ordering, locking, or transaction isolation for correctness, that dependency MUST be explicit in code, schema, tests, or authoritative documentation as appropriate.

---

## 48. Transport/adapters layer

HTTP, messaging, CLI, RPC, and other adapters SHOULD:

- parse transport-specific input;
- perform syntactic/boundary validation;
- translate transport models to application/domain contracts;
- translate known failures to stable transport semantics;
- prevent framework/transport types from leaking inward unnecessarily;
- avoid exposing stack traces/internal exceptions to untrusted clients;
- keep protocol-specific status/error mapping at the boundary.

Adapters MUST NOT duplicate domain invariants as separate business truth.

---

## 49. Runtime/configuration/composition layer

Composition code SHOULD:

- construct and wire dependencies;
- validate required configuration early;
- keep construction separate from business behavior;
- make lifecycle ownership clear;
- make external resource ownership clear;
- avoid hidden service-locator/global state patterns.

Configuration failures SHOULD occur during startup or boundary initialization where practical rather than much later in business execution.

---

# Part XVI — Testing

## 50. Test quality

Tests are production engineering artifacts.

Tests MUST be:

- deterministic;
- readable;
- independently executable unless the test category explicitly requires shared environment;
- self-validating;
- repeatable.

Tests MUST NOT depend on execution order.

---

## 51. Test observable behavior

Prefer tests that prove:

- public behavior;
- invariants;
- boundary behavior;
- important failure paths;
- compatibility;
- architecture constraints.

Avoid asserting incidental internal call sequences unless those interactions are themselves part of the contract.

Refactoring SHOULD NOT require widespread test rewrites when observable behavior has not changed.

---

## 52. Unit tests

Unit tests SHOULD:

- run quickly;
- isolate the behavior under test;
- avoid network/filesystem/database dependencies unless those are the unit's actual responsibility;
- use real value objects where mocks add no value;
- mock/fake at meaningful boundaries rather than every collaborator.

Do not test language/framework behavior that the project does not own.

---

## 53. Integration tests

Integration tests SHOULD prove real integration behavior where fakes would hide important risk.

Examples include:

- database mappings;
- transactions;
- serialization;
- framework wiring;
- external protocol adapters.

Integration environments SHOULD be reproducible.

Tests MUST clean up or isolate state sufficiently to remain repeatable.

---

## 54. Architecture tests

Projects with explicit architectural boundaries SHOULD enforce stable, mechanically expressible rules with **ArchUnit** or equivalent tooling.

Examples:

- dependency direction;
- forbidden cross-module dependencies;
- cycles;
- package access;
- adapter/domain separation.

Architecture tests MUST reflect accepted architecture.

They MUST NOT invent new architecture merely because a rule is easy to encode.

---

## 55. End-to-end tests

End-to-end tests SHOULD be reserved for flows whose integrated behavior cannot be established sufficiently by lower-level tests.

They SHOULD prove high-value outcomes and contracts rather than duplicate every unit-level case.

Failures SHOULD provide enough diagnostics to identify the failing boundary.

---

## 56. Test naming and structure

Test names SHOULD describe behavior and expected outcome.

Use arrange/act/assert or equivalent conceptual structure when it helps readability, but do not require ceremonial comments.

Parameterized tests SHOULD be used when multiple inputs prove the same rule more clearly than duplicated tests.

---

## 57. Time and asynchronous tests

Tests SHOULD avoid arbitrary sleeps.

Use:

- deterministic clocks;
- latches;
- explicit conditions;
- bounded polling;
- test scheduler/executor controls

where appropriate.

All asynchronous waits MUST have finite timeouts.

---

# Part XVII — Performance

## 58. Performance rule

Performance optimization MUST be requirement- or evidence-driven when it adds complexity.

Before introducing a non-obvious optimization:

- identify the bottleneck;
- measure a representative baseline;
- define the target;
- validate the result;
- check correctness regressions.

Do not sacrifice clear code for micro-optimizations without evidence.

Known complexity hazards such as accidental unbounded work, N+1 queries, or quadratic behavior on expected large inputs SHOULD be addressed when relevant even before production measurement.

---

# Part XVIII — Generated code

## 59. Generated source ownership

Generated Java SHOULD be treated as derived output.

The generator/configuration owns generated-source correctness.

Do not manually edit generated files unless the project's generation model explicitly permits it.

Style/static-analysis tools SHOULD either:

- validate generated code that is intended to meet the same standard; or
- exclude generated code explicitly and validate the generator/source contract instead.

Generated-code exclusions MUST be deliberate rather than broad path-based loopholes for hand-written code.

---

# Part XIX — Compatibility and evolution

## 60. API compatibility

When Java APIs are externally or cross-component consumed, changes MUST consider applicable:

- source compatibility;
- binary compatibility;
- serialization compatibility;
- behavioral compatibility.

Do not change a public signature merely to improve local aesthetics when compatibility is part of the contract.

Deprecation SHOULD include:

- replacement guidance;
- migration path where needed;
- removal policy when the project has one.

---

## 61. Persistence compatibility

Persistence model changes MUST follow schema/migration authority.

Java model changes MUST NOT silently redefine durable data semantics.

Forward/backward compatibility requirements SHOULD be explicit when multiple application versions may coexist.

---

# Part XX — Refactoring

## 62. Refactoring rule

Refactoring preserves intended behavior while improving structure.

A refactor SHOULD:

- have a concrete quality purpose;
- remain within authorized scope;
- preserve or improve validation;
- avoid combining unrelated semantic changes.

Small local refactors needed to implement a change safely MAY be included.

Large cleanup, global formatting, or broad package/API restructuring SHOULD be separate work.

---

## 63. Touched-code rule

Touched code SHOULD NOT be made materially less maintainable by the current change.

Touching a file does not authorize bringing the entire file or class into compliance.

Pre-existing violations are corrected only when they are:

- required by the accepted outcome;
- minimum required enabling work;
- small and directly related within the project's accepted migration mode; or
- separately authorized.

Nearby improvements MUST NOT create unrelated diff churn.

---

# Part XXI — Enforcement model

## 64. Quality ladder

A Java project SHOULD build a deterministic validation ladder approximately like:

1. project-pinned source-format/convention checks;
2. compilation;
3. Error Prone / correctness static analysis;
4. nullness analysis where adopted;
5. focused unit tests;
6. component/integration tests;
7. architecture tests;
8. contract/schema tests where relevant;
9. end-to-end tests where relevant;
10. project-wide check;
11. diff/scope review.

The exact commands belong in the Project Profile.

A later green gate MUST NOT hide an earlier unresolved failure.

---

## 65. Mechanical versus review-only rules

Automate rules that can be checked reliably.

Examples of good mechanical enforcement:

- formatting;
- imports;
- compilation;
- common correctness bugs;
- selected nullness rules;
- architecture dependencies;
- tests;
- generated-source consistency.

Keep human/agent review for semantic concerns such as:

- responsibility;
- naming quality;
- abstraction value;
- ownership;
- domain invariant placement;
- API minimality;
- failure semantics;
- scope creep.

Do not encode subjective preferences as hard CI failures unless the project has deliberately accepted them.

---

## 66. Suppression policy

Suppressions are exceptions to enforcement, not normal design tools.

Every suppression SHOULD be:

- narrow;
- justified;
- local;
- removable when the cause disappears.

Broad package/module-wide suppressions require explicit project justification.

A suppression MUST NOT hide a correctness failure whose risk is not understood.

---

# Part XXII — Java review gate

## 67. Pre-completion review

Before Java work is considered complete, verify as applicable:

### Scope
- [ ] Every Java change belongs to the accepted outcome or minimum enabling work.
- [ ] No unrelated cleanup was included.
- [ ] No speculative abstraction or dependency was added.

### Source
- [ ] Hand-written Java follows this standard's source conventions.
- [ ] The project's pinned formatting/import gates pass.
- [ ] Naming communicates intent.
- [ ] Comments/Javadoc add useful information.
- [ ] No dead/commented-out code or untracked correctness TODOs were introduced.

### Correctness
- [ ] Compilation passes.
- [ ] Required static analysis passes.
- [ ] Nullness is explicit where required.
- [ ] No unexpected failures are swallowed.

### Design
- [ ] Public visibility is minimal.
- [ ] Types represent important concepts clearly.
- [ ] Constructors/factories establish valid state and required invariants.
- [ ] Required invariants remain preserved by observable operations.
- [ ] Mutable state is justified and owned.
- [ ] No unnecessary abstraction was introduced.

### APIs
- [ ] Public/cross-component contracts are minimal.
- [ ] Absence and failure semantics are clear.
- [ ] Compatibility impact was considered.

### Boundaries
- [ ] Domain/core is free of accidental adapter/persistence leakage.
- [ ] Application code owns orchestration rather than transport/persistence mechanics.
- [ ] Persistence details remain in persistence boundaries.
- [ ] Transport/framework details remain in adapters.
- [ ] Runtime wiring remains separate from business behavior.

### Resources/concurrency
- [ ] Resource ownership is explicit.
- [ ] Closeable resources are reliably closed.
- [ ] Concurrency exists only for a concrete reason.
- [ ] Thread-safety/cancellation semantics are clear where applicable.

### Security
- [ ] Security-sensitive changes use applicable safe APIs and validation.
- [ ] No secrets/sensitive data were introduced into logs/code/tests.
- [ ] Trust-boundary input/output handling is appropriate.

### Tests
- [ ] Changed behavior is adequately tested.
- [ ] Relevant failure paths are tested.
- [ ] Tests are deterministic.
- [ ] Integration/architecture/E2E tests are used only where they add evidence.

### Final
- [ ] Required project-wide validation passes.
- [ ] Diff review confirms one coherent purpose.
- [ ] Remaining known issues are explicit rather than hidden.

---

# Part XXIII — Project Profile

## 68. Purpose

The Project Profile maps this standard to a concrete project.

It SHOULD remain small and reference existing authoritative sources rather than duplicating them.

## 69. Composable Domain Platform Project Profile

This profile specializes the standard for the current accepted repository state.

It intentionally references existing authoritative/executable sources instead of duplicating module ownership, architecture, schema, contract, or build truth.

### 69.1 Java baseline

- **JDK/toolchain:** Java 21, configured by the repository Java convention plugins.
- **Language release:** 21.
- **Platform semantics:** Java SE 21 JLS/JVMS/API specifications apply for Java-platform semantics.
- **Preview features:** Not accepted by this profile. Any introduction requires explicit governed approval.

### 69.2 Source conventions

- **Standard source conventions:** This document, without project-specific source-convention deviations.
- **Formatter:** Not adopted under issue #75.
- **Formatter authority:** None beyond the source rules in this document.
- **Formatting scope:** Review-only for this adoption. No whole-repository mechanical rewrite is authorized.
- **Import/static source checker:** Not adopted under issue #75.
- **Line endings:** No additional Java-specific line-ending rule is introduced by issue #75.
- **External style references:** Appendix C.2 is informative only.

Until a deterministic formatter is separately admitted and pinned, hand-written Java SHOULD remain locally consistent with the surrounding accepted source where this standard does not prescribe a mechanical layout rule.

### 69.3 Static correctness

- **Compiler baseline:** `javac` through the accepted Gradle Java 21 toolchain/release configuration.
- **Additional compiler-warning policy:** No new Java-specific warning gate is introduced by issue #75.
- **Error Prone:** Not adopted under issue #75.
- **Additional Java analyzers:** No new analyzer is adopted under issue #75.

Introducing Error Prone, Checkstyle, SpotBugs, PMD, Sonar rules, custom compiler plugins, or equivalent tooling requires a separate accepted tooling/technology change when not already present in executable repository truth.

### 69.4 Nullness

- **JSpecify annotations:** Not adopted under issue #75.
- **Nullness checker:** Not adopted under issue #75.
- **Migration mode:** No nullness annotation migration is authorized by issue #75.

The nullness rules in Part IV remain the target engineering model. Mechanical adoption requires a separate accepted change that defines scope, dependencies, tooling, migration behavior, and validation.

### 69.5 Architecture

- **Authoritative architecture model:** `docs/architecture/workspace.dsl`.
- **Architecture narrative/boundaries:** `docs/architecture.md`, `docs/modules.md`, applicable module `module.md` files, and accepted ADRs.
- **Architecture verification:** ArchUnit 1.4.2 using the existing repository architecture tests.
- **Current architecture-test locations:** executable tests under the relevant Java components, including platform, Event, Registration, and Event-Registration composition architecture tests.

This profile does not duplicate package/module ownership rules. The architecture authorities above remain the source of truth.

### 69.6 Layer/component mapping

Use accepted repository architecture rather than a second Java-owned layer map:

- core/business-neutral platform code: `platform/core/`;
- business capabilities: `platform/modules/`;
- cross-capability compositions: `platform/compositions/`;
- external adapters/interfaces: `platform/interfaces/`;
- executable composition/runtime: `platform/apps/`;
- executable contracts: `platform/contracts/`.

The exact responsibilities, public/private boundaries, dependency direction, and ownership are defined by architecture/module authorities, not by this directory summary.

### 69.7 Testing

- **Unit/integration framework:** JUnit 5.14.4.
- **Real-infrastructure integration:** Testcontainers 2.0.5 where accepted tests require real infrastructure.
- **Architecture tests:** Existing ArchUnit tests.
- **End-to-end strategy:** Existing repository/application tests as required by accepted work; this profile introduces no new E2E framework.

Tests for an active change MUST follow the applicable work-item validation and `docs/workflow.md`.

### 69.8 Persistence

Where a Java component owns persistence under accepted architecture:

- **Database direction:** PostgreSQL.
- **Migration:** Flyway 12.8.1.
- **SQL access:** jOOQ 3.21.6.
- **Schema/migration authority:** bounded-context-owned Flyway migrations.
- **Persistence ownership:** defined by accepted architecture/module authorities.

This profile does not authorize persistence for components that do not already own it.

### 69.9 Security

- **Project authority:** applicable accepted scope, architecture, contracts, ADRs, and governance.
- **Java-specific baseline:** Parts XI–XIII of this standard when the active change touches those concerns.
- **New security technology:** requires normal technology/architecture admission.

Issue #75 does not introduce an authentication, authorization, secrets-management, cryptography, or security-analysis technology.

### 69.10 Dependencies

Technology admission is governed by `docs/governance.md` and accepted direction by `docs/tech-stack.md`.

Exact dependency/version truth is owned by Gradle build files and the version catalog.

Issue #75 introduces no Java dependency, plugin, formatter, analyzer, annotation library, or test framework.

### 69.11 Validation

**Focused Java validation**

Use the focused Gradle test/check tasks required by the active work item and affected component.

**Project-wide build-affecting validation**

```bash
./gradlew --no-daemon check
```

**Diff validation**

```bash
git diff --check
```

For documentation-only changes that do not affect executable build state, use the documentation/structure gates required by `docs/workflow.md`; do not run Gradle solely because this Java standard exists.

### 69.12 Adoption

- **Mode:** Changed-code / incremental brownfield adoption.
- **New Java:** MUST follow this standard.
- **Materially changed Java:** SHOULD comply within the minimum affected change surface and active migration mode.
- **Existing Java:** Is not implicitly brought into scope merely because this standard is adopted.
- **Baseline inventory:** Issue #75 does not accept an exhaustive inventory of historical Java-standard violations.
- **Whole-repository cleanup:** Not authorized by issue #75.

A future enforcement-tool adoption MUST define how existing files are handled before a whole-file formatter or analyzer can become a required gate.

### 69.13 Explicit adoption deviations

The following `SHOULD` recommendations in the general standard are intentionally not mechanically adopted by issue #75:

- deterministic Java formatter;
- Error Prone or equivalent compile-time bug detector;
- JSpecify annotations/nullness checker.

Reason: issue #75 is a documentation/governance decision only. Introducing new build tooling or dependencies is explicitly outside its scope and remains subject to normal technology/tooling admission.

These are bounded adoption deviations, not a decision that the tools are undesirable. They may be reconsidered through separate accepted work.

---

# Part XXIV — Adoption strategy

## 70. Greenfield projects

Greenfield Java projects SHOULD enable the selected mechanical gates before significant code volume accumulates.

Recommended early baseline:

- this standard's source conventions;
- a pinned deterministic formatter (recommended default: `google-java-format`);
- compiler validation;
- Error Prone;
- JSpecify/nullness analysis where practical;
- unit test framework;
- architecture tests once stable boundaries exist.

Do not add tools before their concern exists.

---

## 71. Existing projects

For an existing codebase:

1. inventory current violations without changing behavior;
2. decide the accepted target standard;
3. choose full/baseline/changed-code adoption;
4. pin Java/tooling baselines where reproducibility matters;
5. treat external guidance as review input rather than an automatic standards update;
6. introduce tooling as a separate coherent change where needed;
7. prevent new violations;
8. migrate legacy violations incrementally;
9. keep behavior changes and large mechanical rewrites separate where practical.

A standards migration MUST NOT become an unbounded rewrite.

---

# Part XXV — Decision rule

## 72. Final engineering rule

When multiple Java implementations satisfy the accepted requirement:

> Choose the simplest implementation that preserves correctness, makes contracts and ownership explicit, fits the accepted architecture, is easy to test, and introduces the least accidental complexity.

When a rule conflicts with accepted architecture or contracts:

> Stop and resolve the authority conflict rather than silently redesigning the system.

When an improvement is useful but not required by the active work:

> Keep it separate unless it is minimum required enabling work.

When a change is proposed primarily because a newer Java feature, style guide, or tool exists:

> Require a concrete correctness, readability, maintainability, compatibility, security, or delivery benefit; novelty alone is not sufficient.

---

# Appendix A — Recommended enforcement responsibilities

**Non-normative summary.** If this appendix conflicts with the normative body, the normative body prevails.

| Concern | Preferred mechanism | Nature |
|---|---|---|
| Formatting | Project-pinned formatter (recommended default: google-java-format) | Mechanical |
| Source conventions not covered by formatter | Project-configured Checkstyle/custom checks | Mechanical where reliable |
| Java compilation | javac/build tool | Mechanical |
| Common correctness bugs | Error Prone | Mechanical |
| Null contracts | JSpecify + compatible analyzer | Mechanical + semantic |
| Unit/integration behavior | JUnit or project equivalent | Executable |
| Architecture boundaries | ArchUnit or equivalent | Executable |
| Domain ownership | Accepted architecture + review | Semantic |
| API minimality | Review | Semantic |
| Exception translation | Tests + review | Mixed |
| Persistence correctness | Integration tests + schema/migrations | Executable + authority |
| Transport mapping | Contract/integration tests | Executable |
| Security | Applicable tooling + review + tests | Risk-based |
| Scope creep | Diff/work-item review | Semantic |

No tool is the single authority for Java quality.

---

# Appendix B — Tool adoption principles

A tool SHOULD be adopted when:

- it catches an accepted class of defect or inconsistency;
- its signal is sufficiently reliable;
- it can run deterministically;
- its maintenance cost is proportionate;
- its failure output is actionable.

A tool SHOULD NOT be adopted merely because it has many rules.

When multiple tools cover the same concern, designate one as canonical or configure them to agree.

---

# Appendix C — External references

## C.1 Normative Java platform authorities

The applicable version is determined by the project's accepted Java baseline.

These sources are normative only for the Java/platform semantics they define:

- Java Language Specification (JLS)
  https://docs.oracle.com/javase/specs/

- Java Virtual Machine Specification (JVMS)
  https://docs.oracle.com/javase/specs/

- Java SE / JDK API Specification
  https://docs.oracle.com/en/java/

- Other official Java/JDK specifications used by the project
  https://docs.oracle.com/en/java/

A project's Project Profile SHOULD record the concrete Java release/specification baseline rather than relying on whichever release is newest.

## C.2 Reviewed engineering/style guidance

The following are informative sources used to derive, compare, or review rules in this standard. They are not live normative authorities and do not change this standard automatically.

- Google Java Style Guide
  https://google.github.io/styleguide/javaguide.html

- OpenJDK Java Style Guidelines — Draft v6 (December 2015)
  https://cr.openjdk.org/~alundblad/styleguide/index-v6.html

- OpenJDK Developers' Guide
  https://openjdk.org/guide/

- Cornell Java Style Guide
  https://www.cs.cornell.edu/courses/JavaAndDS/JavaStyle.html

These sources intentionally differ on some details such as indentation, line length, documentation style, and historical source-encoding advice. This standard incorporates selected rules deliberately rather than treating consensus or recency as automatic authority.

## C.3 Tooling and specialist references

These references support implementation and enforcement. They do not override the normative body of this document or accepted project authority.

- google-java-format
  https://github.com/google/google-java-format

- Error Prone
  https://errorprone.info/
  https://github.com/google/error-prone

- JSpecify Nullness Specification and User Guide
  https://jspecify.dev/docs/spec/
  https://jspecify.dev/docs/user-guide/

- ArchUnit User Guide
  https://www.archunit.org/userguide/html/000_Index.html

- Checkstyle
  https://checkstyle.org/

- Java SE Security Developer's Guide
  https://docs.oracle.com/en/java/javase/21/security/

- OWASP Java Security Cheat Sheet
  https://cheatsheetseries.owasp.org/cheatsheets/Java_Security_Cheat_Sheet.html

## C.4 External-reference maintenance rule

When an external source changes:

1. do not silently change this standard;
2. review whether the new guidance reveals a correctness, maintainability, security, compatibility, or tooling issue;
3. verify the guidance against the applicable Java platform baseline;
4. adopt it only through a deliberate standard/Profile revision;
5. update pinned tools and validation together when mechanical behavior changes.

---

# Appendix D — Short agent rule

**Non-normative operational summary.** If this appendix conflicts with the normative body, the normative body prevails.

An agent applying this standard should:

1. verify applicable project authority;
2. preserve accepted architecture and ownership;
3. keep the change inside scope;
4. produce hand-written Java that follows this standard's source conventions;
5. use explicit contracts and meaningful types;
6. avoid ambiguous nullness;
7. prefer immutability and explicit dependencies;
8. keep infrastructure concerns at their boundaries;
9. handle errors/resources/concurrency deliberately;
10. add evidence for changed behavior;
11. run applicable mechanical gates;
12. report remaining violations instead of hiding them;
13. stop once the accepted outcome is satisfied.
