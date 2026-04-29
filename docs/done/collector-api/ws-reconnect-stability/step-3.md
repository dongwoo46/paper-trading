# Step 3: Testing and QA Verification
Assigned agent: test-engineer

## Working Directory
.worktrees/collector-api-ws-reconnect-stability

## Files to Read
- CLAUDE.md
- backend/CLAUDE.md
- docs/phase/collector-api/ws-reconnect-stability/spec.md
- backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/kis/source/ws/KisWebSocketCollector.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/kis/source/ws/KisWsConnectionRegistry.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/service/KisWsHealthService.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/kis/KisWsHealthController.kt
- backend/collector-api/src/test/kotlin/com/papertrading/collector/infra/kis/source/ws/KisWsConnectionRegistryTest.kt
- backend/collector-api/src/test/kotlin/com/papertrading/collector/application/kis/service/KisWsHealthServiceTest.kt

## Tasks

### Substep 1 — Feature-Scoped Tests (verify or add missing)

**`KisWsConnectionRegistryTest`**

Confirm the following cases are covered; add any that are missing:
1. Initial state for a new mode: status=DISCONNECTED, lastConnectedAt=null, reconnectAttempts=0
2. `markConnected` → status=CONNECTED, lastConnectedAt is non-null and close to Instant.now(), reconnectAttempts=0
3. `markReconnecting(mode, 3)` → status=RECONNECTING, reconnectAttempts=3
4. `markDisconnected` after `markConnected` → status=DISCONNECTED, lastConnectedAt still non-null (not wiped)
5. `snapshot` with a mode not yet registered → WsHealthSnapshot with DISCONNECTED, null lastConnectedAt, 0 attempts
6. `snapshot` returns list sorted by mode for multiple modes
7. Concurrent safety: 50 goroutines alternating `markConnected` / `markReconnecting` for the same mode produce no NPE or corrupt state

**`KisWsHealthServiceTest`**

Confirm the following:
1. Returns list sorted by mode (e.g., "live" before "paper" alphabetically)
2. Delegates to `registry.snapshot(normalizedModes())` with the exact modes from `KisProperties.normalizedModes()`

---

### Substep 2 — Integration / Controller Test

**New test file:** `backend/collector-api/src/test/kotlin/com/papertrading/collector/presentation/kis/KisWsHealthControllerTest.kt`

Use `@WebMvcTest(KisWsHealthController::class)` with a mocked `KisWsHealthService`.

Test cases:
1. `GET /api/kis/ws/health returns 200 with correct JSON shape`
   - Mock `KisWsHealthService.health()` to return two snapshots (paper=CONNECTED, live=RECONNECTING with reconnectAttempts=2)
   - Assert response is a JSON array of two objects
   - Assert each object has fields: `mode`, `status`, `lastConnectedAt`, `reconnectAttempts`
   - Assert `status` is a string (e.g., "CONNECTED"), not an enum ordinal
   - Assert `lastConnectedAt` is either null or an ISO-8601 string

2. `GET /api/kis/ws/health returns 200 with empty array when no modes configured`
   - Mock returns empty list
   - Assert response is `[]`

3. `GET /api/kis/ws/health — lastConnectedAt is null when never connected`
   - Mock returns snapshot with `lastConnectedAt = null`
   - Assert `lastConnectedAt` JSON field is `null` (not absent, not empty string)

---

### Substep 3 — Coverage Check

Run the test suite and verify:
- `KisWsConnectionRegistry`: all public methods covered
- `KisWsHealthService`: all branches covered
- `KisWsHealthController`: all test cases pass

```bash
cd .worktrees/collector-api-ws-reconnect-stability/backend/collector-api && ./gradlew test
```

If any test fails, diagnose and fix the test (not the implementation, unless the implementation has a genuine bug — document in the report).

## Acceptance Criteria
```bash
cd .worktrees/collector-api-ws-reconnect-stability/backend/collector-api && ./gradlew test
```

All tests pass. No skipped tests. No TODO/FIXME in test files.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to worktree root>
- Test result: <passed N/N | failed N — list failing cases>
- Blockers: <none | description>
---
