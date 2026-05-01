# Step 3: Testing and QA Verification
Assigned agent: test-engineer

## Working Directory
`.worktrees/trading-api-kis-execution-ws/backend/trading-api`

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/CODEX.md`
- `C:/Users/dw/Desktop/paper-trading/docs/ADR.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/spec.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/step-2.md`
- Implementation files changed by Step 2.
- Relevant tests changed or added by Step 2.

## Tasks
Verify the implementation against the spec and Step 2 acceptance criteria.

Required checks:
- Run feature-scoped unit tests for:
  - KIS execution notice parser.
  - KIS execution notice use case/idempotency.
  - KIS REST error mapping.
  - KIS WebSocket subscribe/reconnect/re-auth behavior.
  - Existing `ExecutionProcessorTest`.
  - Existing SSE tests.
- Run broader trading-api test suite if feature-scoped tests pass.
- Run Kotlin compilation.
- Inspect logs/assertions in tests to ensure tokens, app secrets, and approval keys are not logged.
- Confirm no scheduler bean remains active for `KisPaperPollingScheduler`.
- Confirm existing SSE endpoint contract remains `GET /api/v1/executions/stream` with `execution` events.
- Confirm all financial quantity/money conversions use `BigDecimal`.

## Acceptance Criteria
- Feature-scoped tests pass.
- Full `backend/trading-api` tests pass or any failures are clearly unrelated and documented with evidence.
- `compileKotlin` passes.
- No application code is changed unless fixing a test-discovered defect within this phase scope.
- Verification commands:
```bash
cd backend/trading-api && ./gradlew test
cd backend/trading-api && ./gradlew compileKotlin
```

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to repo root>
- Test result: <not run | passed N/N | failed N - list failing cases>
- Blockers: <none | description>
---
