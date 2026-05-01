# kis-execution-ws — Feature Summary

## Feature Purpose and Scope

Subscribe `trading-api` to KIS account execution notice WebSocket channels (H0STCNI9 for paper trading, H0STCNI0 for live trading) and process incoming order execution notices through the existing order, execution, position, settlement, and SSE flow. This replaces the previous polling-based `KisPaperPollingScheduler` approach with a real-time WebSocket push mechanism.

## Key Implementation Decisions

### Architecture

- **WebSocket ownership in trading-api**: Account execution notices are processed directly in `trading-api` rather than via `collector-api`, keeping account/order bounded context concerns co-located.
- **Reused `executions.external_execution_id`**: Idempotency is guaranteed via a unique constraint on the existing `external_execution_id` column, avoiding a new inbox table.
- **`KisPaperPollingScheduler` deactivated**: Scheduler bean disabled via `@Conditional`; WebSocket replaces polling for paper and live modes.

### Bounded Reconnect / Re-auth

- Initial connect failures (DNS/refused/TLS/handshake before a WebSocket session exists) are routed into bounded reconnect via `StandardWebSocketClient.execute(...)` exceptional completion wired to `request.onFailure`.
- Post-session transport errors and socket close events also route into the same bounded reconnect path.
- Auth/control frame errors trigger approval key eviction and fresh approval key fetch on reconnect.
- Reconnect uses exponential backoff with a configured maximum attempt count.

### Account-Scoped Notice Correlation

- `KisExecutionNoticeParser` extracts `CANO` and `ACNT_PRDT_CD` from each incoming notice frame.
- `KisExecutionNoticeService` uses account identifiers alongside `externalOrderId` and trading mode for order lookup and correlation.
- `OrderRepositoryImpl` query includes account scope to prevent multi-account collisions and incorrect deduplication.
- `external_execution_id` includes account scope in its key to prevent cross-account idempotency collisions.

### Typed KIS REST Exception Mapping

- Non-zero KIS REST response codes map to typed domain exceptions (`KisApiException` and subtypes) rather than generic runtime exceptions.

### Secret Safety

- Token, approval key, and app secret values are never logged in plaintext. Log redaction verified in code review.

### Financial Safety

- All monetary fields remain `BigDecimal` throughout the execution notice processing flow.

## Test Results

- **Final full test suite: 122/122 PASS** (Step 10 — Test Engineer final gate)
- Targeted step 6 rework tests: 11/11 PASS
- Targeted step 8 initial connect failure tests: 6/6 PASS
- SSE `/api/v1/executions/stream` verified to emit `ExecutionFilledEvent` unchanged

## Review Findings Resolved

### Review Round 1 (Step 4 — FAIL)

1. **WS reconnect routing gap**: `KisExecutionWebSocketManager` did not route production control/auth/error frames or socket close/failure into reconnect/re-auth; reconnect was immediate and unbounded.
2. **Account-scope correlation gap**: Notice correlation ignored `CANO` + `ACNT_PRDT_CD` identifiers; idempotency key omitted account scope, causing potential multi-account collisions.

Both resolved in Step 6 rework (bounded reconnect/re-auth + account-scoped idempotency).

### Review Round 2 (Step 7 — FAIL)

3. **Initial connect failure not routed**: `SpringKisExecutionWebSocketTransport` invoked `StandardWebSocketClient.execute(handler, request.url)` but ignored the returned async completion handle, so pre-session failures (DNS/refused/TLS) did not reach `request.onFailure` or trigger bounded reconnect.

Resolved in Step 8 rework; confirmed in Step 9 final review (PASS, no blocking findings).

## Files Changed (by Area)

### New Files

- `application/order/KisExecutionNoticeService.kt` — account-scoped notice correlation and idempotent fill dispatch
- `infrastructure/kis/KisExecutionNotice.kt` — ACL DTO for parsed execution notice frames
- `infrastructure/kis/KisExecutionNoticeParser.kt` — KIS frame parser with CANO/ACNT_PRDT_CD extraction
- `infrastructure/kis/KisExecutionWebSocketManager.kt` — WebSocket lifecycle manager with bounded reconnect/re-auth
- `infrastructure/kis/KisApiException.kt` — typed KIS REST exception hierarchy
- `test/.../KisExecutionNoticeServiceTest.kt` — service-level idempotency and correlation tests
- `test/.../KisExecutionWebSocketManagerTest.kt` — manager reconnect, re-auth, and initial connect failure tests
- `test/.../KisExecutionNoticeParserTest.kt` — parser account-scope extraction tests

### Modified Files

- `application/order/ExecutionProcessor.kt` — wired to accept execution notice fills
- `infrastructure/kis/KisOrderProperties.kt` — added WebSocket configuration properties
- `infrastructure/kis/KisOrderRestClient.kt` — typed exception mapping for non-zero response codes
- `infrastructure/kis/KisTokenManager.kt` — approval key management for WebSocket auth
- `infrastructure/persistence/OrderRepositoryCustom.kt` — account-scoped order lookup interface
- `infrastructure/persistence/OrderRepositoryImpl.kt` — account-scoped query implementation
- `infrastructure/scheduler/KisPaperPollingScheduler.kt` — deactivated (conditional disabled)
- `resources/application.yaml` — WebSocket configuration entries
- `build.gradle` — WebSocket client dependency
- `test/.../ExecutionProcessorTest.kt` — updated for new fill dispatch path
