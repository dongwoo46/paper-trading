# Tax Summary UI

## Core Feature
계좌/연도 기준 세금 요약을 조회하고, 사용자가 수동으로 재계산을 실행해 최신 스냅샷과 실행 상태를 확인하는 운영 UI를 `frontend/trading-web`에 제공한다.

## Considerations
- 기존 `account-dashboard`, `order-management-ui`와 동일한 대시보드 UX 패턴(계좌 선택, 패널 구성, 명시적 새로고침)을 유지한다.
- 세금 금액/세율은 서버 계산값을 그대로 표시하며, 프론트에서 재계산하지 않는다.
- 금액은 문자열(BigDecimal 직렬화) 기반 파싱/포맷을 적용하고, 표시 정밀도/단위를 일관되게 유지한다.
- 재계산 요청은 비동기 실행 상태(`RUNNING` 등)를 동반할 수 있으므로 로딩/중복 실행 방지 UX가 필요하다.

## Trade-offs
- 연도 선택 UI를 자유 입력 대신 Select 기반으로 제한: 잘못된 포맷 입력 방지, 운영 화면 일관성 확보.
- 자동 폴링 대신 수동 새로고침 + 재계산 후 명시적 invalidate 선택: API 부하를 줄이고 사용자 의도를 명확히 반영.
- 단일 계좌 단일 연도 조회 중심으로 시작하고, 연도 구간 조회는 확장 가능한 API 훅으로 분리해 단계적 확장을 허용.

## Implementation Approach
- `entities/tax-summary`: 타입, API 클라이언트, 상태 매핑 유틸 정의.
- `features/tax-summary`: 계좌/연도 선택, 요약 카드, 재계산 액션 패널 컴포넌트 구성.
- `pages/tax-summary`: 기존 페이지 패턴과 동일한 오케스트레이션(선택 상태, 쿼리 트리거, 에러/빈 상태 처리).
- 라우팅/사이드바에 `tax-summary` 진입점을 추가하고 기존 내비게이션 규칙을 유지.

## Workflow
1. 사용자 `/tax-summary` 진입
2. 계좌 선택(기본 첫 계좌) + 연도 선택(기본 현재 연도)
3. `GET /api/v1/accounts/{accountId}/tax-summaries/{taxYear}` 호출
4. 요약 값/상태/계산 시각 렌더링
5. 사용자가 재계산 클릭 시 `POST /api/v1/accounts/{accountId}/tax-summaries/{taxYear}/recalculate`
6. 성공 시 관련 쿼리 invalidate 후 최신 상태 재조회

## API
GET `/api/v1/accounts/{accountId}/tax-summaries/{taxYear}` — 단일 연도 세금 요약 조회  
Request: path `accountId: number`, `taxYear: number`  
Response: `{ accountId: number, taxYear: number, realizedProfit: string, realizedLoss: string, taxableBase: string, taxAmount: string, localTaxAmount: string, effectiveTaxRate: string, status: "READY"|"RUNNING"|"FAILED", computedAt: string|null }`  
Errors: `400`(연도 범위/파라미터 오류), `404`(계좌 또는 요약 없음)

GET `/api/v1/accounts/{accountId}/tax-summaries?fromYear=&toYear=` — 연도 구간 요약 조회(확장 대비)  
Request: path `accountId: number`, query `fromYear: number`, `toYear: number`  
Response: `{ items: TaxSummaryResponse[] }`  
Errors: `400`(구간 역전/형식 오류), `404`(계좌 없음)

POST `/api/v1/accounts/{accountId}/tax-summaries/{taxYear}/recalculate` — 수동 재계산 트리거  
Request: path `accountId: number`, `taxYear: number`  
Response: `{ runId: number, status: "RUNNING"|"READY"|"FAILED", requestedAt: string }`  
Errors: `400`(요청 불가 파라미터), `404`(계좌 없음), `409`(동일 계좌/연도 RUNNING 중복)

## DB
- `tax_summaries`
  - key columns: `account_id`, `tax_year`
  - display columns: `realized_profit`, `realized_loss`, `taxable_base`, `tax_amount`, `local_tax_amount`, `effective_tax_rate`, `status`, `computed_at`
  - indexes: `(account_id, tax_year)` unique
- `tax_summary_runs`
  - key columns: `id`, `account_id`, `tax_year`, `status`, `requested_at`, `completed_at`
  - indexes: `(account_id, tax_year, status)`, `requested_at`
