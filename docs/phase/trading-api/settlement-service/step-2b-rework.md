# Step 2b: Rework — Batch Transaction Isolation Fix
Assigned agent: Full Stack Developer

## Working Directory
`.worktrees/trading-api-settlement-service`

## Context
Code review identified two 🔴 must-fix issues in `SettlementCommandService`.

---

## Files to Read
- `CLAUDE.md`
- `docs/phase/trading-api/settlement-service/spec.md`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/settlement/SettlementCommandService.kt`
- `backend/trading-api/src/test/kotlin/com/papertrading/api/application/settlement/SettlementCommandServiceTest.kt`

---

## Issues to Fix

### Issue 1: Batch failure isolation broken by shared outer transaction

`processSettlements` is `@Transactional`. All iterations share one transaction. If any JPA operation inside `processOne` throws (e.g., `ConstraintViolationException`), the `EntityManager` is marked rollback-only — even though `runCatching` catches the Kotlin exception, Spring rolls back the entire batch on commit.

**Fix:** Extract `processOne` into a separate Spring bean with `@Transactional(propagation = Propagation.REQUIRES_NEW)`. Self-invocation does not work — it must be a separate injectable component.

Suggested structure:
```
SettlementCommandService        — orchestrates batch, no @Transactional on processSettlements
SettlementProcessor (new bean)  — processOne(@Transactional REQUIRES_NEW)
```

`processSettlements` injects `SettlementProcessor` and calls `settlementProcessor.processOne(ps)` inside the loop.

### Issue 2: Missing `accountRepository.save(account)` in processOne

After `account.receiveSellProceeds(amount)`, the account mutation must be explicitly persisted. With `REQUIRES_NEW`, the account entity is loaded in a new transaction — dirty-check on commit will handle it if the entity is managed, but an explicit `accountRepository.save(account)` makes the intent unambiguous and prevents silent data loss if the entity becomes detached.

Add `accountRepository.save(account)` after `account.receiveSellProceeds(amount)`.

---

## Tasks

1. Create `SettlementProcessor` in `application/settlement/` package:
   - Single public method: `fun processOne(ps: PendingSettlement)` with `@Transactional(propagation = Propagation.REQUIRES_NEW)`
   - Move processOne logic from `SettlementCommandService` into this bean
   - Add explicit `accountRepository.save(account)` after `account.receiveSellProceeds()`

2. Update `SettlementCommandService`:
   - Inject `SettlementProcessor`
   - Remove `@Transactional` from `processSettlements` (loop-level transaction not needed)
   - Keep `@Transactional` on `processSettlement(id)` (single re-process delegates to `settlementProcessor.processOne`)
   - Call `settlementProcessor.processOne(ps)` inside `runCatching` loop

3. Update `SettlementCommandServiceTest`:
   - Add `SettlementProcessor` mock
   - Verify batch isolation: `settlementProcessor.processOne()` called per item, failure on one does not affect others

---

## Acceptance Criteria

```bash
cd .worktrees/trading-api-settlement-service/backend/trading-api
./gradlew test --tests "com.papertrading.api.application.settlement.SettlementCommandServiceTest"
./gradlew compileKotlin
```

All tests pass. No compile errors.

## Agent Return Protocol

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <list>
- Test result: <passed N/N>
- Blockers: <none | description>
---
