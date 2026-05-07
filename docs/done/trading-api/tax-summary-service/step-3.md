# Step 3: 세금 요약 API/배치 엔드포인트 구현
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-tax-summary-service/backend/trading-api

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/trading-api/tax-summary-service/spec.md
- docs/phase/trading-api/tax-summary-service/step-2.md
- backend/trading-api/src/main/kotlin/com/papertrading/api/application
- backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller
- backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto

## Tasks
1. Query 서비스 추가
- `TaxSummaryQueryService.get/list`

2. Controller/DTO 구현
- `GET /api/accounts/{accountId}/tax-summaries/{taxYear}`
- `GET /api/accounts/{accountId}/tax-summaries`
- `POST /api/accounts/{accountId}/tax-summaries/{taxYear}/recalculate`
- `POST /api/tax-summaries/jobs/year-end`

3. 에러 매핑 정렬
- 400/404/409/422/500 spec 코드 매핑

4. 배치 트리거 서비스
- `TaxSummaryBatchService.runYearEnd(...)`

## Acceptance Criteria
```bash
cd .worktrees/trading-api-tax-summary-service/backend/trading-api
./gradlew compileKotlin
./gradlew test --tests "*TaxSummaryController*"
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
