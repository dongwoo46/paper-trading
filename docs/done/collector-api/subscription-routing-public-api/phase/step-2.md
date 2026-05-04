# Step 2: Implementation (TDD)
Assigned agent: fullstack-dev

## Working Directory
.worktrees/collector-api-subscription-routing-public-api

## Files to Read
- docs/phase/collector-api/subscription-routing-public-api/spec.md
- docs/phase/collector-api/subscription-routing-public-api/index.json
- backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/kis/KisSymbolCatalogController.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/subscriptions/SubscriptionStatusController.kt

## Tasks
1. spec.md 계약에 맞춰 외부 API를 구현한다.
2. favorites CRUD API 구현.
3. strategy-priority symbol CRUD API 구현.
4. routing status 조회 API 구현(`sources`, `ws/rest` 결과 포함).
5. 입력 검증/정규화(mode/channel/symbol) 및 status contract를 spec과 일치시킨다.
6. TDD로 테스트를 먼저 작성하고 구현한다.

## Acceptance Criteria
- spec.md에 정의된 엔드포인트/스키마/status 값과 구현이 일치
- 중복 POST/없는 DELETE 멱등 결과(`already_exists`/`not_found`) 검증 테스트 포함
- `./gradlew test --tests "*subscription*"` 또는 관련 테스트 통과
- `./gradlew compileKotlin` 통과

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
