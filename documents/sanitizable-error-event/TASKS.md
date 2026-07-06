# Sanitizable Error Event — Tasks

Work through in order. See `DECISIONS.md` for the rationale behind each of these.

## 1. Core: event & sanitizer foundations

- [x] 1.1 Add `ExceptionUtil` (util class, `net.onestorm.ket4j.util`) that turns a `Throwable`
      into `exceptionMessage`/`stackTrace` strings, so logger-module `ErrorEvent` implementations
      don't duplicate this logic.
- [x] 1.2 Remove the existing `ErrorEvent` record (JSON payload) — no replacement class needed.
      `ErrorTracker` builds the wire JSON directly from the sanitized `ErrorEvent` (1.3) plus
      `ErrorTrackerConfiguration` (`environment`/`release`), per DECISIONS.md #4.
- [x] 1.3 Add the new `ErrorEvent` interface: `getMessage`/`setMessage`, `getThrowable`
      (read-only), `getExceptionMessage`/`setExceptionMessage`, `getStackTrace`/`setStackTrace`.
- [x] 1.4 Change `Sanitizer` to a single `void sanitize(ErrorEvent event)` method; remove
      `sanitize(String)` and `sanitize(String, Throwable)`.
- [x] 1.5 Add a shared helper for regex-style sanitizers that applies a `String -> String`
      transform to `message`, `exceptionMessage`, and `stackTrace` on an event, so the 9 regex
      sanitizers don't each repeat that plumbing.

## 2. Core: sanitizer rewrites

- [x] 2.1 Rewrite `JwtSanitizer` against the new `Sanitizer` signature.
- [x] 2.2 Rewrite `BearerTokenSanitizer` against the new `Sanitizer` signature.
- [x] 2.3 Rewrite `DsnPasswordSanitizer` against the new `Sanitizer` signature.
- [x] 2.4 Rewrite `StripeApiKeySanitizer` against the new `Sanitizer` signature.
- [x] 2.5 Rewrite `AwsApiKeySanitizer` against the new `Sanitizer` signature.
- [x] 2.6 Rewrite `Ipv4Sanitizer` against the new `Sanitizer` signature.
- [x] 2.7 Rewrite `EmailSanitizer` against the new `Sanitizer` signature.
- [x] 2.8 Rewrite `BsnSanitizer` against the new `Sanitizer` signature.
- [x] 2.9 Rewrite `PathSanitizer` against the new `Sanitizer` signature.
- [x] 2.10 Rewrite `SqlExceptionSanitizer` to overwrite `exceptionMessage` only (not `message`),
       per DECISIONS.md #5.
- [x] 2.11 Update/add unit tests for every rewritten sanitizer (valid matches redacted,
       non-matches left alone, multiple occurrences, edge cases — same coverage bar as before,
       against the new `ErrorEvent`-based signature).

## 3. Core: ErrorTracker

- [x] 3.1 Change `ErrorTracker.report(Throwable, String)` to `report(ErrorEvent event)`: run all
      configured sanitizers' `sanitize(event)` in a single pass, then build the wire JSON directly
      from the sanitized event (see 1.2 / DECISIONS.md #4) and send it.
- [x] 3.2 Update `ErrorTracker` tests for the new `report(ErrorEvent)` flow.

## 4. ket4j-log4j2

- [x] 4.1 Implement `Log4j2ErrorEvent implements ErrorEvent`, wrapping a Log4j2 `LogEvent`
      (message + throwable), using core's `ExceptionUtil` for
      `exceptionMessage`/`stackTrace`.
- [x] 4.2 Update `KendoErrorAppender` to build a `Log4j2ErrorEvent` from the incoming `LogEvent`
      and call `ErrorTrackerProvider.getInstance().report(event)`.
- [x] 4.3 `KendoErrorAppenderTest` needed no changes — it exercises `append()` as a black box and
      already passed unchanged. Added a dedicated `Log4j2ErrorEventTest` instead, matching this
      project's one-test-class-per-production-class convention.
