# Step 2: Settlement Cleanup Core Refactor (TDD)
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-settlement-cleanup

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/PRD.md
- docs/phase/trading-api/settlement-cleanup/spec.md
- backend/trading-api/graphify-out/graph.json
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/Settlement.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/SettlementExecution.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/ReceivableSettlement.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/settlement/SettlementCommandService.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/settlement/SettlementProcessor.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/settlement/BusinessDayCalculator.kt

## Tasks
1. Remove receivable settlement domain/application path.
- Delete `ReceivableSettlement` entity and dependent application services.
- Delete `SettlementCommandService`, `SettlementProcessor`, and related settlement command DTO that only exists for receivable settlement flow.

2. Simplify KIS branch in `ExecutionProcessor`.
- Keep `LOCAL` sell settlement recording in DB.
- Remove KIS (`KIS_PAPER`, `KIS_LIVE`) receivable settlement creation and internal cash settlement mutation path.
- Ensure KIS execution flow remains fill/status synchronization only.

3. Apply LOCAL sell tax rule.
- In LOCAL sell settlement calculation, set securities transaction tax to `grossProceeds * 0.0020`.
- Preserve BigDecimal scale/rounding consistency used by existing settlement calculations.

4. Clean compile references.
- Remove constructor injections/imports/references to deleted services and classes.
- Keep only dependencies required by LOCAL settlement recording and KIS sync flow.

## Acceptance Criteria
```bash
cd .worktrees/trading-api-settlement-cleanup/backend/trading-api
./gradlew compileKotlin
./gradlew test --tests "*ExecutionProcessorTest"
```

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---