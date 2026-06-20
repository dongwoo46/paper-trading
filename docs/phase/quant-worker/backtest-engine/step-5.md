# Step 5: Targeted QA Verification
Assigned agent: Test Engineer

## Working Directory
.worktrees/quant-worker-backtest-engine

## Files to Read
- CODEX.md
- docs/phase/quant-worker/backtest-engine/spec.md
- docs/phase/quant-worker/backtest-engine/step-2.md
- docs/phase/quant-worker/backtest-engine/step-3.md
- docs/phase/quant-worker/backtest-engine/step-4.md
- all files modified by Steps 2-4

## Open Questions
- None.

## Confirmed Design Choices
- Verification is targeted to quant-worker backtest changes.

## Tasks
1. Run targeted unit/API tests added for backtest domain, repository, exporter, runner, orchestrator, and API routes.
2. Run compile checks for modified Python files.
3. Run migration SQL checks for `backtest_runs`.
4. Verify no modified file contains unresolved TODO/FIXME related to this phase.
5. If Docker is available, run an optional LEAN smoke test with a minimal KR/US fixture and report the result. If Docker is unavailable, report it as an environment limitation, not a code failure, unless the code path cannot be tested by fakes.
6. Run `git diff --check`.

## Acceptance Criteria
- All targeted tests pass.
- Python compile checks pass.
- `git diff --check` passes.
- Docker smoke status is clearly reported.
- Any failure includes exact failing command, error summary, and suspected owner step.

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
