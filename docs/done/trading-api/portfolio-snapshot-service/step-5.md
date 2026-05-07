# Step 5: Code Review and Cleanup
Assigned agent: code-reviewer

## Working Directory
.worktrees/trading-api-portfolio-snapshot-service/backend/trading-api

## Files to Read
- CODEX.md
- docs/phase/trading-api/portfolio-snapshot-service/spec.md
- docs/phase/trading-api/portfolio-snapshot-service/step-2.md
- docs/phase/trading-api/portfolio-snapshot-service/step-3.md
- docs/phase/trading-api/portfolio-snapshot-service/step-4.md
- docs/phase/trading-api/portfolio-snapshot-service/index.json

## Tasks
1. 신규 코드에서 금액 계산/멱등성/도메인 경계 위반 여부를 우선 점검한다.
2. API 계약 일관성, 예외 매핑, 테스트 누락을 리뷰한다.
3. must-fix 이슈가 있으면 재작업 지시 가능한 문장으로 정리한다.

## Acceptance Criteria
- PASS 또는 FAIL(BLOCKER 포함)로 리뷰 결론이 명확하다.
- 후속 재작업이 필요하면 파일/라인/영향이 식별 가능해야 한다.

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
