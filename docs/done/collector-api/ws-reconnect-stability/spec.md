# ws-reconnect-stability

## Core Feature
Detect silent WebSocket drops via heartbeat timeout and automatically reconnect with exponential backoff, re-subscribing all active tickers, while exposing per-mode connection health via a REST endpoint.

## Considerations
- KIS WebSocket sends market tick data roughly every second during market hours. A 60-second silence window is a safe threshold for detecting silent drops without false positives during after-hours quiet periods.
- Reactor's existing `Retry.backoff` on `startMode()` already handles reconnect scheduling (1s initial, 30s cap). The implementation must hook into this loop — not replace it.
- Health state is volatile runtime state, not financial business state. It does not need persistence or domain modeling. It belongs in the infrastructure layer.
- `reconnect-min-delay` (1s) and `reconnect-max-delay` (30s) already exist in `KisProperties`. No changes needed for backoff config.
- The `emit()` method silently no-ops when `outboundSinks[mode]` is null (during reconnect). This is already safe. No change needed.

## Trade-offs
- **Heartbeat via timeout operator vs. dedicated ping/pong frame**: KIS WebSocket does not document a PING/PONG protocol. Using `timeout()` on the receive flux is simpler and reliable since ticks flow continuously during market hours. Chose `timeout()`.
- **Centralized registry vs. state fields directly in collector**: A separate `KisWsConnectionRegistry` component makes health state readable from outside the collector without adding HTTP coupling to the collector itself. Chose registry.
- **Single endpoint (all modes) vs. per-mode path**: A single `GET /api/kis/ws/health` returning a list is simpler to consume and mirrors how modes are configured. Chose single endpoint.
- **Persisted health history vs. in-memory only**: This feature is about liveness detection, not audit. Persisting health state adds DB dependency with no payoff. Chose in-memory.

## Implementation Approach

**domain layer** — no changes. No new domain entities. `WsConnectionStatus` enum (CONNECTED, DISCONNECTED, RECONNECTING) is a pure value type and may live in `domain.kis` or `application.kis`.

**infrastructure layer**
- New `KisWsConnectionRegistry` (@Component): thread-safe per-mode state holder. Tracks `status`, `lastConnectedAt` (Instant?), `reconnectAttempts` (AtomicLong per mode). Updated by `KisWebSocketCollector` lifecycle callbacks.
- Modified `KisWebSocketCollector`: add `.timeout(heartbeatTimeout)` on the receive flux inside `connectOnce()`. Add callbacks on connect (`registry.markConnected(mode)`), on retry-before (`registry.markReconnecting(mode, attempt)`), on finalize-disconnect (`registry.markDisconnected(mode)`).
- `KisProperties`: add `heartbeatInterval: Duration` and `heartbeatTimeout: Duration` with defaults.

**application layer**
- New `KisWsHealthService` (@Service): reads `KisWsConnectionRegistry`, returns `List<WsHealthSnapshot>`.
- New data class `WsHealthSnapshot`: mode, status, lastConnectedAt, reconnectAttempts.

**presentation layer**
- New `KisWsHealthController` (@RestController, GET /api/kis/ws/health): calls `KisWsHealthService`, returns list of health snapshots.

## Workflow

```
State machine per mode:

  DISCONNECTED ──(start / connect attempt)──► RECONNECTING
  RECONNECTING ──(connect success)──────────► CONNECTED
  CONNECTED    ──(heartbeat timeout / error)► RECONNECTING
  CONNECTED    ──(graceful stop)────────────► DISCONNECTED
  RECONNECTING ──(graceful stop)────────────► DISCONNECTED

Reconnect flow:
  1. receive flux emits timeout error (silence > heartbeatTimeout)
     OR Reactor Netty signals TCP disconnect
  2. connectOnce() terminates → doFinally marks DISCONNECTED transiently
  3. startMode() retryWhen fires → doBeforeRetry marks RECONNECTING + increments attempt counter
  4. connectOnce() runs again → new approval key issued → symbols re-subscribed via initial Flux.fromIterable
  5. On successful WebSocket connect → markConnected(mode), reset attempt counter
```

## API

```
GET /api/kis/ws/health

Response 200 OK:
[
  {
    "mode": "paper",
    "status": "CONNECTED",          // CONNECTED | DISCONNECTED | RECONNECTING
    "lastConnectedAt": "2026-04-29T10:00:00Z",   // null if never connected
    "reconnectAttempts": 0
  },
  {
    "mode": "live",
    "status": "RECONNECTING",
    "lastConnectedAt": "2026-04-29T09:55:00Z",
    "reconnectAttempts": 3
  }
]

No request body. No path parameters. Returns one entry per configured mode.
```

## Config

New keys added to `application.yaml` under `collector.source.kis`:

```yaml
collector:
  source:
    kis:
      heartbeat-interval: 20s     # (informational, not used in logic — kept for documentation)
      heartbeat-timeout: 60s      # silence window on receive flux before triggering reconnect
                                  # default: 60s. Set longer for after-hours if false positives occur.
```

`KisProperties` additions:
```
heartbeatTimeout: Duration = Duration.ofSeconds(60)
```

Existing fields unchanged:
```
reconnectMinDelay: Duration = Duration.ofSeconds(1)   // already present
reconnectMaxDelay: Duration = Duration.ofSeconds(30)  // already present
```
