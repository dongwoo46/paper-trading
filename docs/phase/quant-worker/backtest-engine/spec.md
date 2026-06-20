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
- DSL scope: multi-factor DSL skeleton. MVP executable factors are `price` and `technical`; `flow`, `fundamental`, `macro`, `news_sentiment`, `disclosure`, `paper_factor`, and `event` are allowed as planned categories but rejected for execution with a stable validation error until follow-up phases implement point-in-time data and LEAN custom-data adapters.

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

Planned categories that must validate as recognized but non-executable:
- `flow`
- `fundamental`
- `macro`
- `news_sentiment`
- `disclosure`
- `paper_factor`
- `event`

Validation rules:
- Reject unknown categories, operators, indicators, condition types, markets, fields, negative windows, empty symbol lists, and unsupported executable planned factors.
- Reject look-ahead-prone constructs such as future-shifted data references.
- Require deterministic JSON serialization for stored strategy snapshots.

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
  data/
  results/
  logs/
```

The template reads `strategy.json` and `run_config.json`; generated Python code should be fixed, reviewed project code rather than AI-generated code.

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
- KR: `currency=KRW`, Korean stock defaults for fees/tax/slippage, Korean symbol format such as `005930`.
- US: `currency=USD`, US stock defaults for fees/slippage, ticker format such as `AAPL`.

## API

Add routes to the existing FastAPI app or a router included by it:

- `POST /backtest-runs`
  - validates request
  - creates a run with status `PENDING`
  - stores metadata/strategy JSON
  - starts or queues background execution
  - returns `runId` and status

- `GET /backtest-runs/{runId}`
  - returns metadata, status, summary if available, and artifact references

- `GET /backtest-runs/{runId}/result`
  - returns normalized summary plus detailed result loaded from artifacts where available

- `GET /backtest-runs/{runId}/logs`
  - returns captured logs or stable not-ready/not-found errors

Long-running LEAN execution must not block normal API responsiveness beyond request creation.

## DB Persistence

Create a Python service migration for a `backtest_runs` table using `CREATE TABLE IF NOT EXISTS`, following existing `src/migrations` conventions.

Only run metadata and summary metrics are stored. Detailed artifacts stay under `runs/{runId}` or a configured artifact root.

## Validation And Tests

Required targeted coverage:
- DSL validation accepts price/technical factors and rejects unsupported planned categories with stable errors.
- KR and US request validation sets correct market/currency defaults.
- DB query/export reads from `market_daily_ohlcv` for daily runs and keeps weekly extension seams.
- exporter writes deterministic LEAN data artifacts from synthetic rows.
- run directory creation copies the fixed template and injects config/strategy without modifying the source template.
- Docker runner builds the expected command and handles success, timeout, and non-zero exit code.
- API tests cover create, status, result, logs, invalid DSL, missing run, and failed run.
- summary parser maps LEAN output into required metrics without float money persistence.

## Follow-Up Work
Follow-up TODO already exists for `backtest-multifactor-dsl-expansion` to implement non-price/non-technical factors with point-in-time data contracts and LEAN custom data loaders.
