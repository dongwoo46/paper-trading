# Step 5: Cleanup and PR Preparation
Assigned agent: cleanup

## Working Directory
.worktrees/collector-api-market-indicators-api

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/collector-api/market-indicators-api/index.json
- C:/Users/dw/Desktop/paper-trading/docs/TODO.md
- C:/Users/dw/Desktop/paper-trading/docs/state.md

## Tasks
1. Prepare completion documentation.
- Summarize implemented scope, tests, and known limitations.
- Ensure phase index status/result fields reflect final step outcomes.

2. Update orchestration docs.
- Mark feature completion in root `docs/TODO.md` when all prior steps PASS.
- Update `docs/state.md` to next correct orchestration status.
- Create done summary doc path per project convention.

3. Prepare PR-ready checklist.
- Include spec compliance table (intervals, period modes, indicators, missing policy).
- Include verification command outputs summary.
- Include rollback and operational risk notes.

## Acceptance Criteria
```bash
cd backend/collector-api && ./gradlew test
cd backend/collector-api && ./gradlew compileKotlin
```
- Root docs are synchronized with actual implementation state.
- PR preparation notes are complete and auditable.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
