# Step 1 — 도메인 모델 및 Redis 읽기 서비스

## Goal

`MarketBar` 도메인 모델, `MarketBarRepository` 포트 인터페이스, Redis 구현체, `MarketBarQueryService`를 TDD 순서로 작성한다.

## Success Criteria

- [ ] `MarketBarQueryServiceTest` 전체 통과 (4개 케이스)
- [ ] `MarketBarRedisRepositoryTest` 전체 통과 (직렬화 / 집계 로직)
- [ ] `./gradlew compileKotlin` 에러 없음

---

## TDD 순서: Red → Green → Refactor

### [Red] 테스트 먼저 작성

**1. 서비스 단위 테스트**

파일: `src/test/kotlin/com/papertrading/collector/application/marketbar/service/MarketBarQueryServiceTest.kt`

```kotlin
package com.papertrading.collector.application.marketbar.service

import com.papertrading.collector.application.marketbar.port.MarketBarRepository
import com.papertrading.collector.domain.marketbar.MarketBar
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant

class MarketBarQueryServiceTest {
    private lateinit var repository: MarketBarRepository
    private lateinit var service: MarketBarQueryService

    @BeforeEach
    fun setUp() {
        repository = mockk()
        service = MarketBarQueryService(repository)
    }

    @Test
    fun `유효한 1m 조회 — 결과 반환`() {
        every { repository.findBars("005930", "1m", 60) } returns listOf(bar("005930", "1m"))
        val result = service.getBars("005930", "1m", 60)
        assertEquals(1, result.size)
        assertEquals("1m", result[0].interval)
    }

    @Test
    fun `유효한 5m 조회 — 결과 반환`() {
        every { repository.findBars("005930", "5m", 20) } returns listOf(bar("005930", "5m"))
        val result = service.getBars("005930", "5m", 20)
        assertEquals(1, result.size)
        assertEquals("5m", result[0].interval)
    }

    @Test
    fun `잘못된 interval — 400 예외`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            service.getBars("005930", "3m", 60)
        }
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.statusCode.value())
    }

    @Test
    fun `limit 0 이하 — 400 예외`() {
        val ex = assertThrows(ResponseStatusException::class.java) {
            service.getBars("005930", "1m", 0)
        }
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.statusCode.value())
    }

    @Test
    fun `limit 100 초과 — 100으로 클램프 후 정상 조회`() {
        every { repository.findBars("005930", "1m", 100) } returns emptyList()
        // 101을 넣으면 내부에서 100으로 클램프
        val result = service.getBars("005930", "1m", 101)
        assertEquals(0, result.size)
    }

    private fun bar(symbol: String, interval: String) = MarketBar(
        symbol = symbol,
        interval = interval,
        startedAt = Instant.parse("2026-05-08T10:00:00Z"),
        open = BigDecimal("70000"),
        high = BigDecimal("70500"),
        low = BigDecimal("69500"),
        close = BigDecimal("70200"),
        volume = BigDecimal("1000"),
        tradeValue = BigDecimal("70200000"),
        vwap = BigDecimal("70200"),
        tickCount = 42,
    )
}
```

**2. Redis 리포지토리 집계 단위 테스트**

파일: `src/test/kotlin/com/papertrading/collector/infra/redis/MarketBarRedisRepositoryTest.kt`

```kotlin
package com.papertrading.collector.infra.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papertrading.collector.domain.marketfeature.MinuteBarState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class MarketBarRedisRepositoryTest {

    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

    // 헬퍼: 분 단위 MinuteBarState 생성
    private fun minuteBar(minuteLabel: String, startedAt: Instant, close: BigDecimal = BigDecimal("100")) =
        MinuteBarState(
            minute = minuteLabel,
            open = BigDecimal("100"),
            high = BigDecimal("101"),
            low = BigDecimal("99"),
            close = close,
            volume = BigDecimal("500"),
            tradeValue = BigDecimal("50000"),
            buyVolume = BigDecimal("300"),
            sellVolume = BigDecimal("200"),
            tickCount = 10,
            startedAt = startedAt,
            updatedAt = startedAt.plusSeconds(59),
        )

    @Test
    fun `5m 집계 — open=첫봉, high=max, low=min, close=마지막봉, volume=합계`() {
        val base = Instant.parse("2026-05-08T10:00:00Z")
        val bars = (0..4).map { i ->
            minuteBar(
                minuteLabel = "2026-05-08T10:0${i}:00",
                startedAt = base.plusSeconds(i * 60L),
                close = BigDecimal((100 + i).toString()),
            )
        }

        val repo = MarketBarRedisRepository(
            redisTemplate = mockk(relaxed = true),
            objectMapper = objectMapper,
        )

        val aggregated = repo.aggregate5m(bars)

        assertEquals(1, aggregated.size, "5개 1m봉 → 1개 5m봉")
        val bar = aggregated[0]
        assertEquals(BigDecimal("100"), bar.open)
        assertEquals(BigDecimal("104"), bar.close)
        assertEquals(BigDecimal("101"), bar.high)
        assertEquals(BigDecimal("99"), bar.low)
        assertEquals(BigDecimal("2500"), bar.tradeValue)
        assertEquals(50, bar.tickCount)
    }

    @Test
    fun `1m bars JSON 직렬화-역직렬화 무결성`() {
        val bar = minuteBar("2026-05-08T10:00:00", Instant.parse("2026-05-08T10:00:00Z"))
        val json = objectMapper.writeValueAsString(bar)
        val restored = objectMapper.readValue(json, MinuteBarState::class.java)
        assertEquals(bar.open, restored.open)
        assertEquals(bar.startedAt, restored.startedAt)
    }
}
```

> 참고: `mockk` import는 `io.mockk.mockk`이며, 테스트에서 RedisTemplate은 `relaxed = true`로 모킹한다.

---

### [Green] 구현 파일 작성

#### 1. 도메인 모델

**파일**: `src/main/kotlin/com/papertrading/collector/domain/marketbar/MarketBar.kt`

```
package com.papertrading.collector.domain.marketbar

data class MarketBar(
    val symbol: String,
    val interval: String,          // "1m" | "5m" | "10m"
    val startedAt: java.time.Instant,
    val open: java.math.BigDecimal,
    val high: java.math.BigDecimal,
    val low: java.math.BigDecimal,
    val close: java.math.BigDecimal,
    val volume: java.math.BigDecimal,
    val tradeValue: java.math.BigDecimal,
    val vwap: java.math.BigDecimal,
    val tickCount: Int,
)
```

- 모든 가격·금액 필드: `BigDecimal` (float/Double 절대 금지)
- `vwap = tradeValue / volume` — 집계 시 계산, 저장 시 불변

#### 2. 포트 인터페이스

**파일**: `src/main/kotlin/com/papertrading/collector/application/marketbar/port/MarketBarRepository.kt`

```
package com.papertrading.collector.application.marketbar.port

import com.papertrading.collector.domain.marketbar.MarketBar

interface MarketBarRepository {
    /**
     * Redis에서 최근 [limit]개의 바를 반환한다.
     * interval이 "5m"/"10m"인 경우 내부적으로 1m 바를 조회 후 집계하여 반환한다.
     * limit는 호출 전에 이미 [1..100] 범위로 검증된 값이어야 한다.
     */
    fun findBars(symbol: String, interval: String, limit: Int): List<MarketBar>
}
```

#### 3. Redis 구현체

**파일**: `src/main/kotlin/com/papertrading/collector/infra/redis/MarketBarRedisRepository.kt`

구현 명세:

- `findBars(symbol, "1m", limit)`:
  - Redis key: `RedisKeyPolicy.barsKey(symbol)` → `"bars:1m:{symbol}"`
  - `LRANGE bars:1m:{symbol} -limit -1` (최신 limit개, 오래된 것부터 반환)
  - 각 JSON 항목을 `MinuteBarState`로 역직렬화 후 `MarketBar`로 변환
  - 파싱 실패 항목은 `runCatching { }.getOrNull()`로 skip

- `findBars(symbol, "5m", limit)`:
  - `LRANGE bars:1m:{symbol} -(limit * 5) -1` 최대 `limit * 5`개의 1m 바를 조회
  - `startedAt`을 `truncatedTo(ChronoUnit.MINUTES)`로 변환, `(minute / 5) * 5`로 5분 버킷 계산
  - 버킷별 그룹핑: `open=첫봉.open`, `high=max(high)`, `low=min(low)`, `close=마지막봉.close`, `volume=sum(volume)`, `tradeValue=sum(tradeValue)`, `tickCount=sum(tickCount)`, `vwap=tradeValue/volume` (volume=0이면 BigDecimal.ZERO)
  - 최신 `limit`개 버킷 반환 (오래된→최신 순)
  - `startedAt = 버킷 시작 Instant`

- `findBars(symbol, "10m", limit)`:
  - 위와 동일하되 `(minute / 10) * 10`으로 10분 버킷 계산
  - `LRANGE bars:1m:{symbol} -(limit * 10) -1`

- `aggregate5m(bars: List<MinuteBarState>): List<MarketBar>` — 내부 헬퍼 (테스트 가능하도록 `internal` 또는 package-private)
- `aggregate10m(bars: List<MinuteBarState>): List<MarketBar>` — 동일 패턴

VWAP 계산:
```kotlin
val vwap = if (totalVolume.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
           else totalTradeValue.divide(totalVolume, 4, RoundingMode.HALF_UP)
```

#### 4. 서비스

**파일**: `src/main/kotlin/com/papertrading/collector/application/marketbar/service/MarketBarQueryService.kt`

```
package com.papertrading.collector.application.marketbar.service

@Service
class MarketBarQueryService(
    private val repository: MarketBarRepository,
) {
    companion object {
        private val VALID_INTERVALS = setOf("1m", "5m", "10m")
        private const val MIN_LIMIT = 1
        private const val MAX_LIMIT = 100
        private const val DEFAULT_LIMIT = 60
    }

    fun getBars(symbol: String, interval: String, limit: Int): List<MarketBar> {
        if (interval !in VALID_INTERVALS) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid interval: $interval")
        }
        if (limit < MIN_LIMIT) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be >= 1")
        }
        val clampedLimit = minOf(limit, MAX_LIMIT)
        return repository.findBars(symbol, interval, clampedLimit)
    }
}
```

---

### [Refactor] 점검 항목

- `BigDecimal` 나눗셈 시 `RoundingMode` 명시 확인
- `runCatching` 블록에서 예외 로그 누락 없는지 확인 (`log.warn`)
- `MinuteBarState → MarketBar` 변환 로직이 중복되지 않도록 private 확장 함수로 추출

---

## Files to Create

| 경로 | 유형 |
|------|------|
| `src/main/kotlin/com/papertrading/collector/domain/marketbar/MarketBar.kt` | 신규 |
| `src/main/kotlin/com/papertrading/collector/application/marketbar/port/MarketBarRepository.kt` | 신규 |
| `src/main/kotlin/com/papertrading/collector/application/marketbar/service/MarketBarQueryService.kt` | 신규 |
| `src/main/kotlin/com/papertrading/collector/infra/redis/MarketBarRedisRepository.kt` | 신규 |
| `src/test/kotlin/com/papertrading/collector/application/marketbar/service/MarketBarQueryServiceTest.kt` | 신규 |
| `src/test/kotlin/com/papertrading/collector/infra/redis/MarketBarRedisRepositoryTest.kt` | 신규 |

---

## Verification

```bash
# 빌드 검증
cd backend/collector-api && ./gradlew compileKotlin

# 테스트 실행 (해당 테스트만)
cd backend/collector-api && ./gradlew test --tests "*.MarketBarQueryServiceTest" --tests "*.MarketBarRedisRepositoryTest"
```

## Commit Message (Korean)

```
feat(collector): MarketBar 도메인 모델 및 Redis 읽기 서비스 구현
```
