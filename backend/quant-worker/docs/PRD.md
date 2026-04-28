# PRD — quant-worker

## 역할
일봉 OHLCV 배치 수집 워커. collector-api HTTP 요청으로 트리거, pykrx/yfinance로 수집 후 PostgreSQL 적재.

## 아키텍처

### 디렉토리 구조
```
quant-worker/
├── src/
│   ├── fetch/          pykrx_fetcher.py, yfinance_fetcher.py
│   └── db/             upsert.py (ON CONFLICT DO UPDATE)
├── scripts/            수동 실행 스크립트
├── main.py             CLI 실행 진입점
└── api_main.py         FastAPI 서버 진입점
```

### 패턴
- 함수형 스타일: 클래스보다 함수 단위 구성
- Upsert Pattern: 중복 적재 방지 (ON CONFLICT DO UPDATE)
- Incremental Fetch: catalog watermark 기준 증분 수집

### 데이터 흐름
```
collector-api POST /collect/daily
  → api_main.py (FastAPI)
  → fetch/pykrx_fetcher or yfinance_fetcher (provider 분기)
  → 종목별 일봉 OHLCV 수집 (watermark 이후 날짜만)
  → db/upsert.py → PostgreSQL market_daily_ohlcv
```

## 핵심 기능 (MVP)
- pykrx 일봉 OHLCV 수집 (2010~현재, 국내 주식)
- yfinance 일봉 OHLCV 수집 (2010~현재, 해외 주식)
- catalog watermark 기반 증분 수집
- DB upsert (중복 방지)
- POST /collect/daily API

## 미구현 (제외)
- 수집 실패 종목 재시도 로직
- 수집 이력 로그 DB 적재
- 분봉 수집
- Binance/DART 등 추가 소스
- 스케줄러 자체 내장 (현재는 외부 트리거 방식)
