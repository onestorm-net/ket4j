# Sanitizable Error Event — Tasks

Work through in order. See `DECISIONS.md` for the rationale behind each of these.

## 1. Core: event & sanitizer foundations

- [x] 1.1 Add `ExceptionRenderer` (or similarly named) utility in `net.onestorm.ket4j` that turns
      a `Throwable` into `exceptionMessage`/`stackTrace` strings, so logger-module `ErrorEvent`
      implementations don't duplicate this logic.
- [x] 1.2 Rename the existing `ErrorEvent` record (JSON payload) to `ErrorEventPayload`.
- [x] 1.3 Add the new `ErrorEvent` interface: `getMessage`/`setMessage`, `getThrowable`
      (read-only), `getExceptionMessage`/`setExceptionMessage`, `getStackTrace`/`setStackTrace`.
- [ ] 1.4 Change `Sanitizer` to a single `void sanitize(ErrorEvent event)` method; remove
      `sanitize(String)` and `sanitize(String, Throwable)`.
- [ ] 1.5 Add a shared helper for regex-style sanitizers that applies a `String -> String`
      transform to `message`, `exceptionMessage`, and `stackTrace` on an event, so the 8 regex
      sanitizers don't each repeat that plumbing.

## 2. Core: sanitizer rewrites

- [ ] 2.1 Rewrite `JwtSanitizer` against the new `Sanitizer` signature.
- [ ] 2.2 Rewrite `BearerTokenSanitizer` against the new `Sanitizer` signature.
- [ ] 2.3 Rewrite `DsnPasswordSanitizer` against the new `Sanitizer` signature.
- [ ] 2.4 Rewrite `StripeApiKeySanitizer` against the new `Sanitizer` signature.
- [ ] 2.5 Rewrite `AwsApiKeySanitizer` against the new `Sanitizer` signature.
- [ ] 2.6 Rewrite `Ipv4Sanitizer` against the new `Sanitizer` signature.
- [ ] 2.7 Rewrite `EmailSanitizer` against the new `Sanitizer` signature.
- [ ] 2.8 Rewrite `BsnSanitizer` against the new `Sanitizer` signature.
- [ ] 2.9 Rewrite `PathSanitizer` against the new `Sanitizer` signature.
- [ ] 2.10 Rewrite `SqlExceptionSanitizer` to overwrite `exceptionMessage` only (not `message`),
       per DECISIONS.md #5.
- [ ] 2.11 Update/add unit tests for every rewritten sanitizer (valid matches redacted,
       non-matches left alone, multiple occurrences, edge cases — same coverage bar as before,
       against the new `ErrorEvent`-based signature).

## 3. Core: ErrorTracker

- [ ] 3.1 Change `ErrorTracker.report(Throwable, String)` to `report(ErrorEvent event)`: run all
      configured sanitizers' `sanitize(event)` in a single pass, build an `ErrorEventPayload`
      from the sanitized event, and send it.
- [ ] 3.2 Update `ErrorTracker` tests for the new `report(ErrorEvent)` flow and
      `ErrorEventPayload` construction.

## 4. ket4j-log4j2

- [ ] 4.1 Implement `Log4j2ErrorEvent implements ErrorEvent`, wrapping a Log4j2 `LogEvent`
      (message + throwable), using core's `ExceptionRenderer` for
      `exceptionMessage`/`stackTrace`.
- [ ] 4.2 Update `KendoErrorAppender` to build a `Log4j2ErrorEvent` from the incoming `LogEvent`
      and call `ErrorTrackerProvider.getInstance().report(event)`.
- [ ] 4.3 Update `KendoErrorAppender` tests for the new event-based flow.
