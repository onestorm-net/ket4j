# Sanitizable Error Event — Tasks

Work through in order. See `DECISIONS.md` for the rationale behind each of these.

## ket4j-core

- [ ] Add `ExceptionRenderer` (or similarly named) utility in `net.onestorm.ket4j` that turns a
      `Throwable` into `exceptionMessage`/`stackTrace` strings, so logger-module `ErrorEvent`
      implementations don't duplicate this logic.
- [ ] Rename the existing `ErrorEvent` record (JSON payload) to `ErrorEventPayload`.
- [ ] Add the new `ErrorEvent` interface: `getMessage`/`setMessage`, `getThrowable` (read-only),
      `getExceptionMessage`/`setExceptionMessage`, `getStackTrace`/`setStackTrace`.
- [ ] Change `Sanitizer` to a single `void sanitize(ErrorEvent event)` method; remove
      `sanitize(String)` and `sanitize(String, Throwable)`.
- [ ] Add a shared helper for regex-style sanitizers that applies a `String -> String` transform
      to `message`, `exceptionMessage`, and `stackTrace` on an event, so the 8 regex sanitizers
      don't each repeat that plumbing.
- [ ] Rewrite `JwtSanitizer` against the new `Sanitizer` signature.
- [ ] Rewrite `BearerTokenSanitizer` against the new `Sanitizer` signature.
- [ ] Rewrite `DsnPasswordSanitizer` against the new `Sanitizer` signature.
- [ ] Rewrite `StripeApiKeySanitizer` against the new `Sanitizer` signature.
- [ ] Rewrite `AwsApiKeySanitizer` against the new `Sanitizer` signature.
- [ ] Rewrite `Ipv4Sanitizer` against the new `Sanitizer` signature.
- [ ] Rewrite `EmailSanitizer` against the new `Sanitizer` signature.
- [ ] Rewrite `BsnSanitizer` against the new `Sanitizer` signature.
- [ ] Rewrite `PathSanitizer` against the new `Sanitizer` signature.
- [ ] Rewrite `SqlExceptionSanitizer` to overwrite `exceptionMessage` only (not `message`), per
      DECISIONS.md #5.
- [ ] Change `ErrorTracker.report(Throwable, String)` to `report(ErrorEvent event)`: run all
      configured sanitizers' `sanitize(event)` in a single pass, build an `ErrorEventPayload`
      from the sanitized event, and send it.
- [ ] Update/add unit tests for every rewritten sanitizer (valid matches redacted, non-matches
      left alone, multiple occurrences, edge cases — same coverage bar as before, against the
      new `ErrorEvent`-based signature).
- [ ] Update `ErrorTracker` tests for the new `report(ErrorEvent)` flow and `ErrorEventPayload`
      construction.

## ket4j-log4j2

- [ ] Implement `Log4j2ErrorEvent implements ErrorEvent`, wrapping a Log4j2 `LogEvent` (message +
      throwable), using core's `ExceptionRenderer` for `exceptionMessage`/`stackTrace`.
- [ ] Update `KendoErrorAppender` to build a `Log4j2ErrorEvent` from the incoming `LogEvent` and
      call `ErrorTrackerProvider.getInstance().report(event)`.
- [ ] Update `KendoErrorAppender` tests for the new event-based flow.

## Wrap-up

- [ ] Run `mvn verify` — fix any coverage gaps back to 100% line coverage.
- [ ] Update `CLAUDE.md`'s "What to Build" section (Sanitizer interface, ErrorEvent/
      ErrorEventPayload, ErrorTracker.report signature) to describe the new design instead of the
      old string-based one.
- [ ] Update `README.md` if it shows sanitizer usage/config examples referencing the old API.
