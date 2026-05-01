# Step 2: Implementation (TDD)
Assigned agent: fullstack-dev

## Working Directory
`.worktrees/trading-api-kis-execution-ws/backend/trading-api`

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/CODEX.md`
- `C:/Users/dw/Desktop/paper-trading/docs/ADR.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/spec.md`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/build.gradle`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/resources/application.yaml`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/KisPaperPollingScheduler.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClient.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisTokenManager.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderProperties.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepository.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepository.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryCustom.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryImpl.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseEventHandler.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt`
- `.worktrees/trading-api-kis-execution-ws/backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseRegistryTest.kt`

## Tasks
Use TDD. Add failing tests before implementation for parser behavior, duplicate notice handling, KIS error mapping, and WebSocket re-auth/reconnect behavior.

Implementation directives:
- Add WebSocket support dependency if needed in `build.gradle`.
- Extend `KisOrderProperties` and `application.yaml` with WebSocket settings:
  - `paperWsUrl: String`
  - `liveWsUrl: String`
  - `executionWsEnabled: Boolean`
  - `paperExecutionChannel: String = "H0STCNI9"`
  - `liveExecutionChannel: String = "H0STCNI0"`
  - bounded reconnect/backoff settings.
- Add normalized ACL DTO/class under `src/main/kotlin/com/papertrading/api/infrastructure/kis/`:
  - `KisExecutionNotice`
  - fields: `mode`, `channelId`, `externalOrderId`, `externalExecutionId`, `ticker`, `side`, `executedQty`, `executedPrice`, `executedAt`, optional account identifiers.
- Add parser under `infrastructure.kis`:
  - `fun parse(rawMessage: String, mode: String): KisExecutionNotice?`
  - It must accept only account execution notice channels `H0STCNI9` and `H0STCNI0`.
  - It must parse all numeric values as `BigDecimal`.
  - It must derive a deterministic idempotency key/external execution id when KIS supplies separate order and execution numbers.
  - It must reject malformed or market-wide messages without mutating state.
- Add KIS exception/error mapping:
  - Replace generic `check(response?.rtCd == "0")` failures in `KisOrderRestClient` with typed exceptions.
  - Map non-zero KIS `rt_cd`/message values to explicit error codes listed in `spec.md`.
  - Keep credentials and tokens out of exception messages and logs.
- Add an application use case for KIS execution notices:
  - suggested class: `KisExecutionNoticeService`
  - suggested method: `fun handle(notice: KisExecutionNotice)`
  - Resolve local order by `externalOrderId` and trading mode.
  - Check `ExecutionRepository.findByExternalExecutionId(notice.externalExecutionId)`.
  - Ignore duplicates without publishing duplicate events.
  - Call an execution processor method that persists the given external execution id.
- Adjust `ExecutionProcessor` with the minimum new signature needed:
  - existing `fun fill(orderId: Long, fillPrice: BigDecimal, fillQty: BigDecimal)` must remain compatible.
  - add a signature that accepts `externalExecutionId` and uses it when creating `Execution`.
  - idempotency must be checked before financial mutation.
- Add repository query support:
  - find active KIS orders by external order id and mode.
  - include account fetch where needed to avoid lazy-loading problems.
- Add KIS WebSocket client/manager under `infrastructure.kis`:
  - connect per enabled mode.
  - subscribe paper `H0STCNI9`, live `H0STCNI0`.
  - pass parsed notices to `KisExecutionNoticeService`.
  - reconnect with bounded backoff.
  - fetch fresh auth/approval data on reconnect or auth/control error.
  - never retry REST order submit/cancel as part of reconnect.
- Remove active polling replacement:
  - Delete `KisPaperPollingScheduler` or disable/remove it as a Spring component.
  - `KisPaperOrderExecutor.pollFills()` must no longer be used by a scheduler. Keep only if tests or diagnostics still require it.
- Preserve existing SSE behavior:
  - KIS fills must publish the same `ExecutionFilledEvent`.
  - Existing `/api/v1/executions/stream` must not change.

Do not implement unrelated DDD aggregate refactors in this phase.

## Acceptance Criteria
- Failing tests are added before implementation and pass after implementation.
- Duplicate KIS notices do not create duplicate `Execution`, account ledger, position, settlement, or SSE events.
- Paper mode subscribes to `H0STCNI9`; live mode subscribes to `H0STCNI0`.
- `KisPaperPollingScheduler` is not active.
- KIS REST non-zero responses map to typed exceptions.
- WebSocket reconnect re-authenticates and does not log tokens/secrets.
- Existing order and SSE tests remain passing.
- Verification command:
```bash
cd backend/trading-api && ./gradlew test compileKotlin
```

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to repo root>
- Test result: <not run | passed N/N | failed N - list failing cases>
- Blockers: <none | description>
---
