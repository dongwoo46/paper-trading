# Step 4: Code Review

**Agent**: code-reviewer
**Branch**: feature/collector-api-subscription-monitor
**Working Directory**: C:\Users\dw\Desktop\paper-trading\.worktrees\collector-api-subscription-monitor

---

## Files to Read (in order)

1. `C:\Users\dw\Desktop\paper-trading\docs\phase\collector-api\subscription-monitor\spec.md`
2. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\main\kotlin\com\papertrading\collector\application\subscriptions\dto\SubscriptionModeStatus.kt`
3. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\main\kotlin\com\papertrading\collector\application\subscriptions\dto\SubscriptionStatusReport.kt`
4. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\main\kotlin\com\papertrading\collector\application\subscriptions\service\SubscriptionStatusService.kt`
5. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\main\kotlin\com\papertrading\collector\presentation\subscriptions\dto\SubscriptionStatusResponse.kt`
6. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\main\kotlin\com\papertrading\collector\presentation\subscriptions\SubscriptionStatusController.kt`
7. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\test\kotlin\com\papertrading\collector\application\subscriptions\service\SubscriptionStatusServiceTest.kt`
8. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\test\kotlin\com\papertrading\collector\presentation\subscriptions\SubscriptionStatusControllerTest.kt`
9. `C:\Users\dw\Desktop\paper-trading\backend\CLAUDE.md`

---

## Review Checklist

### 1. Architecture & DDD

- [ ] Dependency direction: presentation → application → domain. No upward dependency.
- [ ] No domain entities leaked into presentation layer (response DTOs use primitive types only).
- [ ] `SubscriptionStatusReport` and `SubscriptionModeStatus` live in `application/subscriptions/dto/`, not in domain.
- [ ] Controller delegates entirely to `SubscriptionStatusService` — no business logic in controller.
- [ ] `SubscriptionStatusService` is read-only (no side effects, no writes).

### 2. Kotlin Best Practices (CLAUDE.md)

- [ ] Constructor injection only — no `@Autowired` field injection.
- [ ] All properties are `val`, not `var`.
- [ ] No `!!` (non-null assertion) used anywhere in new files.
- [ ] `data class` used for DTOs.
- [ ] No hardcoded credentials or secrets.

### 3. API Contract

- [ ] `GET /api/subscriptions/status` maps to correct URL.
- [ ] `lastConnectedAt` is serialized as `null` (omitted or null in JSON) when no connection has been established.
- [ ] `generatedAt`, `lastConnectedAt` are ISO-8601 UTC strings (not epoch longs).
- [ ] `connectionStatus` is serialized as the enum name string (CONNECTED/DISCONNECTED/RECONNECTING).
- [ ] `modes` list is sorted alphabetically.
- [ ] `wsSymbols` and `restSymbols` are sorted alphabetically.

### 4. Thread Safety

- [ ] `SubscriptionStatusService.report()` does not mutate any shared state.
- [ ] `KisWsConnectionRegistry` (used via `KisWsHealthService`) is already thread-safe — verify service does not bypass it.
- [ ] `KisWsSubscriptionService.listSymbolsPerMode()` reads from Redis — verify no mutation.

### 5. Test Quality

- [ ] Mocks use MockK (`mockk()`, not Mockito).
- [ ] Each test name follows `snake_case_describes_scenario` convention.
- [ ] Tests cover: disabled state, mode assembly, sorting, slot calculation, edge cases (empty maps, missing health).
- [ ] Controller test uses `MockMvc` standalone setup — no Spring context boot.
- [ ] No `@SpringBootTest` for unit tests (performance).

### 6. Error Handling

- [ ] Service does not throw on missing mode in health/subscription maps — uses `.orEmpty()` fallback.
- [ ] `GlobalExceptionHandler` handles 500 — no need for extra try/catch in new code.

---

## Issues to File (if found)

For each issue found, document:
```
**Issue [n]**: <title>
- Severity: CRITICAL | MAJOR | MINOR
- Location: <file>:<line>
- Description: <what is wrong>
- Fix: <what should be done>
```

---

## Required Actions

1. Fix any CRITICAL issues before marking review as PASS.
2. Fix MAJOR issues in this step.
3. MINOR issues may be noted and deferred if they don't affect correctness.

---

## Build Verification

After any fixes:
```bash
cd backend/collector-api && ./gradlew compileKotlin
cd backend/collector-api && ./gradlew test
```

---

## Completion Criteria

- [ ] All CRITICAL and MAJOR issues resolved
- [ ] Build passes
- [ ] All tests pass
- [ ] API contract matches spec.md

---

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
