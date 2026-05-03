# Step 3: 세금 요약 API/배치 엔드포인트 구현
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-tax-summary

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/trading-api/tax-summary/spec.md
- docs/phase/trading-api/tax-summary/step-2.md
- backend/trading-api/src/main/kotlin/com/papertrading/api/application
- backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller
- backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto

## Tasks
1. Query 서비스 추가.
- `TaxSummaryQueryService`
  - `fun get(accountId: Long, taxYear: Int): TaxSummaryResult`
  - `fun list(accountId: Long, fromYear: Int, toYear: Int): List<TaxSummaryResult>`

2. Controller 및 DTO 구현.
- `TaxSummaryController`
  - `GET /api/accounts/{accountId}/tax-summaries/{taxYear}`
  - `GET /api/accounts/{accountId}/tax-summaries`
  - `POST /api/accounts/{accountId}/tax-summaries/{taxYear}/recalculate`
- `TaxSummaryJobController`
  - `POST /api/tax-summaries/jobs/year-end`

3. 에러 매핑 추가.
- 400/404/409/422/500을 spec 코드에 맞춰 일관된 에러 응답으로 매핑.

4. 배치 트리거 서비스 추가.
- `TaxSummaryBatchService`
  - `fun runYearEnd(taxYear: Int, accountIds: List<Long>?): String`

## Acceptance Criteria
```bash
cd .worktrees/trading-api-tax-summary/backend/trading-api
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
- Blockers: <none | description>
---
