# Step 4: Code Review
Assigned agent: code-reviewer

## Open Questions
- Before executing this step, ask the user to confirm the Step 4 review focus.
- Confirm review should prioritize scheduler correctness, data integrity, retry safety, Slack/log secret safety, and missing tests.
- Confirm the review should not re-open the approved market/interval split unless the implementation contradicts it.
- Confirm behavior-equivalent naming deviations are acceptable when Step 2/3 documented them.
- If the user wants stricter naming, security, or operations review scope, update this step document before review.

## Confirmed Design Choices
- Automated collection must be split into four independent KST schedules.
- The scheduler must use realistic provider-availability buffers instead of exact exchange close times.
- The scheduler must preserve existing manual API behavior.
- Retried operations are safe because collection is incremental and repository writes are idempotent upserts.
- Slack notifications and logs are required for failures.
- Empty symbol set, partial data, and no-new-bar outcomes require distinct handling.

## Working Directory
.worktrees/batch-schedule

## Files to Read
- CODEX.md
- backend/quant-worker/CODEX.md
- backend/quant-worker/docs/ADR.md
- backend/quant-worker/docs/PRD.md
- docs/phase/quant-worker/batch-schedule/spec.md
- docs/phase/quant-worker/batch-schedule/step-2.md
- docs/phase/quant-worker/batch-schedule/step-3.md
- backend/quant-worker/src/interfaces/api/app.py
- backend/quant-worker/src/jobs/batch_schedule.py
- backend/quant-worker/src/application/daily_fetch_service.py
- backend/quant-worker/src/application/weekly_fetch_service.py
- backend/quant-worker/src/jobs/catalog_daily_fetch_job.py
- backend/quant-worker/src/jobs/catalog_weekly_fetch_job.py
- backend/quant-worker/src/repositories/market_daily_ohlcv_repository.py
- backend/quant-worker/src/repositories/market_weekly_ohlcv_repository.py
- backend/quant-worker/tests/

## Tasks
1. Review for correctness first, then maintainability.
2. Check schedule correctness:
   - `kr_daily` is pykrx daily, Mon-Fri 18:30 KST
   - `us_daily` is yfinance daily, Tue-Sat 08:30 KST
   - `kr_weekly` is pykrx weekly, Fri 19:30 KST
   - `us_weekly` is yfinance weekly, Sat 10:00 KST
   - no automated run uses `provider="all"`
3. Check incremental/idempotent safety:
   - service execution still uses catalog watermarks
   - successful rows update collection status only when `fetched_until_date` exists
   - repository upsert remains the write boundary
4. Check retry behavior:
   - only recoverable statuses are retried
   - backoff is delayed and exponential
   - retry attempts are bounded
   - final exhaustion emits notification
5. Check failure classification:
   - empty universe is no-op warning
   - partial data is not all-or-nothing
   - no-new-bar does not advance watermark
   - hard exceptions become failure results with safe error logging
6. Check Slack/logging safety:
   - no hardcoded webhook URLs
   - secrets are not logged
   - notification failures do not crash the scheduler loop
7. Check test quality:
   - tests are deterministic
   - provider and Slack calls are mocked
   - calendar/time tests use controlled clocks
   - existing weekly tests still cover collector, repository, and API contracts
8. Provide findings ordered by severity with file and line references.

## Acceptance Criteria
- Review findings focus on bugs, regressions, data integrity, retry safety, and missing tests.
- Any request for changes includes a concrete fix direction and the affected file path.
- If no findings are discovered, state that explicitly and list residual risks.
- Do not modify files unless the orchestrator explicitly asks for review fixes.
- Verification evidence from Step 3 is considered in the review.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was reviewed and whether findings were found>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
