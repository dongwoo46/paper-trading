# Tax Summary Phase Summary

## 개요
`trading-api/tax-summary` phase에서 계좌 연도별 세금 요약 스냅샷 산출/조회/재계산 기능을 구현하고, 실행 이력 추적 및 API/테스트까지 완료했다.

## 구현 결과
- `tax_summaries` 확장(`computed_at`, `status`) 및 `tax_summary_runs` 실행 이력 테이블/인덱스 추가.
- Tax 도메인 모델/VO(`TaxYear`, `TaxComputationResult`, `TaxSummaryRun`)와 저장소 시그니처 추가.
- `TaxSummaryCommandService`/`TaxSummaryCalculator`로 재계산 규칙(BigDecimal scale 4, RUNNING 중복 차단, 실패 전이) 구현.
- `TaxSummaryQueryService`, `TaxSummaryBatchService`, TaxSummary 관련 컨트롤러/DTO 및 spec 기반 에러 매핑 구현.
- 세금 요약 단위/통합 테스트와 주문·정산 회귀 스모크를 포함한 `*TaxSummary*` 검증 통과.

## 산출물
- 세금 요약 조회 API: `GET /api/accounts/{accountId}/tax-summaries/{taxYear}`
- 세금 요약 구간 조회 API: `GET /api/accounts/{accountId}/tax-summaries?fromYear=&toYear=`
- 수동 재계산 API: `POST /api/accounts/{accountId}/tax-summaries/{taxYear}/recalculate`
- 연말 배치 API: `POST /api/tax-summaries/jobs/year-end`

## 검증
- `./gradlew compileKotlin`
- `./gradlew test --tests "*TaxSummary*"`
