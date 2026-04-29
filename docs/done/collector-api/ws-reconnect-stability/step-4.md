# Step 4: Code Review
Assigned agent: code-reviewer

## Working Directory
.worktrees/collector-api-ws-reconnect-stability

## Files to Read
- CLAUDE.md
- backend/CLAUDE.md
- docs/phase/collector-api/ws-reconnect-stability/spec.md
- backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/kis/WsConnectionStatus.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/dto/WsHealthSnapshot.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/kis/source/ws/KisWsConnectionRegistry.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/kis/source/ws/KisWebSocketCollector.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/service/KisWsHealthService.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/kis/KisWsHealthController.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/kis/KisProperties.kt
- backend/collector-api/src/test/kotlin/com/papertrading/collector/infra/kis/source/ws/KisWsConnectionRegistryTest.kt
- backend/collector-api/src/test/kotlin/com/papertrading/collector/application/kis/service/KisWsHealthServiceTest.kt
- backend/collector-api/src/test/kotlin/com/papertrading/collector/presentation/kis/KisWsHealthControllerTest.kt

## Review Checklist

### Architecture
- [ ] `WsConnectionStatus` has zero framework dependencies (no Spring, no JPA)
- [ ] `WsHealthSnapshot` has zero framework dependencies
- [ ] `KisWsConnectionRegistry` is in the infrastructure layer (`infra.kis.source.ws`)
- [ ] `KisWsHealthService` is in the application layer (`application.kis.service`)
- [ ] `KisWsHealthController` is in the presentation layer (`presentation.kis`)
- [ ] Domain types (`WsConnectionStatus`) are not leaked into HTTP response DTOs — controller maps to `WsHealthSnapshotResponse`

### Concurrency Safety (KisWsConnectionRegistry)
- [ ] Per-mode state updates are atomic — no possibility of reading partially updated state
- [ ] `markConnected` resets `reconnectAttempts` to 0 atomically with setting `lastConnectedAt`
- [ ] No global lock across all modes — only per-mode synchronization
- [ ] No use of `!!` operator anywhere

### KisWebSocketCollector Changes
- [ ] `markConnected(mode)` is called inside the session handler, after the session is established
- [ ] `markDisconnected(mode)` is called in `doFinally` (fires on both normal and error termination)
- [ ] `markReconnecting(mode, attempt)` is called in `doBeforeRetry`
- [ ] `.timeout(properties.heartbeatTimeout)` is applied on the receive flux, not the sender
- [ ] The heartbeat timeout causes a `TimeoutException` that propagates through the existing retry chain without bypassing it
- [ ] `emit()`, `stop()`, `start()`, `buildSubscribeMessages()` are not modified

### API
- [ ] `GET /api/kis/ws/health` returns HTTP 200 in all cases (no 4xx for no-data scenarios)
- [ ] `lastConnectedAt` is serialized as ISO-8601 UTC string or null — never an epoch long
- [ ] `status` is serialized as string name — not enum ordinal
- [ ] Response is a JSON array (even for single mode or zero modes)

### Config
- [ ] `heartbeatTimeout` has a sensible default (60s)
- [ ] `application.yaml` entry is present under `collector.source.kis`
- [ ] `KisProperties` field uses `Duration` type, not `Long`

### Tests
- [ ] Registry concurrency test uses actual threads (not coroutines bypassing JVM thread safety)
- [ ] All controller tests assert the JSON shape (field names, types)
- [ ] No `@Disabled` or skipped tests
- [ ] Test file names match the class under test

### General Code Quality
- [ ] No hardcoded strings where constants should be used
- [ ] No unresolved TODO/FIXME in any modified file
- [ ] Constructor injection only (no `@Autowired` field injection)
- [ ] `val` preferred over `var` in all new code

## Tasks

For each failed checklist item:
1. Note the file and line number
2. Describe the issue
3. Provide the fix (edit the file in the worktree)

If any fix changes logic (not just style), re-run the build to verify:
```bash
cd .worktrees/collector-api-ws-reconnect-stability/backend/collector-api && ./gradlew compileKotlin
```

## Acceptance Criteria
All checklist items pass. Build compiles cleanly. No regressions introduced by review fixes.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was reviewed and what was found>
- Files modified: <list of paths relative to worktree root, or "none" if no fixes needed>
- Test result: <passed N/N | failed N — list failing cases> (if applicable)
- Blockers: <none | description>
---
