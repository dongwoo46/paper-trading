# Step 2: 세금 정산 도메인/마이그레이션/서비스 구현 (TDD)
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-tax-summary-service/backend/trading-api

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/trading-api/tax-summary-service/spec.md
- backend/trading-api/graphify-out/graph.json
- backend/trading-api/src/main/resources/db/migration
- backend/trading-api/src/main/kotlin/com/papertrading/api/domain
- backend/trading-api/src/main/kotlin/com/papertrading/api/application

## Tasks
1. Flyway 마이그레이션 추가/보완
- `tax_summaries` 컬럼 검증(`computed_at`, `status`) 및 필요한 인덱스 보강
- `tax_summary_runs` 테이블/인덱스 생성

2. Domain/VO/Repository 시그니처 추가
- `TaxYear`, `TaxComputationResult`, `TaxSummaryRun`
- `TaxSummaryRunRepository` / Settlement 집계 조회 포트

3. Application 서비스(TDD)
- `TaxSummaryCommandService.recalculate(accountId, taxYear, force)`
- `TaxSummaryCalculator.compute(...)`
- RUNNING 중복 차단, 실패 시 FAILED 상태 저장

## Acceptance Criteria
```bash
cd .worktrees/trading-api-tax-summary-service/backend/trading-api
./gradlew compileKotlin
./gradlew test --tests "*TaxSummaryCommandServiceTest"
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
