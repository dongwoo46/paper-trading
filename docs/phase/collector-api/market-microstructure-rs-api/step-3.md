# Step 3: Testing and QA Verification
Assigned agent: test-engineer

## Working Directory
.worktrees/collector-api-market-microstructure-rs-api

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-microstructure-rs-api/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-microstructure-rs-api/step-2.md
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/market
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/application/market/service
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/test/kotlin/com/papertrading/collector

## Tasks
1. Verify functional correctness against spec.
- Validate quote/depth/flow field mapping.
- Validate RS ratio and returnDelta formula behavior.
- Validate missing-data/null policy.

2. Verify contract behavior.
- intervals `1m/5m/10m/1d/1w`
- session `regular/pre/after`
- period query `limit` and `from~to`
- error contract 400/404/422 code stability

3. Fill critical coverage gaps only.
- Add tests where required to prove contract invariants.
- Do not change business logic unless required to unblock tests; report explicitly.

4. Report residual risks.
- sparse intraday snapshots
- baseline unavailability and proxy benchmark behavior

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test --tests "*MarketMicrostructure*" --tests "*RelativeStrength*"
cd backend/collector-api && ./gradlew test
```
- All tests pass or blockers are explicitly documented.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---