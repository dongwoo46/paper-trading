# Step 2: Domain and Application Routing Policy
Assigned agent: fullstack-dev

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/collector-api/subscription-routing/spec.md
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/service/KisSubscriptionOrchestrator.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/service/KisRestWatchlistService.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/kis/KisWsSubscription.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/kis/KisRestWatchlist.kt

## Tasks
- 생성: `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/subscriptions/SubscriptionSource.kt`
  - `enum class SubscriptionSource { STRATEGY, MANUAL }`
- 생성: `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/subscriptions/SubscriptionChannel.kt`
  - `enum class SubscriptionChannel { WS, REST, DROPPED }`
- 생성: `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/subscriptions/DropReason.kt`
  - `enum class DropReason { NONE, WS_FULL, WS_REST_FULL, MANUAL_EVICTED }`
- 생성: `backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/subscriptions/SubscriptionRoutingRequest.kt`
  - Aggregate 필드 시그니처: `symbol: String`, `source: SubscriptionSource`, `favorite: Boolean`, `channel: SubscriptionChannel`, `dropReason: DropReason`, `priorityScore: Int`
  - 도메인 메서드 시그니처: `assignWs()`, `assignRest(reason: DropReason)`, `drop(reason: DropReason)`, `toggleFavorite(favorite: Boolean)`
- 생성: `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/subscriptions/service/SubscriptionRoutingService.kt`
  - 메서드 시그니처:
    - `fun registerManual(symbol: String, favorite: Boolean, idempotencyKey: String?): RoutingResult`
    - `fun unregisterManual(symbol: String): RoutingResult`
    - `fun registerStrategy(symbol: String, strategyId: String?, idempotencyKey: String?): RoutingResult`
    - `fun unregisterStrategy(symbol: String): RoutingResult`
    - `fun rebalanceAndPromote(): RoutingSnapshot`
- 생성: `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/subscriptions/dto/RoutingResult.kt`
- 생성: `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/subscriptions/dto/RoutingSnapshot.kt`
- 규칙 반영:
  - STRATEGY WS 최우선, MANUAL 후순위.
  - WS 포화 시 STRATEGY 유입은 MANUAL eviction 우선 시도.
  - MANUAL은 REST 포화 시 drop 허용.
  - WS 여유 시 REST STRATEGY 우선 승격.

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew compileKotlin --no-daemon
```

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
