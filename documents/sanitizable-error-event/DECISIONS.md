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
   (what the developer logged), `exceptionMessage` (the top-level throwable's own message, or
   `""` if none/null), and `stackTrace` (the full rendered throwable text — header, frames, and
   the entire `Caused by:` chain, same shape `printStackTrace()` produces today) — each
   individually gettable/settable. This preserves today's message-pass/stack-trace-pass
   distinction and lets sanitizers target one without touching the others.

   `exceptionMessage` only reflects the *top-level* throwable — Java exceptions can have a cause
   chain, each link with its own message, and modelling that fully (e.g. a list of per-cause
   messages) would be over-engineering for what this update needs. `stackTrace` already contains
   every message in the chain (each cause renders its own header line), so generic regex
   sanitizers scanning `stackTrace` as a whole already catch secrets anywhere in the chain — that
   part isn't a gap. `exceptionMessage` exists purely so a sanitizer can reason about "the
   exception's own message" without regexing into `stackTrace`'s embedded header line.
   Sanitizers that need the full chain (e.g. `SqlExceptionSanitizer`, walking causes for a
   `SQLException`) still do so via `event.getThrowable()`, exactly like today — `exceptionMessage`
   doesn't replace that.

   Importantly: **only `stackTrace` is transmitted** — the wire API has one `stack_trace` field
   (see decision 5's `SqlExceptionSanitizer` note for why this matters).

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

4. **No separate payload class — `ErrorTracker` builds the wire JSON directly.** The old
   `ErrorEvent` record (the flat 5-field DTO serialized to the API body) is removed rather than
   renamed. It existed only to carry 5 values from `report()` to the private `send()`/`buildJson()`
   methods a few lines away on the same object — an intermediate object with one call site isn't
   worth keeping once the sanitizable `ErrorEvent` interface claims the name. `ErrorTracker`
   already holds `ErrorTrackerConfiguration` as an instance field, so `environment`/`release` are
   read from `config` directly inside `buildJson()`/`send()`; `exceptionClass` is derived from
   `event.getThrowable()`; `message`/`stackTrace` come straight off the sanitized `ErrorEvent`. No
   object needs to bundle these back together first.

5. **`SqlExceptionSanitizer` no longer touches `message`, and now also scrubs `stackTrace` — a
   real behavior fix, not just a rename.** Today it wipes the entire log message when a
   `SQLException` is found in the chain, and is a no-op on the stack trace text (its
   string-only `sanitize(String)` override just returns the input unchanged) — so a SQL
   exception's message, which can embed the statement or bound values, ships untouched inside
   `Caused by: java.sql.SQLException: ...` in the stack trace. That's a pre-existing gap.

   With the new model: it leaves the developer's `message` alone entirely (it never leaked SQL
   text — only the exception's own message does), and instead:
   - sets `exceptionMessage` to `ExceptionClass [SQLSTATE x] [error code y]`, and
   - replaces every occurrence of the found `SQLException`'s original message text inside
     `stackTrace` with that same synthetic string, before `stackTrace` is transmitted.

   This closes the leak instead of just relocating which field the (still-transmitted) SQL text
   lives in.

6. **`ErrorTracker.report` takes an `ErrorEvent`.** Signature changes from
   `report(Throwable throwable, String message)` to `report(ErrorEvent event)`. Sanitization
   becomes a single pass: run every configured sanitizer's `sanitize(event)` in order (each free
   to touch `message`, `exceptionMessage`, and/or `stackTrace`), then build the wire JSON straight
   from the sanitized event (see decision 4) and send it. The old two-pass split (message pass vs.
   stack-trace pass) is gone — each sanitizer is now responsible for touching whichever fields are
   relevant to it.

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
  later `ket4j-logback`, core exposes a shared `net.onestorm.ket4j.util.ExceptionUtil` (static
  helpers only — see CLAUDE.md's utility-class convention) that implementations call from their
  constructor.
- `exception_class` (for the final payload) is derived straight from `event.getThrowable()`'s
  class name at send time — it isn't part of the sanitizable surface, no sanitizer needs to
  touch it.
- When `throwable` is null (no exception on the log event), `exceptionMessage` and `stackTrace`
  default to empty string, matching current "empty string when no throwable" API contract
  behavior.
