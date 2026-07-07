# ket4j Implementation Tasks

- [x] **Task 1** — Set up parent `pom.xml` with version properties and `dependencyManagement`
  - Add version properties: `log4j2-core` 2.26.0, `junit-jupiter` 5.12.2, `assertj-core` 3.27.3, `jacoco-maven-plugin` 0.8.13, `maven-surefire-plugin` 3.5.6
  - Add `<dependencyManagement>` block (log4j2-core, junit-jupiter + assertj-core as test scope); child modules reference deps without `<version>`
  - Add `maven-surefire-plugin` 3.5.2+ to `<pluginManagement>` for JUnit 5 support
  - Add `jacoco-maven-plugin` to `<build><plugins>`: `prepare-agent` (initialize), `report` (verify, XML only), `check` with 100% line coverage minimum
  - Java 25 already set — do not change

- [x] **Task 2** — Implement `Sanitizer` interface and eight built-in sanitizers
  - `Sanitizer.java` interface: `String sanitize(String input)`
  - `JwtSanitizer` → `[REDACTED:jwt]`
  - `BearerTokenSanitizer` → `[REDACTED:bearer]` (two-pass)
  - `DsnPasswordSanitizer` → `scheme://user:[REDACTED:dsn-password]@host`
  - `StripeApiKeySanitizer` → `[REDACTED:api-key]` (Stripe `sk_live_`)
  - `AwsApiKeySanitizer` → `[REDACTED:api-key]` (AWS `AKIA`)
  - `Ipv4Sanitizer` → `[REDACTED:ip]`
  - `EmailSanitizer` → `[REDACTED:email]`
  - `BsnSanitizer` → `[REDACTED:bsn]` (elfproef validated)
  - All in `net.onestorm.ket4j.sanitizer`

- [x] **Task 3** — Implement `PathSanitizer` (implements `Sanitizer`)
  - Takes `basePath` in constructor; strips it from every occurrence in input
  - Redacts `/home/<user>/` → `/home/[REDACTED:user]/` and `/Users/<user>/` → `/Users/[REDACTED:user]/`
  - Lives in `net.onestorm.ket4j.sanitizer`

- [x] **Task 4** — Implement `ErrorTrackerConfiguration` with builder
  - Fields: `kendoUrl`, `projectId`, `token` (required), `environment` (default `"production"`), `release` (nullable), `connectTimeoutSeconds` (default 2.0), `timeoutSeconds` (default 5.0), `sanitizers` (default: all five in order)

- [x] **Task 5** — Implement `ErrorEvent` and `ErrorTracker`
  - `ErrorEvent` — Java record for `{environment, release?, exceptionClass, message, stackTrace}`
  - `Sanitizer` — added `default String sanitize(String input, Throwable throwable)` overload; delegates to `sanitize(input)` by default; override to intercept throwable
  - `SqlExceptionSanitizer` — throwable-aware sanitizer; if throwable chain contains a `SQLException`, replaces entire message with `ExceptionClass [SQLSTATE x] [error code y]`; NOT in default list (opt-in)
  - `ErrorTracker.report(Throwable throwable, String message)` — two-pass sanitization (message with throwable, stack trace string-only), builds `ErrorEvent`, calls `send()`; never throws
  - `ErrorTracker.send(ErrorEvent)` — private; HTTP POST via `java.net.http.HttpClient`; serialize to JSON manually with a private `escapeJson(String)` helper; swallow on non-202 or exception
  - No external dependencies — `ket4j-core` is pure JDK

- [x] **Task 6** — Implement `ErrorTrackerProvider` (static singleton)
  - `initialize(ErrorTrackerConfiguration)` — thread-safe via `AtomicReference.compareAndSet`; throws `IllegalStateException` on double-init
  - `getInstance()` — throws `IllegalStateException` if called before `initialize()`

- [x] **Task 7** — Implement `KendoErrorAppender` (Log4j2)
  - Extends `AbstractAppender`, `@Plugin(name = "KendoError")` + `@PluginFactory`
  - Acts on `WARN`/`ERROR`/`FATAL` events with a `Throwable`; calls `ErrorTrackerProvider.getInstance().report(throwable, message)`
  - Wraps everything in try/catch — delegates to inherited `error()` (StatusLogger) on failure

- [x] **Task 8** — Write unit tests for sanitizers and core logic
  - Add `junit-jupiter` and `assertj-core` test deps to `ket4j-core/pom.xml`
  - One test class per sanitizer: valid matches redacted, non-matches left alone, multiple in one string, edge cases
  - `PathSanitizerTest` — prefix stripped, pass-through when basePath null/blank
  - `ErrorTrackerConfigurationTest` — blank required fields rejected, defaults correct
  - Use `@ParameterizedTest` where it reduces boilerplate
