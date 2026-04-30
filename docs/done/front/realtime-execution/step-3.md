# Step 3 — test-engineer: Testing and QA Verification

## Context
- Phase: front/realtime-execution
- Branch: feature/front-realtime-execution
- Worktree: C:\Users\dw\Desktop\paper-trading\.worktrees\front-realtime-execution
- Main repo root: C:\Users\dw\Desktop\paper-trading

## Files to Read First

1. `C:\Users\dw\Desktop\paper-trading\docs\phase\front\realtime-execution\spec.md`
2. `C:\Users\dw\Desktop\paper-trading\backend\trading-api\src\main\kotlin\com\papertrading\api\infrastructure\sse\ExecutionSseRegistry.kt`
3. `C:\Users\dw\Desktop\paper-trading\backend\trading-api\src\main\kotlin\com\papertrading\api\infrastructure\sse\ExecutionSseEventHandler.kt`
4. `C:\Users\dw\Desktop\paper-trading\backend\trading-api\src\main\kotlin\com\papertrading\api\application\order\ExecutionProcessor.kt`
5. `C:\Users\dw\Desktop\paper-trading\backend\trading-api\src\main\kotlin\com\papertrading\api\presentation\controller\ExecutionSseController.kt`
6. `C:\Users\dw\Desktop\paper-trading\backend\trading-api\src\main\kotlin\com\papertrading\api\presentation\dto\sse\ExecutionSseEvent.kt`
7. `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\src\shared\lib\sse\useExecutionSse.ts`
8. `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\src\features\execution-toast\model\useToastStore.ts`
9. `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\src\shared\ui\Toast\ToastItem.tsx`
10. `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\src\shared\ui\Toast\ToastContainer.tsx`
11. `C:\Users\dw\Desktop\paper-trading\backend\trading-api\src\test\kotlin\com\papertrading\api\application\order\ExecutionProcessorTest.kt`

---

## Substep 1: Backend Unit Tests

### Test 1A: ExecutionSseRegistry

**Create** `backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseRegistryTest.kt`

Test cases (use `@ExtendWith(MockKExtension::class)`, `mockk`):
1. `register then broadcast — emitter receives event`
   - Create real `ExecutionSseRegistry`
   - Create `mockk<SseEmitter>(relaxed = true)`
   - Register emitter
   - Call `broadcast(sampleSseEvent())`
   - Verify `emitter.send(...)` was called once
2. `broadcast after remove — emitter not called`
   - Register, then remove, then broadcast
   - Verify `emitter.send(...)` was NOT called
3. `broadcast with failed emitter — emitter removed from registry`
   - Register emitter that throws `IOException` on `send`
   - Broadcast → no exception propagated to caller
   - Registry should be empty after failed send
4. `register multiple emitters — all receive broadcast`
   - Register 3 emitters
   - Broadcast once
   - Verify all 3 emitters received the event

Helper: `private fun sampleSseEvent() = ExecutionSseEvent(1L, 2L, "005930", null, "BUY", "10", "75000", "150", "KRW", "2026-04-30T08:30:00Z")`

---

### Test 1B: ExecutionSseEvent.from()

**Create** `backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/dto/sse/ExecutionSseEventTest.kt`

Test cases (plain JUnit 5, no Spring context):
1. `from — BigDecimal fields are plain strings`
   - Input: `quantity = BigDecimal("10.00")`, `price = BigDecimal("75000.0000")`
   - Assert: `sseEvent.quantity == "10.00"`, `sseEvent.price == "75000.0000"` (no scientific notation)
2. `from — executedAt is ISO instant string`
   - Input: `executedAt = Instant.parse("2026-04-30T08:30:00Z")`
   - Assert: `sseEvent.executedAt == "2026-04-30T08:30:00Z"`
3. `from — side is string name of enum`
   - Input: `side = OrderSide.BUY`
   - Assert: `sseEvent.side == "BUY"`

---

### Test 1C: ExecutionProcessor publishes event (unit test)

**Modify** `backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt`

Add test:
- Mock `ApplicationEventPublisher` via mockk
- After `fill()` completes successfully, verify `eventPublisher.publishEvent(any<ExecutionFilledEvent>())` was called once
- Check: captured event has correct `ticker`, `side`, `executionId`

---

## Substep 2: Frontend Unit Tests

### Test 2A: useToastStore

**Create** `frontend/trading-web/src/features/execution-toast/model/useToastStore.test.ts`

Test cases (Vitest):
1. `addToast — toast appears in list`
   - Call `addToast(sampleEvent())`
   - Assert `toasts.length === 1`, `toasts[0].event.ticker === '005930'`
2. `removeToast — toast removed by id`
   - Add toast, capture id, call `removeToast(id)`
   - Assert `toasts.length === 0`
3. `addToast — capped at 5 toasts (oldest dropped)`
   - Add 6 toasts
   - Assert `toasts.length === 5`
   - Assert the 6th (oldest) event is no longer in the list
4. `addToast — newest toast is first in list`
   - Add toast A, then toast B
   - Assert `toasts[0].event` is toast B's event

Helper: `function sampleEvent(ticker = '005930'): ExecutionEvent`

Use `beforeEach` to reset store state via `useToastStore.setState({ toasts: [] })`.

---

### Test 2B: ToastItem rendering

**Create** `frontend/trading-web/src/shared/ui/Toast/ToastItem.test.tsx`

Test cases (`@testing-library/react`, Vitest):
1. `BUY toast shows 매수 체결 label`
   - Render `<ToastItem>` with `side: 'BUY'` event
   - Assert `screen.getByText(/매수 체결/)` is present
2. `SELL toast shows 매도 체결 label`
   - Assert `screen.getByText(/매도 체결/)` is present
3. `KRW price is formatted with Korean locale`
   - Event: `price: '75000', currency: 'KRW'`
   - Assert text contains `75,000원`
4. `auto-dismiss fires onDismiss after 4500ms`
   - Use `vi.useFakeTimers()`
   - Render with mock `onDismiss`
   - `vi.advanceTimersByTime(4500)`
   - Assert `onDismiss` was called with correct toast id

---

### Test 2C: ToastContainer renders list

**Create** `frontend/trading-web/src/shared/ui/Toast/ToastContainer.test.tsx`

Test cases:
1. `renders empty when no toasts`
   - Set store state to `{ toasts: [] }`
   - Assert container renders but no `toast-item` elements
2. `renders one item per toast in store`
   - Set store state with 2 toasts
   - Assert 2 toast items are rendered

---

## Substep 3: Build + Full Test Suite

### Backend
Run in order:
```
cd backend/trading-api && ./gradlew compileKotlin
cd backend/trading-api && ./gradlew test
```
All tests must pass. Zero compilation errors.

### Frontend
Run in order:
```
cd frontend/trading-web && npm run build
cd frontend/trading-web && npm run test -- --run
```
All tests must pass. Zero TypeScript errors in build.

---

## Edge Cases to Verify Manually (document in report)

1. **SSE reconnect**: Close browser tab and reopen → new EventSource connects, existing emitter removed cleanly.
2. **Rapid-fire executions**: If 6+ executions arrive in quick succession, only 5 toasts shown simultaneously.
3. **Currency non-KRW**: If `currency = 'USD'`, price displays as `$152.30` not `152.30원`.
4. **Null tickerName**: Falls back to showing ticker code.

---

## Agent Return Protocol

Return a completion report in this exact format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <tests written and results>
- Test results: <backend: X/Y tests pass> | <frontend: X/Y tests pass>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
