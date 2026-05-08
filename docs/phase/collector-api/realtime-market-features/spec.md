# Realtime Market Features

## Core Feature
WebSocket tick stream을 분 단위 집계 + 멀티윈도우 feature snapshot으로 즉시 재사용 가능한 Redis state로 변환한다.

## Considerations
- Raw tick 장기 저장 금지 정책을 유지하면서 전략 판단에 필요한 최신 feature를 제공해야 한다.
- 기존 `RawEventPipeline -> QuoteRedisPublisher` 경로를 깨지 않고 확장해야 한다.
- Redis 키 수명 정책(TTL/maxlen)으로 메모리 상한을 강제해야 한다.
- collector-api는 계산/저장 책임만 갖고, 전략 실행 책임은 trading-api가 가진다.

## Trade-offs
- Option A: Tick마다 1m/5m/10m를 모두 재계산
- Option B: 1m current를 증분 업데이트하고 5m/10m는 최근 1m bars에서 합산
- Chosen: Option B. 쓰기 비용/복잡도 균형이 좋고 분 롤오버 시점 정합성 관리가 명확하다.

## Implementation Approach
- Application layer: `KisQuoteEvent`를 받아 feature 집계 유스케이스를 실행하는 서비스 추가.
- Domain layer: minute bucket 상태와 snapshot VO를 정의하고 집계/롤오버 규칙을 캡슐화.
- Infra layer: Redis hash/list 기반 feature 저장소 구현, TTL/maxlen 정책 적용.
- Presentation layer: 최신 feature 조회용 read-only API 제공.

## Workflow
1. WS tick 수신 → `RawEventPipeline.publish()`.
2. parser가 `KisQuoteEvent` 생성.
3. 기존 `QuoteRedisPublisher`가 `quote:{ticker}` + `quote:{ticker}` pub/sub 처리.
4. 신규 feature 서비스가 `agg:1m:{ticker}:current` 증분 갱신.
5. minute boundary 감지 시 current bucket을 `bars:1m:{ticker}`에 append 후 새 current 시작.
6. 최신 1m bar 기반으로 `feature:{ticker}:1m`, `feature:{ticker}:5m`, `feature:{ticker}:10m` snapshot overwrite.
7. (옵션) 디버그 모드에서 최근 30~60초 tick ring buffer만 유지.

## API
GET /api/market/features/{symbol}?windows=1m,5m,10m — symbol 최신 feature snapshot 조회
Request:
- Path: `symbol: string` (종목코드, 공백 불가)
- Query: `windows: string` (comma-separated, optional, default `1m,5m,10m`)

Response:
- `symbol: string`
- `asOf: string(ISO-8601)`
- `features: [{ window: "1m"|"5m"|"10m", open: string, high: string, low: string, close: string, returnRate: string, volume: string, tradeValue: string, vwap: string, buyVolume: string, sellVolume: string, tradeImbalance: string, tickCount: number, startedAt: string, updatedAt: string }]`

Errors:
- 400: invalid symbol / invalid windows
- 404: snapshot not found for symbol
- 409: aggregation state temporarily inconsistent (rollover race)

## DB
Redis only (no RDB table change)
- `latest:{symbol}` (Hash): 최근 시세 보조 캐시
  - keys: price, askp1, bidp1, high, low, volume, updatedAt
  - TTL: 60s
- `agg:1m:{symbol}:current` (Hash): 현재 1분 진행중 버킷
  - keys: minute, open, high, low, close, volume, tradeValue, buyVolume, sellVolume, tickCount, startedAt, updatedAt
  - TTL: 180s
- `bars:1m:{symbol}` (List/Stream-like ring)
  - element JSON fields: minuteStart, open, high, low, close, volume, tradeValue, buyVolume, sellVolume, tickCount, startedAt, updatedAt
  - retention: maxlen 600 (약 10시간) + TTL 12h
- `feature:{symbol}:1m|5m|10m` (Hash)
  - standardized feature fields + updatedAt
  - TTL: 30m
- `debug:tick:{symbol}` (List, optional)
  - raw tick compact JSON
  - maxlen: 1200 (20ticks/s * 60s 가정 상한) + TTL: 120s

Indexes/Relationships:
- Redis key namespace 관계만 존재 (RDB 인덱스 없음).
- `feature`는 `bars:1m`를 부모 데이터로 재계산된다.

## DDD Model
- Bounded Context: `market-feature` (collector-api 내부 시장데이터 처리 컨텍스트)
- Entity: `MinuteBarState` (symbol + minuteStart 식별)
- Value Objects:
  - `FeatureWindow` (`1m|5m|10m`)
  - `FeatureSnapshot`
  - `TradeFlow` (buyVolume/sellVolume/tradeImbalance)
- Aggregate: `MarketFeatureAggregate`
  - Invariant: high >= max(open, close), low <= min(open, close), tickCount >= 1
  - Invariant: window snapshot은 source bars 존재 시에만 생성
- Domain Event:
  - `MinuteRolledOver(symbol, minuteStart, minuteEnd)`
  - `FeatureSnapshotUpdated(symbol, window, updatedAt)`

## External Dependencies
- Existing: Spring Data Redis (`StringRedisTemplate`), Jackson `ObjectMapper`
- No new infrastructure dependency for this phase.

## Non-Functional Requirements
- Per tick 처리 시간 p95 < 5ms (in-process 기준)
- Redis write 실패는 로깅 + 다음 tick에서 회복 가능해야 함 (프로세스 중단 금지)
- 분 경계 rollover는 동일 symbol 기준 at-most-once append를 보장해야 함

## Out of Scope
- 원본 tick 장기 저장소(Kafka/ClickHouse/TimescaleDB) 도입
- 전략 시그널 생성/주문 트리거 로직
- 프론트 대시보드 UI 변경
