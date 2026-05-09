# Step 5: Ingest Health Controller + KisProperties Config + Full Test Gate
Assigned agent: fullstack-dev

## Files to Read
- CLAUDE.md
- docs/ADR.md
- docs/phase/collector-api/market-microstructure-orderbook-ws/spec.md
- backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/kis/KisWsHealthController.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/service/OrderbookIngestMetrics.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/kis/KisProperties.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/common/GlobalExceptionHandler.kt

## Tasks

### 1. New Controller: `OrderbookIngestHealthController`
File: `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/kis/OrderbookIngestHealthController.kt`

- `@RestController`, `@RequestMapping("/api/internal/kis/orderbook")`
- Constructor-inject `OrderbookIngestMetrics`
- `@GetMapping("/ingest-health")` — `fun getIngestHealth(): ResponseEntity<OrderbookIngestHealthResponse>`
  - Call `OrderbookIngestMetrics.snapshot()`
  - Map to `OrderbookIngestHealthResponse`
  - Return `ResponseEntity.ok(...)`

### 2. New DTO: `OrderbookIngestHealthResponse`
File: `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/kis/dto/OrderbookIngestHealthResponse.kt`

- Data class with fields:
  - `receivedCount: Long`
  - `parseFailCount: Long`
  - `parseFailRatePct: Double`
  - `lastReceivedAtMs: Long?`
  - `lastReceivedAgeMs: Long?`

### 3. Update `KisProperties` default `trIds`
File: `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/kis/KisProperties.kt`

- Change default: `var trIds: List<String> = listOf("H0STCNT0", "H0STASP0")`
- The `trId` single-value field (legacy) remains unchanged for backward compatibility
- `resolvedTrIds()` already handles deduplication; no logic change needed
- Update the comment to reflect that both TR_IDs are now default

### 4. Write Controller Tests (TDD — write tests first)

File: `backend/collector-api/src/test/kotlin/com/papertrading/collector/presentation/kis/OrderbookIngestHealthControllerTest.kt`

Test cases:
- `GET /api/internal/kis/orderbook/ingest-health 200 — zero state`
  - Mock `OrderbookIngestMetrics.snapshot()` → all zeros, nulls
  - Assert `$.receivedCount = 0`, `$.parseFailCount = 0`, `$.parseFailRatePct = 0.0`
  - Assert `$.lastReceivedAtMs` is null (absent in JSON)
- `GET /api/internal/kis/orderbook/ingest-health 200 — 수신 있을 때`
  - Mock snapshot with `receivedCount=100, parseFailCount=1, parseFailRatePct=1.0`
  - Assert `$.receivedCount = 100`, `$.parseFailRatePct = 1.0`

### 5. End-to-end compile gate
Run full compile to confirm all wiring is correct across all steps:

```bash
cd backend/collector-api && ./gradlew compileKotlin
```

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test --tests "*.OrderbookIngestHealthControllerTest" && ./gradlew compileKotlin
```
Controller tests pass. Full `compileKotlin` succeeds with no errors or unresolved references.
