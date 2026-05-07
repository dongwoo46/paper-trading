# Step 7: Rework for Atomic Rollback Behavior Proof
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-portfolio-snapshot-service/backend/trading-api

## Files to Read
- CODEX.md
- docs/phase/trading-api/portfolio-snapshot-service/spec.md
- docs/phase/trading-api/portfolio-snapshot-service/step-5-rework.md
- docs/phase/trading-api/portfolio-snapshot-service/step-6-review.md
- docs/phase/trading-api/portfolio-snapshot-service/index.json

## Rework Items
1. `generateDailySnapshots` 실패 시 DailyBalance/PortfolioSnapshot이 함께 롤백되는 행동 기반 테스트를 추가한다.
2. 단순 어노테이션 확인 테스트가 아니라, 실제 저장 동작/예외 유발/DB 상태 검증으로 원자성을 증명한다.
3. 필요 시 구현을 보완해 테스트를 Green으로 만든다.
4. compileKotlin 및 관련 타깃 테스트를 통과시킨다.

## Acceptance Criteria
- 부분 커밋이 발생하지 않음을 재현 가능한 테스트로 증명한다.
- 리뷰 지적사항(행동 기반 원자성 증명 누락)이 해소된다.

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
