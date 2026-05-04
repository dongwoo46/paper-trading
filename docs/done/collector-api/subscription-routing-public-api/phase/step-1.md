# Step 1: Feature Spec and API Spec
Assigned agent: service-planner

## Working Directory
.worktrees/collector-api-subscription-routing-public-api

## Files to Read
- CODEX.md
- docs/state.md
- docs/TODO.md
- docs/phase/collector-api/subscription-routing-public-api/index.json
- docs/phase/front/subscription-routing-ui/spec.md
- backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/internal/InternalSubscriptionController.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/kis/KisSymbolCatalogController.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/subscriptions/SubscriptionStatusController.kt

## Tasks
1. front/subscription-routing-ui 차단 해제를 위한 collector-api 외부 API spec을 작성한다.
2. 최소 범위 API를 정의한다:
   - favorites CRUD
   - strategy-priority symbol CRUD
   - routing status 조회
3. 계약을 고정한다: request/response, error code, idempotency, mode/channel validation.
4. 기존 internal API와 역할 경계를 명확히 문서화한다.
5. step-2~5 문서를 생성한다.

## Acceptance Criteria
- docs/phase/collector-api/subscription-routing-public-api/spec.md 생성
- docs/phase/collector-api/subscription-routing-public-api/step-2.md~step-5.md 생성
- front/subscription-routing-ui 차단 해제 조건이 명확히 정의됨

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
