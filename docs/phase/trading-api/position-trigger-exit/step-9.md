# Step 9: Rework for Step 4 Second-Review Duplicate-Key Gap
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-position-trigger-exit

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/step-4.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/index.json

## Must-fix Review Finding
1. Duplicate-key/idempotency terminal-state determinism:
- In `PositionExitTriggerOrchestrator`, `DataIntegrityViolationException` is treated as success without verifying an existing idempotent order.
- Implement deterministic handling: on duplicate/conflict, load the existing order by deterministic idempotency key (or equivalent explicit confirmation path) and only mark trigger `TRIGGERED` when existence is confirmed.
- If confirmation fails, transition trigger to `FAILED` and publish failure notification with traceable identifiers.

## Verification Commands
- `cd backend/trading-api && ./gradlew test --tests "*PositionExitTrigger*Test"`
- `cd backend/trading-api && ./gradlew test --tests "*QuoteEventListenerTest"`
- `cd backend/trading-api && ./gradlew compileKotlin`

## Deliverables
- Duplicate-key path no longer produces ambiguous success.
- Trigger state transitions are deterministic and auditable for both confirmed-duplicate and non-confirmed failure paths.
- Targeted tests and compile command pass.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
