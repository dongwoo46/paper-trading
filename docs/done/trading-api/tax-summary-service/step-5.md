# Step 5: 문서 정리 및 phase 완료 처리
Assigned agent: cleanup

## Working Directory
.worktrees/trading-api-tax-summary-service/backend/trading-api

## Files to Read
- CODEX.md
- docs/TODO.md
- docs/state.md
- docs/phase/trading-api/tax-summary-service/index.json
- docs/phase/trading-api/tax-summary-service/spec.md
- docs/phase/trading-api/tax-summary-service/step-2.md
- docs/phase/trading-api/tax-summary-service/step-3.md
- docs/phase/trading-api/tax-summary-service/step-4.md

## Tasks
1. 요약 문서 작성
- `docs/done/trading-api/tax-summary-service/tax-summary-service-summary.md`

2. phase 상태 마감
- index step 결과 반영, `status=completed`

3. TODO/state 반영
- `docs/TODO.md`의 `tax-summary-service` 항목 `[x]` 처리

## Acceptance Criteria
```bash
cd .worktrees/trading-api-tax-summary-service/backend/trading-api
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
- Test result: <passed N/N | failed N — list failing cases>
- Blockers: <none | description>
---
