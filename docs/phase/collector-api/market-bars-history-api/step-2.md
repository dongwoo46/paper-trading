# Step 2 — API 컨트롤러 및 DTO

## Goal

`MarketBarController`와 응답 DTO를 TDD 순서로 작성한다.
기존 `GlobalExceptionHandler`를 그대로 활용하고, 컨트롤러는 최대한 얇게 유지한다.

## Success Criteria

- [ ] `MarketBarControllerTest` 전체 통과 (5개 케이스)
- [ ] `./gradlew compileKotlin` 에러 없음
- [ ] 400/404 에러 응답 본문이 기존 `ErrorResponse` 형식과 일치

---

## TDD 순서: Red → Green → Refactor

### [Red] 컨트롤러 테스트 먼저 작성

**파일**: `src/test/kotlin/com/papertrading/collector/presentation/marketbar/MarketBarControllerTest.kt`

패턴 참고: 기존 `MarketFeatureControllerTest` — `MockMvcBuilders.standaloneSetup`, `io.mockk.mockk`

```kotlin
package com.papertrading.collector.presentation.marketbar

import com.papertrading.collector.application.marketbar.service.MarketBarQueryService
import com.papertrading.collector.domain.marketbar.MarketBar
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.Instant

class MarketBarControllerTest {
    private lateinit var queryService: MarketBarQueryService
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        queryService = mockk()
        val controller = MarketBarController(queryService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `GET bars 200 — 1m 기본 조회`() {
        every { queryService.getBars("005930", "1m", 60) } returns listOf(sampleBar("005930", "1m"))

        mockMvc.perform(
            get("/api/market/bars/005930")
                .queryParam("interval", "1m")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.symbol").value("005930"))
            .andExpect(jsonPath("$.interval").value("1m"))
            .andExpect(jsonPath("$.bars[0].open").value("70000"))
            .andExpect(jsonPath("$.bars[0].vwap").value("70200"))
            .andExpect(jsonPath("$.bars[0].tickCount").value(42))
    }

    @Test
    fun `GET bars 200 — limit 명시`() {
        every { queryService.getBars("005930", "5m", 10) } returns listOf(sampleBar("005930", "5m"))

        mockMvc.perform(
            get("/api/market/bars/005930")
                .queryParam("interval", "5m")
                .queryParam("limit", "10")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.bars.length()").value(1))
    }

    @Test
    fun `GET bars 400 — 잘못된 interval`() {
        every { queryService.getBars("005930", "3m", any()) } throws
            ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid interval: 3m")

        mockMvc.perform(
            get("/api/market/bars/005930")
                .queryParam("interval", "3m")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET bars 404 — 데이터 없음`() {
        every { queryService.getBars("005930", "1m", 60) } returns emptyList()

        mockMvc.perform(
            get("/api/market/bars/005930")
                .queryParam("interval", "1m")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET bars 200 — limit 100 초과 시 100으로 클램프`() {
        every { queryService.getBars("005930", "1m", 100) } returns listOf(sampleBar("005930", "1m"))

        mockMvc.perform(
            get("/api/market/bars/005930")
                .queryParam("interval", "1m")
                .queryParam("limit", "200")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
    }

    private fun sampleBar(symbol: String, interval: String) = MarketBar(
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

---

### [Green] 구현 파일 작성

#### 1. 응답 DTO

**파일**: `src/main/kotlin/com/papertrading/collector/presentation/marketbar/dto/MarketBarResponse.kt`

```kotlin
package com.papertrading.collector.presentation.marketbar.dto

data class MarketBarResponse(
    val symbol: String,
    val interval: String,
    val bars: List<BarDto>,
)

data class BarDto(
    val startedAt: String,   // ISO-8601 Instant string
    val open: String,        // BigDecimal.toPlainString()
    val high: String,
    val low: String,
    val close: String,
    val volume: String,
    val tradeValue: String,
    val vwap: String,
    val tickCount: Int,
)
```

- 숫자 필드는 `String` 직렬화 (`toPlainString()`) — 부동소수점 정밀도 손실 방지
- 기존 `FeatureItemResponse` 패턴 동일하게 따름

#### 2. 컨트롤러

**파일**: `src/main/kotlin/com/papertrading/collector/presentation/marketbar/MarketBarController.kt`

명세:

- `@RestController`, `@RequestMapping("/api/market/bars")`
- 생성자 주입만 허용 (`@Autowired` 필드 주입 금지)

```kotlin
@GetMapping("/{symbol}")
fun getBars(
    @PathVariable symbol: String,
    @RequestParam(defaultValue = "1m") interval: String,
    @RequestParam(defaultValue = "60") limit: Int,
): ResponseEntity<MarketBarResponse>
```

처리 흐름:
1. `limit = minOf(limit, 100)` 클램프 (서비스에서도 하지만 컨트롤러에서도 방어)
2. `queryService.getBars(symbol, interval, clampedLimit)` 호출
   - `ResponseStatusException`은 컨트롤러에서 catch하지 않음 — Spring MVC가 전파
3. 결과 list가 비어있으면 `ResponseEntity.status(404).body(...)` 반환 또는 `ResponseStatusException(NOT_FOUND)` throw
4. 정상: `ResponseEntity.ok(MarketBarResponse(...))`

`MarketBar → BarDto` 변환 헬퍼:
```kotlin
private fun MarketBar.toDto() = BarDto(
    startedAt = startedAt.toString(),
    open = open.toPlainString(),
    high = high.toPlainString(),
    low = low.toPlainString(),
    close = close.toPlainString(),
    volume = volume.toPlainString(),
    tradeValue = tradeValue.toPlainString(),
    vwap = vwap.toPlainString(),
    tickCount = tickCount,
)
```

#### 3. 에러 응답

기존 `GlobalExceptionHandler` (`presentation/common/GlobalExceptionHandler.kt`)는 수정하지 않는다.
`ResponseStatusException`은 Spring MVC 기본 처리로 적절한 HTTP 상태코드를 반환한다.

에러 응답 예시 (400):
```json
{
  "status": 400,
  "error": "bad_request",
  "message": "invalid interval: 3m",
  "path": "/api/market/bars/005930",
  "timestamp": "2026-05-08T10:00:00Z"
}
```

> 참고: `GlobalExceptionHandler`는 `IllegalArgumentException`을 처리하지만 `ResponseStatusException`은 Spring 기본 처리에 위임된다. 컨트롤러에서 발생하는 `ResponseStatusException`은 Spring이 적절한 HTTP 상태로 자동 변환한다.

---

### [Refactor] 점검 항목

- 컨트롤러 메서드 길이 20줄 이하 유지
- `toDto()` 확장 함수가 컨트롤러 파일 내 private으로 정의되어 있는지 확인
- DTO 필드 명이 API 스펙(`startedAt`, `open`, `high`, `low`, `close`, `volume`, `tradeValue`, `vwap`, `tickCount`)과 일치 확인

---

## Files to Create

| 경로 | 유형 |
|------|------|
| `src/main/kotlin/com/papertrading/collector/presentation/marketbar/dto/MarketBarResponse.kt` | 신규 |
| `src/main/kotlin/com/papertrading/collector/presentation/marketbar/MarketBarController.kt` | 신규 |
| `src/test/kotlin/com/papertrading/collector/presentation/marketbar/MarketBarControllerTest.kt` | 신규 |

---

## Verification

```bash
# 빌드 검증
cd backend/collector-api && ./gradlew compileKotlin

# 테스트 실행 (해당 테스트만)
cd backend/collector-api && ./gradlew test --tests "*.MarketBarControllerTest"
```

## Commit Message (Korean)

```
feat(collector): MarketBar API 컨트롤러 및 DTO 구현
```
