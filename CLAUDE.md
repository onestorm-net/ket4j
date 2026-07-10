# ket4j — Kendo Error Tracker for Java

Java port of [kendo-error-tracker](https://github.com/script-development/kendo-error-tracker) (PHP/Laravel).
Sends sanitized exception reports to kendo's ingestion API from Java applications via Log4j2.

## Major Update Workflow

For updates that change a lot of the public API surface, use this pattern instead of working
straight on `main`:

- Work on a dedicated branch named `development-<fitting-name>`, branched from `development`
  (not `main` — see Branching below).
- Create `documents/<fitting-name>/DECISIONS.md` — describes the update: what it is, the goal,
  the decisions made along the way, and any findings/tradeoffs discovered during implementation.
  Keep it updated as decisions are actually made, not just written once up front — it's the
  source of truth for *why* the update ended up the way it did.
- Create `documents/<fitting-name>/TASKS.md` — derived from the plan for the update; a list of
  smaller tasks to work through one by one to reach the goal described in `DECISIONS.md`.
  - Number every task so it can be referenced (e.g. "see 2.4"). Group related tasks under a
    shared prefix (e.g. `2.1`–`2.10` for a batch of sanitizer rewrites) rather than one flat
    list. Keep the `- [ ]` checkboxes.
  - Splitting groups per module (core, log4j2, ...) is allowed when it aids clarity, but not
    required — don't split further than the update actually needs.
  - Don't include a wrap-up/finalization group — that's standing process, covered below, not
    specific to any one update.
- Before opening a PR from `development-<name>` back to `development`, always:
  - Run `mvn verify` and fix any coverage gaps back to 100% line coverage.
  - Bump the version in the root `pom.xml` and both modules' `<parent>` blocks — major for
    breaking API changes, minor/patch otherwise (semver). This is the version that will
    eventually ship from `main`; get it right here so it doesn't need revisiting later. CI
    refuses to deploy over an unchanged version as a backstop, but don't rely on that — decide
    the right bump deliberately.
  - Update this file's architecture documentation (e.g. "What to Build", package structure) so
    it describes the new design instead of the one the update replaced.
  - Update `README.md` if it shows usage/config examples referencing the changed API.

### Branching: `development-<name>` → `development` → `main`

- `development-<name>` branches PR into `development`, not `main`. Every push to `development`
  triggers a CI deploy of a build-numbered pre-release (`<version>-BUILD.<run id>`) — see CI/CD
  below — so a WIP feature can be pulled into a downstream app and tested before it ships for
  real.
- `development` PRs into `main` to actually cut a release. Merging to `main` deploys the exact
  version in `pom.xml` (no build suffix) — it should already be correct from whichever
  `development-<name>` merge last bumped it.
- **Lesson learned:** the very first version of this project's `development-<name>` workflow PR'd
  straight into `main`, and got merged before its version bump landed — silently overwriting a
  previously published release's artifacts under the same version number. That's exactly what the
  `development` staging step and the CI refuse-to-overwrite guard now exist to catch.

## Modules

| Module | Package | Purpose |
|---|---|---|
| `ket4j-core` | `net.onestorm.ket4j` | Config, sanitizers, HTTP client, provider |
| `ket4j-log4j2` | `net.onestorm.ket4j.log4j2` | Log4j2 `AbstractAppender` that delegates to core |

## API Contract (KD-0771)

```
POST {kendoUrl}/api/projects/{projectId}/error-events
Authorization: Bearer {token}
Content-Type: application/json
Accept: application/json

{
  "environment":     "production",        // required
  "release":         "1.2.3",             // optional — omit when unset
  "exception_class": "java.lang.NullPointerException", // required — always a real FQCN, never empty
  "message":         "...",
  "stack_trace":     "..."                // required — empty string allowed
}

→ 202 Accepted  (only success)
→ anything else  log locally, swallow
```

`exception_class` uses Laravel's `required` rule server-side, which rejects `null` **and** empty string —
there is no accommodation for a throwable-less event. `ErrorTracker.report(ErrorEvent)` therefore skips
sending entirely when `event.getThrowable()` is null, rather than inventing a placeholder value.

## What to Build

### ket4j-core

**`ErrorTrackerConfiguration`** — immutable config object (builder pattern):
- `kendoUrl` (String, required)
- `projectId` (String, required)
- `token` (String, required)
- `environment` (String, default `"production"`)
- `release` (String, nullable — omit from payload when null)
- `connectTimeoutSeconds` (double, default 2.0, must be > 0)
- `timeoutSeconds` (double, default 5.0, must be > 0)
- `sanitizers` (List<Sanitizer>, default: all nine built-in sanitizers in order — see below)

**`ErrorEvent`** — interface for the mutable, sanitizable unit of work. Carries everything a
logger module knows about one log event:
- `getMessage()` / `setMessage(String)` — what the developer logged
- `getThrowable()` — the original throwable, if any; **read-only**, not settable
- `getExceptionMessage()` / `setExceptionMessage(String)` — the top-level throwable's own message
  (`""` if none/null)
- `getStackTrace()` / `setStackTrace(String)` — the full rendered throwable text (header, frames,
  entire `Caused by:` chain); this is the only one of the three text fields actually transmitted
- Implemented per logger module: `ket4j-log4j2`'s `Log4j2ErrorEvent` wraps a Log4j2 `LogEvent`;
  a future `ket4j-logback` module would implement it against its own event type.

**`Sanitizer`** — a single-method functional interface:
- `void sanitize(ErrorEvent event)` — mutates the event's fields in place; no return value

All sanitizers live in `net.onestorm.ket4j.sanitizer`.
Nine built-in regex-based sanitizers (in default list) apply their transform to `message`,
`exceptionMessage`, and `stackTrace` via the shared `net.onestorm.ket4j.util.ErrorEventUtil`:
- `JwtSanitizer` — replaces `eyJ<b64>.<b64>.<b64>` with `[REDACTED:jwt]`
- `BearerTokenSanitizer` — two-pass *within a single field*: replace `Bearer <token>`, then any bare reuse of that credential → `[REDACTED:bearer]`
- `DsnPasswordSanitizer` — strips password from `scheme://user:pass@host` → `scheme://user:[REDACTED:dsn-password]@host`
- `StripeApiKeySanitizer` — redacts Stripe live secret keys (`sk_live_...`) → `[REDACTED:api-key]`
- `AwsApiKeySanitizer` — redacts AWS access key IDs (`AKIA...`) → `[REDACTED:api-key]`
- `Ipv4Sanitizer` — replaces IPv4 addresses (each octet bounded 0–255) with `[REDACTED:ip]`
- `EmailSanitizer` — replaces email addresses with `[REDACTED:email]`
- `BsnSanitizer` — replaces Dutch BSN numbers validated by elfproef with `[REDACTED:bsn]`
- `PathSanitizer` — strips a configurable base path prefix; also redacts OS usernames in `/home/<user>/` and `/Users/<user>/` paths → `[REDACTED:user]`

One built-in throwable-aware sanitizer (NOT in default list — opt-in):
- `SqlExceptionSanitizer` — if the throwable chain contains a `SQLException`, sets
  `exceptionMessage` to `ExceptionClass [SQLSTATE x] [error code y]` (falls back to `"unknown"`
  if SQLSTATE is absent) **and** replaces every occurrence of that `SQLException`'s original
  message text inside `stackTrace` with the same fingerprint — dropping embedded SQL and bound
  values from both places. Leaves the developer's `message` untouched.

`ErrorTracker.report(ErrorEvent event)` first checks `event.getThrowable()`; if null, it returns
immediately — no sanitization, no HTTP call. Otherwise it sanitizes in a single pass: runs every
configured sanitizer's `sanitize(event)` in order, each free to touch whichever of `message`,
`exceptionMessage`, and `stackTrace` are relevant to it.

Default sanitizer order: JWT → Bearer → DSN → Stripe → AWS → IPv4 → Email → BSN → Path
(structured/narrower secrets first to avoid partial leakage into looser patterns)

`PathSanitizer` takes a `basePath` in its constructor. If `basePath` is null/blank, only username redaction runs.
Users configure which sanitizers to use (and in what order) via `ErrorTrackerConfiguration.sanitizers`.
The default list includes all nine regex sanitizers. `SqlExceptionSanitizer` must be added explicitly by applications that use a database.

**`net.onestorm.ket4j.util`** — static-helper utility classes (see Code Style's utility-class convention):
- `ExceptionUtil` — turns a `Throwable` into `exceptionMessage`/`stackTrace` strings (`""` when
  the throwable is null); used by `ErrorEvent` implementations to populate those fields
- `ErrorEventUtil` — `applyToTextFields(ErrorEvent, UnaryOperator<String>)` applies a transform to
  `message`, `exceptionMessage`, and `stackTrace` uniformly (nulls treated as `""`); backs the 9
  regex sanitizers so none of them repeat that plumbing

**`ErrorTrackerProvider`** — static singleton provider:
```java
ErrorTrackerProvider.initialize(ErrorTrackerConfiguration config); // call once at app startup
ErrorTracker tracker = ErrorTrackerProvider.getInstance();         // used by all appenders
```
Throws `IllegalStateException` if `getInstance()` called before `initialize()`.

**`ErrorTracker`** — core logic:
- `void report(ErrorEvent event)` — returns immediately if `event.getThrowable()` is null (kendo's
  `exception_class` is required server-side and never accepts an empty value); otherwise runs
  single-pass sanitization and builds the wire JSON straight from the sanitized event plus config
  (no intermediate payload object), calls `send()`; never throws (swallow-on-failure)
- `void send(String exceptionClass, String message, String stackTrace)` — private; performs the
  HTTP POST; never throws. `exceptionClass` comes from `event.getThrowable()`'s class name;
  `environment`/`release` are read from `ErrorTrackerConfiguration` directly inside
  `send()`/`buildJson()`
- HTTP client: `java.net.http.HttpClient` (JDK built-in, no extra dep)
- JSON serialization: manual — private `escapeJson(String)` helper (handle `"`, `\`, `\n`, `\r`, `\t`, control chars); payload is a flat 5-field object, no library needed
- Timeouts: `connectTimeoutSeconds` and `timeoutSeconds` from config
- Package-private constructor — instantiated only by `ErrorTrackerProvider`
- **ket4j-core has zero external dependencies**

### ket4j-log4j2

**`Log4j2ErrorEvent`** — `ErrorEvent` implementation wrapping a Log4j2 `LogEvent`: extracts the
formatted message and `Throwable` from the event, and uses core's `ExceptionUtil` to populate
`exceptionMessage`/`stackTrace`.

**`KendoErrorAppender`** — extends `AbstractAppender`:
- No hardcoded level threshold. `append()` calls the inherited `isFiltered(event)` (from
  `AbstractFilterable`, which `AbstractAppender` extends) so the `Filter` already accepted via the
  `@PluginElement("Filter")` factory parameter is actually honored — previously that parameter was
  accepted but never applied. Users who want a level floor configure a standard Log4j2
  `<ThresholdFilter level="WARN"/>` (or any other `Filter`) inside `<KendoError>` in their XML
  config; with no filter attached, every level reaches the appender.
- Skips events without a `Throwable` before building a `Log4j2ErrorEvent` at all (redundant with
  `ErrorTracker.report()`'s own null-throwable check, but avoids the pointless work of rendering a
  stack trace that will never be sent)
- Builds a `Log4j2ErrorEvent` from the incoming `LogEvent` and calls
  `ErrorTrackerProvider.getInstance().report(event)`
- Wraps in try/catch — appender must never throw
- Configured via Log4j2 XML (standard `@Plugin` + `@PluginFactory`)

**Plugin discovery requires the Log4j2 annotation processor to actually run at compile time.**
`ket4j-log4j2/pom.xml` registers `log4j-core` as a `maven-compiler-plugin`
`annotationProcessorPath`, which generates
`META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat` in the jar — that file
is how Log4j2 finds `@Plugin`-annotated classes like `KendoErrorAppender` declaratively. Without
it, Log4j2 falls back to (deprecated, unreliable) package scanning and fails with `Appenders
contains an invalid element or attribute "KendoError"` / `Unable to locate appender "Kendo"`.
Relying on implicit classpath-based annotation processing doesn't work here — recent javac
versions (this project targets Java 25) don't reliably run annotation processors found only on
the compile classpath without an explicit `annotationProcessorPaths` entry.
`KendoErrorAppenderPluginDiscoveryTest` parses a real XML config with `<KendoError/>` and asserts
it resolves, specifically to catch a regression here.

## Sanitizer Regex Patterns (from PHP source, PR #14)

```
JWT:          eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+
Bearer:       Bearer[ \t]+([A-Za-z0-9._~+\-]+=*)                        (case-insensitive)
DSN password: ([a-zA-Z][a-zA-Z0-9+.\-]*://[^:/?#\s@]+:)([^@/?#\s]+)(@) (replace group 2 only)
Stripe key:   \bsk_live_[A-Za-z0-9]{10,}\b
AWS key:      \bAKIA[0-9A-Z]{16}\b
IPv4:         \b(?:(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\b
Email:        [\p{L}\p{N}._%+\-]+@[\p{L}\p{N}.\-]+\.[\p{L}]{2,}
BSN grouped:  \d{3}[.\s\-]\d{3}[.\s\-]\d{3}                             (validate with elfproef)
BSN run:      \d{9,}                                                      (scan 9-digit windows, validate with elfproef)
Path/home:    /home/[^/\s]+/  →  /home/[REDACTED:user]/
Path/Users:   /Users/[^/\s]+/ →  /Users/[REDACTED:user]/
```

`BearerTokenSanitizer` two-pass: first replace `Bearer <token>`, then replace any bare reuse
of the captured credential value elsewhere in the string.

## Design Principles

- **Swallow-on-failure** — `report()` and `send()` never throw; catch all `Exception`/`Throwable`,
  log via `java.util.logging.Logger` (or `StatusLogger` in log4j2 module), and return.
- **Never block** — use connect + read timeouts from config; a hung kendo host must not stall the caller.
- **Sanitize before send** — run all sanitizers against the `ErrorEvent` synchronously in `report()`, before building the wire JSON.
- **Missing config short-circuit** — if `kendoUrl`, `projectId`, or `token` is blank, log and skip the POST.
- **No-throwable short-circuit** — `report()` skips entirely (no sanitization, no HTTP call) when
  `event.getThrowable()` is null. kendo's `exception_class` field is `required` server-side with no
  allowance for null/empty, so there's no valid payload to send for a throwable-less log event.

## Testing

- **JUnit 5** (`org.junit.jupiter:junit-jupiter`) — `@Test`, `@ParameterizedTest`
- **AssertJ** (`org.assertj:assertj-core`) — `assertThat(...)` style assertions
- **JaCoCo** (`jacoco-maven-plugin`) — coverage reports + build enforcement
- JUnit and AssertJ declared in parent `dependencyManagement` (test scope); child modules add them without `<version>`
- `maven-surefire-plugin` 3.5.2+ in parent `<pluginManagement>` for JUnit 5 support
- `jacoco-maven-plugin` in parent `<build><plugins>` so all modules inherit it:
  - `prepare-agent` goal bound to `initialize` phase
  - `report` goal bound to `verify` phase — XML only (no HTML)
  - `check` goal with 100% line coverage minimum — fails build if not met
- Run with `mvn verify` — tests + coverage check in one command
- For missed lines: use IntelliJ's built-in coverage runner; the `check` failure shows which class fell short

Test coverage targets:
- Each sanitizer — valid matches redacted, non-matches left alone, multiple occurrences, edge cases
- `PathSanitizer` — prefix stripped, pass-through when basePath null/blank
- `ErrorTrackerConfiguration` builder — blank required fields rejected, defaults applied

`TestErrorEvent` (`ket4j-core/src/test/java/net/onestorm/ket4j/TestErrorEvent.java`) is a shared
mutable `ErrorEvent` test double — construct with `new TestErrorEvent(message)` or
`new TestErrorEvent(message, throwable)` (the latter derives `exceptionMessage`/`stackTrace` via
`ExceptionUtil`, matching what a real logger-module implementation would do). Sanitizer and
`ErrorTracker` tests both use it rather than each rolling their own `ErrorEvent` implementation.

## Code Style

- **No `var`** — always use explicit types (`List<String> tokens = new ArrayList<>()`, not `var tokens = ...`)
- **No wildcard imports** — always use explicit imports (`import java.util.List`, not `import java.util.*`)
- **Declare with interfaces** — use the interface type on the left-hand side (`List<String>`, `Map<K,V>`) and the concrete type only on the right (`new ArrayList<>()`, `new HashMap<>()`)
- **Descriptive variable names** — no `pass1`, `pass2`; name variables by what they hold (`result`, `tokens`, `matcher`)
- **Utility classes** — a class with only static helper methods is a util class: suffix its name with `Util` (e.g. `ExceptionUtil`, not `ExceptionRenderer`) and place it in a `util` subpackage (e.g. `net.onestorm.ket4j.util`)

## Maven Conventions

### Parent pom.xml — version properties
All external dependency versions go in `<properties>` as `<artifactId>.version`:
```xml
<log4j2-core.version>2.26.0</log4j2-core.version>
<junit-jupiter.version>5.12.2</junit-jupiter.version>
<assertj-core.version>3.27.3</assertj-core.version>
<jacoco-maven-plugin.version>0.8.13</jacoco-maven-plugin.version>
<maven-surefire-plugin.version>3.5.6</maven-surefire-plugin.version>
```

### Parent pom.xml — dependencyManagement
All external deps declared in parent `<dependencyManagement>` using version properties.
Child modules reference them without a `<version>` tag.
`ket4j-core` has no external compile dependencies — only `ket4j-log4j2` depends on log4j2-core.
Test deps (junit-jupiter, assertj-core) apply to all modules.

### Java version
Java 25. `maven.compiler.source` and `maven.compiler.target` are already set to `25` in the parent pom.

## Package Structure

```
ket4j-core/src/main/java/net/onestorm/ket4j/
├── ErrorTracker.java
├── ErrorTrackerConfiguration.java
├── ErrorTrackerProvider.java
├── ErrorEvent.java              (interface — message/throwable/exceptionMessage/stackTrace)
├── sanitizer/
│   ├── Sanitizer.java              (interface — void sanitize(ErrorEvent))
│   ├── JwtSanitizer.java
│   ├── BearerTokenSanitizer.java
│   ├── DsnPasswordSanitizer.java
│   ├── StripeApiKeySanitizer.java
│   ├── AwsApiKeySanitizer.java
│   ├── Ipv4Sanitizer.java
│   ├── EmailSanitizer.java
│   ├── BsnSanitizer.java
│   ├── PathSanitizer.java
│   └── SqlExceptionSanitizer.java
└── util/
    ├── ExceptionUtil.java          (Throwable -> exceptionMessage/stackTrace)
    └── ErrorEventUtil.java         (applyToTextFields helper for regex sanitizers)

ket4j-log4j2/src/main/java/net/onestorm/ket4j/log4j2/
├── KendoErrorAppender.java
└── Log4j2ErrorEvent.java        (ErrorEvent implementation wrapping a Log4j2 LogEvent)
```

## CI/CD (`.github/workflows/ci.yml`)

**On every push and PR** — `test` job runs `mvn -B -e clean verify`; the build fails if any test fails or JaCoCo line coverage drops below 100%.

**On push to `main` only** — `deploy` job runs after `test` passes:
1. `actions/setup-java` writes a `~/.m2/settings.xml` `<server>` entry for the
   `onestorm` id, sourcing username/password from the `MAVEN_USERNAME`/
   `MAVEN_PASSWORD` env vars set on the deploy step (`github` / `secrets.REPOSILITE_TOKEN`).
2. `mvn -U -B -e clean deploy` — builds all modules and deploys straight to the Reposilite
   `maven-public` repository at `https://repo.onestorm.net/maven-public/` (see
   `distributionManagement` in the root `pom.xml`). No local staging directory, no rsync.

**On push to `development` only** — `deploy-development` job runs after `test` passes, mirroring
`deploy` except:
1. Before building, reads the current `project.version` (`mvn help:evaluate`) and rewrites it to
   `<version>-BUILD.<github.run_number>` via `mvn versions:set` — short and human-readable
   (`2.0.0-BUILD.47`) rather than the long, opaque `run_id`. `run_number` is a per-workflow
   counter shared across every trigger of `ci.yml` (pushes to any branch, all PRs), so development
   build numbers will have gaps rather than a clean 1, 2, 3, ... — fine for a low-traffic repo, and
   still guarantees uniqueness so this pre-release never collides with a real release. This
   rewrite is local to the CI checkout — it's never committed.
2. Same deploy step as `deploy`, publishing the build-numbered version instead.

Every `development` push permanently adds a new version to the repo — there's no cleanup/pruning
mechanism. Acceptable for a low-traffic personal repo; revisit if it grows unwieldy.

**Overwrite protection** lives on the Reposilite server, not in CI: the `maven-public` repository
is configured there to reject redeployment of an artifact/version that already exists. `mvn
deploy` simply fails with an HTTP error if someone forgets the version bump — this replaced the
old CI-side rsync `--dry-run` guard now that artifacts are deployed straight to Reposilite instead
of rsynced to a bare directory.

### Required GitHub Actions secrets
| Secret | Purpose |
|---|---|
| `REPOSILITE_TOKEN` | Password for the `github` user on the Reposilite server (`repo.onestorm.net`), used as `MAVEN_PASSWORD` for `mvn deploy` |

### Maven deploy plugin
Declared in parent `<pluginManagement>` (version `3.1.4`), no extra configuration needed — the
target repository comes from `distributionManagement` in the root `pom.xml`, which points at the
Reposilite `onestorm` repository (`https://repo.onestorm.net/maven-public/`). Both release and
development-build versions deploy into this same repository.
