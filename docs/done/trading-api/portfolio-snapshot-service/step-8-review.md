# Step 8: Final Re-review After Atomic Rollback Proof
Assigned agent: code-reviewer

## Working Directory
.worktrees/trading-api-portfolio-snapshot-service/backend/trading-api

## Files to Read
- CODEX.md
- docs/phase/trading-api/portfolio-snapshot-service/spec.md
- docs/phase/trading-api/portfolio-snapshot-service/step-6-review.md
- docs/phase/trading-api/portfolio-snapshot-service/step-7-rework.md
- docs/phase/trading-api/portfolio-snapshot-service/index.json

## Tasks
1. Step 7에서 추가된 원자성 롤백 행동 테스트가 리뷰 지적사항을 충족하는지 검증한다.
2. SNAPSHOT_ALREADY_RUNNING / SNAPSHOT_COMPUTE_FAILED 계약 경로와 트랜잭션 일관성을 재점검한다.
3. PASS/FAIL을 명확히 제시하고, FAIL이면 must-fix를 파일/라인 기준으로 제시한다.

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
