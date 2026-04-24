# Step 2: 구현 (TDD)
담당 에이전트: Full Stack Developer

## 작업 경로
`.worktrees/trading-api-position-service`

모든 파일 경로는 `.worktrees/trading-api-position-service/` 기준 상대 경로로 표기한다.

---

## 읽어야 할 파일 (필수)

1. `CLAUDE.md` — 아키텍처 원칙, BigDecimal 규칙
2. `docs/ADR.md` — ADR-003 Redis, ADR-005 Strategy 패턴
3. `docs/phase/trading-api/position-service/spec.md` — 전체 설계 명세
4. `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Position.kt`
5. `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Account.kt`
6. `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/port/MarketQuotePort.kt`
7. `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/port/QuoteSnapshot.kt`
8. `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionRepository.kt`
9. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/AccountCommandService.kt` — 서비스 패턴 참고
10. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/AccountQueryService.kt` — 서비스 패턴 참고
11. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderQueryService.kt` — listPositions/getPosition 현재 위치
12. `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/PositionController.kt` — 교체 대상
13. `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/order/OrderResponse.kt` — PositionResponse 분리 대상
14. `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt` — 수정 대상
15. `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/AccountController.kt` — 컨트롤러 패턴 참고

---

## 구현 작업 목록 (TDD: Red → Green 순서)

### 작업 0: 테스트 먼저 작성 (Red)
각 작업 단계마다 테스트를 먼저 작성한 후 구현한다.

---

### 작업 1: PositionResult 결과 객체 생성 (신규)

**파일**: `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/result/PositionResult.kt`

```kotlin
package com.papertrading.api.application.position.result

import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.domain.model.Position
import java.math.BigDecimal
import java.time.Instant

data class PositionResult(
    val id: Long,
    val accountId: Long,
    val ticker: String,
    val marketType: MarketType,
    val quantity: BigDecimal,
    val orderableQuantity: BigDecimal,
    val lockedQuantity: BigDecimal,
    val avgBuyPrice: BigDecimal,
    val totalBuyAmount: BigDecimal,
    val currentPrice: BigDecimal?,
    val evaluationAmount: BigDecimal?,
    val unrealizedPnl: BigDecimal?,
    val returnRate: BigDecimal?,
    val priceSource: PriceSource,
    val priceUpdatedAt: Instant?,
) {
    companion object {
        fun from(p: Position): PositionResult = PositionResult(
            id = requireNotNull(p.id) { "position.id is null" },
            accountId = requireNotNull(p.account?.id) { "position.account.id is null" },
            ticker = requireNotNull(p.ticker) { "position.ticker is null" },
            marketType = requireNotNull(p.marketType) { "position.marketType is null" },
            quantity = p.quantity,
            orderableQuantity = p.orderableQuantity,
            lockedQuantity = p.lockedQuantity,
            avgBuyPrice = p.avgBuyPrice,
            totalBuyAmount = p.totalBuyAmount,
            currentPrice = p.currentPrice,
            evaluationAmount = p.evaluationAmount,
            unrealizedPnl = p.unrealizedPnl,
            returnRate = p.returnRate,
            priceSource = p.priceSource,
            priceUpdatedAt = p.priceUpdatedAt,
        )
    }
}
```

---

### 작업 2: PositionQueryService 생성 (신규)

**파일**: `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionQueryService.kt`

```kotlin
package com.papertrading.api.application.position

import com.papertrading.api.application.position.result.PositionResult
import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.domain.port.MarketQuotePort
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class PositionQueryService(
    private val positionRepository: PositionRepository,
    private val marketQuotePort: MarketQuotePort,
) {
    /** 보유 포지션 목록 (quantity > 0). Redis 현재가 주입. */
    fun listPositionsWithCurrentPrice(accountId: Long): List<PositionResult> {
        val positions = positionRepository.findByAccountIdAndQuantityGreaterThan(accountId, BigDecimal.ZERO)
        return positions.map { position ->
            val quote = marketQuotePort.getQuote(requireNotNull(position.ticker))
            if (quote != null) {
                position.updatePrice(quote.price, PriceSource.REALTIME)
            }
            PositionResult.from(position)
        }
    }

    /** 단건 조회. Redis 현재가 주입. */
    fun getPositionWithCurrentPrice(accountId: Long, ticker: String): PositionResult {
        val position = positionRepository.findByAccountIdAndTicker(accountId, ticker)
            .orElseThrow { NoSuchElementException("포지션을 찾을 수 없습니다. ticker=$ticker") }
        val quote = marketQuotePort.getQuote(ticker)
        if (quote != null) {
            position.updatePrice(quote.price, PriceSource.REALTIME)
        }
        return PositionResult.from(position)
    }
}
```

**주의사항**:
- `readOnly = true` 트랜잭션 내에서 `position.updatePrice()` 호출은 DB 플러시가 발생하지 않는다 (Hibernate dirty checking 비활성). 응답 전용 계층에서 안전하게 사용 가능.
- `marketQuotePort.getQuote(ticker)` 반환값이 null이면 DB에 저장된 마지막 시세 사용 (graceful degradation).

---

### 작업 3: PositionCommandService 생성 (신규)

**파일**: `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionCommandService.kt`

```kotlin
package com.papertrading.api.application.position

import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.infrastructure.persistence.PositionRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class PositionCommandService(
    private val positionRepository: PositionRepository,
) {
    private val log = KotlinLogging.logger {}

    /**
     * Redis 시세 수신 시 해당 ticker 보유 포지션 평가손익 갱신.
     * QuoteEventListener에서 호출.
     */
    @Transactional
    fun updateCurrentPriceByTicker(ticker: String, price: BigDecimal, source: PriceSource) {
        val positions = positionRepository.findByTickerAndQuantityGreaterThan(ticker, BigDecimal.ZERO)
        if (positions.isEmpty()) return
        positions.forEach { it.updatePrice(price, source) }
        log.debug { "포지션 시세 갱신: ticker=$ticker, price=$price, count=${positions.size}" }
    }
}
```

---

### 작업 4: PositionRepository 메서드 추가

**파일**: `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionRepository.kt`

기존 파일에 아래 메서드를 추가한다:

```kotlin
// ticker 기준 보유 포지션 전체 조회 (QuoteEventListener 시세 갱신용)
fun findByTickerAndQuantityGreaterThan(ticker: String, minQuantity: BigDecimal): List<Position>
```

---

### 작업 5: PositionResponse DTO 분리 (이동)

**신규 파일**: `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/position/PositionResponse.kt`

```kotlin
package com.papertrading.api.presentation.dto.position

import com.papertrading.api.application.position.result.PositionResult
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceSource
import java.math.BigDecimal
import java.time.Instant

data class PositionResponse(
    val ticker: String,
    val marketType: MarketType,
    val quantity: BigDecimal,
    val orderableQuantity: BigDecimal,
    val lockedQuantity: BigDecimal,
    val avgBuyPrice: BigDecimal,
    val currentPrice: BigDecimal?,
    val evaluationAmount: BigDecimal?,
    val unrealizedPnl: BigDecimal?,
    val returnRate: BigDecimal?,
    val priceSource: PriceSource,
    val priceUpdatedAt: Instant?,
) {
    companion object {
        fun from(r: PositionResult) = PositionResponse(
            ticker = r.ticker,
            marketType = r.marketType,
            quantity = r.quantity,
            orderableQuantity = r.orderableQuantity,
            lockedQuantity = r.lockedQuantity,
            avgBuyPrice = r.avgBuyPrice,
            currentPrice = r.currentPrice,
            evaluationAmount = r.evaluationAmount,
            unrealizedPnl = r.unrealizedPnl,
            returnRate = r.returnRate,
            priceSource = r.priceSource,
            priceUpdatedAt = r.priceUpdatedAt,
        )
    }
}
```

**기존 파일 수정**: `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/order/OrderResponse.kt`
- `PositionResponse` data class 전체 삭제
- `import com.papertrading.api.domain.model.Position` 삭제 (Position 참조 제거)

---

### 작업 6: PositionController 교체

**파일**: `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/PositionController.kt`

기존 파일을 완전히 교체한다:

```kotlin
package com.papertrading.api.presentation.controller

import com.papertrading.api.application.position.PositionQueryService
import com.papertrading.api.presentation.dto.position.PositionResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/positions")
class PositionController(
    private val positionQueryService: PositionQueryService,
) {
    @GetMapping
    fun listPositions(@PathVariable accountId: Long): List<PositionResponse> =
        positionQueryService.listPositionsWithCurrentPrice(accountId)
            .map { PositionResponse.from(it) }

    @GetMapping("/{ticker}")
    fun getPosition(
        @PathVariable accountId: Long,
        @PathVariable ticker: String,
    ): PositionResponse =
        PositionResponse.from(
            positionQueryService.getPositionWithCurrentPrice(accountId, ticker.trim().uppercase())
        )
}
```

---

### 작업 7: OrderQueryService 정리

**파일**: `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderQueryService.kt`

`listPositions`, `getPosition` 메서드를 제거한다. `PositionRepository` import도 제거.

제거 후 클래스 시그니처:
```kotlin
@Service
@Transactional(readOnly = true)
class OrderQueryService(
    private val orderRepository: OrderRepository,
    private val executionRepository: ExecutionRepository,
) {
    fun getOrder(accountId: Long, orderId: Long): Order
    fun listOrders(accountId: Long): List<Order>
    fun listExecutions(accountId: Long, orderId: Long): List<Execution>
}
```

---

### 작업 8: QuoteEventListener 수정

**파일**: `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt`

`PositionCommandService` 주입 추가, `onMessage`에서 시세 갱신 호출 추가:

```kotlin
// 기존 import 유지 + 추가
import com.papertrading.api.application.position.PositionCommandService
import com.papertrading.api.domain.enums.PriceSource

@Component
class QuoteEventListener(
    private val localMatchingEngine: LocalMatchingEngine,
    private val positionCommandService: PositionCommandService,  // 추가
    private val objectMapper: ObjectMapper,
) : MessageListener {

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val quote = parseMessage(message.body) ?: return
        runCatching { localMatchingEngine.tryMatchPendingOrders(quote.ticker, quote) }
            .onFailure { log.warn { "매칭 처리 오류: ticker=${quote.ticker}, reason=${it.message}" } }
        // 포지션 평가손익 갱신 (체결 엔진과 독립적으로 처리)
        runCatching { positionCommandService.updateCurrentPriceByTicker(quote.ticker, quote.price, PriceSource.REALTIME) }
            .onFailure { log.warn { "포지션 시세 갱신 오류: ticker=${quote.ticker}, reason=${it.message}" } }
    }
    // parseMessage 기존 유지
}
```

---

### 작업 9: Position 엔티티 @Index 추가

**파일**: `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Position.kt`

`@Table` 어노테이션에 `indexes` 추가:

```kotlin
@Table(
    name = "positions",
    uniqueConstraints = [UniqueConstraint(name = "uk_positions_account_ticker", columnNames = ["account_id", "ticker"])],
    indexes = [
        Index(name = "idx_positions_account_qty", columnList = "account_id, quantity"),
        Index(name = "idx_positions_ticker_qty", columnList = "ticker, quantity"),
    ]
)
```

import 추가: `import jakarta.persistence.Index`

---

## 핵심 비즈니스 로직 규칙

### 평균단가 계산 (Position.applyBuy — 기존 구현 확인)
```
newTotalBuyAmount = totalBuyAmount + executedQty * executedPrice
newQuantity = quantity + executedQty
avgBuyPrice = newTotalBuyAmount / newQuantity  (scale=4, HALF_UP)
```

### 평가손익 계산 (Position.updatePrice — 기존 구현 확인)
```
evaluationAmount = currentPrice * quantity
unrealizedPnl = evaluationAmount - totalBuyAmount
returnRate = (currentPrice - avgBuyPrice) / avgBuyPrice  (scale=4, HALF_UP)
```

### 포지션 청산 (applySell 후 quantity == 0)
- 별도 status 컬럼 없음. quantity=0이면 청산 상태.
- `findByAccountIdAndQuantityGreaterThan(accountId, ZERO)` 조회 시 자동 제외됨.

---

## 파일 생성/수정 요약

| 작업 | 파일 | 유형 |
|------|------|------|
| 1 | `application/position/result/PositionResult.kt` | 신규 |
| 2 | `application/position/PositionQueryService.kt` | 신규 |
| 3 | `application/position/PositionCommandService.kt` | 신규 |
| 4 | `infrastructure/persistence/PositionRepository.kt` | 수정 (메서드 추가) |
| 5 | `presentation/dto/position/PositionResponse.kt` | 신규 |
| 5 | `presentation/dto/order/OrderResponse.kt` | 수정 (PositionResponse 제거) |
| 6 | `presentation/controller/PositionController.kt` | 수정 (교체) |
| 7 | `application/order/OrderQueryService.kt` | 수정 (포지션 관련 제거) |
| 8 | `infrastructure/redis/QuoteEventListener.kt` | 수정 (시세 갱신 추가) |
| 9 | `domain/model/Position.kt` | 수정 (@Index 추가) |

---

## Acceptance Criteria

```bash
cd .worktrees/trading-api-position-service/backend/trading-api
./gradlew compileKotlin
```

- 컴파일 오류 없음
- `OrderQueryService`에서 `listPositions`, `getPosition`, `PositionRepository` import가 제거됨
- `PositionController`가 `PositionQueryService`에 의존함
- `dto/order/OrderResponse.kt`에 `PositionResponse` 클래스가 없음
