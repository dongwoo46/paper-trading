# Feature Spec: subscription-monitor

## Overview

`GET /api/subscriptions/status` — 현재 KIS WebSocket/REST 구독 종목과 연결 상태를 운영 대시보드에 제공한다.

ws-reconnect-stability (PR #8)에서 구축한 `KisWsConnectionRegistry` + `KisWsHealthService` 위에
구독 종목 목록과 각 모드별 슬롯 사용량을 추가 노출하는 것이 목표다.

---

## Context

| 항목 | 현황 |
|---|---|
| WS 연결 상태 인프라 | `KisWsConnectionRegistry` (in-memory, thread-safe) |
| 기존 health endpoint | `GET /api/kis/ws/health` → mode별 연결 상태만 노출 |
| WS 구독 목록 | `KisWsSubscriptionService.listSymbolsPerMode()` — Redis set |
| REST watchlist | `KisRestWatchlistService.listSymbolsPerMode()` — Redis set |
| 슬롯 한계 | `KisProperties.maxRealtimeRegistrations` (기본 41) |
| 모드 | `paper`, `live` (또는 둘 다) |

---

## Domain Model

### SubscriptionModeStatus (Value Object)

```
SubscriptionModeStatus(
  mode: String,                    // "paper" | "live"
  connectionStatus: WsConnectionStatus,  // CONNECTED | DISCONNECTED | RECONNECTING
  lastConnectedAt: Instant?,
  reconnectAttempts: Long,
  wsSymbols: List<String>,         // WebSocket 구독 중인 종목 (sorted)
  restSymbols: List<String>,       // REST polling 감시 중인 종목 (sorted)
  wsSlotUsed: Int,                 // wsSymbols.size * trIds.size
  wsSlotMax: Int,                  // maxRealtimeRegistrations
)
```

### SubscriptionStatusReport (Aggregate Read Model)

```
SubscriptionStatusReport(
  generatedAt: Instant,            // 응답 생성 시각
  modes: List<SubscriptionModeStatus>,  // sorted by mode name
  totalWsSlotUsed: Int,            // modes.sumOf { wsSlotUsed }
  totalWsSlotMax: Int,             // maxRealtimeRegistrations (global cap)
)
```

이 모델은 Application 레이어에서 조립한다. DB 저장 없이 in-memory/Redis 상태를 조합한다.

---

## API Spec

### `GET /api/subscriptions/status`

**Purpose**: 운영 대시보드에서 KIS WS/REST 구독 현황을 조회한다.

**Auth**: 없음 (내부 운영 대시보드, 방화벽 격리 전제)

**성공 응답 `200 OK`**:

```json
{
  "generatedAt": "2026-04-30T10:00:00.000Z",
  "totalWsSlotUsed": 3,
  "totalWsSlotMax": 41,
  "modes": [
    {
      "mode": "live",
      "connectionStatus": "CONNECTED",
      "lastConnectedAt": "2026-04-30T09:55:00.000Z",
      "reconnectAttempts": 0,
      "wsSymbols": ["005930", "035720"],
      "restSymbols": [],
      "wsSlotUsed": 2,
      "wsSlotMax": 41
    },
    {
      "mode": "paper",
      "connectionStatus": "DISCONNECTED",
      "lastConnectedAt": null,
      "reconnectAttempts": 0,
      "wsSymbols": ["005930"],
      "restSymbols": ["035420"],
      "wsSlotUsed": 1,
      "wsSlotMax": 41
    }
  ]
}
```

**필드 설명**:

| 필드 | 타입 | 설명 |
|---|---|---|
| `generatedAt` | ISO-8601 UTC | 응답 조립 시각 |
| `totalWsSlotUsed` | Int | 전체 WS 슬롯 사용량 (symbol × trId 수) |
| `totalWsSlotMax` | Int | KIS API 실시간 등록 상한 |
| `modes[].mode` | String | "paper" 또는 "live" |
| `modes[].connectionStatus` | String | CONNECTED / DISCONNECTED / RECONNECTING |
| `modes[].lastConnectedAt` | String? | ISO-8601 UTC, 한 번도 연결 안 됐으면 null |
| `modes[].reconnectAttempts` | Long | 현재 재연결 시도 횟수 |
| `modes[].wsSymbols` | String[] | WebSocket 구독 종목 (알파벳 정렬) |
| `modes[].restSymbols` | String[] | REST polling 종목 (알파벳 정렬) |
| `modes[].wsSlotUsed` | Int | 이 모드의 WS 슬롯 사용량 |
| `modes[].wsSlotMax` | Int | 이 모드의 WS 슬롯 상한 (globalMax와 동일) |

**에러 케이스**:

| 상황 | HTTP | body |
|---|---|---|
| 정상 (KIS disabled) | 200 | modes 빈 배열, totalWsSlotUsed=0 |
| 내부 예외 | 500 | GlobalExceptionHandler 처리 |

KIS disabled 시에도 빈 목록을 200으로 반환한다 (운영 대시보드가 항상 응답을 받을 수 있어야 함).

---

## DDD Layer Design

```
presentation/subscriptions/
  SubscriptionStatusController.kt          ← GET /api/subscriptions/status
  dto/
    SubscriptionStatusResponse.kt          ← API 응답 DTO (controller 전용)

application/subscriptions/
  service/
    SubscriptionStatusService.kt           ← 조립 로직 (query-only)
  dto/
    SubscriptionStatusReport.kt            ← Application 내부 read model
    SubscriptionModeStatus.kt              ← Value Object (모드별 상태)

domain/kis/
  (기존) WsConnectionStatus.kt            ← enum, 변경 없음
  (기존) KisWsSubscription.kt             ← entity, 변경 없음

infra/kis/
  (기존) KisWsConnectionRegistry.kt       ← snapshot() 재사용
  (기존) persistence/KisWsSubscriptionRepository.kt

application/kis/service/
  (기존) KisWsSubscriptionService.kt      ← listSymbolsPerMode() 재사용
  (기존) KisRestWatchlistService.kt       ← listSymbolsPerMode() 재사용
  (기존) KisWsHealthService.kt            ← health() snapshot 재사용
```

**의존 방향**: presentation → application → domain/infra (기존 DDD 구조 준수)

**새로 추가되는 파일 5개**:

1. `application/subscriptions/dto/SubscriptionModeStatus.kt`
2. `application/subscriptions/dto/SubscriptionStatusReport.kt`
3. `application/subscriptions/service/SubscriptionStatusService.kt`
4. `presentation/subscriptions/dto/SubscriptionStatusResponse.kt`
5. `presentation/subscriptions/SubscriptionStatusController.kt`

**수정되는 파일**: 없음 (기존 서비스 재사용, 새 레이어만 추가)

---

## Implementation Notes

- `SubscriptionStatusService`는 `KisWsHealthService`, `KisWsSubscriptionService`, `KisRestWatchlistService`, `KisProperties`를 생성자 주입받는다.
- `wsSlotUsed = wsSymbols.size * trIds.size` (trIds는 `KisProperties.resolvedTrIds().size`)
- `wsSlotMax = KisProperties.maxRealtimeRegistrations`
- KIS disabled(`!properties.enabled`)면 빈 `SubscriptionStatusReport`를 반환한다.
- 종목 목록은 `sorted()` 적용 후 반환한다.
- `generatedAt = Instant.now()` (서비스 내부에서 생성)
- controller에서 `Instant` → ISO-8601 문자열 변환 (`toString()` 또는 `DateTimeFormatter.ISO_INSTANT`)

---

## TDD Plan

### Unit Tests (application layer)

**`SubscriptionStatusServiceTest`**:
1. `report_returns_empty_when_kis_disabled` — KIS disabled 시 modes 빈 목록 반환
2. `report_assembles_mode_status_correctly` — WS health + WS symbols + REST symbols 올바르게 조립
3. `report_sorts_modes_alphabetically` — `live` < `paper` 순서 보장
4. `report_calculates_wsSlotUsed_correctly` — wsSlotUsed = symbols.size × trIds.size
5. `report_sets_totalWsSlotUsed_as_sum_of_modes` — 여러 모드의 합계 계산

### Controller Tests (presentation layer)

**`SubscriptionStatusControllerTest`** (MockMvc):
1. `GET_subscriptions_status_returns_200_with_correct_structure`
2. `GET_subscriptions_status_maps_null_lastConnectedAt_to_null_in_json`
3. `GET_subscriptions_status_returns_200_with_empty_modes_when_disabled`
