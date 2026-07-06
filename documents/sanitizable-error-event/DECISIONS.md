# Sanitizable Error Event

## Goal

Today, sanitizers operate on plain strings: `Sanitizer.sanitize(String)` and a throwable-aware
overload `sanitize(String, Throwable)`. `ErrorTracker.report(Throwable, String message)` runs a
message pass and a separate stack-trace pass, each calling every configured sanitizer in order.

This update replaces that with a single mutable event object. The logger module (ket4j-log4j2
today, ket4j-logback later) builds the event from its native log record — carrying the log
message and the full `Throwable` — and forwards it to `ErrorTracker`. Sanitizers receive the
event and can read/modify any of its fields directly, instead of returning a transformed string.

This is a breaking change to `ket4j-core`'s public API (`Sanitizer`, `ErrorTracker`) and to how
logger modules integrate with it.

## Decisions

1. **Split fields, not one blob.** The event exposes three independent text fields — `message`
   (what the developer logged), `exceptionMessage` (derived from the throwable), and
   `stackTrace` (rendered from the throwable) — each individually gettable/settable. This
   preserves today's message-pass/stack-trace-pass distinction and lets sanitizers target one
   without touching the others.

2. **`Sanitizer` becomes a single mutating method.** Replace both `sanitize(String)` and
   `sanitize(String, Throwable)` with `void sanitize(ErrorEvent event)`. No dual API, no
   default-delegation — every built-in sanitizer is rewritten against the new signature. Chosen
   over keeping the old string methods around because this update is explicitly meant to change
   the API surface, and maintaining two mental models (string transform vs. event mutation) long
   term isn't worth it.

3. **`ErrorEvent` is an interface owned by core, implemented per logger module.** `ket4j-core`
   defines the `ErrorEvent` interface (message / throwable / exceptionMessage / stackTrace).
   `ket4j-log4j2` implements it by wrapping a Log4j2 `LogEvent`; `ket4j-logback` will later do
   the same for its native event type. `throwable` is read-only on the interface (sanitizers
   inspect it — e.g. `SqlExceptionSanitizer` checks the exception chain for `SQLException` — but
   don't replace it); `message`, `exceptionMessage`, and `stackTrace` are mutable.

4. **The existing `ErrorEvent` record (JSON payload) is renamed.** The name `ErrorEvent` is
   claimed by the new sanitizable interface. The current record — the flat 5-field DTO that gets
   serialized to the API body (`environment`, `release`, `exception_class`, `message`,
   `stack_trace`) — is renamed to `ErrorEventPayload`. `ErrorTracker` builds an `ErrorEventPayload`
   from the (now sanitized) `ErrorEvent` right before sending.

5. **`SqlExceptionSanitizer` replaces `exceptionMessage` only, not `message`.** Today it wipes
   the entire log message when a `SQLException` is found in the chain, because the "message" it
   operated on was the log message. With the fields split, that scope narrows: it now overwrites
   only `exceptionMessage` with `ExceptionClass [SQLSTATE x] [error code y]`. The log message a
   developer wrote (e.g. `"Failed to save user"`) is left alone, since it doesn't inherently leak
   SQL text — only the exception's own message does.

6. **`ErrorTracker.report` takes an `ErrorEvent`.** Signature changes from
   `report(Throwable throwable, String message)` to `report(ErrorEvent event)`. Sanitization
   becomes a single pass: run every configured sanitizer's `sanitize(event)` in order (each free
   to touch `message`, `exceptionMessage`, and/or `stackTrace`), then build the
   `ErrorEventPayload` and send it. The old two-pass split (message pass vs. stack-trace pass) is
   gone — each sanitizer is now responsible for touching whichever fields are relevant to it.

## Findings

- Regex-based sanitizers (JWT, Bearer, DSN, Stripe, AWS, IPv4, Email, BSN, Path) all need to
  apply the *same* transform to up to three fields now (`message`, `exceptionMessage`,
  `stackTrace`) instead of being called once per pass externally. Worth introducing a small
  shared helper (e.g. a base class or static utility that takes the event and a
  `String -> String` transform and applies it to all three fields) so each sanitizer doesn't
  duplicate that plumbing. Not a new sanitizer class — an implementation detail inside
  `net.onestorm.ket4j.sanitizer` to keep the actual 9 sanitizer classes small.
- Rendering a `Throwable` into `exceptionMessage`/`stackTrace` strings is logic every logger
  module implementation of `ErrorEvent` needs. To avoid duplicating it in `ket4j-log4j2` and
  later `ket4j-logback`, core should expose a small shared utility (e.g.
  `ExceptionRenderer.stackTraceOf(Throwable)`) that implementations call from their constructor.
- `exception_class` (for the final payload) is derived straight from `event.getThrowable()`'s
  class name at send time — it isn't part of the sanitizable surface, no sanitizer needs to
  touch it.
- When `throwable` is null (no exception on the log event), `exceptionMessage` and `stackTrace`
  default to empty string, matching current "empty string when no throwable" API contract
  behavior.
