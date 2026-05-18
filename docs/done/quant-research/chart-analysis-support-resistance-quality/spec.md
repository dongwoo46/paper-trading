# Support/Resistance Quality Improvement

## Overview
Long-window chart analysis currently reuses one peak-detection policy across 3M, 6M, 1Y, 2Y, and MAX. This causes stale or visually weak support/resistance levels to be selected, and the first returned level is used directly for trade-plan stop and target prices.

The improvement keeps the external Decimal level contract stable while adding internal scored levels, window-specific policies, trend endpoint candidates, quality-aware ranking, and quantitative acceptance fixtures.

## Current Problem Evidence
- `PostgresOhlcvRepository` maps windows to very different bar counts: 3M-D 60, 6M-D 120, 1Y-D 240, 1Y-W 52, 2Y-W 104, MAX-W full history.
- `ScipyPeakSupportResistanceFinder` uses fixed `distance=5`, `prominence=ATR*0.3`, and `cluster_width=ATR*0.5` for every window.
- Candidate clusters are sorted by recency and truncated to 5 without touch count, rejection strength, volume, current-price relevance, or trend context.
- `PrecomputePipelineService._build_trade_plan` uses the first support and resistance directly.
- `WeightedRuleConfidenceScorer` does not include support/resistance quality or trade-plan validity.
- Existing fixtures assert only existence, Decimal type, range, and count; they do not assert ranked top-K quality, correct side of close, or expected zones.

## Confirmed Design Choices
- Window Policy: use window-specific parameters for distance, prominence, cluster width, and top-N.
- Level Ranking: use a composite score based on touch count, rejection size, recency, volume, and price proximity.
- Trend Handling: add endpoint candidates using current/recent high as resistance reference and recent low as support reference.
- Output Contract: introduce internal scored level data while preserving the existing serialized Decimal `LevelSet` contract.
- Confidence Integration: apply bounded confidence adjustment from level quality and trade-plan validity.
- Acceptance Dataset: add curated fixtures for monotonic trend, sideways range, role flip, long weekly, and MAX-like history.
- Trade Plan Level Selection: choose nearest valid support below close and nearest valid resistance above close.
- MAX Window Scope: use full history but decay old touches.
- Validation Metrics: use ATR or percent-band zone tolerance, ranked top-K assertions, and correct-side-of-close checks.

## Algorithm Policy
The finder should compute raw support and resistance candidates from peaks, troughs, and trend endpoints, then cluster candidates into price zones using a window-specific cluster width.

Each internal scored level should include enough information to rank levels deterministically:
- level price
- side: support or resistance
- touch count
- rejection strength
- recency contribution
- volume contribution
- price proximity contribution
- endpoint contribution flag when applicable
- aggregate score

The external `LevelSet` should still expose only ordered Decimal supports and resistances.

## Window-Specific Policy
Window policies should be explicit and test-covered. Exact numeric defaults may be tuned during implementation, but the policy must vary by window and interval.

Required policy dimensions:
- peak distance
- prominence multiplier against ATR
- cluster width multiplier against ATR
- returned top-N levels
- recency decay strength
- endpoint lookback

Expected direction:
- 3M/6M daily windows should keep moderate sensitivity and prioritize current-price relevance.
- 1Y daily and weekly windows should increase peak distance and require stronger clustered evidence.
- 2Y/MAX weekly windows should use stronger recency decay so older touches support confidence but do not dominate recent market structure.

## Trend Endpoint Candidates
The algorithm should add endpoint candidates for strong directional windows so monotonic trends do not return only stale interior peaks.

Rules:
- For uptrends, recent/current highs may be resistance references and recent swing lows may be support references.
- For downtrends, recent/current lows may be support references and recent swing highs may be resistance references.
- Endpoint candidates must still pass quality or side-of-close validation before trade-plan use.

## Trade Plan Policy
Trade plan support and resistance selection should not blindly use the first returned level.

Rules:
- Stop-loss reference: nearest valid support below the latest close.
- Target reference: nearest valid resistance above the latest close.
- If no valid level exists on the correct side, keep existing percentage fallback behavior.
- Risk/reward must remain Decimal-based.

## Confidence Policy
Support/resistance quality should adjust confidence within a bounded range so it reflects chart reliability without overpowering trend, RSI, pattern, and volume signals.

Required behavior:
- High-quality levels on the correct side of close may add a small positive adjustment.
- Missing or invalid trade-plan levels may subtract a small penalty.
- Adjustment must be bounded and deterministic.
- Existing grade thresholds should remain configurable through the current environment variables.

## Validation Requirements
Acceptance tests must cover:
- monotonic uptrend and downtrend endpoint handling
- sideways range with repeated touches
- role-flip zone where prior resistance becomes support or prior support becomes resistance
- long weekly 2Y-like data
- MAX-like history where old touches exist but recent levels should rank higher after decay

Validation metrics:
- expected zones must appear in ranked top-K
- level price must fall within ATR-based or percent-based tolerance of expected zone
- trade-plan support must be below close and resistance must be above close when available
- old MAX-window levels must not outrank materially better recent levels solely due to age or raw touch count
- final public output must remain Decimal-compatible with the existing response shape

## Non-Goals
- Do not change the public API response shape in this phase.
- Do not add synthetic or property-based tests in this phase; note them as a future optional enhancement.
- Do not convert final price-level output to float.
- Do not redesign the entire recommendation model beyond bounded support/resistance quality adjustment.
- Do not implement frontend changes.

