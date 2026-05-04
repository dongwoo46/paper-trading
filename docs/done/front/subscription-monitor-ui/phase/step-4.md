# Step 4: Code Review
Assigned agent: code-reviewer

## Files to Read
- docs/phase/front/subscription-monitor-ui/spec.md
- docs/phase/front/subscription-monitor-ui/step-2.md
- frontend/trading-web/src/features/kis-management/ui/KisPanel.tsx
- frontend/trading-web/src/shared/api/index.ts

## Tasks
1. 버그/회귀/경계 위반 중심으로 코드 리뷰를 수행한다.
2. monitor 섹션이 read-only 요구사항을 침해하지 않는지 검토한다.
3. 성능/가독성/유지보수 관점에서 필수 개선사항만 제안한다.
4. 발견 사항을 severity 기준으로 정리한다.

## Acceptance Criteria
- High/Medium 이슈는 재현 가능 근거와 함께 보고된다.
- 이슈가 없다면 no finding을 명시한다.
- 잔여 리스크(있다면) 문서화.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
