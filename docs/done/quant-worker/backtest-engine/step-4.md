# Step 4: Async Execution Integration And Result Normalization
Assigned agent: Quant Dev

## Working Directory
.worktrees/quant-worker-backtest-engine

## Files to Read
- CODEX.md
- docs/phase/quant-worker/backtest-engine/spec.md
- docs/phase/quant-worker/backtest-engine/step-2.md
- docs/phase/quant-worker/backtest-engine/step-3.md
- backend/quant-worker/src/interfaces/api/app.py
- backend/quant-worker/src/jobs/batch_schedule.py
- backend/quant-worker/tests/interfaces/test_investor_flow_api.py
- backend/quant-worker/tests/jobs/test_batch_schedule.py

## Open Questions
- None. Use an in-process background task or a small service abstraction consistent with existing FastAPI patterns; do not introduce a new external queue unless needed.

## Confirmed Design Choices
- `POST /backtest-runs` creates async runs.
- Result summary is stored in DB; detailed artifacts remain file-based.

## Tasks
1. Write failing API/service tests for:
   - `POST /backtest-runs` creates `PENDING` and starts execution path
   - status transitions to `RUNNING`, `COMPLETED`, or `FAILED`
   - result endpoint returns normalized summary and artifact-backed details
   - logs endpoint returns captured logs
   - failed LEAN execution stores error message and stable status
2. Implement execution orchestrator:
   - load run
   - update `RUNNING`
   - export data
   - build workspace
   - run LEAN Docker runner
   - parse result artifacts
   - store summary and `COMPLETED`, or store failure and `FAILED`
3. Implement LEAN result parser/normalizer for required metrics:
   - total return
   - max drawdown
   - annualized return
   - Sharpe
   - Calmar
   - win rate
   - total trades
4. Wire API to orchestrator with clear error handling and idempotent run lookup.
5. Keep detailed artifacts on disk and expose them through result/log endpoints.

## Acceptance Criteria
- Async run path is covered with faked exporter/runner/parser dependencies.
- DB summary fields are populated only after successful completion.
- Failed execution preserves logs/artifacts where available.
- API remains responsive during run creation.
- Decimal/string serialization is preserved in API responses.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <paths relative to worktree root>
- Test result: <commands run and result>
- Blockers: <none | description>
---
