# Step 6: Rework — Fix routing-status manual source attribution
Assigned agent: fullstack-dev

## Working Directory
.worktrees/collector-api-subscription-routing-public-api

## Files to Read
- docs/phase/collector-api/subscription-routing-public-api/spec.md
- docs/phase/collector-api/subscription-routing-public-api/step-2.md
- docs/phase/collector-api/subscription-routing-public-api/step-4.md
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/subscriptions/service/SubscriptionRoutingService.kt
- backend/collector-api/src/test/kotlin/com/papertrading/collector/application/subscriptions/service/SubscriptionRoutingServiceTest.kt

## Rework Context
Code review medium finding:
- `routing-status.sources.manual`가 merged routed 결과(ws+rest) 기준으로 계산되어 계약 의미와 불일치.

## Tasks
1. 테스트 먼저 수정/추가: manual source는 manual-source 저장소 기준임을 검증.
2. 구현 수정: `sources.manual`을 manual source only로 계산.
3. favorites/strategyPriority source attribution은 기존 의미 유지.
4. targeted tests + compileKotlin 실행.

## Acceptance Criteria
- `sources.manual` 계약 의미 일치.
- step-4 medium finding 해소.
- 테스트/컴파일 통과.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
