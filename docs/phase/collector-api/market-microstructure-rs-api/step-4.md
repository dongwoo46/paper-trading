# Step 4: Code Review
Assigned agent: code-reviewer

## Working Directory
.worktrees/collector-api-market-microstructure-rs-api

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-microstructure-rs-api/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-microstructure-rs-api/step-2.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-microstructure-rs-api/step-3.md
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/market
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/market
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/test/kotlin/com/papertrading/collector

## Tasks
1. Review for spec/contract mismatch and regressions.
2. Review architecture boundaries (domain purity, app orchestration, infra separation).
3. Review risk areas (precision, sparse data handling, query limit performance).
4. Produce prioritized findings with file/line references and concrete fix direction.

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew compileKotlin
```
- Review report includes severity-ordered findings and clear blocker markings.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---