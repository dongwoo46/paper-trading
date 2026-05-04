# Step 4: Code Review
Assigned agent: code-reviewer

## Working Directory
.worktrees/collector-api-subscription-routing-public-api

## Files to Read
- docs/phase/collector-api/subscription-routing-public-api/spec.md
- step-2 구현 변경 파일
- step-3 검증 리포트

## Tasks
1. API contract 일치 여부를 우선 검토한다.
2. 입력 검증 누락, 상태값 불일치, 경계 위반(`/api/internal` 혼용)을 확인한다.
3. 회귀 위험(기존 `/api/subscriptions/status`, `/api/kis/symbols/subscriptions`)을 점검한다.

## Acceptance Criteria
- High/Medium 이슈 없으면 PASS
- 이슈가 있으면 파일/라인/영향/수정가이드 포함

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
