# Step 2: Support/Resistance Algorithm and Confidence Improvement
Assigned agent: Quant Developer

## Working Directory
.worktrees/quant-research-chart-analysis-support-resistance-quality

## Files to Read
- CODEX.md
- docs/phase/quant-research/chart-analysis-support-resistance-quality/spec.md
- docs/phase/quant-research/chart-analysis-support-resistance-quality/step-1.md
- docs/phase/quant-research/chart-analysis-support-resistance-quality/step-2.md
- backend/quant-research/src/chart_analysis/infrastructure/support_resistance_finder.py
- backend/quant-research/src/chart_analysis/infrastructure/confidence_scorer.py
- backend/quant-research/src/chart_analysis/infrastructure/trend_classifier.py
- backend/quant-research/src/chart_analysis/infrastructure/indicator_calculator.py
- backend/quant-research/src/chart_analysis/application/precompute_pipeline_service.py
- backend/quant-research/src/chart_analysis/domain/value_objects.py
- backend/quant-research/src/chart_analysis/domain/chart_analysis_result.py
- backend/quant-ai/tests/unit/chart_analysis/infrastructure/test_support_resistance_finder.py

## Open Questions
None. Use the confirmed design choices below.

## Confirmed Design Choices
- Use window-specific parameters for distance, prominence, cluster width, top-N, endpoint lookback, and recency decay.
- Rank internal levels with composite score: touch count, rejection size, recency, volume, and price proximity.
- Add trend endpoint candidates.
- Preserve the external Decimal `LevelSet` contract; internal scored levels must not require a public response schema change.
- Apply bounded confidence adjustment from support/resistance quality and trade-plan validity.
- Select trade-plan levels using nearest valid support below close and nearest valid resistance above close.
- For MAX, use full history with decay for old touches.

## Tasks
1. Add focused failing tests first for the algorithm behavior that can be covered without new fixture files from Step 3.
2. Introduce internal support/resistance policy objects or constants keyed by `(window, interval)`.
3. Extend the finder flow to produce internal scored levels while returning the existing `LevelSet`.
4. Add composite ranking based on touch count, rejection strength, recency, volume, and price proximity.
5. Add trend endpoint candidates using recent highs/lows and ensure they are ranked through the same scoring path.
6. Update pipeline usage so the finder receives enough context to apply window-specific and current-price-aware policy.
7. Update trade-plan level selection to choose nearest valid support below close and resistance above close before falling back to existing percentage defaults.
8. Add bounded confidence adjustment from level quality and trade-plan validity without changing public recommendation fields.
9. Keep all final price outputs as `Decimal`.
10. Run targeted unit tests for changed support/resistance, confidence, and pipeline behavior.

## Acceptance Criteria
- Window-specific policy is explicit and covered by tests.
- Internal scoring improves ranking without changing the serialized `LevelSet` shape.
- Trade plan uses correct-side nearest levels when available.
- Confidence adjustment is bounded, deterministic, and test-covered.
- Existing Decimal safety constraints remain intact.
- Targeted tests for Step 2 pass.

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

