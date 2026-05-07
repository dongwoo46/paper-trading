# Step 5: Rework for Review Findings
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-portfolio-snapshot-service/backend/trading-api

## Files to Read
- CODEX.md
- docs/phase/trading-api/portfolio-snapshot-service/spec.md
- docs/phase/trading-api/portfolio-snapshot-service/step-2.md
- docs/phase/trading-api/portfolio-snapshot-service/step-3.md
- docs/phase/trading-api/portfolio-snapshot-service/index.json

## Rework Items
1. `SNAPSHOT_ALREADY_RUNNING` 경로를 실제로 발생시키는 실행 중복 방지 로직을 구현한다.
2. 스냅샷 배치 경로에서 실패 시 `SNAPSHOT_COMPUTE_FAILED`로 일관되게 매핑되도록 예외 경계를 구현한다.
3. `generateDailySnapshots` 경로를 단일 트랜잭션/동등 원자성으로 보장해 DailyBalance와 PortfolioSnapshot의 부분 커밋을 방지한다.
4. 위 두 계약을 검증하는 테스트(충돌/실패/롤백 시나리오)를 추가한다.
5. compileKotlin 및 관련 타깃 테스트를 통과시킨다.

## Acceptance Criteria
- 리뷰 지적 2건이 코드/테스트로 해소된다.
- 409/500 에러 계약이 실제 런타임 경로에서 재현 가능하다.
- 부분 커밋 위험이 제거된다.

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
