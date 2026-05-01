# Step 10: Final Full Test Suite Gate
Assigned agent: test-engineer

## Working Directory
`.worktrees/trading-api-kis-execution-ws/backend/trading-api`

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/CODEX.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/index.json`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/spec.md`

## Tasks
Run the final full-suite gate for the touched service after all implementation and review steps are complete.

This is the only phase step where the full trading-api test suite is expected.

Required command from the worktree service directory:
```bash
./gradlew.bat test compileKotlin
```

## Acceptance Criteria
- Full trading-api test suite passes.
- `compileKotlin` passes.
- If the command fails, classify whether it is implementation, test, or environment related.
- Do not change application code unless the failure is clearly a test-discovered defect and the fix remains in phase scope.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was verified>
- Files modified: <list of paths relative to repo root>
- Test result: <not run | passed N/N | failed N - list failing cases>
- Blockers: <none | description>
---
