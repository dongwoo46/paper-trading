# Step 4: Persistence and Slot State Integration
Assigned agent: fullstack-dev

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/collector-api/subscription-routing/spec.md
- docs/phase/collector-api/subscription-routing/step-2.md
- backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/kis/persistence/KisWsSubscriptionRepository.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/kis/persistence/KisRestWatchlistRepository.kt
- backend/collector-api/src/main/resources/db/migration

## Tasks
- 생성: `backend/collector-api/src/main/resources/db/migration/V{next}__create_kis_subscription_routing_tables.sql`
  - 테이블: `kis_subscription_request`, `kis_routing_slot_state`
  - 인덱스: spec.md 명시 인덱스 반영.
- 생성: `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/subscriptions/persistence/SubscriptionRoutingEntity.kt`
- 생성: `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/subscriptions/persistence/SubscriptionRoutingJpaRepository.kt`
- 생성: `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/subscriptions/persistence/RoutingSlotStateEntity.kt`
- 생성: `backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/subscriptions/persistence/RoutingSlotStateJpaRepository.kt`
- 수정: `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/subscriptions/service/SubscriptionRoutingService.kt`
  - 신규 저장소 연동.
  - 재배치/승격/강등 처리 시 트랜잭션 경계 보장.
- 규칙:
  - 동일 `(symbol, source)` 유니크 충돌은 멱등 처리 분기와 명시적으로 구분.
  - WS/REST used 카운트는 라우팅 결과와 동기화되어야 함.

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew compileKotlin --no-daemon
cd backend/collector-api && ./gradlew test --tests "*SubscriptionRoutingService*" --no-daemon
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
