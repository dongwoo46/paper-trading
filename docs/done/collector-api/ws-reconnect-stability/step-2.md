# Step 2: Implementation (TDD)
Assigned agent: fullstack-dev

## Working Directory
.worktrees/collector-api-ws-reconnect-stability

## Files to Read
- CLAUDE.md
- backend/CLAUDE.md
- backend/collector-api/CLAUDE.md
- docs/phase/collector-api/ws-reconnect-stability/spec.md
- backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/kis/KisProperties.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/kis/source/ws/KisWebSocketCollector.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/service/KisWsSubscriptionService.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/kis/KisSubscriptionController.kt
- backend/collector-api/src/main/resources/application.yaml

## Tasks

### TDD Order: write failing test → implement → green → next

---

### Task 1 — `WsConnectionStatus` enum

**File:** `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/kis/WsConnectionStatus.kt`

Define enum with values: `CONNECTED`, `DISCONNECTED`, `RECONNECTING`.
No framework dependencies.

---

### Task 2 — `WsHealthSnapshot` data class

**File:** `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/dto/WsHealthSnapshot.kt`

Fields:
- `mode: String`
- `status: WsConnectionStatus`
- `lastConnectedAt: java.time.Instant?`
- `reconnectAttempts: Long`

Immutable data class. No framework dependencies.

---

### Task 3 — `KisWsConnectionRegistry`

**File:** `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/kis/source/ws/KisWsConnectionRegistry.kt`

`@Component`. Thread-safe. Holds per-mode state using `ConcurrentHashMap`.

Internal state per mode:
- `status: WsConnectionStatus` (volatile or AtomicReference)
- `lastConnectedAt: Instant?`
- `reconnectAttempts: Long` (AtomicLong per mode)

Public API:
```
fun markConnected(mode: String)
    → status = CONNECTED, lastConnectedAt = Instant.now(), reconnectAttempts = reset to 0

fun markReconnecting(mode: String, attempt: Long)
    → status = RECONNECTING, reconnectAttempts = attempt

fun markDisconnected(mode: String)
    → status = DISCONNECTED (do NOT reset lastConnectedAt or reconnectAttempts)

fun snapshot(modes: List<String>): List<WsHealthSnapshot>
    → returns one WsHealthSnapshot per mode; for unknown modes returns status=DISCONNECTED, lastConnectedAt=null, reconnectAttempts=0
```

Concurrency rule: each mode's fields are updated atomically using a per-mode data holder class (inner data class with AtomicReference or synchronized block). Do not use a global lock across all modes.

---

### Task 4 — `KisWsHealthService`

**File:** `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/service/KisWsHealthService.kt`

`@Service`. Constructor-injected: `KisWsConnectionRegistry`, `KisProperties`.

```
fun health(): List<WsHealthSnapshot>
    → calls registry.snapshot(properties.normalizedModes())
    → returns result sorted by mode ascending
```

No `@Transactional` — no DB access.

---

### Task 5 — `KisWsHealthController`

**File:** `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/kis/KisWsHealthController.kt`

`@RestController`. `@RequestMapping("/api/kis/ws")`. Constructor-injected: `KisWsHealthService`.

```
@GetMapping("/health")
fun health(): List<WsHealthSnapshotResponse>
```

Response DTO: `WsHealthSnapshotResponse` (defined in same package or `dto` sub-package)
Fields matching `WsHealthSnapshot` but with JSON serialization:
- `mode: String`
- `status: String`  (enum name, e.g. "CONNECTED")
- `lastConnectedAt: String?`  (ISO-8601 UTC, nullable)
- `reconnectAttempts: Long`

Map `WsHealthSnapshot` → `WsHealthSnapshotResponse` in the controller. Do not leak domain types into the HTTP response.

---

### Task 6 — Modify `KisProperties`

**File:** `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/kis/KisProperties.kt`

Add field:
```kotlin
var heartbeatTimeout: Duration = Duration.ofSeconds(60)
```

No other changes to existing fields.

---

### Task 7 — Modify `application.yaml`

**File:** `backend/collector-api/src/main/resources/application.yaml`

Under `collector.source.kis`, add:
```yaml
heartbeat-timeout: 60s
```

Place after `reconnect-max-delay` line.

---

### Task 8 — Modify `KisWebSocketCollector`

**File:** `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/kis/source/ws/KisWebSocketCollector.kt`

Constructor: add `KisWsConnectionRegistry` parameter.

**In `connectOnce(mode)`:**

1. After `webSocketClient.execute(endpoint) { session ->` and the log line, call `registry.markConnected(mode)`.

2. On the receiver flux chain, add `.timeout(properties.heartbeatTimeout)` immediately after `.handle<String> { ... }` and before `.doOnNext { rawEventPipeline.publish(...) }`.
   - This causes a `TimeoutException` after `heartbeatTimeout` seconds of silence, which propagates as an error and triggers the existing retry loop.

3. In `doFinally { signal -> ... }` (already present), add call to `registry.markDisconnected(mode)`.

**In `startMode(mode)`:**

In the `retryWhen` block's `.doBeforeRetry { signal -> ... }` (already present), add:
```
registry.markReconnecting(mode, signal.totalRetries() + 1)
```

**Ordering rule:** `markConnected` must be called inside the WebSocket session handler after the session is established, before the sender/receiver are wired. `markDisconnected` must be called in `doFinally` (which fires on both normal completion and error). `markReconnecting` is called in `doBeforeRetry` before the next attempt.

**Do not** modify `emit()`, `stop()`, `start()`, or `buildSubscribeMessages()`.

---

### Task 9 — Tests

**Test file:** `backend/collector-api/src/test/kotlin/com/papertrading/collector/infra/kis/source/ws/KisWsConnectionRegistryTest.kt`

Write unit tests for `KisWsConnectionRegistry`:

1. `markConnected sets status to CONNECTED and resets reconnectAttempts`
2. `markReconnecting sets status to RECONNECTING and stores attempt count`
3. `markDisconnected sets status to DISCONNECTED without clearing lastConnectedAt`
4. `snapshot returns DISCONNECTED with nulls for unknown mode`
5. `snapshot returns correct state after connect-disconnect-reconnect sequence`
6. `concurrent markConnected and markReconnecting calls do not corrupt state` (use multiple threads)

**Test file:** `backend/collector-api/src/test/kotlin/com/papertrading/collector/application/kis/service/KisWsHealthServiceTest.kt`

Write unit tests for `KisWsHealthService`:

1. `health returns snapshots sorted by mode`
2. `health delegates to registry with normalizedModes`

Use MockK or Mockito for mocks.

## Acceptance Criteria
```bash
cd .worktrees/collector-api-ws-reconnect-stability/backend/collector-api && ./gradlew compileKotlin
cd .worktrees/collector-api-ws-reconnect-stability/backend/collector-api && ./gradlew test
```

All tests pass. No compile errors. No unresolved TODO/FIXME in modified files.

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
