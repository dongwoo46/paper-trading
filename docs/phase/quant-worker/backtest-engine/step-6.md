# Step 6: Code And Quant Review
Assigned agent: Code Reviewer

## Working Directory
.worktrees/quant-worker-backtest-engine

## Files to Read
- CODEX.md
- docs/phase/quant-worker/backtest-engine/spec.md
- docs/phase/quant-worker/backtest-engine/step-2.md
- docs/phase/quant-worker/backtest-engine/step-3.md
- docs/phase/quant-worker/backtest-engine/step-4.md
- docs/phase/quant-worker/backtest-engine/step-5.md
- all files modified by Steps 2-5

## Open Questions
- None.

## Confirmed Design Choices
- Review must focus on correctness, safety, reproducibility, financial precision, and look-ahead bias.

## Tasks
1. Review the full diff against `spec.md`.
2. Check security and execution safety:
   - no AI-generated Python execution
   - Docker command does not expose secrets unnecessarily
   - generated workspaces do not write credentials
   - path handling prevents traversal outside configured run/artifact roots
3. Check quant correctness:
   - no look-ahead-prone strategy execution
   - market/currency policy is explicit
   - KR/US handling is not accidentally conflated
   - metrics are parsed and persisted consistently
4. Check financial precision:
   - no float money persistence
   - Decimal/string serialization for API and DB values
5. Check tests cover the acceptance criteria and important failure modes.
6. Return must-fix findings first, with file/line references.

## Acceptance Criteria
- Review returns PASS only if no must-fix issue remains.
- Any requested changes are concrete and scoped.
- Non-blocking follow-up suggestions are separated from must-fix items.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <paths relative to worktree root>
- Test result: N/A (review step)
- Blockers: <none | description>
---
