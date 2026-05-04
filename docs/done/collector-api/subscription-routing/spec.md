# Collector API Subscription Routing

## Core Feature
전략 종목을 최우선으로 WS 슬롯에 유지하고, 수동 종목은 후순위로 WS/REST를 유연하게 사용하되 포화 시 드롭 가능한 라우팅 정책을 제공한다.

## Considerations
- 전략 종목(source=STRATEGY)은 가능한 한 WS를 보장한다.
- WS 포화 시 전략 종목만 REST fallback 허용한다.
- 수동 종목(source=MANUAL)은 WS 포화 시 REST overflow, WS/REST 동시 포화 시 드롭 허용한다.
- 즐겨찾기(favorite)는 MANUAL 내부 우선순위에만 영향을 주고 STRATEGY 우선순위를 넘지 않는다.
- 내부 API는 멱등성을 기본으로 설계한다.

## Trade-offs
- Option A: 단순 FIFO eviction.
- Option B: source/favorite/updatedAt 기반 우선순위 eviction.
- 선택: B. 전략 우선 보장과 수동 드롭 정책을 명확히 강제하기 위해 우선순위 기반 eviction이 필요하다.

## Implementation Approach
- presentation: 내부/운영 API 엔드포인트 추가(수동 등록, 즐겨찾기, 전략 등록, 상태 조회).
- application: 라우팅 결정 서비스(승격/강등/드롭), 명령/조회 유스케이스 분리.
- domain: SubscriptionRequest Aggregate, SlotAssignment VO, DropReason/Channel/Source enum 정의.
- infrastructure: 기존 `KisWsSubscriptionRepository`, `KisRestWatchlistRepository` 확장 + 라우팅 상태 저장소 추가.

## Workflow
1. 요청 수신(전략 등록/수동 등록/해제/즐겨찾기 변경).
2. 현재 WS/REST 슬롯 사용량 조회.
3. 우선순위 규칙으로 배치:
   - STRATEGY는 WS 우선 배치.
   - WS 포화 시 MANUAL(비즐겨찾기 우선) eviction 후 STRATEGY를 WS로 승격.
   - 더 이상 WS 확보 불가 시 STRATEGY를 REST로 fallback.
   - MANUAL은 WS 포화 시 REST로 overflow.
   - MANUAL은 REST도 포화면 drop 처리.
4. 변경사항을 원자적으로 저장하고 상태 스냅샷 반환.

## API
POST /api/internal/subscriptions/manual — 수동 종목 등록
Request: `{ "symbol": "005930", "favorite": false, "idempotencyKey": "manual-005930" }`
Response: `{ "symbol": "005930", "source": "MANUAL", "channel": "WS|REST|DROPPED", "reason": "NONE|WS_FULL|WS_REST_FULL" }`
Errors: 400(잘못된 symbol), 409(동시 갱신 충돌)

DELETE /api/internal/subscriptions/manual/{symbol} — 수동 종목 해제
Request: path param `symbol`
Response: `{ "symbol": "005930", "removed": true }`
Errors: 404(미등록)

PUT /api/internal/subscriptions/manual/{symbol}/favorite — 즐겨찾기 등록/해제
Request: `{ "favorite": true, "idempotencyKey": "fav-005930" }`
Response: `{ "symbol": "005930", "favorite": true }`
Errors: 404(미등록), 409(동시 갱신 충돌)

GET /api/internal/subscriptions/manual/favorites — 즐겨찾기 목록 조회
Request: none
Response: `{ "items": [ { "symbol": "005930", "channel": "WS", "updatedAt": "2026-05-04T12:00:00Z" } ] }`
Errors: 200 only

POST /api/internal/subscriptions/strategy — 전략 종목 등록(향후 strategy-execution 연동)
Request: `{ "symbol": "005930", "strategyId": "mean-revert-v1", "idempotencyKey": "stg-005930-v1" }`
Response: `{ "symbol": "005930", "source": "STRATEGY", "channel": "WS|REST", "fallback": true|false }`
Errors: 400(잘못된 symbol), 409(동시 갱신 충돌)

DELETE /api/internal/subscriptions/strategy/{symbol} — 전략 종목 해제
Request: path param `symbol`
Response: `{ "symbol": "005930", "removed": true }`
Errors: 404(미등록)

GET /api/internal/subscriptions/routing/status — 현재 라우팅 상태 조회
Request: none
Response: `{ "wsCapacity": 40, "wsUsed": 40, "restCapacity": 120, "restUsed": 85, "items": [ { "symbol": "005930", "source": "STRATEGY", "favorite": false, "channel": "WS", "dropReason": null } ] }`
Errors: 200 only

## DB
`kis_subscription_request`
- key columns: `id`, `symbol`, `source`, `favorite`, `channel`, `priority_score`, `drop_reason`, `last_routed_at`, `updated_at`
- unique index: `(symbol, source)`
- index: `(channel, source, priority_score DESC)`
- index: `(favorite, source, updated_at DESC)`

`kis_routing_slot_state`
- key columns: `slot_type`(WS|REST), `capacity`, `used`, `updated_at`
- PK: `slot_type`

정책 규칙:
- Source priority: `STRATEGY > MANUAL(favorite=true) > MANUAL(favorite=false)`
- Eviction order(WS): `MANUAL(favorite=false)` → `MANUAL(favorite=true)` (STRATEGY는 WS에서 eviction 금지)
- Overflow:
  - STRATEGY: WS full 시 MANUAL eviction 시도 후 실패하면 REST fallback.
  - MANUAL: WS full 시 REST overflow, REST full 시 DROPPED.
- Promotion:
  - WS 슬롯 여유 발생 시 REST의 STRATEGY를 WS로 즉시 승격.
  - 그 다음 MANUAL favorite, 마지막 MANUAL non-favorite 순으로 승격.
- Idempotency:
  - 동일 `idempotencyKey` + 동일 payload는 200 재응답.
  - 동일 key + 상이 payload는 409.
