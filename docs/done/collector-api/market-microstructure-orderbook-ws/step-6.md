# Step 6: QA Verification — Full Test Suite + Regression Gate
Assigned agent: test-engineer

## Files to Read
- CLAUDE.md
- docs/ADR.md
- docs/phase/collector-api/market-microstructure-orderbook-ws/spec.md
- backend/collector-api/src/test/kotlin/com/papertrading/collector/application/kis/pipeline/KisOrderbookEventParserTest.kt
- backend/collector-api/src/test/kotlin/com/papertrading/collector/infra/redis/OrderbookRedisStoreTest.kt
- backend/collector-api/src/test/kotlin/com/papertrading/collector/application/market/service/MarketMicrostructureQueryServiceTest.kt
- backend/collector-api/src/test/kotlin/com/papertrading/collector/presentation/market/MarketMicrostructureControllerTest.kt
- backend/collector-api/src/test/kotlin/com/papertrading/collector/domain/market/analytics/RelativeStrengthCalculatorTest.kt

## Tasks

### 1. Run full test suite
Run all collector-api tests to confirm no regressions.

### 2. Verify scope-specific test coverage

All of the following tests must be present and passing:

**Parser (KisOrderbookEventParserTest):**
- H0STASP0 정상 파싱 (bestAsk, bestBid, qty 검증)
- 잘못된 TR_ID → null
- flag 문자 잘못됨 → null
- 필드 수 부족 → null
- 숫자 파싱 오류 → null

**Redis (OrderbookRedisStoreTest):**
- save → Hash 저장 및 TTL 검증
- depthImbalance 분모 0 → 필드 없음
- load → OrderbookSnapshot 반환
- load → Hash 비어있을 때 null

**Metrics (OrderbookIngestMetricsTest):**
- recordReceived → receivedCount 증가, lastReceivedAtMs 갱신
- recordParseFail → parseFailCount 증가
- parseFailRatePct receivedCount=0 → 0.0

**Service (MarketMicrostructureQueryServiceTest):**
- 1d 요청 → Postgres 소스 사용
- 1w 요청 → Postgres 소스 사용
- intraday + orderbook null → 나머지 필드 정상
- intraday + orderbook 있음 → bestBid/bestAsk/spread/depth 매핑됨
- 1d/1w → microstructure 전체 null

**Domain (RelativeStrengthCalculatorTest):**
- timestamp 역순 입력 → IllegalArgumentException
- timestamp 정순 → 정상 계산

**Controller (MarketMicrostructureControllerTest + OrderbookIngestHealthControllerTest):**
- 기존 200/400/404/422 케이스 유지
- ingest-health 200 zero state
- ingest-health 200 수신 있을 때

### 3. Verify no regression in existing tests

Confirm the following pre-existing tests still pass without modification:
- `KisRawEventParserTest` — H0STCNT0 parse path unchanged
- `RawEventPipelineTest` — existing H0STCNT0 path unchanged
- `QuoteRedisPublisherTest` — unchanged
- `MarketIndicatorsControllerTest` — unchanged
- `MarketBarControllerTest` — unchanged
- `RelativeStrengthCalculatorTest` — existing cases unchanged (new cases added)

### 4. Build verification
Confirm lint/format and compile gate pass.

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test && ./gradlew compileKotlin
```
All tests pass (zero failures). `compileKotlin` succeeds. No pre-existing test regressions.
