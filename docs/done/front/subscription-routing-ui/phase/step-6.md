# Step 6: Rework — Retry should replay original failed payload
Assigned agent: fullstack-dev

## Working Directory
.worktrees/front-subscription-routing-ui

## Files to Read
- docs/phase/front/subscription-routing-ui/spec.md
- docs/phase/front/subscription-routing-ui/step-2.md
- docs/phase/front/subscription-routing-ui/step-4.md
- frontend/trading-web/src/features/kis-management/ui/KisPanel.tsx
- frontend/trading-web/src/features/kis-management/ui/KisPanel.test.tsx

## Rework Context
Code review medium finding:
- Retry 버튼이 실패 당시 payload가 아니라 현재 입력값으로 재시도함.

## Tasks
1. 테스트 먼저 수정/추가: 실패 후 input 변경 시에도 retry는 원래 실패 payload(symbol/action/mode/channel)를 재사용해야 함.
2. 구현 수정: retry 핸들러가 `last*Action`의 저장 payload를 그대로 사용하도록 변경.
3. 기존 in-flight guard/validation 동작 유지.
4. targeted tests + build 검증.

## Acceptance Criteria
- retry semantics가 "failed request replay"로 고정됨.
- 리뷰 medium finding 해소.
- tests/build 통과.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
