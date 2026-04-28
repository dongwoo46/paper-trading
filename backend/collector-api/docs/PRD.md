# PRD — collector-api

## 역할
외부 시장 데이터 수집 + Redis 시세 발행 + PostgreSQL 히스토리 적재 + 구독 관리 API 제공.

## 아키텍처

### 디렉토리 구조
```
src/
└── main/kotlin/com/papertrading/collector/
    ├── domain/
    ├── application/
    │   ├── fred/           FredObservationIngestionService, FredObservationScheduler
    │   ├── kis/
    │   │   └── pipeline/   RawEventPipeline, KisRawEventParser
    │   ├── market/         MarketDailyOhlcvQueryService
    │   └── upbit/
    ├── infrastructure/
    │   ├── redis/          QuoteRedisPublisher
    │   └── external/       KisWebSocketCollector, FredClient
    └── interfaces/
        ├── http/           각 소스별 Controller
        └── internal/       InternalSubscriptionController
```

### 패턴
- Clean Architecture: presentation → application → domain ← infrastructure
- Pipeline Pattern: KIS WebSocket 원시 메시지 → Parser → Redis Publisher
- Scheduler Pattern: FRED 관측값 주기 수집 (AtomicBoolean 중복 실행 방지)

### 데이터 흐름
```
KIS WebSocket 메시지
  → RawEventPipeline.publish()
  → KisRawEventParser (H0STCNT0 파싱 → KisQuoteEvent)
  → QuoteRedisPublisher
      ├── Redis Hash quote:{ticker} 저장 (TTL 300s)
      └── Redis Pub/Sub quote:{ticker} 발행

trading-api → POST /api/internal/subscriptions/{ticker}
  → KisWebSocketCollector.emit() (동적 구독/해제)

collector-api → POST /collect/daily (quant-worker HTTP)
  → quant-worker가 pykrx/yfinance 수집 후 PostgreSQL 적재
```

## 핵심 기능 (MVP)
- KIS WebSocket 실시간 시세 수신 + Redis 발행
- KIS/Upbit/pykrx/yfinance/FRED 구독 관리 API
- FRED 관측값 주기 수집 (1시간 간격, 증분)
- 일봉 OHLCV 조회 API (pykrx, yfinance)
- 내부 구독 API (trading-api 연동용)

## 미구현 (제외)
- WebSocket 재연결 안정성 강화
- ECOS / IMF / Binance 수집
- DART / EDGAR 공시 수집
- 분봉 수집 파이프라인
- 수집 실패 모니터링 대시보드
