# Step 8: Rework for Step 4 Code Review Findings
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-position-trigger-exit

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/step-4.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/index.json

## Must-fix Review Findings
1. Trigger/order consistency and idempotency safety:
- Do not leave trigger state ambiguous when duplicate-key or race happens.
- Ensure deterministic terminal state by loading existing idempotent order and persisting consistent trigger outcome.

2. Failure/retry/notification flow:
- Add explicit failed transition persistence for non-duplicate order failures.
- Add bounded retry behavior and notification event/log for permanent failure path.

3. API stale version conflict contract:
- Implement version conflict path for trigger upsert and map to HTTP 409.

4. Percent validation contract:
- Enforce range and scale (scale <= 4) with 400 mapping.

5. Account default trigger API:
- Implement `PUT /api/accounts/{accountId}/exit-trigger-default` per spec,
  or if intentionally out of scope, update spec.md and step docs to de-scope explicitly.

## Verification Commands
- `cd backend/trading-api && ./gradlew test --tests "*PositionExitTrigger*Test"`
- `cd backend/trading-api && ./gradlew test --tests "*QuoteEventListenerTest"`
- `cd backend/trading-api && ./gradlew compileKotlin`

## Deliverables
- All five review findings resolved or explicitly de-scoped in spec/docs with rationale.
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