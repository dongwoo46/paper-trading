# Tax Summary Service

## Core Feature
계좌별 연간 실현손익/과세대상 손익/예상세액을 집계하고, 조회·재계산 API를 제공하는 tax-summary-service 구현.

## Scope
- 포함: TaxSummaryCommandService, TaxSummaryQueryService, TaxSummaryController/DTO, 연말 배치 트리거, 테스트 보강.
- 제외: 전략 실행/자동매수 로직, 해외통화 환산 고도화, 백테스트 연동.

## Domain Rules
- 금액 연산은 BigDecimal scale 4 고정.
- 집계 대상은 `Settlement.settledAt` 기준 확정 체결만 포함.
- 동일 `accountId + taxYear` 재계산은 멱등하게 마지막 성공 스냅샷으로 치환.
- 동일 `accountId + taxYear` RUNNING 중복 실행은 차단.

## API Contract
- `GET /api/accounts/{accountId}/tax-summaries/{taxYear}`
- `GET /api/accounts/{accountId}/tax-summaries?fromYear=&toYear=`
- `POST /api/accounts/{accountId}/tax-summaries/{taxYear}/recalculate`
- `POST /api/tax-summaries/jobs/year-end`

## Error Contract
- 400 INVALID_TAX_YEAR_RANGE
- 400 TAX_YEAR_NOT_CLOSED
- 404 ACCOUNT_NOT_FOUND
- 404 TAX_SUMMARY_NOT_FOUND
- 409 TAX_SUMMARY_ALREADY_RUNNING
- 422 UNSUPPORTED_CURRENCY
- 500 TAX_SUMMARY_COMPUTE_FAILED

## Data Model
- `tax_summaries`: `computed_at`, `status` 포함 스냅샷 유지.
- `tax_summary_runs`: 실행 이력(run_type/status/start/finish/error_message) 저장.
- 인덱스
  - `uk_tax_summaries_account_year (account_id, tax_year)`
  - `idx_tax_summary_runs_account_year_started (account_id, tax_year, started_at desc)`

## Implementation Plan
1. 도메인/마이그레이션/커맨드 서비스(TDD).
2. 조회/컨트롤러/배치 API 구현.
3. 테스트 보강 및 회귀 검증.
4. 코드리뷰.
5. 문서 정리/phase 마감.

## Open Decisions (MVP 기본값)
- 과세대상 손익은 KRW 단일 통화 기준으로 계산한다.
- 연도 종료 전 재계산은 `force=false`면 차단한다.
- 권한 제어는 기존 API 인증/인가 정책을 따르며 신규 role은 추가하지 않는다.
