# Step 5: Cleanup and PR Preparation
Assigned agent: cleanup

## Working Directory
.worktrees/collector-api-market-microstructure-rs-api

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-microstructure-rs-api/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-microstructure-rs-api/index.json
- C:/Users/dw/Desktop/paper-trading/docs/TODO.md
- C:/Users/dw/Desktop/paper-trading/docs/state.md

## Tasks
1. Prepare completion summary and PR checklist.
2. Sync phase index/state/TODO once prior steps are PASS.
3. Document verification command summary and rollback/ops notes.

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test
cd backend/collector-api && ./gradlew compileKotlin
```
- Root docs are synchronized with final implementation state.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---