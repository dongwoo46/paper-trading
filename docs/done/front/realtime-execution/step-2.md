# Step 2 — fullstack-dev: Implementation (TDD)

## Context
- Phase: front/realtime-execution
- Branch: feature/front-realtime-execution
- Worktree: C:\Users\dw\Desktop\paper-trading\.worktrees\front-realtime-execution
- Main repo root: C:\Users\dw\Desktop\paper-trading

## Files to Read First

1. `C:\Users\dw\Desktop\paper-trading\docs\phase\front\realtime-execution\spec.md`
2. `C:\Users\dw\Desktop\paper-trading\backend\trading-api\src\main\kotlin\com\papertrading\api\application\order\ExecutionProcessor.kt`
3. `C:\Users\dw\Desktop\paper-trading\backend\trading-api\src\main\kotlin\com\papertrading\api\domain\model\Execution.kt`
4. `C:\Users\dw\Desktop\paper-trading\backend\trading-api\src\main\kotlin\com\papertrading\api\presentation\controller\OrderController.kt`
5. `C:\Users\dw\Desktop\paper-trading\backend\trading-api\src\main\kotlin\com\papertrading\api\presentation\dto\order\OrderResponse.kt`
6. `C:\Users\dw\Desktop\paper-trading\backend\trading-api\build.gradle`
7. `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\src\App.tsx`
8. `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\src\main.tsx`
9. `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\src\shared\api\index.ts`
10. `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\src\entities\order\model\types.ts`
11. `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\src\features\order-list\ui\OrderTablePanel.tsx`
12. `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\package.json`

Also explore to understand CORS config existence:
- `find C:\Users\dw\Desktop\paper-trading\backend\trading-api\src -name "*Cors*" -o -name "*WebMvc*" | head -5`

---

## TDD Order: Write failing test → implement → make it pass → refactor

---

## Part A: Backend (trading-api)

### A-1. Domain Event

**Create** `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/event/ExecutionFilledEvent.kt`

Directives:
- Plain Kotlin `data class`, no Spring annotations
- Fields: `executionId: Long`, `orderId: Long`, `ticker: String`, `side: OrderSide`, `quantity: BigDecimal`, `price: BigDecimal`, `fee: BigDecimal`, `currency: String`, `executedAt: Instant`
- Import `com.papertrading.api.domain.enums.OrderSide`

---

### A-2. SSE DTO

**Create** `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/sse/ExecutionSseEvent.kt`

Directives:
- `data class ExecutionSseEvent(val executionId: Long, val orderId: Long, val ticker: String, val tickerName: String?, val side: String, val quantity: String, val price: String, val fee: String, val currency: String, val executedAt: String)`
- Companion: `fun from(e: ExecutionFilledEvent): ExecutionSseEvent` — all BigDecimal fields `.toPlainString()`, `executedAt` via `DateTimeFormatter.ISO_INSTANT`
- `side` is `e.side.name` (String)

---

### A-3. SSE Registry

**Create** `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseRegistry.kt`

Directives:
- `@Component` class
- Field: `private val emitters = ConcurrentHashMap<String, SseEmitter>()`
- `fun register(clientId: String, emitter: SseEmitter)`: put into map; register completion, timeout, error callbacks that call `remove(clientId)`
- `fun remove(clientId: String)`: remove from map
- `fun broadcast(event: ExecutionSseEvent)`: iterate snapshot of map values, for each emitter call `emitter.send(SseEmitter.event().name("execution").data(event, MediaType.APPLICATION_JSON))`; on send failure call `remove(clientId)` and complete emitter
- Logger: `KotlinLogging.logger {}`; log warn on send failure

---

### A-4. SSE Controller

**Create** `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/ExecutionSseController.kt`

Directives:
- `@RestController`, `@RequestMapping("/api/v1/executions")`
- Constructor inject `ExecutionSseRegistry`
- `@GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])`
- Method `streamExecutions(response: HttpServletResponse): SseEmitter`:
  - `response.setHeader("Cache-Control", "no-cache")`, `response.setHeader("X-Accel-Buffering", "no")`
  - Create `SseEmitter(300_000L)` (5 minutes)
  - `clientId = UUID.randomUUID().toString()`
  - Register in registry
  - Send initial keep-alive comment: `emitter.send(SseEmitter.event().comment("connected"))`
  - Return emitter
- CORS: add `@CrossOrigin(origins = ["*"])` on class (or configure via WebMvcConfigurer if global config file exists)

---

### A-5. SSE Event Handler

**Create** `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/sse/ExecutionSseEventHandler.kt`

Directives:
- `@Component` class, constructor inject `ExecutionSseRegistry`
- Method annotated `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` and `@Async`
  - Parameter: `ExecutionFilledEvent`
  - Convert to `ExecutionSseEvent.from(event)`
  - Call `registry.broadcast(sseEvent)`
  - Log info: `"SSE broadcast: executionId=${event.executionId}, ticker=${event.ticker}"`
- `@Async` requires `@EnableAsync` on main application class — add if not present

---

### A-6. ExecutionProcessor modification

**Modify** `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt`

Directives:
- Add constructor parameter: `private val eventPublisher: ApplicationEventPublisher`
- At the **end** of `fill()` method, after the `log.info { "filled: ..." }` line, add:
  ```
  eventPublisher.publishEvent(
      ExecutionFilledEvent(
          executionId = executionId,
          orderId = orderId,
          ticker = ticker,
          side = requireNotNull(order.orderSide),
          quantity = fillQty,
          price = fillPrice,
          fee = fee,
          currency = account.baseCurrency,
          executedAt = Instant.now(),
      )
  )
  ```
- Import `org.springframework.context.ApplicationEventPublisher`
- Import `com.papertrading.api.domain.event.ExecutionFilledEvent`

---

### A-7. Enable Async

**Check** `backend/trading-api/src/main/kotlin/com/papertrading/api/TradingApiApplication.kt`
- If `@EnableAsync` is missing, add it to the application class.

---

### A-8. Backend Build Verification

Run: `cd backend/trading-api && ./gradlew compileKotlin`
Must pass with zero errors.

---

## Part B: Frontend (trading-web)

Work inside worktree: `C:\Users\dw\Desktop\paper-trading\.worktrees\front-realtime-execution\frontend\trading-web\`

**No new npm packages**. Use existing: `zustand`, `lucide-react`, vanilla `EventSource` API.

---

### B-1. Execution Entity Type

**Create** `frontend/trading-web/src/entities/execution/model/types.ts`

Directives:
```typescript
export type ExecutionSide = 'BUY' | 'SELL';

export interface ExecutionEvent {
  executionId: number;
  orderId: number;
  ticker: string;
  tickerName: string | null;
  side: ExecutionSide;
  quantity: string;   // BigDecimal string
  price: string;      // BigDecimal string
  fee: string;        // BigDecimal string
  currency: string;   // 'KRW' | 'USD' etc.
  executedAt: string; // ISO 8601
}
```

---

### B-2. SSE Custom Hook

**Create** `frontend/trading-web/src/shared/lib/sse/useExecutionSse.ts`

Directives:
- Function signature: `useExecutionSse(url: string, onEvent: (event: ExecutionEvent) => void): { connected: boolean }`
- Uses `useEffect` only; returns `{ connected }` ref state
- Creates `new EventSource(url)` inside effect
- `source.onopen`: set `connected = true`
- `source.addEventListener('execution', (e: MessageEvent) => { ... })`:
  - Parse `JSON.parse(e.data) as ExecutionEvent`
  - Call `onEvent(parsed)`
  - Wrap in try/catch; on error log to console.warn
- `source.onerror`: set `connected = false` (EventSource auto-reconnects; no custom backoff needed)
- Cleanup: `return () => source.close()`
- Import `ExecutionEvent` from `../../../entities/execution/model/types`
- `url` derived from `import.meta.env.VITE_API_BASE_URL` — hook consumer passes full URL

---

### B-3. Toast Store

**Create** `frontend/trading-web/src/features/execution-toast/model/useToastStore.ts`

Directives:
- Zustand store with `create<ToastStore>()`
- `interface Toast { id: string; event: ExecutionEvent }`
- `interface ToastStore { toasts: Toast[]; addToast: (event: ExecutionEvent) => void; removeToast: (id: string) => void }`
- `addToast`: prepend new `{ id: crypto.randomUUID(), event }` to `toasts`. If `toasts.length >= 5`, drop the last (oldest).
- `removeToast`: filter by id
- No persistence (no `persist` middleware)

---

### B-4. Toast UI Components

**Create** `frontend/trading-web/src/shared/ui/Toast/ToastItem.tsx`

Directives:
- Props: `{ toast: Toast; onDismiss: (id: string) => void }`
- Import `TrendingUp`, `TrendingDown`, `X` from `lucide-react`
- Display logic:
  - `side === 'BUY'` → icon `TrendingUp`, label `매수 체결`
  - `side === 'SELL'` → icon `TrendingDown`, label `매도 체결`
  - Price format: `currency === 'KRW'` → `Number(price).toLocaleString('ko-KR')` + `원`; else `$` + `Number(price).toFixed(2)`
  - Text: `{label}: {tickerName ?? ticker} {quantity}주 @ {formattedPrice}`
- Auto-dismiss: `useEffect(() => { const t = setTimeout(() => onDismiss(toast.id), 4500); return () => clearTimeout(t); }, [toast.id, onDismiss])`
- Apply CSS class `toast-item` + `toast-buy` or `toast-sell`

**Create** `frontend/trading-web/src/shared/ui/Toast/ToastContainer.tsx`

Directives:
- No props
- Read `toasts` and `removeToast` from `useToastStore`
- Render `<div className="toast-container">` with `{toasts.map(t => <ToastItem key={t.id} toast={t} onDismiss={removeToast} />)}`
- `div` uses `className="toast-container"` (fixed position via CSS)

**Create** `frontend/trading-web/src/shared/ui/Toast/toast.css`

Directives (CSS only, no Tailwind):
```css
.toast-container {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 320px;
}

.toast-item {
  background: var(--surface-elevated, #1e2432);
  border: 1px solid var(--border-color, #2d3348);
  border-radius: 8px;
  padding: 12px 16px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  animation: toast-slide-in 0.2s ease;
  font-size: 13px;
  color: var(--text-primary, #e2e8f0);
}

.toast-buy  { border-left: 3px solid var(--status-success, #10b981); }
.toast-sell { border-left: 3px solid var(--status-error, #ef4444); }

@keyframes toast-slide-in {
  from { transform: translateX(110%); opacity: 0; }
  to   { transform: translateX(0);    opacity: 1; }
}

.toast-dismiss-btn {
  margin-left: auto;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-secondary, #94a3b8);
  padding: 0;
  line-height: 1;
}
```

**Create** `frontend/trading-web/src/shared/ui/Toast/index.ts`

Directives:
```typescript
export { ToastContainer } from './ToastContainer';
export { ToastItem } from './ToastItem';
```

---

### B-5. ExecutionToastProvider

**Create** `frontend/trading-web/src/features/execution-toast/ui/ExecutionToastProvider.tsx`

Directives:
- Functional component, no props (renders nothing — `return null`)
- Reads `VITE_API_BASE_URL` via `import.meta.env.VITE_API_BASE_URL ?? ''`
- Calls `useExecutionSse(url + '/api/v1/executions/stream', handleEvent)`
- `handleEvent`: calls `useToastStore.getState().addToast(event)` or use Zustand selector
- Import `useExecutionSse` from `../../../shared/lib/sse/useExecutionSse`
- Import `useToastStore` from `../model/useToastStore`
- Import `ExecutionEvent` from `../../../entities/execution/model/types`

---

### B-6. App.tsx Modification

**Modify** `frontend/trading-web/src/App.tsx`

Directives:
- Add imports:
  - `import { ExecutionToastProvider } from './features/execution-toast/ui/ExecutionToastProvider'`
  - `import { ToastContainer } from './shared/ui/Toast'`
  - `import './shared/ui/Toast/toast.css'`
- Inside the returned JSX `<div className="app-shell">`, add `<ExecutionToastProvider />` as the **first child** (before `<Sidebar>`)
- Add `<ToastContainer />` as the **last child** inside `<div className="app-shell">` (after `<main className="main-wrapper">`)

---

### B-7. Frontend Build Verification

Run from worktree frontend dir: `npm run build`
Must pass with zero TypeScript errors.

---

## Completion Criteria

- `./gradlew compileKotlin` passes for trading-api
- `npm run build` passes for trading-web
- No new `any` types introduced in TypeScript
- No BigDecimal/floating-point monetary math in frontend
- No hardcoded URLs (use `VITE_API_BASE_URL`)

---

## Agent Return Protocol

Return a completion report in this exact format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <what was implemented>
- Files modified: <list of paths relative to main repo root>
- Build results: <trading-api compileKotlin: PASS/FAIL> | <trading-web npm run build: PASS/FAIL>
- Blockers: <none | description>
---
