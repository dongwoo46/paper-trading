# Subscription Monitor

## Core Feature
`GET /api/subscriptions/status` — exposes KIS WebSocket and REST subscription state per trading mode for the operations dashboard.

## Considerations
- Must aggregate three independent data sources (WS health, WS symbols, REST symbols) into a single unified response
- Response must be consistent with the `ws-reconnect-stability` health contract (explicit `null` for `lastConnectedAt` when disconnected, not omitted)
- No persistence needed — state is assembled from in-memory services at query time

## Trade-offs
- `@JsonInclude(NON_NULL)` on `lastConnectedAt` vs explicit `null` → chose explicit `null` to match the API spec; omitting the field would break dashboard null-checks
- Assembling in the application layer (`SubscriptionStatusService`) vs controller → application layer keeps the controller thin and keeps business assembly testable in isolation

## Implementation Approach
- **Domain/Infra**: reused existing `KisWsHealthService`, `KisWsSubscriptionService`, `KisRestWatchlistService` (no new infrastructure)
- **Application DTOs**: `SubscriptionModeStatus` (per-mode data), `SubscriptionStatusReport` (wrapper list)
- **Application Service**: `SubscriptionStatusService` — iterates modes, calls three services, assembles `SubscriptionModeStatus` per mode, returns `SubscriptionStatusReport`
- **Presentation DTO**: `SubscriptionStatusResponse` — wraps report for HTTP JSON response
- **Controller**: `SubscriptionStatusController` — `GET /api/subscriptions/status` → delegates to service → returns 200 with `SubscriptionStatusResponse`

## Workflow
```
GET /api/subscriptions/status
  → SubscriptionStatusController
  → SubscriptionStatusService.getStatus()
      → KisWsHealthService.getHealthByMode()         // WS connection health per mode
      → KisWsSubscriptionService.getSymbolsByMode()  // WS subscribed symbols per mode
      → KisRestWatchlistService.getSymbolsByMode()   // REST watchlist symbols per mode
  → SubscriptionStatusReport (application layer)
  → SubscriptionStatusResponse (presentation layer)
  ← 200 OK JSON
```

## Key APIs
`GET /api/subscriptions/status` — returns unified subscription status report (WS health, symbols, REST symbols) per trading mode

## New Files
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/subscriptions/dto/SubscriptionModeStatus.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/subscriptions/dto/SubscriptionStatusReport.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/subscriptions/service/SubscriptionStatusService.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/subscriptions/dto/SubscriptionStatusResponse.kt`
- `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/subscriptions/SubscriptionStatusController.kt`
- `backend/collector-api/src/test/kotlin/com/papertrading/collector/application/subscriptions/service/SubscriptionStatusServiceTest.kt`
- `backend/collector-api/src/test/kotlin/com/papertrading/collector/presentation/subscriptions/SubscriptionStatusControllerTest.kt`

## Test Coverage
8 service unit tests (`SubscriptionStatusServiceTest`) — disabled state, mode assembly, symbol sorting, WS slot calculation, edge cases (empty modes, null lastConnectedAt)
3 controller tests (`SubscriptionStatusControllerTest`) — 200 response, null `lastConnectedAt` serialization, empty modes list

37/37 tests passing in full suite.

## Completed / PR
2026-04-30 / #TBD
