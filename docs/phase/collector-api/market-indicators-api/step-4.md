# Step 4: Code Review
Assigned agent: code-reviewer

## Working Directory
.worktrees/collector-api-market-indicators-api

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/step-2.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/step-3.md
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/domain/market/indicator
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/market
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/test/kotlin/com/papertrading/collector

## Tasks
1. Review for regressions and contract mismatch.
- Verify API params and error codes match `spec.md`.
- Verify interval/period/multi-indicator requirements are complete.
- Verify BB/RSI/MACD formulas and missing-data policy are implemented as specified.

2. Review architecture boundaries.
- Domain calculation logic must remain pure and framework-agnostic.
- Application service handles orchestration/validation only.
- Infra adapters isolate Redis/PostgreSQL access.

3. Review risk areas.
- Numeric precision and rounding policy consistency.
- Large `limit` performance and cache behavior.
- Edge-case behavior for sparse/partial bar sequences.

4. Produce prioritized findings with file/line references and remediation direction.

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew compileKotlin
```
- Review report includes severity ordering and concrete references.
- Any blocker-level findings are clearly marked.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
