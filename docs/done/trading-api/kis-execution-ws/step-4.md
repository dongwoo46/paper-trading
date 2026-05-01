# Step 4: Code Review
Assigned agent: code-reviewer

## Working Directory
`.worktrees/trading-api-kis-execution-ws/backend/trading-api`

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/CODEX.md`
- `C:/Users/dw/Desktop/paper-trading/docs/ADR.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/spec.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/step-2.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/step-3.md`
- Step 2 and Step 3 completion reports.
- Full diff for the phase branch.

## Tasks
Review for correctness, safety, and architectural fit.

Focus areas:
- Account execution notice vs market-wide trade print separation.
- Correct channel usage: paper `H0STCNI9`, live `H0STCNI0`.
- Idempotency before financial mutation.
- `executions.external_execution_id` uniqueness and duplicate handling.
- KIS order correlation by external order id plus trading mode/account scope.
- Explicit order/execution/settlement transitions.
- No `double`/`float` for money or quantity.
- No token, approval key, app secret, or full credential-bearing raw frame in logs.
- Reconnect/re-auth behavior is bounded and does not retry non-idempotent REST order commands.
- KIS REST error mapping uses explicit exceptions rather than generic `check` failures.
- `KisPaperPollingScheduler` is removed or inactive.
- Existing SSE event flow remains after-commit.
- Tests meaningfully cover parser, idempotency, error mapping, reconnect/re-auth, and regression behavior.

## Acceptance Criteria
- Findings are listed first, ordered by severity, with file/line references.
- If no blocking findings exist, state that clearly and identify any residual risk.
- Do not make code changes unless explicitly requested by the orchestrator.
- Optional verification command if needed:
```bash
cd backend/trading-api && ./gradlew test compileKotlin
```

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was reviewed and the result>
- Files modified: <list of paths relative to repo root>
- Test result: <not run | passed N/N | failed N - list failing cases>
- Blockers: <none | description>
---
