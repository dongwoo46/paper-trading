# Step 6: 코드 리뷰 2차 검증
Assigned agent: code-reviewer

## Goal
Step 5 재작업 결과가 MUST FIX를 모두 해소했는지 재검증한다.

## Files to Read
- CODEX.md
- docs/phase/front/portfolio-chart/spec.md
- docs/phase/front/portfolio-chart/step-4.md
- docs/phase/front/portfolio-chart/step-4b-rework.md
- step 5에서 변경된 파일 전체

## Tasks
- MUST FIX 3건 해소 여부를 명시적으로 확인.
- 회귀/접근성/데이터 정합성 체크리스트를 재검토.

## Acceptance Criteria
```bash
cd .worktrees/front-portfolio-chart/frontend/trading-web
npm run build
```
- MUST FIX 0건일 때만 PASS.

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
