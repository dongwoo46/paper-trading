# Position Service — 기능 명세

## 현재 상태 (코드 리뷰 결과)

기존 코드에서 이미 구현된 것:
- `Position` 엔티티: `applyBuy`, `applySell`, `lockQuantity`, `unlockQuantity`, `updatePrice` 완성
- `ExecutionProcessor`: 체결 시 포지션 업데이트 포함 (BUY → `applyBuy`, SELL → `applySell`)
- `PositionRepository`: `findByAccountIdAndTicker`, `findByAccountIdAndTickerWithLock`, `findByAccountIdAndQuantityGreaterThan`
- `PositionController`: 존재하지만 `OrderQueryService`에 의존 (전용 서비스 없음)
- `PositionResponse` DTO: `dto/order/OrderResponse.kt`에 혼재
- `OrderQueryService.listPositions / getPosition`: 존재하지만 포지션 전담 계층 없음

**이번 구현 범위**: 포지션 전담 애플리케이션 서비스 계층 신설 + 컨트롤러 분리 + 시세 연동 응답 보강

---

## 핵심 기능

### 1. PositionQueryService (신규)
- `listActivePositions(accountId: Long): List<Position>` — 보유 수량 > 0인 포지션 목록
- `getPosition(accountId: Long, ticker: String): Position` — 단건 조회 (없으면 404)
- `getPositionWithCurrentPrice(accountId: Long, ticker: String): PositionResult` — 조회 + Redis 현재가 주입
- `listPositionsWithCurrentPrice(accountId: Long): List<PositionResult>` — 목록 + Redis 현재가 일괄 주입

### 2. PositionCommandService (신규)
- `updateCurrentPrice(accountId: Long, ticker: String, price: BigDecimal, source: PriceSource)` — Redis 시세 수신 시 호출
- `closePosition(accountId: Long, ticker: String)` — 수량이 0인 포지션 명시적 청산 (ExecutionProcessor가 sell 처리 후 호출 가능)

### 3. PositionController 리팩터링
- `OrderQueryService` 의존 제거 → `PositionQueryService` 의존으로 교체
- `PositionResponse` DTO를 `dto/order/` 에서 `dto/position/` 패키지로 이동

### 4. QuoteEventListener 확장
- Redis 시세 수신 시 `LocalMatchingEngine.tryMatchPendingOrders` 뿐 아니라 `PositionCommandService.updateCurrentPrice` 호출 추가
- 해당 ticker를 보유한 활성 포지션의 평가손익·현재가를 즉시 갱신

---

## 고려사항

### 포지션 청산 타이밍
- `ExecutionProcessor.fill`에서 `applySell` 호출 후 `position.quantity == 0` 이면 자동으로 청산 처리(totalBuyAmount=0, avgBuyPrice=0 리셋)
- 별도 CLOSED 상태 컬럼 불필요 (수량 0 = 청산 상태). 조회 시 `quantity > 0` 필터로 충분

### 평균단가 계산 (이미 도메인에 구현됨)
```
applyBuy: avgBuyPrice = (totalBuyAmount + executedQty * executedPrice) / newQuantity
```
- `totalBuyAmount`는 매수 체결액 누적 값으로 관리 (재계산 불필요)
- 매도 후: `totalBuyAmount = avgBuyPrice * remainingQuantity` (비례 차감)

### BigDecimal 정밀도 정책
| 컬럼 | scale | 용도 |
|------|-------|------|
| quantity | 8 | 주식 정수 + 크립토 소수 지원 |
| avgBuyPrice | 4 | 원화 기준 소수점 4자리 |
| returnRate | 4 | 0.1234 = 12.34% |
| evaluationAmount | 4 | 평가금액 |

### 트랜잭션 경계
- `PositionQueryService`: `@Transactional(readOnly = true)`
- `PositionCommandService.updateCurrentPrice`: 잦은 호출 → `@Transactional` (짧게 유지)
- Redis 시세를 DB에 저장하는 대신 응답 시점에 주입하는 방식도 가능 (아래 트레이드오프 참조)

### 데드락 방지 (기존 패턴 준수)
- Account 락 획득 → Position 락 획득 순서 (ExecutionProcessor 패턴 동일)

---

## 트레이드오프

### 시세 저장 전략: DB 즉시 기록 vs 응답 시 주입

| 방식 | 장점 | 단점 |
|------|------|------|
| **QuoteEventListener에서 DB 즉시 업데이트** | 언제 조회해도 최신 평가손익 반환 | Redis 시세 수신마다 DB 쓰기 발생 (고빈도 시 부하) |
| **조회 시점에 Redis에서 주입** | DB 쓰기 없음, 빠름 | 응답마다 Redis 조회, 시세 없으면 null |

**결정**: 두 방식 모두 구현. QuoteEventListener는 `updateCurrentPrice` 호출 → DB 업데이트. `getPositionWithCurrentPrice`는 Redis에서 최신 시세를 추가로 override하여 응답. DB에 주기적 스냅샷도 유지.

### PositionResponse 위치
- 기존: `dto/order/OrderResponse.kt` 내 혼재 → 분리 필요
- 결정: `dto/position/PositionResponse.kt`로 이동, `OrderResponse.kt`에서 제거
- PositionController도 `dto/order/PositionResponse` import 제거

---

## 구현 방식 (레이어별)

```
presentation/
  controller/
    PositionController.kt           -- 기존 파일 교체 (OrderQueryService → PositionQueryService)
  dto/
    position/
      PositionResponse.kt           -- 신규 (OrderResponse.kt에서 분리 이동)

application/
  position/
    PositionQueryService.kt         -- 신규
    PositionCommandService.kt       -- 신규
    result/
      PositionResult.kt             -- 신규 (현재가 포함 쿼리 결과)

domain/
  model/
    Position.kt                     -- 기존 유지 (변경 없음)
  port/
    MarketQuotePort.kt              -- 기존 유지 (변경 없음)

infrastructure/
  persistence/
    PositionRepository.kt           -- 기존 유지 (변경 없음)
  redis/
    QuoteEventListener.kt           -- 기존 파일 수정 (updateCurrentPrice 추가 호출)
    RedisMarketQuoteAdapter.kt      -- 기존 유지
```

---

## 워크플로우

### 매수 체결 → 포지션 생성/업데이트
```
OrderCommandService.placeOrder
  → LocalMatchingEngine.tryMatchPendingOrders (Redis 시세)
    → ExecutionProcessor.fill
      → position.applyBuy(qty, price)  ← 이미 구현됨
      → positionRepository.save(position)
```

### Redis 시세 수신 → 포지션 평가손익 업데이트
```
QuoteEventListener.onMessage
  → localMatchingEngine.tryMatchPendingOrders(ticker, quote)  ← 기존
  → positionCommandService.updateCurrentPrice(ticker, price, PriceSource.REALTIME)  ← 신규 추가
    → positionRepository.findByTickerAndQuantityGreaterThan(ticker)  ← 신규 쿼리 필요
      → position.updatePrice(price, source)
      → positionRepository.save(position)
```

### 포지션 조회
```
GET /api/v1/accounts/{accountId}/positions
  → PositionController.listPositions
    → positionQueryService.listPositionsWithCurrentPrice(accountId)
      → positionRepository.findByAccountIdAndQuantityGreaterThan(accountId, ZERO)
      → (각 포지션에 대해) marketQuotePort.getQuote(ticker) → Redis 조회
      → PositionResult 조합하여 반환
```

### 매도 체결 → 포지션 수량 감소 (청산)
```
ExecutionProcessor.fill (OrderSide.SELL)
  → position.applySell(qty)  ← 이미 구현됨
  → quantity == 0 이면 자동 청산 상태 (별도 처리 없음)
```

---

## API

### GET /api/v1/accounts/{accountId}/positions
보유 중인 포지션 목록 (quantity > 0)

**Response 200**
```json
[
  {
    "ticker": "005930",
    "marketType": "KOSPI",
    "quantity": "10.00000000",
    "orderableQuantity": "7.00000000",
    "lockedQuantity": "3.00000000",
    "avgBuyPrice": "73333.3333",
    "currentPrice": "75000.0000",
    "evaluationAmount": "750000.0000",
    "unrealizedPnl": "16666.6700",
    "returnRate": "0.0227",
    "priceSource": "REALTIME",
    "priceUpdatedAt": "2026-04-24T10:00:00Z"
  }
]
```

**Error**
- 404: 계좌 없음 `{ "code": "NOT_FOUND", "message": "계좌를 찾을 수 없습니다. id={accountId}" }`

---

### GET /api/v1/accounts/{accountId}/positions/{ticker}
특정 종목 포지션 단건 조회

**Path Params**: ticker (대소문자 무관, 내부에서 trim + uppercase 처리)

**Response 200** — 위 목록 응답 단건과 동일 구조

**Error**
- 404: 포지션 없음 `{ "code": "NOT_FOUND", "message": "포지션을 찾을 수 없습니다. ticker={ticker}" }`

---

## DB

### positions 테이블 (기존 — 변경 없음)
```sql
CREATE TABLE positions (
    id                 BIGSERIAL PRIMARY KEY,
    account_id         BIGINT NOT NULL REFERENCES accounts(id),
    ticker             VARCHAR(20) NOT NULL,
    market_type        VARCHAR(20) NOT NULL,
    quantity           NUMERIC(20, 8) NOT NULL DEFAULT 0,
    locked_quantity    NUMERIC(20, 8) NOT NULL DEFAULT 0,
    orderable_quantity NUMERIC(20, 8) NOT NULL DEFAULT 0,
    avg_buy_price      NUMERIC(20, 4) NOT NULL DEFAULT 0,
    total_buy_amount   NUMERIC(20, 4) NOT NULL DEFAULT 0,
    current_price      NUMERIC(20, 4),
    evaluation_amount  NUMERIC(20, 4),
    unrealized_pnl     NUMERIC(20, 4),
    return_rate        NUMERIC(10, 4),
    price_updated_at   TIMESTAMPTZ,
    price_source       VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    CONSTRAINT uk_positions_account_ticker UNIQUE (account_id, ticker)
);

-- 기존 인덱스
CREATE INDEX idx_positions_account_qty ON positions(account_id, quantity);

-- 신규 추가 인덱스 (시세 업데이트용 - ticker 기준 전체 조회)
CREATE INDEX idx_positions_ticker_qty ON positions(ticker, quantity);
```

**신규 인덱스 추가 이유**: `QuoteEventListener`에서 ticker 기준으로 보유 포지션 전체 조회 필요. `ddl-auto: create`이므로 Entity에 `@Index` 어노테이션으로 추가.

---

## 에러 처리 (기존 @ControllerAdvice 패턴 준수)
- `NoSuchElementException` → 404 NOT_FOUND
- `IllegalArgumentException`, `IllegalStateException` → 400 BAD_REQUEST
- 신규 도메인 예외 추가 불필요 (기존 예외 재사용)