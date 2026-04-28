# PRD — Paper Trading System

## 목적
모의투자(로컬/KIS) + 실거래(KIS/Upbit) 를 통합 운영하는 알고리즘 트레이딩 플랫폼.
시장 데이터 수집 → 전략 실행 → 주문/체결/정산 → 포트폴리오 분석 전 과정을 커버한다.

## 서비스 구성

| 서비스 | 역할 |
| --- | --- |
| trading-api | 계좌·주문·체결·포지션·정산·전략 |
| collector-api | 시장 데이터 수집·구독 관리·Redis 시세 발행 |
| quant-worker | 일봉 OHLCV 배치 수집 (pykrx/yfinance) |
| trading-web | 운영 대시보드 UI |

## 핵심 기능 (MVP)
- 계좌 생성 및 예수금 관리 (입출금, 원장)
- 주문 생성/체결/취소 — LOCAL / KIS_PAPER / KIS_LIVE 3모드
- 실시간 시세 수집 (KIS WebSocket) → Redis Pub/Sub → 체결 엔진 트리거
- 일봉 OHLCV 수집 및 조회 (pykrx, yfinance, 2010~현재)
- 매크로 지표 수집 (FRED)
- 운영 대시보드 (구독 관리, OHLCV 차트, 매크로 조회)

## MVP 제외
- 전략 자동 실행 / 백테스트 엔진
- Upbit 실거래 어댑터
- DART/EDGAR 공시 수집
- 뉴스/감성 분석 파이프라인
- 분봉 데이터 수집
- 세금 정산 자동화
- 다중 사용자 / 권한 관리

## 서비스 간 데이터 흐름

```
KIS WebSocket
    └─→ collector-api (RawEventPipeline)
            ├─→ Redis Hash  quote:{ticker}     (최신 시세)
            └─→ Redis Pub/Sub  quote:{ticker}  (체결 트리거)
                    └─→ trading-api (QuoteEventListener → LocalMatchingEngine)

collector-api HTTP API
    └─→ quant-worker POST /collect/daily
            └─→ PostgreSQL market_daily_ohlcv  (일봉 적재)

trading-api → collector-api POST /api/internal/subscriptions/{ticker}
    (주문/포지션 생성 시 자동 구독 요청)
```
