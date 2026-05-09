# Step 2: H0STASP0 Parser + KisOrderbookEvent Value Object
Assigned agent: fullstack-dev

## Files to Read
- CLAUDE.md
- docs/ADR.md
- docs/phase/collector-api/market-microstructure-orderbook-ws/spec.md
- backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/entity/kis/KisQuoteEvent.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/pipeline/KisRawEventParser.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/pipeline/RawEventPipeline.kt

## Tasks

### 1. New Value Object: `KisOrderbookEvent`
File: `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/entity/kis/KisOrderbookEvent.kt`

- Data class with fields:
  - `ticker: String`
  - `askPrices: List<BigDecimal>` (top-5, index 0 = best ask, ascending price)
  - `bidPrices: List<BigDecimal>` (top-5, index 0 = best bid, descending price)
  - `askQtys: List<BigDecimal>` (top-5, corresponding quantities)
  - `bidQtys: List<BigDecimal>` (top-5, corresponding quantities)
  - `receivedAt: Instant`
- All price/qty fields must be `BigDecimal` (no Float/Double)
- Class-level KDoc: describe H0STASP0 source and field mapping

### 2. New Parser: `KisOrderbookEventParser`
File: `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/pipeline/KisOrderbookEventParser.kt`

- `@Component` class with `fun parse(payload: String): KisOrderbookEvent?`
- H0STASP0 message format: `{flag}|{tr_id}|{count}|{^-delimited fields}`
  - flag '0'/'1' = realtime data; other = system message (skip)
  - TR_ID must be exactly `"H0STASP0"` (reject others)
- Field indices for H0STASP0 (standard KIS 국내주식 실시간 호가잔량):
  - [0]  `MKSC_SHRN_ISCD` — ticker
  - [3..7]  `ASKP1..ASKP5` — 5 매도호가 (best ask first)
  - [13..17] `BIDP1..BIDP5` — 5 매수호가 (best bid first)
  - [23..27] `ASKP_RSQN1..ASKP_RSQN5` — 5 매도잔량
  - [33..37] `BIDP_RSQN1..BIDP_RSQN5` — 5 매수잔량
  - Minimum field count: 38
- On `NumberFormatException` or field count < 38: log warning with ticker (if available) and return null
- Do NOT throw; always return null on parse error

### 3. Update `RawEventPipeline`
File: `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/pipeline/RawEventPipeline.kt`

- Inject `KisOrderbookEventParser` (constructor injection)
- In `publish()`, extract TR_ID from payload before delegating:
  - If TR_ID == `"H0STCNT0"` → existing path (unchanged)
  - If TR_ID == `"H0STASP0"` → call `KisOrderbookEventParser.parse(payload)`; return if null; otherwise no further action in this step (orderbook save wired in step-3)
  - Other TR_IDs → log debug and return
- TR_ID extraction: `payload.split("|", limit = 4)` → `parts[1]`; guard `parts.size < 2` → return
- Do NOT change the H0STCNT0 path

### 4. Write Tests (TDD — write tests first)

File: `backend/collector-api/src/test/kotlin/com/papertrading/collector/application/kis/pipeline/KisOrderbookEventParserTest.kt`

Test cases:
- `H0STASP0 정상 파싱 — bestAsk, bestBid, top-5 qty 검증`
  - Provide a synthetic payload with 50+ `^`-delimited fields
  - Assert `event.askPrices[0]` = expected best ask
  - Assert `event.bidPrices[0]` = expected best bid
  - Assert `event.askQtys.size` = 5, `event.bidQtys.size` = 5
- `잘못된 TR_ID — null 반환`
- `flag 문자 잘못됨 — null 반환` (flag not in '0'..'1')
- `필드 수 부족 — null 반환`
- `숫자 파싱 오류 — null 반환`

File: `backend/collector-api/src/test/kotlin/com/papertrading/collector/application/kis/pipeline/RawEventPipelineTest.kt` (extend existing)

Test cases to add:
- `H0STASP0 payload — orderbook parser 호출됨` (verify routing; orderbook store is stubbed/no-op in this step)
- `H0STCNT0 payload — 기존 경로 유지됨` (existing behavior unchanged)

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test --tests "*.KisOrderbookEventParserTest" --tests "*.RawEventPipelineTest" && ./gradlew compileKotlin
```
All target tests pass. `compileKotlin` succeeds with no errors.
