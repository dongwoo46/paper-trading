# Step 2: Implement DailyBalance and PortfolioSnapshot Core (TDD)
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-portfolio-snapshot-service/backend/trading-api

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/PRD.md
- docs/phase/trading-api/portfolio-snapshot-service/spec.md
- docs/phase/trading-api/portfolio-snapshot-service/index.json

## Tasks
1. TDD Red 단계로 `DailyBalanceCommandService`, `PortfolioSnapshotCommandService`의 핵심 동작 테스트를 먼저 작성한다.
2. Flyway 마이그레이션으로 `daily_balances`, `portfolio_snapshots` 테이블/인덱스를 추가한다.
3. Aggregate/Repository/JPA 엔티티를 구현하고 `account_id + business_date` 기준 멱등 upsert를 보장한다.
4. 배치 커맨드 서비스에서 계좌/포지션/가격 데이터로 일별 잔고 및 종목 비중을 계산한다.
5. Step 완료 시 index.json step 2 `substeps`를 실제 aggregate 단위로 업데이트한다.

## Acceptance Criteria
- 신규 테이블과 인덱스가 마이그레이션으로 생성된다.
- DailyBalance/PortfolioSnapshot 계산 로직이 테스트로 검증된다.
- 금액/비중 계산에 부동소수점이 사용되지 않는다.
- compileKotlin 및 관련 단위 테스트가 통과한다.

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
