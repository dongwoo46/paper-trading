# Step 3: Acceptance Fixtures and Regression Tests
Assigned agent: Quant Developer

## Working Directory
.worktrees/quant-research-chart-analysis-support-resistance-quality

## Files to Read
- CODEX.md
- docs/phase/quant-research/chart-analysis-support-resistance-quality/spec.md
- docs/phase/quant-research/chart-analysis-support-resistance-quality/step-1.md
- docs/phase/quant-research/chart-analysis-support-resistance-quality/step-2.md
- docs/phase/quant-research/chart-analysis-support-resistance-quality/step-3.md
- backend/quant-research/src/chart_analysis/infrastructure/support_resistance_finder.py
- backend/quant-research/src/chart_analysis/application/precompute_pipeline_service.py
- backend/quant-ai/tests/unit/chart_analysis/infrastructure/test_support_resistance_finder.py
- backend/quant-ai/tests/fixtures/chart_analysis/support_resistance/sample_uptrend_3m.json
- backend/quant-ai/tests/fixtures/chart_analysis/support_resistance/sample_sideways_6m.json

## Open Questions
None. Synthetic/property tests are explicitly future optional work and are not required in this phase.

## Confirmed Design Choices
- Curated fixtures are required for monotonic trend, sideways range, role flip, long weekly, and MAX-like history.
- Validation uses zone tolerance metrics with ATR or percent bands, ranked top-K checks, and correct-side-of-close checks.
- Public output remains Decimal level tuples.

## Tasks
1. Add curated support/resistance fixtures for monotonic trend, role flip, long weekly, and MAX-like history.
2. Strengthen existing 3M uptrend and 6M sideways expectations so tests verify expected zones, not just count.
3. Add test helpers for ATR-based or percent-based zone tolerance.
4. Add ranked top-K assertions for expected support and resistance zones.
5. Add correct-side-of-close assertions for trade-plan selected support and resistance.
6. Add MAX-like regression assertions proving recent high-quality levels outrank stale old levels after decay.
7. Keep fixture data readable and deterministic; avoid randomized generation in this phase.
8. Run the targeted support/resistance test file.

## Acceptance Criteria
- Curated fixture coverage includes monotonic, sideways, role-flip, long weekly, and MAX-like scenarios.
- Tests fail if stale resistance below current price is selected as the primary target in a strong uptrend.
- Tests verify ranked top-K level quality using tolerance bands.
- Tests verify support below close and resistance above close when valid levels exist.
- No public API/schema change is required by the tests.

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

