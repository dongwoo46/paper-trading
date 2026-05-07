# Tax Summary Service Summary

## Scope
- trading-api `tax-summary-service` phase 마무리
- 연간 세금 정산 집계 생성/재계산, 조회 API, 배치 실행 경로 및 예외 매핑 구현

## Key Changes
- 도메인/애플리케이션
  - `TaxSummaryCommandService`, `TaxSummaryQueryService`, `TaxSummaryBatchService` 구현 및 runType(수동/배치) 분기 반영
  - 세금 계산기(`TaxSummaryCalculator`) 및 실행 이력(`TaxSummaryRun`) 처리 강화
- 인프라/DB
  - `V3__tax_summary_service.sql` 추가/보강
  - 실행 중복 방지 제약 및 정산 집계 저장/조회 리포지토리 구현
  - 다중 통화 정산 집계 시 fail-fast 처리로 왜곡 방지
- 프레젠테이션
  - `TaxSummaryController` + DTO + 글로벌 예외 매핑 연결
- 품질 보강
  - 리뷰 지적사항(runType 분류, 통화 집계, 동시성/제약, 테스트 불일치) 리워크 반영

## Tests Run / Result
- `./gradlew compileKotlin` 통과
- `./gradlew test --tests "*TaxSummary*" --tests "*TaxSummaryController*"` 통과

## Known Limitations
- 전체 테스트 스위트 회귀는 본 phase에서 수행하지 않았고, tax-summary 관련 타깃 검증 중심으로 완료함
- PR 번호는 아직 미정(`pr: #TBD`)
