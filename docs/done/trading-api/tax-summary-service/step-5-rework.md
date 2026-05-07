# Step 5 (Rework): 코드리뷰 지적사항 및 실패 테스트 수정
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-tax-summary-service/backend/trading-api

## Files to Read
- CODEX.md
- docs/phase/trading-api/tax-summary-service/spec.md
- docs/phase/trading-api/tax-summary-service/step-2.md
- docs/phase/trading-api/tax-summary-service/step-3.md
- docs/phase/trading-api/tax-summary-service/index.json
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCommandService.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementTaxReadRepositoryImpl.kt
- backend/trading-api/src/main/resources/db/migration/V3__tax_summary_service.sql
- backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/tax/TaxSummaryCommandServiceTest.kt
- backend/trading-api/src/test/kotlin/com/papertrading/api/domain/entity/portfolio/TaxSummaryTest.kt

## Tasks
1. `runType` 분류 개선
- 수동 재계산/배치 실행이 `TaxSummaryRunType`에 맞게 기록되도록 수정.

2. 통화 집계 안전성 보강
- 다중 통화 혼합 집계 시 계산 왜곡이 발생하지 않도록 차단/예외 처리.

3. 동시성 보강
- `tax_summary_runs`에 account 참조 무결성 및 중복 실행 완화 제약 추가.

4. 테스트 정합성 수정
- `TaxSummaryCommandServiceTest` 기대 예외 타입을 실제 도메인 예외와 일치.
- `TaxSummaryTest` 0 세금 scale 기대값 정렬.

5. 검증
```bash
./gradlew compileKotlin
./gradlew test --tests "*TaxSummary*" --tests "*TaxSummaryController*"
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
