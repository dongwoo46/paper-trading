# KIS Account Execution Notice WebSocket

## Core Feature
Subscribe `trading-api` to KIS account execution notice WebSocket channels and process the user's own KIS order execution notices through the existing order, execution, position, settlement, and SSE flow.

## Considerations
- This feature is account execution notice ingestion, not market-wide trade print ingestion.
- Paper trading uses KIS channel `H0STCNI9`; live trading uses `H0STCNI0`.
- Existing polling through `KisPaperPollingScheduler` must be replaced by WebSocket notice ingestion.
- Existing `ExecutionProcessor.fill()` currently creates local-style external execution ids internally, so KIS idempotency requires an application-level entry point that accepts a normalized external execution id before persistence.
- `executions.external_execution_id` already has a unique constraint and `ExecutionRepository.findByExternalExecutionId()` already exists; prefer this as the durable dedupe boundary.
- Existing SSE flow is already event-driven: `ExecutionProcessor.fill()` publishes `ExecutionFilledEvent`, and `ExecutionSseEventHandler` broadcasts after commit through `/api/v1/executions/stream`.
- KIS tokens are cached in Redis under `kis:token:{mode}` and must not be logged.
- KIS WebSocket approval/auth must refresh when reconnecting after token expiration, auth rejection, or explicit KIS error messages.
- Monetary and quantity fields must remain `BigDecimal`; no `double` or `float`.

## Trade-offs
- WebSocket notice ingestion in `trading-api` vs routing through `collector-api` -> choose `trading-api` because account execution notices belong to the order/account bounded context and carry account-specific execution state, unlike public market quotes.
- Add an overloaded fill method vs a new application command service -> choose a small application service/command around `ExecutionProcessor` so KIS payload parsing and idempotency checks stay out of the domain model and infrastructure WebSocket client.
- Persist a new inbox table vs use `executions.external_execution_id` -> choose the existing unique external execution id for this phase. A separate inbox can be added later if raw message replay/audit is required.
- Keep REST polling as fallback vs remove scheduler -> remove `KisPaperPollingScheduler` from active flow. `KisOrderRestClient.inquireFills()` may remain only if still needed for manual diagnostics, but it must not be scheduled.

## Implementation Approach
- Infrastructure: add a KIS WebSocket client/manager under `infrastructure.kis` that connects to paper/live endpoints, obtains approval/auth data through `KisTokenManager` or a dedicated approval method, subscribes to `H0STCNI9`/`H0STCNI0`, parses frames, maps KIS error/control messages, and reconnects with re-authentication.
- ACL/parser: translate raw KIS messages into a normalized `KisExecutionNotice` data object containing mode, external order id, external execution id, ticker, side, cumulative/executed quantity, executed price, executed at, and account identifiers when provided.
- Application: add a KIS execution notice use case that resolves the local `Order` by `externalOrderId` and KIS trading mode, checks `ExecutionRepository.findByExternalExecutionId()`, and calls an idempotent execution-processing method with the external execution id.
- Domain/persistence: keep state changes in existing `Order.applyExecution()` and `ExecutionProcessor`; add only the minimum method signature needed to store KIS external execution ids.
- Presentation: keep the existing SSE endpoint unchanged. Successful KIS fills should emit the same `ExecutionFilledEvent` after commit.
- Configuration: extend `trading.kis` properties for WebSocket URL, WS enable flags, reconnect backoff, and channel ids. Do not hardcode secrets.

## Workflow
1. Application startup creates/enables KIS WebSocket sessions for configured modes with credentials present.
2. For each enabled mode, the client obtains the required token/approval data without logging secret values.
3. The WebSocket client connects and subscribes to `H0STCNI9` for paper or `H0STCNI0` for live.
4. Raw KIS execution notice frame enters the ACL parser.
5. Parser rejects malformed/unsupported market-wide messages and returns a normalized account execution notice.
6. Application use case resolves the local order by external KIS order id and mode.
7. Use case checks `externalExecutionId`; duplicates are ignored without publishing duplicate SSE events.
8. New fills call the execution processor in one transaction, persisting `Execution`, updating `Order`, `Position`, settlement/ledger state, and publishing `ExecutionFilledEvent`.
9. Existing `ExecutionSseEventHandler` broadcasts the execution SSE event after transaction commit.
10. On socket close, token/approval rejection, or KIS auth/control error, the client reconnects with bounded backoff and re-authenticates.

## API
No new public REST API is required for this phase.

Existing endpoint retained:

`GET /api/v1/executions/stream` - server-sent execution notifications.

Response event:
- event name: `execution`
- data: existing `ExecutionSseEvent` shape.

Errors:
- Connection setup failures remain SSE transport failures.
- KIS order rejection/errors are mapped to domain/application exceptions during REST order submit/cancel, not exposed as a new endpoint.

## DB
Existing table: `executions`
- `external_execution_id` remains required and unique through `uk_executions_external_id`.
- KIS execution notice idempotency key format should be deterministic, for example `{mode}:{externalOrderId}:{externalExecutionId}` or the KIS-provided execution number if globally unique per account/mode.

Existing table: `orders`
- `external_order_id` is used to correlate KIS execution notices to local orders.
- Repository query scope must include trading mode to avoid paper/live collisions.

No new migration is required if the implementation can store all KIS execution notice identifiers in `executions.external_execution_id`.

## DDD Model
- Bounded Context: `trading-api` order execution/account context.
- Aggregate Roots: `Order`, `Account`, `Position`.
- Entity: `Execution` remains the persisted fill record.
- Value Object/DTO: `KisExecutionNotice` is an ACL/application data object, not a domain entity.
- Domain Event: existing `ExecutionFilledEvent`.
- Domain invariant: fill state transitions remain explicit and idempotent; duplicate KIS notices must not mutate financial state twice.

## KIS Error Mapping
- REST order submit/cancel responses with non-zero `rt_cd` must map to explicit application/domain exceptions rather than generic `check` failures.
- Suggested exception codes: `KIS_ORDER_REJECTED`, `KIS_AUTH_FAILED`, `KIS_RATE_LIMITED`, `KIS_TEMPORARY_UNAVAILABLE`, `KIS_INVALID_ACCOUNT`, `KIS_DUPLICATE_OR_ALREADY_PROCESSED`.
- WebSocket control/error messages must update connection status and trigger reconnect/re-auth when auth-related.
- Logs may include mode, channel id, local order id, external order id, and KIS error code/message, but never app secret, access token, approval key, or full raw credential-bearing frames.

## Token and WebSocket Re-auth
- `KisTokenManager.getToken(mode)` remains the REST token source.
- Add explicit refresh/evict behavior when KIS reports auth expiration or token invalidation.
- WebSocket connection setup must fetch fresh auth/approval data after reconnect, not reuse stale in-memory values indefinitely.
- Reconnect backoff must be bounded and mode-specific; reconnect should not retry non-idempotent REST order submission.

## Test Strategy
- Unit tests for KIS execution notice parser with paper/live channel ids, malformed messages, unsupported channel ids, numeric parsing, and idempotency key derivation.
- Application tests for duplicate notices: duplicate `externalExecutionId` must not call fill or publish SSE twice.
- Application tests for KIS notice handling: resolve by `externalOrderId` + mode and call execution processing with `BigDecimal` values.
- REST client tests for KIS non-zero response mapping to typed exceptions.
- WebSocket manager tests with mocked session/client: subscribe payload per mode, reconnect with re-auth on auth error, no token values in logs where directly testable.
- Regression tests for existing SSE event conversion and `ExecutionProcessor` event publication.

## Existing Code References
- Worktree: `.worktrees/trading-api-kis-execution-ws/backend/trading-api`
- `src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt`
- `src/main/kotlin/com/papertrading/api/application/order/KisPaperOrderExecutor.kt`
- `src/main/kotlin/com/papertrading/api/application/order/KisLiveOrderExecutor.kt`
- `src/main/kotlin/com/papertrading/api/infrastructure/scheduler/KisPaperPollingScheduler.kt`
- `src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderRestClient.kt`
- `src/main/kotlin/com/papertrading/api/infrastructure/kis/KisTokenManager.kt`
- `src/main/kotlin/com/papertrading/api/infrastructure/kis/KisOrderProperties.kt`
- `src/main/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseEventHandler.kt`
- `src/main/kotlin/com/papertrading/api/infrastructure/persistence/ExecutionRepository.kt`
- `src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepositoryImpl.kt`
