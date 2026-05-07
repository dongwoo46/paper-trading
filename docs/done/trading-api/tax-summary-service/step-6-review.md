# Step 6: Re-review after rework
Assigned agent: code-reviewer

## Working Directory
.worktrees/trading-api-tax-summary-service/backend/trading-api

## Files to Read
- CODEX.md
- docs/phase/trading-api/tax-summary-service/index.json
- docs/phase/trading-api/tax-summary-service/spec.md
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax
- backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementTaxReadRepositoryImpl.kt
- backend/trading-api/src/main/resources/db/migration/V3__tax_summary_service.sql
- backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/tax
- backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryTest.kt

## Tasks
1. Rework 적용 후 리스크 재검토 (정확성/회귀/동시성/API 계약).
2. must-fix 항목 없으면 PASS.

## Agent Return Protocol
---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Findings:
  - <severity> <file:line> <issue>
- Test result: <passed N/N | failed N — list failing cases | not run>
- Blockers: <none | description>
---
