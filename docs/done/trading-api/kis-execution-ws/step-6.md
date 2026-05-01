# Step 6: Code Review Rework
Assigned agent: fullstack-dev

## Working Directory
`.worktrees/trading-api-kis-execution-ws/backend/trading-api`

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/CODEX.md`
- `C:/Users/dw/Desktop/paper-trading/docs/ADR.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/spec.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/step-2.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/step-4.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/index.json`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisExecutionNoticeService.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionNotice.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionNoticeParser.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryCustom.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryImpl.kt`
- Relevant Step 2 tests under `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/test/kotlin/com/papertrading/api/`

## Review Findings To Fix
1. `KisExecutionWebSocketManager.kt:45,65,70,106,123`
   - Production `SpringKisExecutionWebSocketTransport` does not route KIS control/auth/error frames, socket close, or transport failure into reconnect/re-auth.
   - `reconnect(mode)` immediately calls `connectMode(mode)` and does not enforce configured bounded backoff.
   - Fix must fetch fresh approval/auth data on auth/control/close/failure reconnect and must not retry REST order submit/cancel.

2. `KisExecutionNoticeService.kt:27`, `OrderRepositoryImpl.kt:51-62`, `KisExecutionNoticeParser.kt:38,44-45`
   - KIS execution notice correlation scopes only by `externalOrderId + tradingMode`; it ignores account identifiers carried by the notice.
   - External execution id/idempotency key omits account scope, which can collide for same-mode multi-account cases.
   - Fix correlation and idempotency so account scope is included when notice account identifiers are available.

## Tasks
Use TDD for the rework.

Required tests:
- Add or update WebSocket manager/transport tests proving:
  - auth/control error routes to reconnect with fresh approval key,
  - socket close/failure routes to bounded reconnect,
  - reconnect attempts respect configured max attempt/backoff limits,
  - raw credential-bearing frames are not logged.
- Add or update parser/service/repository tests proving:
  - account identifiers are parsed into `KisExecutionNotice`,
  - external execution id/idempotency includes account scope when available,
  - order lookup includes trading mode and account scope when account identifiers are available,
  - duplicates are detected only within the correct account scope.

Implementation constraints:
- Keep changes inside the current phase scope.
- Do not alter public SSE endpoint contract.
- Do not add broad DDD refactors.
- Do not run the full suite repeatedly. Run targeted tests first; run full `gradlew.bat test compileKotlin` once as final verification after targeted tests pass. If the full suite fails because of an environment issue, stop and report BLOCKED rather than looping.

## Acceptance Criteria
- Both review findings are fixed.
- Targeted tests for the rework pass.
- Final verification from the worktree passes:
```bash
./gradlew.bat test compileKotlin
```
- No token, approval key, app secret, or full credential-bearing raw frame is logged.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to repo root>
- Test result: <not run | passed N/N | failed N - list failing cases>
- Blockers: <none | description>
---
