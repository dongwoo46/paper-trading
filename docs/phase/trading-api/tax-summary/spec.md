# Tax Summary Automation

## Core Feature
계좌별 연간 실현손익과 과세대상 손익을 집계해 예상세액을 산출/고정하는 세금 정산 자동화 기능.

## Considerations
- 기존 `TaxSummary` 엔티티(`tax_summaries`)는 존재하지만 집계 트리거/정책/API가 미구현 상태다.
- 금액 연산은 전부 `BigDecimal`로 유지하고 scale(4) 일관성을 강제한다.
- 멱등성 보장을 위해 "계좌+연도 재계산"과 "정산 배치 실행 이력"을 분리한다.
- 환율/해외주식 과세/손익이월 규정은 현재 PRD/ADR에 정의가 없어 MVP에서는 단일 계좌통화(KRW) 기준으로 제한한다.

## Trade-offs
- Option A: 체결 이벤트마다 실시간 누적 업데이트
  - 장점: 조회 즉시성 높음
  - 단점: 보정/취소/정정 주문 반영이 복잡하고 멱등성 위험 증가
- Option B: 요청 시마다 Settlement 전체 스캔 집계
  - 장점: 구현 단순
  - 단점: 데이터 증가 시 성능 저하
- Option C (선택): "연도 단위 배치 재계산 + 결과 스냅샷 저장"
  - 장점: 규칙 변경/재산출 대응이 쉽고 감사 추적에 유리
  - 단점: 배치 전 최신값과 시차 발생

## Implementation Approach
- Domain: `TaxSummary`를 Account Aggregate 하위 정책 산출물로 취급하고, 계산 규칙은 도메인 서비스(`TaxSummaryCalculator`)로 분리.
- Application: `TaxSummaryCommandService`(재계산/확정), `TaxSummaryQueryService`(조회)로 CQRS 분리.
- Infrastructure: Settlement 합계 조회용 read repository + `tax_summary_runs` 실행 이력 테이블 + Flyway 마이그레이션 추가.
- Presentation: 연도별 조회/재계산 API 제공, 운영자 수동 재계산 endpoint 포함.

## Workflow
1. 운영자 또는 스케줄러가 `recalculate(accountId, taxYear)` 호출.
2. 서비스가 `Settlement`에서 해당 계좌/연도 실현손익·수수료·세금 원천값 집계.
3. `TaxSummaryCalculator`가 과세대상 손익과 예상세액 계산.
4. `tax_summaries` upsert(계좌+연도 unique) 후 `tax_summary_runs`에 실행 이력 기록.
5. 조회 API는 `tax_summaries` 스냅샷 반환.

## DDD Model
- Bounded Context: `Trading` 내부 `Settlement & Tax` 하위 컨텍스트
- Entity
  - `TaxSummary(id, accountId, taxYear, totalRealizedPnl, taxablePnl, estimatedTax, computedAt, status)`
  - `TaxSummaryRun(id, accountId, taxYear, runType, startedAt, finishedAt, status, errorMessage)`
- Value Object
  - `TaxYear(value: Int)`
  - `MoneyAmount(amount: BigDecimal, currency: String)`
  - `TaxComputationResult(totalRealizedPnl, taxablePnl, estimatedTax)`
- Aggregate
  - Aggregate Root: `Account`
  - Invariant
    - 계좌+연도당 `TaxSummary`는 1건만 활성 스냅샷
    - 계산 기준은 `Settlement.settledAt` 확정 건만 포함
    - 동일 연도 재계산은 마지막 성공 결과로 치환(멱등)
- Domain Event
  - `TaxSummaryRecalculatedEvent(accountId, taxYear, runId, computedAt)`
  - `TaxSummaryRecalculationFailedEvent(accountId, taxYear, runId, reason)`

## API
`GET /api/accounts/{accountId}/tax-summaries/{taxYear}` — 계좌의 특정 연도 세금 요약 조회
- Request: path `accountId: Long`, `taxYear: Int`
- Response 200
  - `accountId: Long`
  - `taxYear: Int`
  - `totalRealizedPnl: string`
  - `taxablePnl: string`
  - `estimatedTax: string`
  - `computedAt: string(ISO-8601)`
  - `status: string` (`DRAFT|FINAL`)

`GET /api/accounts/{accountId}/tax-summaries?fromYear=2024&toYear=2026` — 연도 구간 조회
- Request: path `accountId: Long`, query `fromYear: Int`, `toYear: Int`
- Response 200: `items: TaxSummaryResponse[]`

`POST /api/accounts/{accountId}/tax-summaries/{taxYear}/recalculate` — 수동 재계산 트리거
- Request: path `accountId: Long`, `taxYear: Int`, body `{ "force": boolean }`
- Response 202
  - `runId: Long`
  - `status: string` (`ACCEPTED|SKIPPED`)
  - `message: string`

`POST /api/tax-summaries/jobs/year-end` — 연말 배치 실행(운영용)
- Request: `{ "taxYear": Int, "accountIds": [Long] | null }`
- Response 202: `{ "jobId": string, "requestedCount": int }`

### Error Cases
- `400 INVALID_TAX_YEAR_RANGE`: `fromYear > toYear` 또는 비정상 연도
- `400 TAX_YEAR_NOT_CLOSED`: 종료되지 않은 연도에 `force=false` 재계산 요청
- `404 ACCOUNT_NOT_FOUND`
- `404 TAX_SUMMARY_NOT_FOUND`
- `409 TAX_SUMMARY_ALREADY_RUNNING`: 동일 계좌/연도 실행 중
- `422 UNSUPPORTED_CURRENCY`: MVP 범위 외 통화
- `500 TAX_SUMMARY_COMPUTE_FAILED`

## DB
`tax_summaries` (기존 확장)
- key columns: `id PK`, `account_id FK`, `tax_year`, `total_realized_pnl`, `taxable_pnl`, `estimated_tax`, `computed_at`, `status`
- indexes
  - `uk_tax_summaries_account_year (account_id, tax_year)` unique
  - `idx_tax_summaries_account_year_desc (account_id, tax_year desc)`
- relationship: `many-to-one account`

`tax_summary_runs` (신규)
- key columns: `id PK`, `account_id FK`, `tax_year`, `run_type`, `status`, `started_at`, `finished_at`, `error_message`
- indexes
  - `idx_tax_summary_runs_account_year_started (account_id, tax_year, started_at desc)`
  - `idx_tax_summary_runs_status_started (status, started_at desc)`
  - optional unique partial for running lock: `(account_id, tax_year) where status='RUNNING'`

## Ambiguities To Confirm
1. 과세 규칙: `taxablePnl` 계산 시 수수료/기납부세/손익상계(이월손실)를 어떤 우선순위로 반영할지?
2. 국내/해외 상품 분리: `ticker` 시장별 서로 다른 세율을 MVP에서 단일화할지, 분리할지?
3. 확정 시점: 연도 종료 전(`DRAFT`) 허용 범위와 `FINAL` 전환 권한(수동/자동) 정책?
4. 재계산 트리거 권한: 운영자 role 제한이 필요한지?
5. 다중 통화: 현행 `Settlement.currency`가 KRW 외 값을 가질 때 환산 기준일/환율 소스?
