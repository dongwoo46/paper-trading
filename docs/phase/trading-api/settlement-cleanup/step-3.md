# Step 3: API/Repository/DB Cleanup + TaxSummary LOCAL Scope
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-settlement-cleanup

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/trading-api/settlement-cleanup/spec.md
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/ReceivableSettlementQueryService.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/query/ReceivableSettlementFilter.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/result/ReceivableSettlementResult.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/ReceivableSettlementController.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/account/ReceivableSettlementDto.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ReceivableSettlementRepository.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ReceivableSettlementRepositoryCustom.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ReceivableSettlementRepositoryImpl.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCommandService.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax/SettlementTaxReadRepository.kt
- backend/trading-api/src/main/resources/db/migration

## Tasks
1. Remove receivable-settlement API and query layer.
- Delete receivable settlement controller, DTO, query service, filter/result model, repository interfaces/impl.
- Remove route exposure and bean wiring references.

2. Migrate database schema cleanup.
- Add Flyway migration to drop `receivable_settlements` table and related indexes/constraints.
- Ensure migration order and naming follow existing convention in `db/migration`.

3. Restrict TaxSummary to LOCAL accounts only.
- Add account trading-mode guard in `TaxSummaryCommandService` and query/recalc entry points.
- Update `SettlementTaxReadRepository` query conditions to aggregate only settlements for LOCAL accounts.
- Define deterministic behavior for KIS accounts (domain exception mapped to 400).

4. Remove obsolete exception types and references.
- Delete `ReceivableSettlementNotFoundException` and related dead paths in common exception mapping.

## Acceptance Criteria
```bash
cd .worktrees/trading-api-settlement-cleanup/backend/trading-api
./gradlew compileKotlin
./gradlew test --tests "*TaxSummary*"
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