# trading-api ERD 설계 확정 (v1)

> 작성일: 2026-04-07
> 상태: 설계 확정 — 다음 단계: Flyway 마이그레이션 SQL + JPA Entity 작성

---

## 설계 배경

- 3가지 거래 모드 지원: 로컬 모의투자 / KIS 연동 모의투자 / 실시간 거래 (KIS, Upbit)
- 실제 금융 서비스 수준의 원장, 정산, 손익 계산 구현
- Python research-service와 Spring trading-api 간 명확한 책임 분리
- FAANG 시니어 + 퀀트 트레이더 페르소나 기준 설계 검토 완료

---

## 최종 테이블 목록 (23개)

### 계좌/자금

| 테이블 | 역할 |
|--------|------|
| accounts | 계좌 (STOCK/CRYPTO, 거래 모드별) |
| account_ledger | 원장 — 모든 자금 이동 이력 (단일 진실 소스) |
| fee_policies | 수수료 정책 (KIS/Upbit/Local 별도, effective_from/until 이력) |
| risk_policies | 계좌별 리스크 한도 (단일 종목 최대 비중, 일 최대 손실 등) |
| risk_events | 리스크 한도 위반 이력 |
| pending_settlements | T+2 정산 예정 관리 (d1/d2 컬럼 대체) |

### 주문/체결

| 테이블 | 역할 |
|--------|------|
| orders | 주문 (DAY/GTC/IOC/FOK/GTD 조건, 낙관적 락 version 컬럼) |
| order_amendments | 주문 정정/취소 이력 |
| order_signals | Python 엔진 주문 신호 — Spring 검증 후 orders로 전환 |
| executions | 체결 (external_execution_id UNIQUE, 다중 통화 지원) |
| settlements | 정산/실현손익 — order_id 기준 (부분 체결 지원) |
| settlement_executions | settlement ↔ execution 조인 (부분 체결 매핑) |

### 포지션/포트폴리오

| 테이블 | 역할 |
|--------|------|
| positions | 보유 포지션 (locked_quantity, price_updated_at, price_source) |
| daily_balances | 일별 잔고 스냅샷 |
| portfolio_snapshots | 일별 포트폴리오 구성 (섹터/자산군 비중 JSONB) |

### 전략

| 테이블 | 역할 |
|--------|------|
| strategies | 전략 (source_type, approval_status, 퀀트 성과 지표) |
| strategy_versions | 버전 이력 (rules JSONB, backtest_run_id 외부 참조) |
| strategy_derivations | 전략 파생 계보 (AI→사람, 사람→사람) |
| strategy_logs | 전략 실행 로그 |
| strategy_performance_snapshots | 기간별 성과 스냅샷 |

### 기타

| 테이블 | 역할 |
|--------|------|
| notifications | 알림 (체결/전략 발동/리스크 위반 등) |
| trading_journals | 트레이딩 기록/생각 (PRE_TRADE/POST_TRADE 등) |
| tax_summaries | 연간 세금 정산 요약 |

---

## 핵심 설계 결정 사항

### 1. 원장(Ledger) 기반 예수금 관리

- `accounts.deposit` 변경 시 반드시 `account_ledger` 기록을 **동일 트랜잭션**에서 함께 처리
- `accounts`에서 제거된 필드: `total_evaluation`, `total_asset` (positions에서 계산)
- account_ledger transaction_type:
  `DEPOSIT / WITHDRAWAL / BUY_LOCK / BUY_UNLOCK / BUY_EXECUTE / SELL_EXECUTE / FEE / TAX / SETTLEMENT`

### 2. positions 캐시 필드 규칙

아래 필드는 **캐시(Cache)** — source of truth가 아님:
- `current_price` — Redis에서 갱신
- `evaluation_amount` — quantity × current_price
- `unrealized_pnl` — evaluation - total_buy_amount
- `return_rate` — (current_price - avg_buy_price) / avg_buy_price

아래 필드는 **원천 데이터** — 반드시 정확해야 함:
- `quantity`, `locked_quantity`, `orderable_quantity`
- `avg_buy_price`, `total_buy_amount`

스탈 감지:
- `price_updated_at TIMESTAMPTZ` — Redis 마지막 갱신 시각
- `price_source ENUM` — REDIS_LIVE / REDIS_STALE / MANUAL / UNKNOWN

### 3. T+2 정산

- `accounts`에서 d1_deposit, d2_deposit 제거
- `pending_settlements` 테이블로 날짜별 정산 예정액 관리
- 배치 스케줄러: settlement_date <= TODAY인 PENDING 행을 available_deposit에 반영

### 4. 다중 통화 (해외주식/코인)

executions 추가 필드:
- `currency VARCHAR(3)` — KRW / USD / USDT
- `fx_rate DECIMAL(15,6)` NULL — 체결 당시 환율
- `krw_executed_price DECIMAL(20,4)` — 원화 환산가

settlements 추가 필드:
- `currency VARCHAR(3)`
- `fx_rate_at_settlement DECIMAL(15,6)` NULL
- `krw_net_pnl DECIMAL(20,4)` — 원화 기준 순손익 (포트폴리오 합산용)

### 5. 부분 체결 정산

- settlements.execution_id(1:1) → settlements.order_id 로 변경
- settlement는 order status = FILLED 시점에 생성
- settlement_executions 조인 테이블로 어떤 체결들이 포함됐는지 추적

### 6. order_signals 구조화

Python 신호 분석을 위한 인덱싱 가능한 컬럼:
- `signal_category` — PRICE_BREAKOUT / MA_CROSS / RSI / PROFIT_TARGET / LOSS_CUT / VOLUME_SPIKE
- `signal_indicator` — RSI / MA5 / MA20 / PRICE / VOLUME
- `signal_threshold` — 임계값
- `signal_actual_value` — 트리거 당시 실제 값
- `condition_snapshot JSONB` — 전체 상세 유지

### 7. 멱등성 (중복 실행 방지)

| 테이블 | 제약 |
|--------|------|
| order_signals | `idempotency_key UNIQUE` |
| orders | `UNIQUE(account_id, idempotency_key)` |
| executions | `external_execution_id UNIQUE` |
| settlements | `UNIQUE(order_id)` |
| account_ledger | `idempotency_key UNIQUE` |

앱 레벨 추가:
- orders: `@Version` 낙관적 락
- account deposit: `SELECT FOR UPDATE` 비관적 락
- 주문 생성: Redis 분산 락으로 동일 계좌 직렬화

### 8. 계좌 타입별 거래 모드

```
account_type = STOCK  →  trading_mode: LOCAL / KIS_PAPER / KIS_LIVE
account_type = CRYPTO →  trading_mode: LOCAL / UPBIT_LIVE
```

### 9. 전략 출처 및 생명주기

```
source_type: HUMAN / AI_QUANT / AI_RECOMMENDED / HYBRID
approval_status: DRAFT → PENDING_REVIEW → APPROVED / REJECTED
```

Python이 생성한 전략은 APPROVED 상태가 되어야만 실거래에 사용 가능.

### 10. 백테스트는 research-service DB

- trading-api는 `strategy_versions.backtest_run_id` (UUID 외부 참조)만 저장
- 백테스트 상세 데이터(`backtest_runs`, `backtest_trades`)는 Python research-service 소유
- Python이 백테스트 완료 시 결과 요약을 trading-api로 전송

---

## 다음 세션에서 할 일

### 즉시 시작 가능

1. **Flyway 마이그레이션 SQL 작성**
   - `V1__create_accounts.sql`
   - `V2__create_ledger_and_fees.sql`
   - `V3__create_orders.sql`
   - `V4__create_executions_settlements.sql`
   - `V5__create_positions.sql`
   - `V6__create_strategies.sql`
   - `V7__create_misc.sql` (notifications, journals, tax)

2. **JPA Entity 작성**
   - 도메인 레이어 Entity (순수 Java, 외부 의존 없음)
   - Repository 인터페이스

3. **ENUM 타입 정의**
   - AccountType, TradingMode, MarketType
   - OrderType, OrderSide, OrderCondition, OrderStatus
   - SignalCategory, PriceSource, TransactionType 등

### 주의사항

- 모든 금액 컬럼: `DECIMAL(20, 4)` (가격/단가), `DECIMAL(20, 8)` (수량/환율)
- 모든 수익률 컬럼: `DECIMAL(10, 4)`
- 모든 날짜 컬럼: `TIMESTAMPTZ` (타임존 포함)
- PostgreSQL ENUM 타입으로 정의 (CHECK 제약 또는 native enum)
- `available_deposit >= 0` CHECK 제약 반드시 추가
- 모든 테이블 `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`
