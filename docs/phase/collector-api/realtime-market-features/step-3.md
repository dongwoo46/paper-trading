# Step 3: Feature Query API and Contract Alignment
Assigned agent: fullstack-dev

## Working Directory
`.worktrees/collector-api-realtime-market-features`

## Files to Read
- `CODEX.md`
- `docs/ADR.md`
- `docs/phase/collector-api/realtime-market-features/spec.md`
- `docs/phase/collector-api/realtime-market-features/step-2.md`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/subscriptions/SubscriptionStatusController.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/subscriptions/dto/SubscriptionStatusResponse.kt`

## Tasks
1. Query service 추가:
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/marketfeature/service/MarketFeatureQueryService.kt`

Required signatures:
- `fun getSnapshots(symbol: String, windows: Set<FeatureWindow>): List<FeatureSnapshot>`

2. Presentation contract 추가:
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/marketfeature/MarketFeatureController.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/marketfeature/dto/MarketFeatureResponse.kt`

Required endpoint:
- `GET /api/market/features/{symbol}`
- Query param `windows` parsing (`1m,5m,10m` 기본값)

Response schema requirement:
- `symbol`, `asOf`, `features[]` with standardized fields from `spec.md`

3. Error contract 정렬:
- invalid symbol/windows -> 400
- snapshot 없음 -> 404
- rollover race/inconsistent state -> 409

4. Existing API 스타일 정렬:
- 기존 controller 응답 포맷 및 Kotlin data class 네이밍 규칙 준수.
- import/패키지 구조는 `presentation/*` 관례를 그대로 따른다.

## Acceptance Criteria
```bash
cd .worktrees/collector-api-realtime-market-features/backend/collector-api && ./gradlew test --tests "*MarketFeatureController*" --tests "*MarketFeatureQueryService*"
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
