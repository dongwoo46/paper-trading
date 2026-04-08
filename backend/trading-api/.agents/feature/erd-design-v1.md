trading-api ERD 설계 확정 v1
작성일: 2026-04-07
상태: 설계 확정
다음 단계: Flyway 마이그레이션 SQL 작성, JPA Entity 작성

설계 배경
3가지 거래 모드 지원: 로컬 모의투자, KIS 연동 모의투자, 실시간 거래(KIS, Upbit)
금융 서비스 수준 원장 정산 손익 계산 구현
Python research-service와 Spring trading-api 책임 분리
설계 검토 완료

최종 테이블 목록 23개
계좌 자금
accounts: 계좌(STOCK CRYPTO, 거래 모드별)
account_ledger: 원장, 모든 자금 이동 이력
fee_policies: 수수료 정책(KIS Upbit Local, 기간 이력)
risk_policies: 계좌별 리스크 한도
risk_events: 리스크 한도 위반 이력
pending_settlements: T+2 정산 예정 관리

주문 체결
orders: 주문(DAY GTC IOC FOK GTD, version 컬럼)
order_amendments: 주문 정정 취소 이력
order_signals: Python 주문 신호, 검증 후 orders 전환
executions: 체결(external_execution_id UNIQUE, 다중 통화)
settlements: 정산 실현손익(order_id 기준)
settlement_executions: settlement execution 조인

포지션 포트폴리오
positions: 보유 포지션(locked_quantity, price_updated_at, price_source)
daily_balances: 일별 잔고 스냅샷
portfolio_snapshots: 일별 포트폴리오 구성(JSONB)

전략
strategies: 전략(source_type, approval_status, 성과 지표)
strategy_versions: 버전 이력(rules JSONB, backtest_run_id)
strategy_derivations: 전략 파생 계보
strategy_logs: 전략 실행 로그
strategy_performance_snapshots: 기간별 성과 스냅샷

기타
notifications: 알림
trading_journals: 트레이딩 기록
tax_summaries: 연간 세금 정산 요약

핵심 설계 결정
1) 원장 기반 예수금 관리
accounts.deposit 변경 시 account_ledger 기록을 동일 트랜잭션에서 함께 처리
accounts 제거 필드: total_evaluation, total_asset
account_ledger transaction_type: DEPOSIT, WITHDRAWAL, BUY_LOCK, BUY_UNLOCK, BUY_EXECUTE, SELL_EXECUTE, FEE, TAX, SETTLEMENT

2) positions 캐시 필드 규칙
캐시 필드: current_price, evaluation_amount, unrealized_pnl, return_rate
원천 데이터 필드: quantity, locked_quantity, orderable_quantity, avg_buy_price, total_buy_amount
스테일 감지 필드: price_updated_at(TIMESTAMPTZ), price_source(REDIS_LIVE, REDIS_STALE, MANUAL, UNKNOWN)

3) T+2 정산
accounts에서 d1_deposit, d2_deposit 제거
pending_settlements로 날짜별 정산 예정액 관리
배치: settlement_date <= TODAY인 PENDING 행을 available_deposit에 반영

4) 다중 통화
executions 추가: currency(VARCHAR(3)), fx_rate(DECIMAL(15,6) NULL), krw_executed_price(DECIMAL(20,4))
settlements 추가: currency(VARCHAR(3)), fx_rate_at_settlement(DECIMAL(15,6) NULL), krw_net_pnl(DECIMAL(20,4))

5) 부분 체결 정산
settlements.execution_id(1:1) 대신 settlements.order_id 사용
settlement 생성 시점: order status FILLED
settlement_executions로 포함 execution 추적

6) order_signals 구조화
signal_category: PRICE_BREAKOUT, MA_CROSS, RSI, PROFIT_TARGET, LOSS_CUT, VOLUME_SPIKE
signal_indicator: RSI, MA5, MA20, PRICE, VOLUME
signal_threshold, signal_actual_value, condition_snapshot(JSONB) 저장

7) 멱등성
order_signals: idempotency_key UNIQUE
orders: UNIQUE(account_id, idempotency_key)
executions: external_execution_id UNIQUE
settlements: UNIQUE(order_id)
account_ledger: idempotency_key UNIQUE
앱 레벨: orders @Version 낙관적 락, account deposit SELECT FOR UPDATE, 주문 생성 시 Redis 분산 락

8) 계좌 타입별 거래 모드
account_type STOCK: LOCAL, KIS_PAPER, KIS_LIVE
account_type CRYPTO: LOCAL, UPBIT_LIVE

9) 전략 출처 생명주기
source_type: HUMAN, AI_QUANT, AI_RECOMMENDED, HYBRID
approval_status: DRAFT, PENDING_REVIEW, APPROVED, REJECTED
Python 생성 전략은 APPROVED 이후에만 실거래 사용

10) 백테스트 저장소 분리
trading-api는 strategy_versions.backtest_run_id(UUID 외부 참조)만 저장
backtest_runs, backtest_trades는 research-service 소유
Python이 백테스트 결과 요약을 trading-api로 전송

다음 세션 작업
즉시 시작 가능
Flyway SQL 작성
V1__create_accounts.sql
V2__create_ledger_and_fees.sql
V3__create_orders.sql
V4__create_executions_settlements.sql
V5__create_positions.sql
V6__create_strategies.sql
V7__create_misc.sql(notifications, journals, tax)
JPA Entity 작성(도메인 Entity, Repository)
ENUM 정의(AccountType, TradingMode, MarketType, OrderType, OrderSide, OrderCondition, OrderStatus, SignalCategory, PriceSource, TransactionType)

주의사항
금액 컬럼: DECIMAL(20,4) 가격 단가, DECIMAL(20,8) 수량 환율
수익률 컬럼: DECIMAL(10,4)
날짜 컬럼: TIMESTAMPTZ
PostgreSQL ENUM 또는 CHECK 제약 사용
available_deposit >= 0 CHECK 제약 필수
모든 테이블 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
