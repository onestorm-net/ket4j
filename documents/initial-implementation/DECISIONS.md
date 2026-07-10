# Initial Implementation

## Goal

Port [kendo-error-tracker](https://github.com/script-development/kendo-error-tracker) (PHP/Laravel)
to Java: a library that sanitizes exception reports and sends them to kendo's ingestion API
(`POST /api/projects/{projectId}/error-events`), with a Log4j2 appender as the first integration
point. Two modules — `ket4j-core` (config, sanitizers, HTTP client, provider) and `ket4j-log4j2`
(the `AbstractAppender` that delegates to core) — matching the module table in CLAUDE.md.

This predates the `development-<name>` major-update workflow (see CLAUDE.md), so there's no
contemporaneous decisions record from when the work happened. This document reconstructs the
key calls from the four commits that built it (`980c25e`, `2805016`, `a911c45`, `85bbfa2`) so the
initial design has a record alongside every update that follows it.

## Decisions

1. **String-based `Sanitizer`, not event-based.** The original `Sanitizer` interface operated on
   plain strings — `String sanitize(String input)`, plus a throwable-aware default overload
   `sanitize(String input, Throwable throwable)`. `ErrorTracker.report(Throwable, String message)`
   ran sanitizers in two passes: once over the message (throwable-aware), once over the rendered
   stack trace (string-only). This was superseded by the mutable `ErrorEvent` model — see
   `documents/sanitizable-error-event/DECISIONS.md`.
2. **Nine regex sanitizers, ordered narrowest-first.** JWT → Bearer → DSN → Stripe → AWS → IPv4 →
   Email → BSN → Path. Structured/narrow secret patterns run before looser ones (email, IPv4) so a
   secret embedded in a larger string doesn't get partially redacted by the wrong pattern first.
3. **`SqlExceptionSanitizer` opt-in, not in the default list.** Wiping the entire log message when
   a `SQLException` is found in the chain is a strong behavior change that only makes sense for
   apps with a database — left out of the default sanitizer list so non-DB consumers aren't
   surprised by it.
4. **Zero external dependencies in `ket4j-core`.** HTTP via `java.net.http.HttpClient` (JDK
   built-in) and hand-written JSON serialization (a private `escapeJson` helper over a flat
   5-field payload) rather than pulling in an HTTP or JSON library, since the payload shape is
   fixed and small.
5. **Swallow-on-failure throughout.** `ErrorTracker.report`/`send` never throw — network failures,
   non-202 responses, and serialization issues are logged and swallowed so a broken kendo
   endpoint can never take down the host application. `KendoErrorAppender` wraps its own call in
   try/catch for the same reason.
6. **Static singleton provider (`ErrorTrackerProvider`).** One `initialize()` call at app startup,
   `getInstance()` everywhere else; throws `IllegalStateException` if `getInstance()` is called
   before `initialize()`, rather than lazily defaulting, so misconfiguration fails loudly at first
   use instead of silently no-op-ing.

## Findings

- 118 tests reached 100% line coverage on the first pass (`a911c45`), which is now enforced going
  forward by the JaCoCo `check` goal bound to `verify`.
- The groupId briefly included "logging" (`net.onestorm.ket4j.logging`) and was dropped to match
  the package structure (`net.onestorm.ket4j`) — see `94d91a4`.
