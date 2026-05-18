# Position Exit Trigger Model Refactor

## Core Feature

Refactor `PositionExitTrigger` so each row represents exactly one automatic position-exit condition and can be evaluated, grouped, ordered, and audited independently.

## Considerations

- Preserve only useful WIP pieces from commit `66fb6ce`: pessimistic lock intent, quote listener/orchestrator shape, `skipReason`, idempotency handling, and tests where they still apply.
- Replace the combined stop-loss/take-profit row model completely. Do not build a compatibility layer for the current trigger API or DTO shape.
- Use development DB policy: discard existing combined trigger rows and create the new schema. No data preservation migration is required.
- Keep financial calculations in `BigDecimal`; do not introduce floating-point monetary or quantity calculations.
- `AccountExitTriggerDefault` is out of scope. Keep it compiling only where needed, but do not refactor it into the new trigger model.
- Automatic exit quantity must account for existing pending sell orders by subtracting pending sell quantity from the current position quantity before deciding order quantity.

## Trade-offs

- Combined row vs single-condition row: chose single-condition row because each trigger needs its own state, ratio, price policy, version, skip reason, and idempotency key.
- Backward compatible API vs new API only: chose new API only because the old DTO shape encodes the combined model and would keep the core ambiguity alive.
- Data preservation migration vs development reset: chose development reset because this phase intentionally replaces a not-yet-stable schema and existing combined rows should not be preserved.
- One order per fired trigger vs grouped order: chose grouping by `positionId + triggerType`; simultaneously fired triggers in the same group create one automatic sell order with summed `exitRatioPercent`.

## Implementation Approach

Domain:
- `PositionExitTrigger` becomes one JPA entity row per exit condition.
- Add/keep enums for `TriggerType`, `TriggerState`, `TriggerSkipReason`, and add `PriceBasisPolicy` with `FIXED_PRICE`, `AVG_PRICE_AT_CREATION`, `FOLLOW_AVG_PRICE`.
- Add fields for `triggerType`, `triggerPercent`, `triggerPrice`, `priceBasisPolicy`, `exitRatioPercent`, `state`, `skipReason`, `triggeredAt`, `lastEvaluatedAt`, `lastEvaluatedPrice`, and optimistic `version`.

Application:
- Command service creates, updates, and cancels individual triggers only.
- Query service returns the position's trigger list from `position_exit_triggers`; account defaults are not merged into the effective result in this phase.
- Evaluator evaluates only ARMED single-condition triggers and returns trigger decisions. Type-specific behavior is limited to comparison direction.
- Orchestrator processes quote ticks, locks position and trigger rows, groups fired decisions by `positionId + triggerType`, computes capped automatic sell quantity, creates one grouped order, and updates trigger states.

Infrastructure:
- Flyway migration resets `position_exit_triggers` to the new development schema.
- Repositories expose list/lock queries for ARMED triggers by ticker and per-position/per-trigger updates.
- Order creation supports `orderGroupId` for grouped automatic exits and uses individual trigger id/version inputs for idempotency and auditability.

Presentation:
- Replace the old singular `/exit-trigger` DTO/API shape with plural single-trigger endpoints and DTOs.
- Expose `exitRatioPercent` explicitly; default is `100.0000` when omitted.

## Workflow

1. A user or strategy creates one or more ARMED triggers for a position.
2. If `triggerPrice` is supplied, `priceBasisPolicy` must be `FIXED_PRICE` and the direct price is stored.
3. If `triggerPercent` is supplied:
   - `AVG_PRICE_AT_CREATION` computes and stores `triggerPrice` from the position average buy price at creation/update time.
   - `FOLLOW_AVG_PRICE` stores the percent and policy; effective trigger price is recalculated from the latest average buy price during evaluation.
4. Quote listener receives a tick and loads candidate ARMED triggers for the ticker.
5. Evaluator compares quote price against each trigger's effective trigger price: `STOP_LOSS` fires on `quote <= triggerPrice`; `TAKE_PROFIT` fires on `quote >= triggerPrice`.
6. Orchestrator groups simultaneous fired triggers by `positionId + triggerType`.
7. For each group, sum `exitRatioPercent`, cap at `100.0000`, subtract pending sell quantity from current position quantity, and create one automatic market sell order if the resulting quantity is positive.
8. On success, each trigger in the group transitions `ARMED -> TRIGGERED`.
9. On order creation or processing failure, affected triggers transition to `FAILED`.
10. On lock conflict, no available quantity, or manual sell conflict, affected triggers transition to `SKIPPED` with `skipReason`.

## API

`POST /api/positions/{positionId}/exit-triggers` - create one exit trigger.

Request fields: `triggerType`, `triggerPercent`, `triggerPrice`, `priceBasisPolicy`, `exitRatioPercent`.
Response fields: `id`, `positionId`, `accountId`, `ticker`, `triggerType`, `triggerPercent`, `triggerPrice`, `priceBasisPolicy`, `exitRatioPercent`, `state`, `skipReason`, `version`, `createdAt`, `updatedAt`.

`GET /api/positions/{positionId}/exit-triggers` - list triggers for a position.

Response fields: `positionId`, `triggers`.

`PATCH /api/positions/{positionId}/exit-triggers/{triggerId}` - update one ARMED trigger.

Request fields: `triggerPercent`, `triggerPrice`, `priceBasisPolicy`, `exitRatioPercent`, `expectedVersion`.
Response: one trigger response.

`DELETE /api/positions/{positionId}/exit-triggers/{triggerId}` - cancel one ARMED trigger.

Request: optional `expectedVersion` query parameter or request body field, following local controller convention chosen during implementation.
Response: canceled trigger response or no-content response if that matches the existing local API style.

Errors:
- `400 Bad Request`: invalid percent/price/ratio range, missing both `triggerPercent` and `triggerPrice`, invalid `priceBasisPolicy` combination.
- `404 Not Found`: position or trigger does not exist, or trigger does not belong to the position.
- `409 Conflict`: stale trigger version, closed position, non-ARMED trigger update/cancel, insufficient orderable quantity during processing.

## DB

`position_exit_triggers`

Key columns:
- `id BIGSERIAL PRIMARY KEY`
- `position_id BIGINT NOT NULL`
- `account_id BIGINT NOT NULL`
- `ticker VARCHAR(32) NOT NULL`
- `trigger_type VARCHAR(16) NOT NULL`
- `trigger_percent NUMERIC(8,4)`
- `trigger_price NUMERIC(20,4)`
- `price_basis_policy VARCHAR(32) NOT NULL`
- `exit_ratio_percent NUMERIC(8,4) NOT NULL DEFAULT 100.0000`
- `state VARCHAR(16) NOT NULL`
- `skip_reason VARCHAR(32)`
- `triggered_at TIMESTAMP`
- `last_evaluated_at TIMESTAMP`
- `last_evaluated_price NUMERIC(20,4)`
- `version BIGINT NOT NULL DEFAULT 0`
- `created_at TIMESTAMP NOT NULL DEFAULT now()`
- `updated_at TIMESTAMP NOT NULL DEFAULT now()`

Indexes:
- `idx_position_exit_triggers_position_state` on `(position_id, state)`.
- `idx_position_exit_triggers_ticker_state` on `(ticker, state)`.
- `idx_position_exit_triggers_group_scan` on `(ticker, position_id, trigger_type, state)`.

Development reset migration:
- Drop the existing `position_exit_triggers` table if it exists.
- Recreate it with the new single-condition schema.
- Remove the need for a separate skip-reason add-column migration if the reset migration supersedes it, or keep Flyway order coherent.

Order audit fields:
- Add `order_group_id VARCHAR(100)` to orders if not already present.
- Grouped automatic order uses one `orderGroupId` per `positionId + triggerType + quoteAt/tick processing group`.
- Individual trigger idempotency/audit input uses `triggerId + version`; grouped order idempotency must be deterministic for the set of triggers in the group.
