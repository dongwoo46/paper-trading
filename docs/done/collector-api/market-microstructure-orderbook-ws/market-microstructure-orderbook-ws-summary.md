# Market Microstructure — Orderbook WebSocket Ingest & Mapping

## Core Feature
KIS WebSocket H0STASP0(호가잔량) 이벤트를 수집하여 Redis에 적재하고, 마이크로구조 API 응답에 bestBid/bestAsk/spread/depth 실데이터를 매핑한다.

## Considerations
- KIS WebSocket은 단일 TR_ID 당 채널 하나; H0STASP0와 H0STCNT0를 동일 파이프라인에서 TR_ID로 분기하여 처리
- Redis Hash 기반 적재로 호가 필드별 부분 업데이트 가능, TTL 30s로 stale 데이터 자동 만료
- 실시간 메트릭은 재시작 내성 없이 AtomicLong만 사용하여 구현 복잡도 최소화

## Trade-offs
- **AtomicLong 메트릭**: 서버 재시작 시 초기화 → 운영 모니터링은 단기 지표로만 사용해야 함
- **top-5 depth만 저장**: 전체 호가 잔량 대비 Redis 메모리 절감, 전략 판단에는 충분
- **KR 국내 심볼 전용**: H0STASP0는 KIS 국내 전용 TR_ID; 해외 종목은 추후 별도 채널 필요
- **스냅샷만 유지**: 호가 변동 이력 없음 — 타임시리즈 분석이 필요하면 별도 append 저장소 요구

## Implementation Approach

| 레이어 | 내용 |
|---|---|
| Domain VO | `KisOrderbookEvent` — TR_ID, symbol, timestamp, bid/ask 5단계 가격·잔량 |
| Parser | `KisOrderbookEventParser` — H0STASP0 raw body 파싱, 필드 수·TR_ID·flagCode 검증 |
| Pipeline | `RawEventPipeline` — TR_ID 분기: H0STCNT0 → quote, H0STASP0 → orderbook store |
| Infra / Redis | `OrderbookRedisStore` — `orderbook:{symbol}` Hash 저장 (TTL 30s, top-5 depth) |
| Metrics | `OrderbookIngestMetrics` — receivedCount / parseFailCount / lastReceivedAt AtomicLong |
| Application | `MarketMicrostructureQueryService` — bestBid/bestAsk/spread/depth 실데이터 매핑, `MarketBarSourceResolver` 주입으로 1d/1w DB 소스 분기 |
| Calculator | `RelativeStrengthCalculator` — timestamp 역순 입력 예외 처리 추가 |
| Presentation | `OrderbookIngestHealthController` — 인제스트 건강 지표 노출 |

## Workflow

**Ingest path**
```
KIS WebSocket → RawEventPipeline.handle()
  → TR_ID == H0STASP0 → KisOrderbookEventParser.parse()
  → OrderbookIngestMetrics.recordReceived()
  → OrderbookRedisStore.save(symbol, event)  [HSET orderbook:{symbol} + EXPIRE 30s]
```

**Query path**
```
GET /api/market/microstructure/{symbol}
  → MarketMicrostructureQueryService
    → OrderbookRedisStore.load(symbol) → bestBid / bestAsk / spread / depth
    → MarketBarSourceResolver → 1d/1w: DB OHLCV / intraday: Redis bars
    → RelativeStrengthCalculator (timestamp 정렬 검증 포함)
  → MicrostructureResponse 반환
```

## Key APIs

`GET /api/market/microstructure/{symbol}` — bestBid/bestAsk/spread/depth 필드에 Redis orderbook 실데이터 매핑 추가; 1d/1w 요청은 MarketBarSourceResolver를 통해 DB 소스 강제 분기

`GET /api/internal/kis/orderbook/ingest-health` — 신규 운영 엔드포인트; receivedCount/parseFailCount/parseFailRate/lastReceivedAt 반환

## Redis Schema

```
orderbook:{symbol}   Hash
  bestBid            : 최우선 매수가 (String, BigDecimal 직렬화)
  bestAsk            : 최우선 매도가
  spread             : bestAsk - bestBid
  bid1..bid5         : 매수 1~5단계 가격
  bidQty1..bidQty5   : 매수 1~5단계 잔량
  ask1..ask5         : 매도 1~5단계 가격
  askQty1..askQty5   : 매도 1~5단계 잔량
  depthImbalance     : (totalBidQty - totalAskQty) / (totalBidQty + totalAskQty)
  timestamp          : 이벤트 수신 시각 (ISO-8601)
TTL: 30s (EXPIRE on every save)
```

## Test Coverage

- KisOrderbookEventParserTest (5 cases) — 정상 파싱, TR_ID 불일치, flagCode 오류, 필드 수 부족, 숫자 변환 오류
- OrderbookRedisStoreTest (4 cases) — save + TTL 검증, depthImbalance 분모 0 처리, load 정상, load null(키 없음)
- OrderbookIngestMetricsTest (4 cases) — receivedCount 증가, parseFailCount 증가, parseFailRate 계산, lastReceivedAt 갱신
- RawEventPipelineTest (8 cases) — H0STASP0/H0STCNT0 분기 라우팅, runCatching 오류 무시
- MarketMicrostructureQueryServiceTest (10 cases) — 1d/1w DB 소스 분기, orderbook 실데이터 매핑, depth null 탈출
- RelativeStrengthCalculatorTest (3 cases) — 역순 timestamp 예외, 정순 정상 계산, 단일 데이터 경계
- OrderbookIngestHealthControllerTest (2 cases) — zero state(수신 없음), 수신 있음 응답

## Known Limitations

- KR 국내 심볼 전용 (H0STASP0는 KIS 국내 전용 TR_ID)
- 최신 스냅샷만 유지 — 호가 변동 이력 없음
- In-memory 메트릭(AtomicLong)은 서버 재시작 시 초기화

## Completed / PR

2026-05-09 / #28
