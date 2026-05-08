# Step 3: QA Verification and Regression Checks
Assigned agent: test-engineer

## Working Directory
.worktrees/trading-api-position-trigger-exit

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/step-2.md

## Objective
Validate correctness, idempotency, and regression safety for position-trigger auto-exit.

## QA Directives
1. Review Step 2 test coverage and add missing tests for edge cases:
- exact boundary percent handling (`>0`, `<100`, scale).
- quote equals threshold behavior.
- closed position behavior (`422` semantics).
- concurrent trigger attempts for same position.

2. Execute targeted and full verification:
- `cd backend/trading-api && ./gradlew test --tests "*PositionExitTrigger*"`
- `cd backend/trading-api && ./gradlew test --tests "*QuoteEventListener*"`
- `cd backend/trading-api && ./gradlew test`
- `cd backend/trading-api && ./gradlew compileKotlin`

3. Validate persistence constraints in integration tests or repository tests:
- unique `(position_id)` in `position_exit_triggers`.
- unique `(account_id, idempotency_key)` for orders.

4. Verify failure and retry semantics:
- initial order failure marks failed state.
- retry path reuses same idempotency key.
- permanent failure publishes notification event.

## Deliverables
- Test result summary with failing/passing counts.
- List of residual risks and non-covered scenarios.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
