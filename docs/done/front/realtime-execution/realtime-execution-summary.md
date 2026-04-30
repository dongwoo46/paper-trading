# Realtime Execution Notification

## Core Feature

Real-time execution (fill) toast notifications on the trading-web operations dashboard.
When a LOCAL-mode order is matched and filled, the backend emits an SSE event and the
frontend renders a color-coded toast (green = BUY, red = SELL) that auto-dismisses after 4.5 s.

## Considerations

- Zero new npm packages: EventSource is native browser API; zustand was already a project dependency.
- BigDecimal monetary fields serialized via `toPlainString()` to avoid scientific notation in JSON.
- All SSE emitters stored in a ConcurrentHashMap — stale emitters pruned on send failure to prevent memory leak.
- `@TransactionalEventListener(AFTER_COMMIT)` ensures the event is only broadcast after the fill transaction
  successfully commits — no phantom notifications on rollback.
- `@Async` on the handler prevents SSE broadcast from blocking the fill transaction thread.

## Trade-offs

SSE vs WebSocket — chose SSE because:
- Pure server-push (no client→server messaging needed for notifications).
- `EventSource` auto-reconnects with exponential back-off for free.
- No extra Spring dependency (WebSocket requires additional stomp/sockjs setup).
- Simpler CORS/proxy configuration in production (standard HTTP).

## Implementation Approach

**Backend**: added to `trading-api`
1. `ExecutionFilledEvent` — domain event published by `ExecutionProcessor.fill()` after the fill completes.
2. `ExecutionSseRegistry` — thread-safe `ConcurrentHashMap<accountId, List<SseEmitter>>` manager;
   prunes completed/failed emitters on broadcast.
3. `ExecutionSseEventHandler` — `@TransactionalEventListener(AFTER_COMMIT) + @Async`;
   converts domain event → `ExecutionSseEvent` DTO → broadcasts via registry.
4. `ExecutionSseController` — `GET /api/v1/executions/stream` (`text/event-stream`);
   registers a new emitter per connection.
5. `ExecutionSseEvent` — DTO with `BigDecimal.toPlainString()` and `Instant.toString()` (ISO-8601).
6. `TradingApiApplication` — annotated with `@EnableAsync`.

**Frontend**: feature-sliced design (FSD)
1. `entities/execution/model/types.ts` — `ExecutionEvent` interface.
2. `shared/lib/sse/useExecutionSse.ts` — React hook that opens `EventSource`, parses JSON, and calls a callback.
   Closes the connection on unmount.
3. `shared/ui/Toast/types.ts` — extracted `Toast` interface (FSD: shared must not import from features).
4. `shared/ui/Toast/ToastItem.tsx` — pure presentational; auto-dismiss via `useEffect` + `setTimeout`.
5. `shared/ui/Toast/ToastContainer.tsx` — pure props-based list renderer.
6. `shared/ui/Toast/toast.css` — slide-in keyframe animation; BUY=green / SELL=red chip.
7. `features/execution-toast/model/useToastStore.ts` — zustand store; max 5 toasts, newest-first insertion.
8. `features/execution-toast/ui/ExecutionToastProvider.tsx` — bridges SSE hook → zustand store.
9. `App.tsx` — mounts `ExecutionToastProvider` and `ToastContainer`.

## Workflow

```
fill() → publishEvent(ExecutionFilledEvent)
       → @TransactionalEventListener(AFTER_COMMIT)
       → @Async ExecutionSseEventHandler.handle()
       → ExecutionSseRegistry.broadcast()
       → SseEmitter.send("execution", ExecutionSseEvent JSON)
       → browser EventSource("execution") fires
       → useExecutionSse callback
       → useToastStore.addToast()
       → ToastItem renders (slide-in, buy=green / sell=red)
       → auto-dismiss after 4 500 ms
```

## Key APIs

```
GET /api/v1/executions/stream
  Content-Type: text/event-stream
  event: execution
  data: {
    "executionId": "...",
    "orderId": "...",
    "accountId": "...",
    "ticker": "005930",
    "tickerName": null,
    "side": "BUY",
    "filledQty": "10",
    "filledPrice": "70000",
    "filledAt": "2026-04-30T10:00:00Z"
  }
```

## New Files

**Backend (trading-api)**
- `src/main/kotlin/.../domain/event/ExecutionFilledEvent.kt`
- `src/main/kotlin/.../infrastructure/sse/ExecutionSseRegistry.kt`
- `src/main/kotlin/.../infrastructure/sse/ExecutionSseEventHandler.kt`
- `src/main/kotlin/.../presentation/controller/ExecutionSseController.kt`
- `src/main/kotlin/.../presentation/dto/sse/ExecutionSseEvent.kt`
- `src/test/kotlin/.../infrastructure/sse/ExecutionSseRegistryTest.kt`
- `src/test/kotlin/.../infrastructure/sse/ExecutionSseEventHandlerTest.kt`
- `src/test/kotlin/.../presentation/dto/sse/ExecutionSseEventTest.kt`

**Modified (trading-api)**
- `src/main/kotlin/.../application/order/ExecutionProcessor.kt`
- `src/main/kotlin/.../TradingApiApplication.kt`
- `src/test/kotlin/.../application/order/ExecutionProcessorTest.kt`

**Frontend (trading-web)**
- `src/entities/execution/model/types.ts`
- `src/shared/lib/sse/useExecutionSse.ts`
- `src/shared/ui/Toast/types.ts`
- `src/shared/ui/Toast/ToastItem.tsx`
- `src/shared/ui/Toast/ToastContainer.tsx`
- `src/shared/ui/Toast/toast.css`
- `src/features/execution-toast/model/useToastStore.ts`
- `src/features/execution-toast/ui/ExecutionToastProvider.tsx`

**Modified (trading-web)**
- `src/App.tsx`

## Test Coverage

| Scope | Tests | Result |
|-------|-------|--------|
| ExecutionSseRegistryTest | 7 unit tests | PASS |
| ExecutionSseEventHandlerTest | 3 unit tests | PASS |
| ExecutionSseEventTest | 3 unit tests | PASS |
| ExecutionProcessorTest | 8 unit tests (incl. publishEvent assertion) | PASS |
| useToastStore.test.ts | 4 vitest tests | PASS |
| ToastItem.test.tsx | 4 tests | PASS |
| ToastContainer.test.tsx | 2 tests | PASS |
| **Total** | **21 backend + 65 frontend** | **ALL PASS** |

Note: 6 pre-existing Docker/Testcontainers integration tests fail when Docker is not running (infrastructure issue, not code regression).

## Deferred

- **CORS**: `@CrossOrigin(origins = ["*"])` on `ExecutionSseController` must be restricted to the dashboard origin in production.
- **tickerName**: Always `null` — no KRX company name map implemented yet.

## Completed / PR

2026-04-30 / #TBD
