# Step 4: Targeted QA Verification
Assigned agent: Test Engineer

## Working Directory
.worktrees/quant-research-chart-analysis-support-resistance-quality

## Files to Read
- CODEX.md
- docs/phase/quant-research/chart-analysis-support-resistance-quality/spec.md
- docs/phase/quant-research/chart-analysis-support-resistance-quality/step-2.md
- docs/phase/quant-research/chart-analysis-support-resistance-quality/step-3.md
- docs/phase/quant-research/chart-analysis-support-resistance-quality/step-4.md
- backend/quant-research/src/chart_analysis/infrastructure/support_resistance_finder.py
- backend/quant-research/src/chart_analysis/infrastructure/confidence_scorer.py
- backend/quant-research/src/chart_analysis/application/precompute_pipeline_service.py
- backend/quant-ai/tests/unit/chart_analysis/infrastructure/test_support_resistance_finder.py
- backend/quant-research/tests/unit/chart_analysis/infrastructure/test_ohlcv_repository.py

## Open Questions
None.

## Confirmed Design Choices
- Run targeted tests only for this phase until the final phase gate.
- Validate window-specific support/resistance quality, confidence adjustment, trade-plan selection, and fixture regression coverage.

## Tasks
1. Run targeted support/resistance tests.
2. Run targeted quant-research chart-analysis tests affected by pipeline, confidence, and OHLCV window behavior.
3. Verify fixtures cover all required scenarios from the spec.
4. Verify Decimal contract preservation by checking tests and response serialization behavior where covered.
5. Report any failures with the smallest reproducible command and likely owner file.
6. Do not broaden to full service test suites unless targeted tests are clean and the phase owner explicitly asks.

## Acceptance Criteria
- Targeted support/resistance tests pass.
- Targeted confidence/pipeline tests pass if present.
- No regression in OHLCV window mapping tests.
- QA report clearly lists commands and results.
- Any failure is classified as implementation issue, test issue, or environment blocker.

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

