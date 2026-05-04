# Step 3: Internal API and Controller Wiring
Assigned agent: fullstack-dev

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/collector-api/subscription-routing/spec.md
- docs/phase/collector-api/subscription-routing/step-2.md
- backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/internal/InternalSubscriptionController.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/common/GlobalExceptionHandler.kt

## Tasks
- 수정: `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/internal/InternalSubscriptionController.kt`
  - 엔드포인트 시그니처 추가:
    - `POST /api/internal/subscriptions/manual`
    - `DELETE /api/internal/subscriptions/manual/{symbol}`
    - `PUT /api/internal/subscriptions/manual/{symbol}/favorite`
    - `GET /api/internal/subscriptions/manual/favorites`
    - `POST /api/internal/subscriptions/strategy`
    - `DELETE /api/internal/subscriptions/strategy/{symbol}`
    - `GET /api/internal/subscriptions/routing/status`
- 생성: `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/internal/dto/SubscriptionRoutingRequestDto.kt`
- 생성: `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/internal/dto/SubscriptionRoutingResponseDto.kt`
- 생성: `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/internal/dto/FavoriteToggleRequestDto.kt`
- 수정: `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/common/GlobalExceptionHandler.kt`
  - 400/404/409 매핑 규칙 추가(중복 key 충돌, 동시성 충돌 포함).
- 규칙:
  - DTO ↔ application DTO 변환은 presentation 레이어에서만 수행.
  - idempotency key는 헤더(`Idempotency-Key`) 또는 body 필드 중 하나로 수용하되 충돌 시 400.

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test --tests "*InternalSubscriptionController*" --no-daemon
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
