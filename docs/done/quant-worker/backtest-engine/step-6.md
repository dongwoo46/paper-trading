# Step 6: MOO Execution Rework And Code/Quant Re-Review
Assigned agent: Quant Dev → Code Reviewer

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
- Entry uses LEAN `MarketOnOpenOrder` submitted immediately after the completed signal bar; the future next-open price is never used to calculate submitted quantity.
- Fixed versioned execution policy `MOO_CLOSE_BUFFER_V1` applies one common `"500"` bps buy gap buffer to KR and US and is snapshotted under `execution_policy` in `run_config.json`.
- The execution policy is separate from the strategy DSL and the existing versioned cost profiles. `KR_DEFAULT_V1` and `US_DEFAULT_V1` fields and snapshot bytes remain unchanged.
- Buy target value uses signal-close total portfolio value and is capped by signal-close available cash.
- Buy sizing applies gap buffer, then adverse slippage, then the commission cash cap multiplicatively using `Decimal` and whole-share floor rounding.
- An unaffordable MOO is rejected in full with audit code `moo_buying_power_rejected`; it creates no financial-state change and is not retried until a ready entry condition evaluates false and later true.
- Sell submits the full held quantity as MOO without gap sizing.
- A final-bar MOO may remain unfilled and must emit audit code `moo_unfilled_at_end` without creating a fill or failing the run.

## Tasks
1. TDD RED: replace tests that inject the future next-open price before order creation with failing tests for completed-bar MOO submission and signal-close sizing.
2. TDD RED: add deterministic workspace tests requiring this exact run-config fragment:
   - `"execution_policy":{"gap_buffer_bps":"500","policy_id":"MOO_CLOSE_BUFFER_V1"}`
   - existing `KR_DEFAULT_V1` and `US_DEFAULT_V1` cost-profile snapshot bytes remain unchanged
3. TDD RED: add hand-calculated buy-sizing tests for the confirmed multiplicative Decimal formula and whole-share floor boundaries.
4. TDD RED: add execution tests for:
   - MOO is submitted during completed-bar processing, not through an after-market-open scheduled market order
   - submitted quantity is invariant when only the future next-open price changes
   - actual-open unaffordability rejects the whole MOO with `moo_buying_power_rejected`
   - rejection leaves cash and holdings unchanged
   - a continuously true entry condition does not retry after rejection
   - a later ready false-to-true transition re-arms entry
   - sell submits the complete held quantity as MOO without buy-gap sizing
   - a final-bar MOO remains unfilled and emits `moo_unfilled_at_end`
5. TDD GREEN: implement separate fixed execution-policy loading/snapshotting without adding fields to the DSL, DB schema, or existing Cost Profile V1 snapshots.
6. TDD GREEN: rework the pure execution trace so buy quantity is fixed from signal-close state, stored as the pending MOO quantity, and never calculated from the next bar open.
7. TDD GREEN: rework the fixed LEAN template to submit buy/sell `MarketOnOpenOrder` requests after completed-bar decisions, enforce full unaffordable-buy rejection before financial-state mutation, track rejected-entry re-arm state, and record both stable audit codes.
8. Run targeted domain, runtime, fixed-template, workspace, execution-trace, runner, and API regression tests; run `py_compile`, lint/format checks, `git diff --check`, and TODO/FIXME checks required by the project.
9. Re-review the full diff against `spec.md`, including:
   - no AI-generated Python execution
   - no unnecessary secret exposure or workspace path escape
   - no future-price access in submitted MOO sizing
   - no float monetary calculation or persistence
   - no negative cash, partial affordability fill, or implicit leverage
   - explicit KR/US market and cost-profile handling without placing the common execution buffer inside either profile
   - consistent metrics and artifact behavior
10. Return must-fix findings first, with file/line references. Review returns PASS only after all deterministic acceptance tests pass and no must-fix issue remains.

## Deterministic Acceptance Tests
1. Policy snapshot:
   - `run_config.json` contains `{"execution_policy":{"gap_buffer_bps":"500","policy_id":"MOO_CLOSE_BUFFER_V1"}}`
   - repeated workspace generation is byte-identical
   - existing cost-profile JSON remains byte-identical and contains no gap-buffer field
2. Golden buy sizing with `portfolio_value_T = available_cash_T = Decimal("100")`, `position_size_percent = "100"`, `close_T = Decimal("20")`, 500 bps gap buffer, 10 bps slippage, and 5 bps commission:
   - `gap_reference_price = Decimal("21.00")`
   - `reference_fill_price = Decimal("21.02100")`
   - `target_limit = 4`
   - `cash_limit = 4`
   - submitted buy MOO quantity = `4`
3. Future-price invariance:
   - varying only the next bar open never changes the already-submitted quantity `4`
   - no next-bar field is read before MOO submission
4. Affordable fill:
   - with next open `Decimal("10")`, the submitted quantity remains `4`
   - actual adverse fill price and commission use the selected Cost Profile V1
   - cash remains nonnegative
5. Unaffordable fill:
   - with next open `Decimal("25")`, actual adverse fill price is `Decimal("25.025")`
   - required cash is `Decimal("100.1500500")`, greater than available cash `Decimal("100")`
   - the complete entry is rejected, quantity remains zero, cash remains `Decimal("100")`, and exactly one `moo_buying_power_rejected` audit record is emitted
6. Re-arm:
   - a later ready entry=true bar does not resubmit while the rejected signal episode remains true
   - one later ready entry=false bar re-arms entry
   - the next ready entry=true bar submits one new MOO
7. Sell:
   - an invested exit submits exactly the negative full-held quantity as MOO
   - the 500 bps buy-gap buffer is not used for sell quantity
8. Terminal order:
   - a final-bar signal may submit MOO but produces no fill without a later tradable bar
   - financial state remains unchanged and exactly one `moo_unfilled_at_end` audit record is emitted
9. Adapter contract:
   - the fixed template calls `market_on_open_order`
   - no `after_market_open(..., 0)` sizing callback or `market_order` is accepted for this flow

## Acceptance Criteria
- All deterministic acceptance tests above pass with exact Decimal values and stable audit codes.
- The implementation uses only completed signal-bar data to size submitted MOO quantity.
- No unaffordable buy changes cash or holdings, creates partial quantity, or introduces leverage.
- Existing DSL, DB schema, and `KR_DEFAULT_V1`/`US_DEFAULT_V1` snapshot contracts remain unchanged.
- Original security, workspace isolation, KR/US, metrics, persistence, API, and financial-precision acceptance coverage remains green.
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
