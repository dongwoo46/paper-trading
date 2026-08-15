# Backtest Engine Spec

## Goal
Build the first project-native backtesting foundation for `quant-worker` using QuantConnect LEAN as the only execution engine.

The feature must let local AI generate a structured strategy JSON DSL, validate that DSL, export local OHLCV data for KR and US markets into LEAN local data artifacts, execute LEAN through Docker, and expose asynchronous backtest run APIs with DB-stored metadata/summary and file-based detailed artifacts.

## Confirmed Decisions
- Execution engine: QuantConnect LEAN only, executed with Docker image `quantconnect/lean:latest`.
- Strategy input: JSON DSL. Local AI produces strategy JSON; project code validates it and converts it into LEAN template configuration. Raw AI-generated LEAN Python code is not allowed in the MVP.
- LEAN project model: maintain one fixed LEAN template project. Each backtest copies the template into `runs/{runId}` and injects run-specific `strategy.json`, symbol/date/cash/market config, data, and result paths.
- Data supply: read local PostgreSQL `market_daily_ohlcv` and `market_weekly_ohlcv`, then export data into LEAN local data artifacts before running Docker.
- MVP data interval: daily bars first. Weekly export must be represented in the design and extension points but does not need full strategy support in the first execution path.
- Markets: KR and US are both supported from MVP.
- API: asynchronous run API only.
- Persistence: DB stores run metadata and summary metrics only. Detailed LEAN raw output, equity curve, trades, logs, generated config, and exported data live as run artifacts.
- DSL scope: multi-factor DSL skeleton. The MVP executable scalar-factor contract is defined below. `flow`, `fundamental`, `macro`, `news_sentiment`, `disclosure`, `paper_factor`, and `event` are recognized planned categories but are rejected for execution with a stable validation error until follow-up phases implement point-in-time data and LEAN custom-data adapters.
- Execution semantics: daily, long-only, and exactly one symbol per run. A fully completed and ready bar at date `T` may create a signal. A valid order quantity is fixed using only state available after bar `T` completes, and the algorithm submits a LEAN `MarketOnOpenOrder` for the next available market open; the next-open price is never used to size that order. Entry does not pyramid an existing position, and exit has precedence when entry and exit are both true.
- Transaction costs: every run selects a versioned market profile. The profile identifier and resolved Decimal-string values are snapshotted in the run artifacts. `KR_DEFAULT_V1` uses 5 bps commission per buy and sell, 10 bps adverse slippage per fill, and 18 bps sell tax. `US_DEFAULT_V1` uses 5 bps commission per buy and sell, 10 bps adverse slippage per fill, and 0 bps sell tax.
- MOO execution policy: every run uses fixed versioned execution policy `MOO_CLOSE_BUFFER_V1`. It applies one common 500 bps buy gap buffer to KR and US, is not part of the strategy DSL or either cost profile, and is snapshotted in `run_config.json` as `{"execution_policy":{"gap_buffer_bps":"500","policy_id":"MOO_CLOSE_BUFFER_V1"}}`. Existing `KR_DEFAULT_V1` and `US_DEFAULT_V1` cost-profile meanings and snapshot bytes do not change.

## Non-Goals
- Do not add `vectorbt`, `backtrader`, `backtesting.py`, or a custom in-process engine.
- Do not execute AI-generated Python code.
- Do not store full equity curves, orders, trades, or logs in relational DB in this phase.
- Do not implement full news/macro/fundamental/disclosure/paper-factor custom data support in this phase.
- Do not implement synchronous `POST /backtest`.

## Domain Model

### Backtest Run
Required state machine:
- `PENDING`
- `RUNNING`
- `COMPLETED`
- `FAILED`
- `CANCELED`

Minimum metadata:
- `run_id` UUID
- `status`
- `market`: `KR` or `US`
- `symbol`
- `resolution`: `daily` initially
- `start_date`, `end_date`
- `initial_cash`
- `currency`: `KRW` for KR, `USD` for US
- `cost_profile`: selected immutable profile ID, `KR_DEFAULT_V1` or `US_DEFAULT_V1`
- `strategy_json`
- `artifact_path`
- `error_message`
- `created_at`, `started_at`, `finished_at`

Minimum summary metrics:
- total return
- max drawdown
- annualized return
- Sharpe
- Calmar
- win rate
- total trades

Monetary values and persisted metrics must use `Decimal` in Python domain code and serialized strings in API responses. Do not use floats for money, cash, prices, fees, or persisted metrics. Numeric conversion for LEAN input/output is allowed at the boundary and must be covered by tests.

## Strategy JSON DSL

The DSL is the contract between local AI and the backtest engine.

MVP request shape:

```json
{
  "name": "example strategy",
  "version": 1,
  "universe": {
    "market": "KR",
    "symbols": ["005930"]
  },
  "factors": [
    {
      "id": "close_above_sma_20",
      "category": "technical",
      "indicator": "sma",
      "params": { "window": 20 },
      "source": { "field": "close" }
    }
  ],
  "entry": {
    "operator": "and",
    "conditions": [
      {
        "type": "cross_above",
        "left": { "field": "close" },
        "right": { "factor": "close_above_sma_20" }
      }
    ]
  },
  "exit": {
    "operator": "or",
    "conditions": [
      {
        "type": "cross_below",
        "left": { "field": "close" },
        "right": { "factor": "close_above_sma_20" }
      }
    ]
  },
  "risk": {
    "position_size_percent": "100",
    "stop_loss_percent": null,
    "take_profit_percent": null
  }
}
```

MVP executable categories:
- `price`
- `technical`

### Executable factor vocabulary

Each factor source is exactly one raw daily field: `open`, `high`, `low`, `close`, or `volume`. Factor-to-factor sources are not executable in this MVP. `adjusted_close` is rejected because the exporter does not provide an adjusted-price series. `lag` must be absent or `0`; `shift` must be absent or `0`. The same lag/shift restriction applies to rule operands.

Category and indicator pairings are strict:

| Category | Indicator | Exact params | Value at completed bar `T` | First ready bar |
|---|---|---|---|---|
| `price` | `price` | `{}` | raw source value `x_T` | first bar |
| `technical` | `returns` | `{"window": N}` | `x_T / x_(T-N) - 1` | after `N + 1` source bars |
| `technical` | `sma` | `{"window": N}` | arithmetic mean of `x_(T-N+1)..x_T` | after `N` source bars |
| `technical` | `ema` | `{"window": N}` | first ready value is the `N`-bar SMA; later `EMA_T = alpha*x_T + (1-alpha)*EMA_(T-1)`, `alpha = 2/(N+1)` | after `N` source bars |
| `technical` | `wma` | `{"window": N}` | linearly weighted mean with weights `1..N` from oldest to newest | after `N` source bars |
| `technical` | `rsi` | `{"window": N}` | Wilder RSI described below | after `N + 1` source bars |
| `technical` | `roc` | `{"window": N}` | `(x_T / x_(T-N) - 1) * 100` | after `N + 1` source bars |
| `technical` | `momentum` | `{"window": N}` | `x_T - x_(T-N)` | after `N + 1` source bars |

`N` must be a positive integer and `params` must contain exactly the keys shown. Unknown, missing, or extra parameter keys are rejected. `returns` and `roc` fail the run with stable execution error `factor_division_by_zero` if their denominator is zero; they must never emit an infinity, NaN, or silently substituted value.

RSI uses Wilder smoothing. For source differences `delta_T = x_T - x_(T-1)`, gain is `max(delta_T, 0)` and loss is `max(-delta_T, 0)`. The first average gain and loss are the arithmetic means of the first `N` differences. Later values use `avg_T = (avg_(T-1)*(N-1) + current_T)/N`. If average loss is zero, RSI is `100`; otherwise `RSI_T = 100 - 100/(1 + average_gain/average_loss)`.

Recognized indicators `macd`, `bollinger`, and `atr` are not executable in this MVP and must return stable `unsupported_indicator_for_execution`. `price` paired with `technical`, or any technical indicator paired with `price`, must return `invalid_category_indicator_pair`.

### Rule vocabulary and truth conditions

Rules are one flat `and` or `or` over one or more conditions; nested rule groups are rejected. Each operand contains exactly one of:

- a raw field reference: `{"field": "close"}`
- a declared factor reference: `{"factor": "sma_20"}`
- a Decimal-string literal: `{"value": "70"}`

`cross_above` and `cross_below` require two time-series operands; literals are rejected for crosses. At completed bar `T`:

- `cross_above`: `left_(T-1) <= right_(T-1)` and `left_T > right_T`
- `cross_below`: `left_(T-1) >= right_(T-1)` and `left_T < right_T`
- `greater_than`: `left_T > right_T`
- `less_than`: `left_T < right_T`

At least one side of `greater_than` or `less_than` must be a field or factor. Literal-to-literal conditions are rejected. Recognized condition `equal` is not executable and returns `unsupported_condition_type`. Comparisons are strict; no hidden tolerance or equality fallback is applied.

The strategy becomes ready only when every factor and every operand used by both entry and exit rules has the history required for its calculation, including the previous pair required by a cross. Before that point neither entry nor exit may emit a signal.

### Position and execution behavior

- The strategy is long-only and has two exposure states: flat or invested.
- An entry signal while flat snapshots `portfolio_value_T` and `available_cash_T` after completed daily bar `T`. Let `position_fraction = position_size_percent/100` and `target_value = portfolio_value_T * position_fraction`. `position_size_percent` must be a Decimal string greater than `0` and at most `100`.
- Buy sizing uses fixed execution policy `MOO_CLOSE_BUFFER_V1`. Let `B = Decimal("500")/10000`, `S = Decimal(slippage_bps_per_fill)/10000`, and `C = Decimal(commission_bps_per_fill)/10000`. Using only the completed signal-bar close:
  - `gap_reference_price = close_T * (1 + B)`
  - `reference_fill_price = gap_reference_price * (1 + S)`
  - `target_limit = floor(target_value / reference_fill_price)`
  - `cash_limit = floor(available_cash_T / (reference_fill_price * (1 + C)))`
  - `q = max(0, min(target_limit, cash_limit))`
- Every value in the sizing formula is a `Decimal`; equity quantity is whole-share only. If `q` is zero, no order is submitted. If `q` is positive, that exact quantity is submitted as a LEAN `MarketOnOpenOrder` immediately after bar `T` completes. It is never recomputed from the next-open price.
- At the next available open, let `actual_buy_fill_price = open_next * (1 + S)` and `required_cash = q * actual_buy_fill_price * (1 + C)`. If `required_cash` exceeds cash available immediately before fill, the entire buy MOO is rejected before financial state changes: no partial fill, negative cash, or implicit leverage is allowed. The run remains active and records stable audit code `moo_buying_power_rejected` in its LEAN order/event or log artifacts.
- After `moo_buying_power_rejected`, no new entry MOO is submitted while the ready entry rule remains true. Entry is re-armed only after a later ready completed bar evaluates entry to false and a subsequent ready completed bar evaluates it to true.
- An entry signal while invested is ignored: it never adds to or rebalances the position.
- An exit signal while invested submits one full-held-quantity sell MOO immediately after the signal bar completes. Sell sizing does not use the 500 bps gap buffer. An exit signal while flat creates no order.
- If entry and exit are both true for bar `T`, exit wins. An invested position submits only the exit; a flat strategy remains flat and does not submit an entry.
- Signals are calculated only from fully completed daily bars. A submitted MOO is eligible to fill only at the next available market open, never at the signal bar's close or open.
- A MOO submitted from the final available bar may remain unfilled when the run ends. It does not create a fill or financial-state change and must be recorded in the LEAN order/event or log artifacts with stable audit code `moo_unfilled_at_end`; this event alone does not fail the run.
- `stop_loss_percent` and `take_profit_percent` must both be `null`; a non-null value returns `unsupported_risk_control`.

### Cost profiles

The backtest request must include `costProfile`; the selected profile must match the request market.

| Profile | Market | `commission_bps_per_fill` | `slippage_bps_per_fill` | `sell_tax_bps` |
|---|---|---:|---:|---:|
| `KR_DEFAULT_V1` | `KR` | `"5"` | `"10"` | `"18"` |
| `US_DEFAULT_V1` | `US` | `"5"` | `"10"` | `"0"` |

All profile values are Decimal strings. One basis point is `1/10000`.

The 500 bps MOO gap buffer is an execution-policy value, not a transaction cost. It must not be added to `CostProfile`, persisted as a cost-profile rate, or alter the byte representation of `KR_DEFAULT_V1` or `US_DEFAULT_V1`.

- Buy fill price = next-open base price multiplied by `1 + slippage_bps_per_fill/10000`.
- Sell fill price = next-open base price multiplied by `1 - slippage_bps_per_fill/10000`.
- Commission = absolute filled notional multiplied by `commission_bps_per_fill/10000`, on every buy and sell fill.
- Sell tax = absolute sell filled notional multiplied by `sell_tax_bps/10000`; no tax is charged on buys.
- Costs apply only to filled quantity, affect cash/equity and normalized metrics, and have no minimum fee or intermediate rounding rule added by project code.

The selected profile ID is durable run metadata and must survive DB persistence/reload without being inferred again from `market`. Each run must deterministically snapshot the profile identifier, market, and all three resolved strings in its artifact directory. The DB stores only the selected profile ID; resolved commission, slippage, and tax strings remain in the immutable artifact snapshot and are not duplicated as mutable DB fields. Unknown profiles return `unknown_cost_profile`; market/profile mismatch returns `cost_profile_market_mismatch`. Re-running the same request with the same profile version must resolve byte-identical profile JSON.

Planned categories that must validate as recognized but non-executable:
- `flow`
- `fundamental`
- `macro`
- `news_sentiment`
- `disclosure`
- `paper_factor`
- `event`

Validation rules:
- Require exactly one universe symbol and require it to match the run request symbol and market. Reject empty or multi-symbol universes with `unsupported_universe_size`.
- Reject unknown categories, operators, indicators, condition types, markets, fields, malformed Decimal strings, and non-positive windows with stable validation codes.
- Reject recognized planned categories with `unsupported_factor_category`.
- Reject all category/indicator/param/source combinations outside the tables above. A JSON document must never validate successfully if the template would ignore any factor, condition, reference, risk field, or cost field.
- Reject `adjusted_close` with `unsupported_price_field`; reject factor sources other than raw fields with `unsupported_factor_source`.
- Reject nonzero `lag` with `unsupported_reference_lag` and any future-shifted reference with `look_ahead_reference`.
- Reject duplicate factor IDs and unknown condition factor references with stable codes.
- Require deterministic JSON serialization for stored strategy snapshots.

Stable validation and execution codes:

| Invalid or unsupported shape | Code |
|---|---|
| unknown factor category | `unknown_factor_category` |
| recognized planned factor category | `unsupported_factor_category` |
| unknown indicator | `unknown_indicator` |
| invalid category/indicator pairing | `invalid_category_indicator_pair` |
| recognized `macd`, `bollinger`, or `atr` | `unsupported_indicator_for_execution` |
| wrong-type or non-positive `window` | `invalid_factor_window` |
| missing or extra indicator params | `invalid_factor_params` |
| unknown raw field | `unknown_price_field` |
| recognized but unavailable `adjusted_close` | `unsupported_price_field` |
| factor-to-factor or other non-raw factor source | `unsupported_factor_source` |
| nonzero `lag` | `unsupported_reference_lag` |
| nonzero/future `shift` | `look_ahead_reference` |
| duplicate factor ID | `duplicate_factor_id` |
| unknown factor reference | `unknown_factor_reference` |
| unknown logical operator | `unknown_logical_operator` |
| nested, empty, or otherwise non-flat rule | `unsupported_rule_shape` |
| unknown condition type | `unknown_condition_type` |
| recognized `equal` condition | `unsupported_condition_type` |
| literal cross, literal-to-literal comparison, or malformed operand | `invalid_condition_operand` |
| malformed or floating-point Decimal input | `decimal_string_required` |
| empty or multi-symbol universe | `unsupported_universe_size` |
| non-null stop-loss or take-profit | `unsupported_risk_control` |
| unknown cost profile | `unknown_cost_profile` |
| profile does not match market | `cost_profile_market_mismatch` |
| zero denominator encountered during `returns` or `roc` execution | `factor_division_by_zero` |

Stable non-fatal execution audit codes:

| Audited event | Code |
|---|---|
| full buy MOO is unaffordable at the actual open | `moo_buying_power_rejected` |
| submitted MOO has no later tradable bar and remains unfilled at run end | `moo_unfilled_at_end` |

These audit events remain in detailed LEAN order/event or log artifacts and are not new mutable DB fields. Neither event alone changes a run to `FAILED`.

Errors must include the code and the JSON path or execution factor ID that caused the failure. A recognized-but-unexecuted construct must never degrade into a warning or no-op.

## LEAN Template And Execution

Maintain a fixed template under `backend/quant-worker/src/backtest/lean_template` or equivalent implementation path.

Each run must create an isolated directory:

```text
runs/{runId}/
  project/
    main.py
    strategy_loader.py
    strategy.json
    run_config.json
    cost_profile.json
  data/
  results/
  logs/
```

The template reads `strategy.json`, `run_config.json`, and `cost_profile.json`; generated Python code must remain fixed, reviewed project code rather than AI-generated code. Loading JSON is not sufficient: the template must calculate the confirmed factors, evaluate warm-up, precedence, and rejected-entry re-arm behavior, calculate buy MOO quantity from the completed signal close and snapshotted execution policy, submit buy and sell `MarketOnOpenOrder` requests at signal-bar completion, apply the resolved profile only to actual fills, and emit the confirmed non-fatal audit codes.

Docker execution must:
- run `quantconnect/lean:latest`
- mount the run directory and exported data read-only where possible
- enforce timeout
- capture stdout/stderr logs
- produce deterministic artifact paths
- map non-zero exit codes to `FAILED`

## Data Export

Source tables:
- `market_daily_ohlcv`
- `market_weekly_ohlcv`

MVP:
- daily export for KR and US
- market/symbol/date range filtering
- ascending `trade_date`
- duplicate-date rejection or deterministic last-write policy
- missing OHLCV field rejection

The exporter must create LEAN-compatible local data artifacts and record the exported path in run artifacts. Exact LEAN local data file naming and symbol mapping should be implemented and tested in Step 3 after inspecting the LEAN Docker/CLI expectations.

Market policy:
- KR: `currency=KRW`, cost profile `KR_DEFAULT_V1`, Korean symbol format such as `005930`.
- US: `currency=USD`, cost profile `US_DEFAULT_V1`, ticker format such as `AAPL`.

## API

Add routes to the existing FastAPI app or a router included by it:

- `POST /backtest-runs`
  - requires `costProfile` and validates it against `market`
  - validates request
  - creates a run with status `PENDING`
  - stores metadata, selected cost profile ID, and strategy JSON
  - starts or queues background execution
  - returns `runId`, status, and the selected `costProfile`

- `GET /backtest-runs/{runId}`
  - returns metadata including the persisted `costProfile`, status, summary if available, and artifact references

- `GET /backtest-runs/{runId}/result`
  - returns normalized summary plus detailed result loaded from artifacts where available

- `GET /backtest-runs/{runId}/logs`
  - returns captured logs or stable not-ready/not-found errors

Long-running LEAN execution must not block normal API responsiveness beyond request creation.

## DB Persistence

Create a Python service migration for a `backtest_runs` table using `CREATE TABLE IF NOT EXISTS`, following existing `src/migrations` conventions. Because `V3__create_backtest_runs.sql` is uncommitted and unreleased, update that migration in place to add a required `cost_profile` column rather than creating a follow-up migration.

Only run metadata, the selected cost profile ID, and summary metrics are stored. Resolved profile rates are not DB columns. Detailed artifacts, including the deterministic resolved cost-profile snapshot, stay under `runs/{runId}` or a configured artifact root. Repository and service round-trips must preserve the exact selected profile ID so queued or recovered execution never silently substitutes the current market default.

## Validation And Tests

Required targeted coverage:
- DSL validation accepts every confirmed price/scalar-technical shape and rejects every unsupported category, indicator, pairing, parameter, source, reference, condition, risk control, universe size, and cost profile with its stable error code.
- Hand-calculated golden vectors cover `price`, `returns`, `sma`, `ema`, `wma`, Wilder `rsi`, `roc`, and `momentum`, including the exact first-ready bar and zero-denominator failure.
- Rule truth tables cover `cross_above`, `cross_below`, `greater_than`, `less_than`, flat `and`/`or`, literals, and all boundary equalities.
- Deterministic execution traces prove no pre-warm-up signals, no same-bar fills/look-ahead, MOO submission after a completed signal bar, next-open fills, whole-share signal-close sizing, no pyramiding, and exit precedence for both flat and invested states.
- Golden sizing tests prove the exact multiplicative order `gap buffer -> adverse slippage -> commission cash cap`, show that changing only the future next-open price never changes the already-submitted quantity, and prove full rejection without financial-state mutation when actual required cash exceeds available cash.
- Rejection-state tests prove `moo_buying_power_rejected` is auditable, blocks repeated entry while the same ready entry condition remains true, and re-arms only after a ready false-to-true transition. Terminal-order tests prove a last-bar MOO has no fill and emits `moo_unfilled_at_end`.
- Cost tests prove adverse buy/sell slippage, commission on both sides, KR sell tax, zero US sell tax, cash/equity impact, market/profile rejection, and byte-identical cost-profile snapshots.
- Workspace tests prove `run_config.json` contains the byte-deterministic `MOO_CLOSE_BUFFER_V1` execution-policy snapshot with `"gap_buffer_bps":"500"`, while existing `KR_DEFAULT_V1` and `US_DEFAULT_V1` cost-profile snapshots remain byte-identical.
- Migration, repository, and service tests prove `cost_profile` is required and survives DB persistence/reload exactly; schema tests prove resolved bps values are not stored as mutable DB columns.
- Create and status API tests prove `costProfile` is required, serialized in both responses, and preserved after a DB-backed round-trip.
- KR and US request validation sets correct market/currency defaults.
- DB query/export reads from `market_daily_ohlcv` for daily runs and keeps weekly extension seams.
- exporter writes deterministic LEAN data artifacts from synthetic rows.
- run directory creation copies the fixed template and injects config/strategy without modifying the source template.
- Fixed-template factor, rule, signal, MOO submission, signal-close sizing, buying-power rejection, false-to-true re-arm, full-quantity sell, terminal unfilled-order audit, and cost behavior are tested without requiring Docker. A scheduled after-open market order, next-open-dependent order sizing, or JSON loading without execution must fail the acceptance suite.
- Docker runner follows the official LEAN image launcher/config contract, builds the expected command, and handles success, timeout, and non-zero exit code.
- API tests cover create, status, result, logs, invalid DSL, missing run, and failed run.
- summary parser maps LEAN output into required metrics without float money persistence.

## Follow-Up Work
Follow-up TODO already exists for `backtest-multifactor-dsl-expansion` to implement non-price/non-technical factors with point-in-time data contracts and LEAN custom data loaders.
