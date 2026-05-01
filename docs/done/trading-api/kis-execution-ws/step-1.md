# Step 1: Feature Spec and API Spec
Assigned agent: Service Planner

## Working Directory
.worktrees/trading-api-kis-execution-ws

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/ADR.md
- C:/Users/dw/Desktop/paper-trading/docs/PRD.md
- C:/Users/dw/Desktop/paper-trading/docs/TODO.md
- C:/Users/dw/Desktop/paper-trading/.codex/references/agent-roles/service-planner.md
- C:/Users/dw/Desktop/paper-trading/backend/trading-api/graphify-out/graph.json

## Background and Requirements

Implement KIS account execution notice WebSocket integration for `trading-api`.

This is for the user's own KIS account order execution notices, not market-wide trade prints.

### Scope from docs/TODO.md
- Subscribe to KIS WebSocket execution notice channels:
  - Paper trading: `H0STCNI9`
  - Live trading: `H0STCNI0`
- Receive execution events and connect them to the existing execution flow:
  - KIS execution notice -> `ExecutionProcessor.fill()` or the current equivalent application flow
  - execution persistence
  - position update
  - SSE execution notification
- Replace/remove the existing `KisPaperPollingScheduler` polling flow.
- Map KIS order rejection/error responses to domain exceptions.
- Stabilize KIS token refresh logic, including WebSocket re-authentication.

### Technical Stack
- Kotlin / Spring Boot 3 / JPA / PostgreSQL / Redis
- Architecture: presentation -> application -> domain <- infrastructure
- Monetary and quantity calculations: `BigDecimal` only; no `double`/`float`.
- Execution/order state transitions must be explicit and idempotent.
- Never log secrets or raw tokens.

### Existing Code Discovery Instructions
Inside `.worktrees/trading-api-kis-execution-ws/backend/trading-api/`:
1. Find the existing KIS order execution and polling implementation.
2. Find `KisPaperPollingScheduler` and determine the exact replacement plan.
3. Find `ExecutionProcessor.fill()` or the current equivalent fill-processing service.
4. Find existing SSE notification code for execution events.
5. Find existing KIS token/client configuration and refresh behavior.
6. Check tests and test style before designing implementation steps.

## Tasks
1. Structure requirements and clarify ambiguities.
2. Confirm DDD/application boundaries for KIS execution notice ingestion.
3. Design message parsing, idempotency key, error mapping, reconnect/re-auth behavior.
4. Design any API/configuration changes needed for live vs paper mode.
5. Write `spec.md` under `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/spec.md`.
6. Generate `step-2.md` through `step-5.md` in the same phase folder.
   - Each step must include concrete implementation directives.
   - Each step must explicitly list required files to read.
   - Each step must reference the worktree path `.worktrees/trading-api-kis-execution-ws`.
   - All orchestration docs must remain under the main repo root `docs/`, not inside the worktree.

## Acceptance Criteria
- `spec.md` is created with architecture, flow, idempotency, error handling, token/re-auth, and test strategy.
- `step-2.md` through `step-5.md` are created.
- Implementation step includes TDD-first directives and scoped substeps.
- Test step includes compile/test commands for `backend/trading-api`.
- Review and cleanup steps include completion criteria.
- Every generated step file includes the `## Agent Return Protocol` section.
- No code changes are made in this planning step.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Test result: <not run | passed N/N | failed N - list failing cases>
- Blockers: <none | description>
---
