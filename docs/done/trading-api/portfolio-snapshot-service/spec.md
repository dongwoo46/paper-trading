# Portfolio Snapshot Service

## Core Feature
장 마감 기준 계좌 잔고 추이와 포트폴리오 종목 비중 스냅샷을 생성/조회하는 `portfolio-snapshot-service` 구현.

## Scope
- 포함: DailyBalance 배치 집계, PortfolioSnapshot 배치 집계, 조회 API(기간별 잔고/일자별 비중), Controller/DTO, 테스트 보강.
- 제외: 실시간 포트폴리오 계산 엔진, 백테스트 연동, 전략 자동매수 로직.

## Domain Rules
- 금액/비중 계산은 `BigDecimal`만 사용한다.
- 일별 잔고는 계좌별 `business_date` 단일 스냅샷으로 멱등 저장한다.
- 비중(`weight`)은 `market_value / total_market_value`로 계산하고 소수 scale은 API 응답에서 고정한다.
- 스냅샷 생성은 배치 재실행 시 기존 동일 키 데이터를 갱신(upsert)한다.

## API Contract
- `GET /api/accounts/{accountId}/daily-balances?fromDate=&toDate=`
- `GET /api/accounts/{accountId}/portfolio-snapshots?date=`
- `POST /api/accounts/{accountId}/portfolio-snapshots/jobs/daily?businessDate=`

## Error Contract
- 400 INVALID_DATE_RANGE
- 400 INVALID_BUSINESS_DATE
- 404 ACCOUNT_NOT_FOUND
- 409 SNAPSHOT_ALREADY_RUNNING
- 500 SNAPSHOT_COMPUTE_FAILED

## Data Model
- `daily_balances`
  - key: `(account_id, business_date)`
  - fields: `cash_balance`, `stock_market_value`, `total_asset_value`, `pnl_amount`, `pnl_rate`, `created_at`, `updated_at`
- `portfolio_snapshots`
  - key: `(account_id, business_date, ticker)`
  - fields: `quantity`, `avg_buy_price`, `close_price`, `market_value`, `weight`, `unrealized_pnl`, `created_at`, `updated_at`
- 인덱스
  - `uk_daily_balances_account_date (account_id, business_date)`
  - `idx_daily_balances_account_date_desc (account_id, business_date desc)`
  - `uk_portfolio_snapshots_account_date_ticker (account_id, business_date, ticker)`
  - `idx_portfolio_snapshots_account_date (account_id, business_date)`

## DDD Model
- Aggregate Root 1: `DailyBalance` (일별 계좌 집계 스냅샷)
- Aggregate Root 2: `PortfolioSnapshot` (일별 종목 단위 비중 스냅샷)
- Value Object: `BusinessDate`, `PortfolioWeight`
- Domain Event: `PortfolioSnapshotGenerated(accountId, businessDate)`

## Open Decisions (MVP Defaults)
- 기준 종가는 기존 시스템의 최신 체결가/종가 조회 포트를 우선 사용한다.
- 영업일 판단은 입력 `businessDate` 유효성만 검증하고 휴장일 캘린더 고도화는 후속으로 분리한다.
- 응답 통화는 계좌 기준 기본 통화(KRW) 단일로 우선 제공한다.

## Implementation Plan
1. 스키마/도메인/Repository/배치 커맨드 서비스(TDD).
2. 조회 서비스/Controller/DTO 구현 및 프론트 portfolio-chart 연동 계약 고정.
3. 타깃 테스트 및 회귀 검증.
4. 코드 리뷰.
5. phase 정리 및 문서/상태 마감.
