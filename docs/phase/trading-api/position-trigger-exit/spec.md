# Position Trigger Exit Spec

## 1. Goal and Scope
Implement percent-triggered auto-exit for open positions with stop-loss and take-profit, evaluated from realtime quotes.

In scope:
- Account-level default trigger settings.
- Position-level override trigger settings.
- Quote-driven trigger evaluation.
- Auto sell order creation when trigger is hit.
- OCO guard (one-cancels-the-other) for stop-loss vs take-profit.
- Idempotency and duplicate prevention.
- Failure retry and operator notification events.

Out of scope:
- New strategy auto-buy logic.
- Non-position exits (short selling, options).

## 2. Functional Requirements
- Trigger basis is entry average price of a position.
- Stop-loss percent and take-profit percent are positive decimal percentages.
- Effective trigger setting resolution order:
  1) position override
  2) account default
  3) disabled (no trigger)
- Trigger check runs on each quote event for matching ticker.
- If stop-loss condition is met, create market sell order for full remaining quantity.
- If take-profit condition is met, create market sell order for full remaining quantity.
- OCO guard:
  - After first trigger transitions to `TRIGGERED`, opposite trigger must become `CANCELED`.
  - Additional quote events must not produce extra exit orders for same position lifecycle.
- Idempotency key is required for auto-created exit orders and must be deterministic per position + trigger type + trigger version.
- On order creation failure, persist failure state and enqueue retry with bounded backoff.
- On permanent failure, publish notification event (Slack/event bus).

## 3. DDD Design

### 3.1 Aggregate Boundaries
- `Position` aggregate (existing): owns quantity, average entry price, accountId, ticker.
- New `PositionExitTrigger` aggregate: owns trigger configuration and runtime state.
- `Order` aggregate (existing): owns order creation and lifecycle; receives auto-exit command with idempotency key.

### 3.2 New Aggregate: PositionExitTrigger
Identity and scope:
- `positionId` (1:1 with position lifecycle)

Core fields:
- `accountId: Long`
- `ticker: String`
- `triggerVersion: Long` (increments on config changes)
- `enabled: Boolean`
- `stopLossPercent: BigDecimal?`
- `takeProfitPercent: BigDecimal?`
- `stopLossState: TriggerState` (`ARMED|TRIGGERED|CANCELED|FAILED`)
- `takeProfitState: TriggerState` (`ARMED|TRIGGERED|CANCELED|FAILED`)
- `triggeredAt: Instant?`
- `triggeredBy: TriggerType?` (`STOP_LOSS|TAKE_PROFIT`)
- `lastEvaluatedAt: Instant?`
- `lastEvaluatedPrice: BigDecimal?`

Invariant rules:
- Percent values must be `> 0` and `< 100`.
- At least one of stop-loss/take-profit must exist if enabled.
- Once either side is `TRIGGERED`, aggregate cannot re-enter `ARMED` without explicit reset/update.
- OCO transition is atomic within aggregate transaction.

### 3.3 Value Objects
- `TriggerThresholds(stopLossPercent, takeProfitPercent)`
- `TriggerDecision(type, thresholdPrice, quotedPrice, decidedAt)`
- `ExitOrderIdempotencyKey(value: String)`

### 3.4 Domain Services
- `PositionExitTriggerEvaluator`
  - Input: position snapshot + effective trigger + current quote.
  - Output: optional `TriggerDecision`.
- `EffectiveTriggerPolicyResolver`
  - Resolves account default + position override.

## 4. API Contract
Base path: `/api/positions/triggers`

### 4.1 Account Default Upsert
- `PUT /api/accounts/{accountId}/exit-trigger-default`
- Request:
```json
{
  "enabled": true,
  "stopLossPercent": "3.5",
  "takeProfitPercent": "7.0"
}
```
- Response `200`:
```json
{
  "accountId": 101,
  "enabled": true,
  "stopLossPercent": "3.5",
  "takeProfitPercent": "7.0",
  "updatedAt": "2026-05-08T10:15:30Z"
}
```

Validation:
- `enabled=false` allows both percent fields null.
- `enabled=true` requires at least one non-null percent.
- Percent type must be decimal string, scale <= 4.

### 4.2 Position Override Upsert
- `PUT /api/positions/{positionId}/exit-trigger`
- Headers:
  - `Idempotency-Key` optional for safe retries by client.
- Request:
```json
{
  "enabled": true,
  "stopLossPercent": "2.0",
  "takeProfitPercent": "6.0"
}
```
- Response `200`:
```json
{
  "positionId": 9001,
  "enabled": true,
  "stopLossPercent": "2.0",
  "takeProfitPercent": "6.0",
  "triggerVersion": 3,
  "updatedAt": "2026-05-08T10:15:30Z"
}
```

### 4.3 Query Effective Trigger
- `GET /api/positions/{positionId}/exit-trigger`
- Response includes source:
```json
{
  "positionId": 9001,
  "source": "POSITION_OVERRIDE",
  "enabled": true,
  "stopLossPercent": "2.0",
  "takeProfitPercent": "6.0",
  "triggerVersion": 3
}
```

### 4.4 Error Contract
- `400` invalid percent/scale/range.
- `404` account or position not found.
- `409` stale version update (if optimistic lock/version mismatch).
- `422` position not eligible (already closed).

## 5. Realtime Execution Flow
1. `QuoteEventListener` receives quote for ticker.
2. `PositionExitTriggerOrchestrator` loads open positions by ticker.
3. For each position:
   - Resolve effective trigger.
   - Skip if disabled/no thresholds/already triggered.
   - Evaluate stop-loss and take-profit against current quote.
4. If triggered:
   - Transition aggregate side to `TRIGGERED`, opposite side `CANCELED` (OCO).
   - Build deterministic idempotency key:
     - `auto-exit:{positionId}:{triggerVersion}:{triggerType}`
   - Call `OrderCommandService.createAutoExitSellOrder(...)`.
5. On success:
   - Persist `triggeredAt`, emitted `PositionExitTriggeredEvent`.
6. On failure:
   - Persist `FAILED` with reason.
   - Enqueue retry job with backoff policy (e.g., 5s, 15s, 60s, max 5).
   - Publish `PositionExitTriggerFailedEvent` for notification.

Duplicate prevention layers:
- Aggregate state gate (`TRIGGERED` check).
- Unique DB constraint on idempotency key at order table.
- Retry worker reuses same idempotency key.

## 6. Persistence and Index Plan

### 6.1 New Table: account_exit_trigger_defaults
- `id` BIGSERIAL PK
- `account_id` BIGINT NOT NULL UNIQUE
- `enabled` BOOLEAN NOT NULL
- `stop_loss_percent` NUMERIC(8,4) NULL
- `take_profit_percent` NUMERIC(8,4) NULL
- `version` BIGINT NOT NULL
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL

Indexes:
- `uk_account_exit_trigger_defaults_account_id(account_id)`

### 6.2 New Table: position_exit_triggers
- `id` BIGSERIAL PK
- `position_id` BIGINT NOT NULL UNIQUE
- `account_id` BIGINT NOT NULL
- `ticker` VARCHAR(32) NOT NULL
- `enabled` BOOLEAN NOT NULL
- `stop_loss_percent` NUMERIC(8,4) NULL
- `take_profit_percent` NUMERIC(8,4) NULL
- `stop_loss_state` VARCHAR(16) NOT NULL
- `take_profit_state` VARCHAR(16) NOT NULL
- `triggered_by` VARCHAR(16) NULL
- `triggered_at` TIMESTAMP NULL
- `last_evaluated_at` TIMESTAMP NULL
- `last_evaluated_price` NUMERIC(18,4) NULL
- `trigger_version` BIGINT NOT NULL
- `version` BIGINT NOT NULL
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL

Indexes:
- `uk_position_exit_triggers_position_id(position_id)`
- `idx_position_exit_triggers_ticker_state(ticker, enabled, stop_loss_state, take_profit_state)`
- `idx_position_exit_triggers_account_id(account_id)`

### 6.3 Existing Table Update: orders
- Ensure unique constraint exists for idempotency:
  - `uk_orders_account_scope_idempotency(account_id, idempotency_key)`

## 7. Observability and Notifications
- Metrics:
  - `position_exit_trigger_evaluated_total`
  - `position_exit_trigger_fired_total{type=stop_loss|take_profit}`
  - `position_exit_trigger_order_fail_total`
- Structured logs with `positionId`, `ticker`, `triggerType`, `idempotencyKey`.
- Notification event payload includes retry count and last error.

## 8. Non-Functional Constraints
- Monetary and percent calculations must use `BigDecimal` only.
- Trigger evaluation must remain idempotent under duplicate quote events.
- No financial state mutation without explicit aggregate transition and audit fields.
- API and domain events must be backward compatible for existing order flow.

## 9. Open Decisions
- Market sell order type mapping for each account mode (LOCAL/KIS_PAPER/KIS_LIVE) should reuse existing order side/type enums.
- Retry executor placement: Spring scheduler vs message queue worker (current recommendation: scheduler-backed retry table for MVP).
