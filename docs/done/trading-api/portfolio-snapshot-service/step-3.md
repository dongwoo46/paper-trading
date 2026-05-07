# Step 3: Implement Query API for Portfolio Chart
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-portfolio-snapshot-service/backend/trading-api

## Files to Read
- CODEX.md
- docs/phase/trading-api/portfolio-snapshot-service/spec.md
- docs/phase/trading-api/portfolio-snapshot-service/step-2.md
- docs/phase/trading-api/portfolio-snapshot-service/index.json

## Tasks
1. `DailyBalanceQueryService`와 `PortfolioSnapshotQueryService`를 구현한다.
2. `PortfolioSnapshotController`(또는 기존 Controller 확장)와 Request/Response DTO를 구현한다.
3. API 계약을 `portfolio-chart` 요구사항에 맞춰 고정한다.
4. 날짜 필터/검증 오류를 공통 예외 매핑 규칙에 맞춰 처리한다.
5. Controller/Service 테스트를 추가하고 Green으로 만든다.

## Acceptance Criteria
- 기간별 일별 잔고 조회 API가 동작한다.
- 특정 일자 포트폴리오 비중 조회 API가 동작한다.
- 잘못된 날짜 입력에 대한 400 에러 계약이 보장된다.
- 관련 테스트와 compileKotlin이 통과한다.

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
