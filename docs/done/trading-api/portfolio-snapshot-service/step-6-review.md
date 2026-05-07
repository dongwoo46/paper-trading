# Step 6: Re-review After Rework
Assigned agent: code-reviewer

## Working Directory
.worktrees/trading-api-portfolio-snapshot-service/backend/trading-api

## Files to Read
- CODEX.md
- docs/phase/trading-api/portfolio-snapshot-service/spec.md
- docs/phase/trading-api/portfolio-snapshot-service/step-5-rework.md
- docs/phase/trading-api/portfolio-snapshot-service/index.json

## Tasks
1. Step 5 rework가 리뷰 지적사항(에러 계약/원자성/테스트 누락)을 해소했는지 확인한다.
2. 회귀 위험(멱등성, 예외 매핑, 트랜잭션 경계)을 다시 점검한다.
3. PASS/FAIL을 명확히 제시하고, FAIL 시 must-fix 항목을 파일/라인 기준으로 제시한다.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to worktree root>
- Test result: <passed N/N | failed N — list failing cases> (if applicable)
- Blockers: <none | description>
---
