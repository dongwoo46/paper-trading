# Step 3: 테스트 및 QA 검증
담당 에이전트: Test Engineer

## 작업 경로
`.worktrees/trading-api-position-service`

---

## 읽어야 할 파일 (필수)

1. `docs/phase/trading-api/position-service/spec.md` — API 명세, 비즈니스 로직 규칙
2. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionQueryService.kt`
3. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionCommandService.kt`
4. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/result/PositionResult.kt`
5. `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/PositionController.kt`
6. `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/position/PositionResponse.kt`
7. `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionRepository.kt`
8. `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Position.kt`
9. `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/redis/QuoteEventListener.kt`
10. `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderQueryService.kt` — 포지션 메서드 제거 확인용
11. `backend/trading-api/src/test/kotlin/com/papertrading/api/application/account/AccountCommandServiceTest.kt` — 테스트 패턴 참고
12. `backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/AccountControllerIntegrationTest.kt` — 통합 테스트 패턴 참고
13. `backend/trading-api/src/test/kotlin/com/papertrading/api/domain/model/PositionTest.kt` — 기존 도메인 테스트 참고

---

## 테스트 파일 목록 (생성할 파일)

### 단위 테스트

1. `backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionQueryServiceTest.kt`
2. `backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionCommandServiceTest.kt`

### 통합 테스트

3. `backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/PositionControllerIntegrationTest.kt`

---

## 테스트 시나리오

### PositionQueryServiceTest (단위 테스트, MockK 사용)

#### listPositionsWithCurrentPrice

| # | 시나리오 | 기대 결과 |
|---|----------|----------|
| 1 | **Happy path**: 2개 포지션 보유, Redis 시세 있음 | 각 포지션에 currentPrice 주입된 PositionResult 2개 반환 |
| 2 | Redis 시세 없음 (getQuote → null) | DB에 저장된 currentPrice 그대로 반환 (null일 수 있음) |
| 3 | 포지션 없음 (빈 계좌) | 빈 리스트 반환 |
| 4 | 보유 수량 0인 포지션 (quantity=0)은 조회 안 됨 | 빈 리스트 반환 |

#### getPositionWithCurrentPrice

| # | 시나리오 | 기대 결과 |
|---|----------|----------|
| 5 | **Happy path**: ticker 보유 중, Redis 시세 있음 | currentPrice 주입된 PositionResult 반환 |
| 6 | ticker 보유 중, Redis 시세 없음 | DB currentPrice 사용 (null 포함 가능) |
| 7 | ticker 미보유 | `NoSuchElementException` 발생 |
| 8 | ticker 소문자 입력 | 컨트롤러에서 uppercase 처리 전제 (서비스는 원본 그대로 수신) |

```kotlin
// 테스트 구조 예시
class PositionQueryServiceTest {
    private val positionRepository = mockk<PositionRepository>()
    private val marketQuotePort = mockk<MarketQuotePort>()
    private lateinit var service: PositionQueryService

    @BeforeEach fun setUp() { service = PositionQueryService(positionRepository, marketQuotePort) }

    @Test fun `보유_포지션에_Redis_시세가_주입된다`() {
        val position = position(ticker = "005930", qty = BigDecimal("10"), avgPrice = BigDecimal("70000"))
        every { positionRepository.findByAccountIdAndQuantityGreaterThan(1L, BigDecimal.ZERO) } returns listOf(position)
        every { marketQuotePort.getQuote("005930") } returns QuoteSnapshot(ticker = "005930", price = BigDecimal("75000"), ...)

        val results = service.listPositionsWithCurrentPrice(1L)

        assertThat(results).hasSize(1)
        assertThat(results[0].currentPrice).isEqualByComparingTo("75000")
        assertThat(results[0].unrealizedPnl).isEqualByComparingTo("50000") // (75000-70000)*10
    }

    @Test fun `Redis_시세_없으면_DB_저장값_사용`() {
        val position = position(ticker = "005930", qty = BigDecimal("10"), currentPrice = BigDecimal("68000"))
        every { positionRepository.findByAccountIdAndQuantityGreaterThan(1L, BigDecimal.ZERO) } returns listOf(position)
        every { marketQuotePort.getQuote("005930") } returns null

        val results = service.listPositionsWithCurrentPrice(1L)

        assertThat(results[0].currentPrice).isEqualByComparingTo("68000") // DB 값 유지
    }

    @Test fun `포지션_없으면_빈_리스트_반환`() {
        every { positionRepository.findByAccountIdAndQuantityGreaterThan(1L, BigDecimal.ZERO) } returns emptyList()
        assertThat(service.listPositionsWithCurrentPrice(1L)).isEmpty()
    }

    @Test fun `단건_조회_포지션_없으면_예외`() {
        every { positionRepository.findByAccountIdAndTicker(1L, "005930") } returns Optional.empty()
        assertThatThrownBy { service.getPositionWithCurrentPrice(1L, "005930") }
            .isInstanceOf(NoSuchElementException::class.java)
            .hasMessageContaining("포지션을 찾을 수 없습니다")
    }
}
```

---

### PositionCommandServiceTest (단위 테스트, MockK 사용)

#### updateCurrentPriceByTicker

| # | 시나리오 | 기대 결과 |
|---|----------|----------|
| 1 | **Happy path**: ticker 보유 포지션 2개 | 2개 모두 updatePrice 호출됨 |
| 2 | 보유 포지션 없음 | 저장 호출 없음 (early return) |
| 3 | price가 정확히 BigDecimal로 처리됨 | double/float 연산 없음 확인 |

```kotlin
class PositionCommandServiceTest {
    private val positionRepository = mockk<PositionRepository>()
    private lateinit var service: PositionCommandService

    @Test fun `시세_갱신_시_보유_포지션_전체_updatePrice_호출`() {
        val pos1 = position(ticker = "005930", qty = BigDecimal("10"))
        val pos2 = position(ticker = "005930", qty = BigDecimal("5"), accountId = 2L)
        every { positionRepository.findByTickerAndQuantityGreaterThan("005930", BigDecimal.ZERO) } returns listOf(pos1, pos2)

        service.updateCurrentPriceByTicker("005930", BigDecimal("75000"), PriceSource.REALTIME)

        assertThat(pos1.currentPrice).isEqualByComparingTo("75000")
        assertThat(pos2.currentPrice).isEqualByComparingTo("75000")
    }

    @Test fun `보유_포지션_없으면_저장_호출_없음`() {
        every { positionRepository.findByTickerAndQuantityGreaterThan("005930", BigDecimal.ZERO) } returns emptyList()
        service.updateCurrentPriceByTicker("005930", BigDecimal("75000"), PriceSource.REALTIME)
        verify(exactly = 0) { positionRepository.saveAll(any()) }
    }
}
```

---

### PositionControllerIntegrationTest (통합 테스트, Testcontainers)

기존 `AccountControllerIntegrationTest` 패턴 동일 적용 (PostgreSQL + Redis Testcontainers).

| # | 시나리오 | 기대 결과 |
|---|----------|----------|
| 1 | **포지션 없는 계좌 조회** | 200 + 빈 배열 `[]` |
| 2 | **포지션 목록 정상 조회** | 200 + 포지션 목록 |
| 3 | **단건 포지션 조회 (uppercase ticker)** | 200 + 포지션 단건 |
| 4 | **소문자 ticker 입력** | 200 (대소문자 무관 동작) |
| 5 | **존재하지 않는 ticker** | 404 + `{ "code": "NOT_FOUND" }` |
| 6 | **존재하지 않는 계좌** | 서비스에서 포지션 없음으로 처리 (빈 배열 또는 404, spec.md 참고) |

```kotlin
// 통합 테스트 핵심 시나리오
@Test fun `포지션_없는_계좌는_빈_배열을_반환한다`() {
    // 계좌 생성 후 포지션 조회
    val accountId = createAccount()
    mockMvc.get("/api/v1/accounts/$accountId/positions")
        .andExpect { status { isOk() }; jsonPath("$") { isArray() }; jsonPath("$.length()") { value(0) } }
}

@Test fun `존재하지_않는_ticker_조회시_404를_반환한다`() {
    val accountId = createAccount()
    mockMvc.get("/api/v1/accounts/$accountId/positions/NOTEXIST")
        .andExpect { status { isNotFound() }; jsonPath("$.code") { value("NOT_FOUND") } }
}
```

**통합 테스트 설정 클래스 헤더** (기존 AccountControllerIntegrationTest 동일 패턴):
```kotlin
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class PositionControllerIntegrationTest {
    companion object {
        @Container @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")
        @Container @ServiceConnection(name = "redis")
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)
    }
}
```

---

## Edge Case 체크리스트

- [ ] 수량 0인 포지션 조회에서 제외됨
- [ ] 매도 후 quantity=0이면 목록에서 사라짐
- [ ] 매수 평균단가 계산: BigDecimal scale=4, HALF_UP 확인
- [ ] returnRate가 0으로 나눌 때 (avgBuyPrice=0 케이스) → Position.updatePrice 내 `avgBuyPrice > ZERO` 가드 확인
- [ ] ticker 대소문자 처리 (컨트롤러 trim + uppercase)
- [ ] Redis 시세 없을 때 null 반환 graceful degradation

---

## 삭제된 기능 회귀 방지 체크리스트

- [ ] `OrderQueryService`에 `listPositions`, `getPosition` 메서드가 없음 확인
- [ ] `dto/order/OrderResponse.kt`에 `PositionResponse` 클래스 없음 확인
- [ ] `PositionController`가 `OrderQueryService`를 import하지 않음 확인

---

## Acceptance Criteria

```bash
cd .worktrees/trading-api-position-service/backend/trading-api
./gradlew test
```

- 모든 테스트 GREEN
- `PositionQueryServiceTest` 전체 통과
- `PositionCommandServiceTest` 전체 통과
- `PositionControllerIntegrationTest` 전체 통과
- 기존 테스트 (`AccountCommandServiceTest`, `OrderCommandServiceTest` 등) 회귀 없음
