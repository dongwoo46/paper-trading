# Step 2: Implement Domain, Persistence, and API (TDD)
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-position-trigger-exit

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/index.json

## Objective
Implement core exit-trigger feature end-to-end with test-first workflow.

## Implementation Directives
1. Add Flyway migration files:
- backend/trading-api/src/main/resources/db/migration/V{next}__create_account_exit_trigger_defaults.sql
- backend/trading-api/src/main/resources/db/migration/V{next}__create_position_exit_triggers.sql
- backend/trading-api/src/main/resources/db/migration/V{next}__add_order_idempotency_scope.sql

2. Add domain models:
- backend/trading-api/src/main/kotlin/com/papertrading/api/domain/position/PositionExitTrigger.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/domain/position/TriggerState.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/domain/position/TriggerType.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/domain/account/AccountExitTriggerDefault.kt

3. Add repositories:
- backend/trading-api/src/main/kotlin/com/papertrading/api/domain/position/PositionExitTriggerRepository.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/domain/account/AccountExitTriggerDefaultRepository.kt

4. Add application services and signatures:
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandService.kt
  - `fun upsertPositionTrigger(command: UpsertPositionExitTriggerCommand): PositionExitTriggerResult`
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerQueryService.kt
  - `fun getEffectiveTrigger(positionId: Long): EffectivePositionExitTriggerResult`
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerEvaluator.kt
  - `fun evaluate(position: Position, trigger: PositionExitTrigger, quotePrice: BigDecimal, now: Instant): TriggerDecision?`

5. Add controller and DTOs:
- backend/trading-api/src/main/kotlin/com/papertrading/api/interfaces/rest/position/PositionExitTriggerController.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/interfaces/rest/position/dto/PositionExitTriggerDtos.kt

6. Integrate realtime trigger orchestration:
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/market/QuoteEventListener.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt
  - `fun onQuote(ticker: String, price: BigDecimal, quoteAt: Instant)`

7. Integrate auto exit order creation:
- backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt
  - add `createAutoExitSellOrder(...)` entrypoint using deterministic idempotency key.

## Test Scope (must be written first)
- backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerEvaluatorTest.kt
- backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandServiceTest.kt
- backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestratorTest.kt
- backend/trading-api/src/test/kotlin/com/papertrading/api/interfaces/rest/position/PositionExitTriggerControllerTest.kt

Required assertions:
- percent range and enabled-state validation.
- stop-loss/take-profit trigger decisions with BigDecimal precision.
- OCO transition atomically cancels opposite side.
- duplicate quote events do not create duplicate orders.
- idempotency key format `auto-exit:{positionId}:{triggerVersion}:{triggerType}`.

## Verification Commands
- `cd backend/trading-api && ./gradlew test --tests "*PositionExitTrigger*"`
- `cd backend/trading-api && ./gradlew compileKotlin`

## Deliverables
- Passing tests for new feature area.
- Migration + domain + API + orchestration code merged in worktree.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
