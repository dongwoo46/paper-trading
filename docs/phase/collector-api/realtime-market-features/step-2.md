# Step 2: Realtime Feature Aggregation Implementation
Assigned agent: fullstack-dev

## Working Directory
`.worktrees/collector-api-realtime-market-features`

## Files to Read
- `CODEX.md`
- `docs/ADR.md`
- `docs/phase/collector-api/realtime-market-features/spec.md`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/pipeline/RawEventPipeline.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/redis/QuoteRedisPublisher.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/kis/KisQuoteEvent.kt`

## Tasks
1. Domain model 추가:
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/marketfeature/MinuteBarState.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/marketfeature/FeatureSnapshot.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/marketfeature/FeatureWindow.kt`

Required signatures:
- `data class MinuteBarState(...)`
- `data class FeatureSnapshot(...)`
- `enum class FeatureWindow { M1, M5, M10 }`

2. Application service + port 추가:
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/marketfeature/service/MarketFeatureAggregationService.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/marketfeature/port/MarketFeatureStore.kt`

Required signatures:
- `fun onTick(event: KisQuoteEvent): Unit`
- `fun loadCurrent(symbol: String): MinuteBarState?`
- `fun saveCurrent(symbol: String, state: MinuteBarState): Unit`
- `fun appendBar(symbol: String, bar: MinuteBarState): Unit`
- `fun loadRecentBars(symbol: String, limit: Int): List<MinuteBarState>`
- `fun saveSnapshot(symbol: String, window: FeatureWindow, snapshot: FeatureSnapshot): Unit`

3. Redis infra 구현:
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/redis/MarketFeatureRedisStore.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/redis/RedisKeyPolicy.kt` (feature 키/TTL/maxlen 정책 상수)

Rules:
- `agg:1m:{symbol}:current`, `bars:1m:{symbol}`, `feature:{symbol}:{window}` 키를 정확히 사용.
- current TTL, bars TTL/maxlen, feature TTL을 `spec.md` 기준으로 상수화.
- raw tick 장기보관 금지. debug ring buffer는 feature flag가 true일 때만 쓰기.

4. Pipeline 연동:
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/pipeline/RawEventPipeline.kt`

Change requirement:
- 기존 `QuoteRedisPublisher.saveAndPublish(event)` 호출 유지.
- 이후 `MarketFeatureAggregationService.onTick(event)`를 호출하도록 의존성 주입 추가.
- 실패 격리: feature 집계 실패가 quote publish 경로 전체를 중단시키지 않도록 예외 처리 규칙 정의.

## Acceptance Criteria
```bash
cd .worktrees/collector-api-realtime-market-features/backend/collector-api && ./gradlew test --tests "*MarketFeature*" --tests "*RawEventPipeline*"
```

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Test result: <passed N/N | failed N — list failing cases> (if applicable)
- Blockers: <none | description>
---
