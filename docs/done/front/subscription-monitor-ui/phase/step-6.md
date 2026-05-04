# Step 6: Rework — Fix polling-driven global loading regression
Assigned agent: fullstack-dev

## Working Directory
.worktrees/front-subscription-monitor-ui

## Files to Read
- docs/phase/front/subscription-monitor-ui/spec.md
- docs/phase/front/subscription-monitor-ui/step-2.md
- docs/phase/front/subscription-monitor-ui/step-4.md
- frontend/trading-web/src/features/kis-management/ui/KisPanel.tsx
- frontend/trading-web/src/features/kis-management/ui/KisPanel.test.tsx

## Rework Context
Code review found medium issue:
- monitor polling fetch state가 global StatusBar loading에 결합되어 10초마다 loading 깜빡임 회귀 발생.

## Tasks
1. 테스트 먼저 추가: monitor polling/background refetch가 global loading indicator를 불필요하게 토글하지 않음을 검증.
2. 구현 수정: monitor polling의 background fetching 상태는 global `loading`에서 제외하고, 필요 시 monitor 섹션 로컬 상태로만 표시.
3. 기존 read-only monitor 동작(10초 polling + manual refresh) 유지.
4. 관련 테스트/빌드 실행 후 결과 보고.

## Acceptance Criteria
- code-review medium finding 해소.
- polling 중 global StatusBar loading이 주기적으로 깜빡이지 않음.
- 기존 테스트 + 빌드 통과.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
