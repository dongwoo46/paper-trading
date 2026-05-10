# Step 5: Cleanup, Summary, and PR Preparation
Assigned agent: orchestrator

## Open Questions
- Before executing this step, ask the user to confirm cleanup and PR-prep behavior.
- Confirm implementation, QA, and review completion reports are available and passing before writing done-state docs.
- Confirm whether to create only a PR draft/description or also run the actual PR creation command.
- Confirm whether to remove the worktree after PR creation.
- If unresolved defects remain, stop and route back to the responsible step before writing done-state docs.

## Confirmed Design Choices
- The final summary must describe the approved market/interval split, not the rejected single 17:00 KST schedule.
- The phase is complete only when scheduler behavior, retry policy, Slack/log notification, and no-data classifications are verified.
- Docs are written only under root `docs/`.
- Code changes, if any, must remain under `.worktrees/batch-schedule`.

## Working Directory
.worktrees/batch-schedule

## Files to Read
- CODEX.md
- docs/TODO.md
- docs/phase/quant-worker/batch-schedule/index.json
- docs/phase/quant-worker/batch-schedule/spec.md
- docs/phase/quant-worker/batch-schedule/step-2.md
- docs/phase/quant-worker/batch-schedule/step-3.md
- docs/phase/quant-worker/batch-schedule/step-4.md
- backend/quant-worker/CODEX.md
- backend/quant-worker/src/interfaces/api/app.py
- backend/quant-worker/src/jobs/batch_schedule.py
- backend/quant-worker/tests/

## Tasks
1. Confirm Step 2, Step 3, and Step 4 completion reports are `PASS`.
2. Inspect `git status --short` from the main repo root and from `.worktrees/batch-schedule`.
3. Confirm modified files are task-relevant:
   - scheduler code under `backend/quant-worker/src/`
   - scheduler tests under `backend/quant-worker/tests/`
   - orchestration docs under root `docs/`
4. Run or verify final commands:

```powershell
cd backend/quant-worker
python -m pytest tests
python -m py_compile src/interfaces/api/app.py
```

5. Write the done summary only if all checks pass:
   - target path: `docs/done/quant-worker/batch-schedule/batch-schedule-summary.md`
   - include schedule table, retry policy, notification behavior, verification commands, and residual operational risks
6. Update root `docs/TODO.md` only after the phase is complete:
   - change `배치 스케줄 자동화 | project: quant-worker | phase: batch-schedule` from `[ ]` to `[x]`
   - append `done: YYYY-MM-DD`
   - append `pr: #TBD` if PR number is not available
7. Update `docs/phase/quant-worker/batch-schedule/index.json` statuses according to actual completed steps.
8. Prepare PR description with:
   - summary
   - tests run
   - schedule table
   - operational notes for Slack env vars and timezone
9. Do not create or merge a PR unless the user explicitly requests it in the current turn.

## Acceptance Criteria
- No phase completion docs are written before implementation, QA, and review pass.
- `docs/TODO.md` is updated only after final verification passes.
- Summary docs are under root `docs/done/quant-worker/batch-schedule/`.
- PR preparation does not include secrets or environment values.
- The final orchestration report includes tests run and any residual risks.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of cleanup/PR-prep state>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
