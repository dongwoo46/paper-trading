# Step 1: Feature Spec and API Spec
Assigned agent: quant-planner

## Working Directory
.worktrees/batch-schedule

## Files to Read
- CODEX.md
- backend/quant-worker/CODEX.md
- backend/quant-worker/docs/ADR.md
- backend/quant-worker/docs/PRD.md
- docs/TODO.md
- docs/phase/quant-worker/batch-schedule/index.json
- backend/quant-worker/tests/application/test_weekly_fetch_service.py
- backend/quant-worker/tests/jobs/test_catalog_weekly_fetch_job.py
- backend/quant-worker/tests/interfaces/test_weekly_api.py
- backend/quant-worker/tests/repositories/test_market_weekly_ohlcv_repository.py
- backend/quant-worker/tests/collectors/test_yfinance_weekly_collector.py
- backend/quant-worker/tests/collectors/test_pykrx_weekly_collector.py
- backend/quant-worker/src/application/weekly_fetch_service.py
- backend/quant-worker/src/jobs/catalog_weekly_fetch_job.py
- backend/quant-worker/src/collectors/yfinance_weekly_collector.py
- backend/quant-worker/src/collectors/pykrx_weekly_collector.py
- backend/quant-worker/src/repositories/market_weekly_ohlcv_repository.py
- backend/quant-worker/src/interfaces/api/app.py

## Tasks
1. Start by asking the user the open design questions listed below and stop after collecting answers.
2. Use the answers to align the design with the user before writing any spec text.
3. Confirm the scheduling model for the daily 17:00 KST run, including retry policy and failure notification expectations.
4. Confirm data flow boundaries:
   - yfinance weekly collection path
   - pykrx weekly collection fallback path
   - repository upsert behavior for `market_weekly_ohlcv`
   - API exposure through `POST /collect/weekly`
5. Only after the user confirms the design, write `spec.md`.
6. Generate `step-2.md` to `step-5.md` with concrete implementation, testing, review, and cleanup directives.
7. Keep all doc paths rooted at the main repo, not the worktree.
8. Apply the same question-first, decision-first pattern to every later step file you generate, not just this one.

## Questions to Ask the User First
- Should the batch schedule run only on trading days, or every calendar day at 17:00 KST?
- If the schedule fails, what retry policy should be used: immediate retry, delayed retry, or manual retry only?
- Should failure notifications go to Slack, logs only, or both?
- Should `yfinance` be the primary weekly source and `pykrx` the fallback, or do you want the opposite?
- Do you want any extra validation rules for empty symbol ranges, partial data, or stale bars?

## Acceptance Criteria
- Open design questions were asked and answered before any spec text was finalized.
- `spec.md` created only after user confirmation.
- `step-2.md` to `step-5.md` created.
- Each generated step file includes explicit file paths and the `## Agent Return Protocol` section.
- The spec is aligned with existing weekly collector, repository, and API tests.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
