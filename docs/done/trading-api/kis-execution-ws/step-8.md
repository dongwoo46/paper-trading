# Step 8: Initial WebSocket Connect Failure Rework
Assigned agent: fullstack-dev

## Working Directory
`.worktrees/trading-api-kis-execution-ws/backend/trading-api`

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/CODEX.md`
- `C:/Users/dw/Desktop/paper-trading/docs/ADR.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/spec.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/step-6.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/index.json`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManagerTest.kt`

## Review Finding To Fix
`KisExecutionWebSocketManager.kt:100-105`

`SpringKisExecutionWebSocketTransport` calls `StandardWebSocketClient.execute(handler, request.url)` but ignores the returned async completion. Initial connection failures before a `WebSocketSession` exists, such as DNS/refused/TLS/handshake failures, can complete exceptionally without calling `request.onFailure`. Those failures must enter the existing bounded reconnect/re-auth path.

## Tasks
Use TDD.

1. Add or update a targeted test proving initial transport connect failure completion calls `request.onFailure`.
2. Update `SpringKisExecutionWebSocketTransport` so the async completion from `StandardWebSocketClient.execute(...)` routes failures to `request.onFailure`.
3. Keep existing post-session `handleTransportError`, close, auth/control, bounded backoff, and fresh approval-key behavior intact.
4. Do not retry REST order submit/cancel from reconnect logic.
5. Do not run the full test suite in this step. Run only targeted WebSocket manager/transport tests and compile for touched Kotlin if needed.

## Acceptance Criteria
- Initial connect failure before session creation enters `request.onFailure`.
- Existing bounded reconnect/re-auth tests remain green.
- Targeted test command passes:
```bash
./gradlew.bat test --tests "com.papertrading.api.infrastructure.kis.KisExecutionWebSocketManagerTest"
```
- No token, approval key, app secret, or raw credential-bearing frame is logged.

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
