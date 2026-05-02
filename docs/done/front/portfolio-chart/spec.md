# Portfolio Return Chart

## Core Feature
계좌의 일별 평가금액 추이와 KOSPI 벤치마크 대비 누적 수익률을 한 화면에서 비교할 수 있는 포트폴리오 차트를 제공한다.

## Considerations
- 데이터 원천은 trading-api의 DailyBalance와 벤치마크 시계열 API이며, 프런트는 계산 규칙을 중복 구현하지 않는다.
- 차트는 account-dashboard와 동일한 디자인 토큰/상태 패턴(loading/error/empty)을 유지한다.
- 수익률 계산은 기준일(base date) 정렬과 결측치 처리 규칙이 명확해야 한다.
- KOSPI 데이터 부재 시 포트폴리오 단독 차트로 degrade 하되, 사용자에게 상태를 표시한다.
- 대량 데이터(최대 1년 이상)에서도 렌더 성능이 유지되도록 단순한 시리즈 구조를 사용한다.

## Trade-offs
- Option A: 백엔드에서 상대수익률까지 완성 반환, Option B: 프런트에서 정규화 계산. 현재는 Option B를 선택한다.
- 이유: 기존 API 변경 범위를 줄이고 front phase 독립성을 유지할 수 있다. 단, 계산 규칙은 spec에 고정해 테스트로 보호한다.

## Implementation Approach
- `entities/portfolio`에 DailyBalance/Benchmark 타입 정의.
- `shared/api`에 portfolio chart 데이터 조회 API 모듈 추가.
- `features/portfolio-chart`에 데이터 정규화 유틸과 차트 컴포넌트 구성.
- `pages/portfolio`에서 계좌 선택, 기간 필터, 상태 UI를 오케스트레이션.
- 라우팅/사이드바에 `/portfolio` 진입점 추가.

## Workflow
1. 페이지 진입 시 계좌 목록 조회 후 기본 계좌 선택.
2. 선택 계좌 + 기간(range)으로 DailyBalance 조회.
3. 동일 기간 KOSPI 벤치마크 조회.
4. 두 시계열을 날짜 기준으로 정렬/정규화해 누적 수익률 시리즈 생성.
5. 평가금액(원화) 라인 + 상대수익률(%) 라인을 렌더.

## API
GET /api/v1/accounts — 계좌 목록 조회
Request: query optional (`isActive`)
Response: `{ accounts: AccountResponse[] }` 또는 `AccountResponse[]`
Errors: 400/401/500

GET /api/v1/accounts/{accountId}/daily-balances?from=YYYY-MM-DD&to=YYYY-MM-DD
Request: path `accountId`, query `from`, `to`
Response: `{ date: string, evaluationAmount: string, deposit: string }[]`
Errors: 400 (기간 오류) / 404 (계좌 없음) / 500

GET /api/v1/benchmarks/kospi?from=YYYY-MM-DD&to=YYYY-MM-DD
Request: query `from`, `to`
Response: `{ date: string, close: string }[]`
Errors: 400 / 404 (데이터 없음) / 500

## DB
- 프런트 단계에서 DB 변경 없음.
- 참조 테이블(백엔드 기준):
  - `daily_balance` (account_id, balance_date, evaluation_amount, deposit)
  - 인덱스: `(account_id, balance_date)`
  - 벤치마크 저장 테이블은 날짜 오름차순 조회 인덱스 필요 `(market, trade_date)`
