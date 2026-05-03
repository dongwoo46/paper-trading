# Step 2: 세금 정산 도메인/마이그레이션/서비스 구현 (TDD)
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-tax-summary

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/trading-api/tax-summary/spec.md
- backend/trading-api/graphify-out/graph.json
- backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/TaxSummary.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Settlement.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountRepository.kt
- backend/trading-api/src/main/resources/db/migration

## Tasks
1. Flyway 마이그레이션 추가.
- `tax_summaries`에 `computed_at`, `status` 컬럼 추가.
- `tax_summary_runs` 테이블/인덱스 생성.

2. Domain 모델/VO 시그니처 추가.
- `TaxYear` VO
  - `class TaxYear(val value: Int)`
- `TaxComputationResult` VO
  - `data class TaxComputationResult(val totalRealizedPnl: BigDecimal, val taxablePnl: BigDecimal, val estimatedTax: BigDecimal)`
- `TaxSummaryRun` Entity
  - `class TaxSummaryRun(...)`

3. Repository 시그니처 추가.
- `TaxSummaryRepository`
  - `fun findByAccountIdAndTaxYear(accountId: Long, taxYear: Int): TaxSummary?`
- `TaxSummaryRunRepository`
  - `fun existsRunning(accountId: Long, taxYear: Int): Boolean`
  - `fun save(run: TaxSummaryRun): TaxSummaryRun`
- Settlement 집계 조회 port/read repository
  - `fun summarizeForTax(accountId: Long, yearStart: Instant, yearEnd: Instant): SettlementTaxAggregate`

4. Application 서비스 골격(TDD) 구현.
- `TaxSummaryCommandService`
  - `fun recalculate(accountId: Long, taxYear: Int, force: Boolean): Long`
- `TaxSummaryCalculator`
  - `fun compute(input: SettlementTaxAggregate): TaxComputationResult`
- 규칙
  - BigDecimal scale 4 고정
  - 동일 계좌/연도 RUNNING 중복 실행 차단
  - 실패 시 run status FAILED 저장

## Acceptance Criteria
```bash
cd .worktrees/trading-api-tax-summary/backend/trading-api
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
- Blockers: <none | description>
---
