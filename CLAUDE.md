# ket4j — Kendo Error Tracker for Java

Java port of [kendo-error-tracker](https://github.com/script-development/kendo-error-tracker) (PHP/Laravel).
Sends sanitized exception reports to kendo's ingestion API from Java applications via Log4j2.

## Major Update Workflow

For updates that change a lot of the public API surface, use this pattern instead of working
straight on `main`:

- Work on a dedicated branch named `development-<fitting-name>`.
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
- Before merging a `development-<name>` branch back to `main`, always:
  - Run `mvn verify` and fix any coverage gaps back to 100% line coverage.
  - Update this file's architecture documentation (e.g. "What to Build", package structure) so
    it describes the new design instead of the one the update replaced.
  - Update `README.md` if it shows usage/config examples referencing the changed API.

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
  "exception_class": "java.lang.NullPointerException", // required — empty string when no throwable
  "message":         "...",
  "stack_trace":     "..."                // required — empty string when no throwable
}

→ 202 Accepted  (only success)
→ anything else  log locally, swallow
```

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
- `sanitizers` (List<Sanitizer>, default: all five built-in sanitizers in order — see below)

**`Sanitizer`** — interface with two methods:
- `String sanitize(String input)` — abstract; string-only transformation (regex sanitizers)
- `default String sanitize(String input, Throwable throwable)` — throwable-aware overload; default delegates to `sanitize(input)`. Override to access the exception.

All sanitizers live in `net.onestorm.ket4j.sanitizer`.
Eight built-in string sanitizers (in default list):
- `JwtSanitizer` — replaces `eyJ<b64>.<b64>.<b64>` with `[REDACTED:jwt]`
- `BearerTokenSanitizer` — two-pass: replace `Bearer <token>`, then any bare reuse of that credential → `[REDACTED:bearer]`
- `DsnPasswordSanitizer` — strips password from `scheme://user:pass@host` → `scheme://user:[REDACTED:dsn-password]@host`
- `StripeApiKeySanitizer` — redacts Stripe live secret keys (`sk_live_...`) → `[REDACTED:api-key]`
- `AwsApiKeySanitizer` — redacts AWS access key IDs (`AKIA...`) → `[REDACTED:api-key]`
- `Ipv4Sanitizer` — replaces IPv4 addresses (each octet bounded 0–255) with `[REDACTED:ip]`
- `EmailSanitizer` — replaces email addresses with `[REDACTED:email]`
- `BsnSanitizer` — replaces Dutch BSN numbers validated by elfproef with `[REDACTED:bsn]`
- `PathSanitizer` — strips a configurable base path prefix; also redacts OS usernames in `/home/<user>/` and `/Users/<user>/` paths → `[REDACTED:user]`

One built-in throwable-aware sanitizer (NOT in default list — opt-in):
- `SqlExceptionSanitizer` — if the throwable chain contains a `SQLException`, replaces the entire message with `ExceptionClass [SQLSTATE x] [error code y]`, dropping embedded SQL and bound values. Falls back to `"unknown"` if SQLSTATE is absent.

`ErrorTracker` applies sanitizers in two passes:
1. **Message pass** — calls `sanitizer.sanitize(message, throwable)` for each sanitizer in order. Throwable-aware sanitizers (like `SqlExceptionSanitizer`) can replace the entire message.
2. **Stack trace pass** — calls `sanitizer.sanitize(stackTrace)` for each sanitizer in order.

Default sanitizer order: JWT → Bearer → DSN → Stripe → AWS → IPv4 → Email → BSN → Path
(structured/narrower secrets first to avoid partial leakage into looser patterns)

`PathSanitizer` takes a `basePath` in its constructor. If `basePath` is null/blank, only username redaction runs.
Users configure which sanitizers to use (and in what order) via `ErrorTrackerConfiguration.sanitizers`.
The default list includes all eight string sanitizers. `SqlExceptionSanitizer` must be added explicitly by applications that use a database.

**`ErrorEvent`** — simple record for the JSON body fields (matches the API endpoint name "error-events").

**`ErrorTrackerProvider`** — static singleton provider:
```java
ErrorTrackerProvider.initialize(ErrorTrackerConfiguration config); // call once at app startup
ErrorTracker tracker = ErrorTrackerProvider.getInstance();         // used by all appenders
```
Throws `IllegalStateException` if `getInstance()` called before `initialize()`.

**`ErrorTracker`** — core logic:
- `void report(Throwable throwable, String message)` — two-pass sanitization (message with throwable context, stack trace string-only), builds `ErrorEvent`, calls `send()`; never throws (swallow-on-failure)
- `void send(ErrorEvent event)` — private; performs the HTTP POST; never throws
- HTTP client: `java.net.http.HttpClient` (JDK built-in, no extra dep)
- JSON serialization: manual — private `escapeJson(String)` helper (handle `"`, `\`, `\n`, `\r`, `\t`, control chars); payload is a flat 5-field object, no library needed
- Timeouts: `connectTimeoutSeconds` and `timeoutSeconds` from config
- Package-private constructor — instantiated only by `ErrorTrackerProvider`
- **ket4j-core has zero external dependencies**

### ket4j-log4j2

**`KendoErrorAppender`** — extends `AbstractAppender`:
- Acts on `WARN`/`ERROR`/`FATAL` log events that include a `Throwable`
- Calls `ErrorTrackerProvider.getInstance().report(throwable)`
- Wraps in try/catch — appender must never throw
- Configured via Log4j2 XML (standard `@Plugin` + `@PluginFactory`)

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
- **Sanitize before send** — apply all sanitizers to `message` and `stackTrace` synchronously in `report()`.
- **Missing config short-circuit** — if `kendoUrl`, `projectId`, or `token` is blank, log and skip the POST.

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

## Code Style

- **No `var`** — always use explicit types (`List<String> tokens = new ArrayList<>()`, not `var tokens = ...`)
- **No wildcard imports** — always use explicit imports (`import java.util.List`, not `import java.util.*`)
- **Declare with interfaces** — use the interface type on the left-hand side (`List<String>`, `Map<K,V>`) and the concrete type only on the right (`new ArrayList<>()`, `new HashMap<>()`)
- **Descriptive variable names** — no `pass1`, `pass2`; name variables by what they hold (`result`, `tokens`, `matcher`)

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
├── ErrorEvent.java
└── sanitizer/
    ├── Sanitizer.java              (interface)
    ├── JwtSanitizer.java
    ├── BearerTokenSanitizer.java
    ├── DsnPasswordSanitizer.java
    ├── StripeApiKeySanitizer.java
    ├── AwsApiKeySanitizer.java
    ├── Ipv4Sanitizer.java
    ├── EmailSanitizer.java
    ├── BsnSanitizer.java
    ├── PathSanitizer.java
    └── SqlExceptionSanitizer.java

ket4j-log4j2/src/main/java/net/onestorm/ket4j/log4j2/
└── KendoErrorAppender.java
```

## CI/CD (`.github/workflows/ci.yml`)

**On every push and PR** — `test` job runs `mvn -B -e clean verify`; the build fails if any test fails or JaCoCo line coverage drops below 100%.

**On push to `main` only** — `deploy` job runs after `test` passes:
1. `mvn -U -B -e clean deploy` — builds all modules and stages artifacts to `target/local-repository/` (root of the repo) via `maven-deploy-plugin` `altDeploymentRepository`.
2. `rsync` pushes the staged repository to the remote Maven server over SSH.

### Required GitHub Actions secrets
| Secret | Purpose |
|---|---|
| `SSH_PRIVATE_KEY` | Private key for rsync SSH connection |
| `SSH_KNOWN_HOST` | Known-hosts entry for the target server |
| `SSH_USER` | SSH username |
| `SSH_HOST` | SSH hostname |

### Maven deploy plugin
Declared in parent `<pluginManagement>` (version `3.1.4`). All modules deploy into `${maven.multiModuleProjectDirectory}/target/local-repository` — a single flat directory at the repo root — so rsync only needs one source path.
