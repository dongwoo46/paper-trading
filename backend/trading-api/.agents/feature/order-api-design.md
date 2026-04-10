order-api 설계 (Phase 4)
작성일: 2026-04-10
상태: 설계 확정, 구현 대기 (collector-api Pub/Sub 선행 필요)

## 개요
3개 거래 모드 동시 운영: LOCAL(내부 체결), KIS_PAPER(KIS 모의), KIS_LIVE(KIS 실거래)
계좌별로 독립 TradingMode → 각 계좌가 서로 다른 모드로 동시 운용 가능

## 아키텍처

### Strategy Pattern (거래 모드별 교체)
OrderExecutionPort (interface)
  submit(order) → ExternalOrderResult
  cancel(order) → Unit
  ├── LocalOrderExecutor      (TradingMode.LOCAL)
  ├── KisPaperOrderExecutor   (TradingMode.KIS_PAPER)
  └── KisLiveOrderExecutor    (TradingMode.KIS_LIVE, stub — 추후 구현)

MarketQuotePort (interface)
  getCurrentPrice(ticker) → QuoteResult?  // price + updatedAt
  └── RedisMarketQuoteAdapter             // Redis Hash quote:{ticker}

### Redis 시세 키 구조
key: quote:{ticker}  (예: quote:005930, quote:BTC-KRW)
type: Hash
fields:
  price      DECIMAL   현재가
  high       DECIMAL   고가
  low        DECIMAL   저가
  volume     DECIMAL   거래량
  updatedAt  epoch ms  stale 판단 기준

stale 기준: 60초 초과 시 시장가 주문 거부 (STALE_PRICE 오류)

### LOCAL 체결 엔진
트리거: Redis Pub/Sub (채널: quote:{ticker}) — collector-api에서 시세 수신 시 발행
  → QuoteEventListener.onMessage() → LocalMatchingEngine.tryMatch(ticker)
  → 해당 ticker의 PENDING 주문만 조회해 조건 판단

보조 스케줄러(@Scheduled 30초):
  - GTD 만료 주문 CANCELLED 처리
  - trading-api 재시작 후 누락 tick 복구
  - 장 마감 처리

### KIS_PAPER 체결
KisPaperOrderExecutor.submit() → KIS 주문 접수 REST API → externalOrderId 저장
KisPaperPollingScheduler (@Scheduled 3초) → KIS_PAPER 미체결 주문만 체결 조회
→ ExecutionProcessor 위임 (LOCAL과 동일 처리)

## API 목록

주문
POST   /api/v1/accounts/{accountId}/orders                    주문 생성 (멱등)
GET    /api/v1/accounts/{accountId}/orders                    주문 목록 (상태 필터, 페이징)
GET    /api/v1/accounts/{accountId}/orders/{orderId}          주문 상세
DELETE /api/v1/accounts/{accountId}/orders/{orderId}          주문 취소

체결
GET    /api/v1/accounts/{accountId}/orders/{orderId}/executions  체결 내역

포지션
GET    /api/v1/accounts/{accountId}/positions                 포지션 목록 (예수금 포함)
GET    /api/v1/accounts/{accountId}/positions/{ticker}        종목별 포지션

## 핵심 설계 결정

### 예수금 흐름 (Account 도메인 메서드)
주문 접수(매수): lockDeposit(금액)       → available↓, locked↑ + Ledger(BUY_LOCK)
주문 취소:       unlockDeposit(금액)     → locked↓, available↑ + Ledger(BUY_UNLOCK)
체결 확정(매수): confirmBuy(금액)        → locked↓, deposit↓  + Ledger(BUY_EXECUTE)
체결 확정(매도): receiveSellProceeds()   → deposit↑, available↑ + Ledger(SELL_EXECUTE)

### 시장가 vs 지정가
시장가(MARKET): Redis 현재가로 즉시 전량 체결. stale 60초 초과 시 거부.
지정가(LIMIT):
  매수: 현재가 <= limitPrice → 즉시 체결 (체결가 = limitPrice)
  매도: 현재가 >= limitPrice → 즉시 체결 (체결가 = limitPrice)
  조건 불충족: PENDING → Pub/Sub 이벤트 또는 보조 스케줄러 재시도

### 주문 조건별 PENDING 처리
DAY/GTC : 체결 불가 시 PENDING 유지
IOC     : 체결 불가 시 즉시 CANCELLED + unlockDeposit
FOK     : 체결 불가 시 즉시 CANCELLED + unlockDeposit
GTD     : expireAt 이전 = DAY와 동일, 만료 시 보조 스케줄러가 CANCELLED

### 공매도 불가 guard
매도 주문 시 position.orderableQuantity >= 요청수량 검증 필수
미보유 종목 매도 → ORDER_REJECTED

### 체결 처리 단일 트랜잭션 (ExecutionProcessor)
1. order.applyExecution(qty, price, fee) → FILLED or PARTIAL
2. Execution 생성
3. Position upsert
   매수: position.applyBuy(qty, price)   → avgBuyPrice 재계산
   매도: position.applySell(qty)         → quantity 감소, 0이면 삭제
4. 매수: account.confirmBuy(lockedAmount)
   매도: account.receiveSellProceeds(netAmount)
5. AccountLedger 기록
6. Order FILLED 시 Settlement 생성

### 데드락 방지
항상 Account 락 먼저, Position 락 나중 순서 준수 (기존 계좌 API와 동일)

## 작업 분해

선행 조건: collector-api Redis Pub/Sub 구현 완료 (realtime-quote-pubsub.md)

| # | 작업                                                     |
|---|----------------------------------------------------------|
| 1 | Position 도메인 메서드 보강 (lockQuantity, unlockQuantity) + 단위 테스트 |
| 2 | MarketQuotePort + RedisMarketQuoteAdapter                |
| 3 | OrderExecutionPort + LocalOrderExecutor                  |
| 4 | ExecutionProcessor (단일 트랜잭션 체결 처리)             |
| 5 | OrderCommandService (주문 생성/취소, guard 포함)          |
| 6 | QuoteEventListener (Redis Pub/Sub 구독)                  |
| 7 | LocalMatchingEngine (ticker별 PENDING 매칭)               |
| 8 | LocalMatchingScheduler (보조: GTD 만료, 복구)            |
| 9 | KIS 주문 REST Client + KisPaperOrderExecutor             |
| 10 | KisPaperPollingScheduler (3초 폴링)                     |
| 11 | KisLiveOrderExecutor stub                               |
| 12 | OrderQueryService + PositionQueryService                |
| 13 | OrderController + DTO                                   |
| 14 | 단위 테스트 (MockK) + 통합 테스트 (Testcontainers)       |