Collector Worker API Reference

유지 규칙: 엔드포인트 추가/변경/삭제 시 이 파일을 반드시 동기화한다.
웹 연결 여부(web 컬럼)는 프론트엔드 코드(frontend/trading-web/src)를 기준으로 표시한다.

Collector Worker (api_main.py → src/interfaces/api/app.py)

Method: Path, 설명, Web 연결
--------: ------, ------, ----------
GET: /health, 서버 상태 확인, ❌ 미연결
POST: /collect/daily, 카탈로그 기반 일봉 수집 실행, ❌ 미연결

POST /collect/daily 요청 바디

{
  "provider": "all",          // "yfinance" | "pykrx" | "all"
  "start": "2010-01-01",
  "end": "오늘 날짜 (자동)",
  "output_root": "data",
  "only_default": false,
  "auto_adjust": false,
  "adjusted": false,
  "disable_redis": false
}

POST /collect/daily 응답

{
  "provider": "all",
  "symbols": 120,
  "success_symbols": 118,
  "failed_symbols": 2,
  "summary_path": "/abs/path/data/summary.csv",
  "start": "2010-01-01",
  "end": "2026-04-06"
}

프론트엔드가 호출하는 외부 API (collector-api 등 별도 서비스)

아래는 frontend/trading-web/src 코드에서 실제로 호출 중인 API 목록이다.
collector-worker가 아니며, 별도 백엔드 서비스에서 제공해야 한다.

Upbit

Method: Path, 설명, Web 연결
--------: ------, ------, ----------
GET: /api/upbit/markets/catalog, 업비트 종목 카탈로그 목록, ✅ 연결됨
GET: /api/upbit/markets/search, 업비트 종목 검색, ✅ 연결됨
GET: /api/upbit/markets/subscriptions, 구독 목록 조회, ✅ 연결됨
POST: /api/upbit/markets/subscriptions, 구독 추가, ✅ 연결됨
DELETE: /api/upbit/markets/subscriptions, 구독 해지, ✅ 연결됨
POST: /api/upbit/markets/catalog/sync, 카탈로그 동기화, ✅ 연결됨

KIS (한국투자증권)

Method: Path, 설명, Web 연결
--------: ------, ------, ----------
GET: /api/kis/symbols/catalog, 종목 카탈로그 (mode/channel 필터), ✅ 연결됨
GET: /api/kis/symbols/subscriptions, 구독 목록 (mode/channel), ✅ 연결됨
POST: /api/kis/symbols/subscriptions, 구독 추가, ✅ 연결됨
DELETE: /api/kis/symbols/subscriptions, 구독 해지, ✅ 연결됨
GET: /api/kis/rest/watchlist/price, 현재가 조회, ✅ 연결됨
GET: /api/kis/ws/subscriptions, 레거시 WS 구독 목록, ✅ 연결됨 (레거시)
GET: /api/kis/rest/watchlist, 레거시 REST 관심 목록, ✅ 연결됨 (레거시)

pykrx (국내 주식)

Method: Path, 설명, Web 연결
--------: ------, ------, ----------
GET: /api/pykrx/symbols/catalog, 종목 카탈로그 목록, ✅ 연결됨
GET: /api/pykrx/symbols/search, 종목 검색, ✅ 연결됨
GET: /api/pykrx/symbols/subscriptions, 구독 목록, ✅ 연결됨
POST: /api/pykrx/symbols/subscriptions, 구독 추가, ✅ 연결됨
DELETE: /api/pykrx/symbols/subscriptions, 구독 해지, ✅ 연결됨
POST: /api/pykrx/symbols/catalog/sync, 카탈로그 동기화, ✅ 연결됨
POST: /api/pykrx/symbols/collection-status, 수집 기준일 수동 업데이트, ✅ 연결됨
GET: /api/pykrx/ohlcv/symbols, OHLCV 수집된 종목 목록, ✅ 연결됨
GET: /api/pykrx/ohlcv/daily, 일봉 데이터 조회, ✅ 연결됨

yfinance (해외 주식)

Method: Path, 설명, Web 연결
--------: ------, ------, ----------
GET: /api/yfinance/symbols/catalog, 종목 카탈로그 목록, ✅ 연결됨
GET: /api/yfinance/symbols/search, 종목 검색, ✅ 연결됨
GET: /api/yfinance/symbols/subscriptions, 구독 목록, ✅ 연결됨
POST: /api/yfinance/symbols/subscriptions, 구독 추가, ✅ 연결됨
DELETE: /api/yfinance/symbols/subscriptions, 구독 해지, ✅ 연결됨
POST: /api/yfinance/symbols/collection-status, 수집 기준일 수동 업데이트, ✅ 연결됨
GET: /api/yfinance/ohlcv/symbols, OHLCV 수집된 종목 목록, ✅ 연결됨
GET: /api/yfinance/ohlcv/daily, 일봉 데이터 조회, ✅ 연결됨

FRED (거시경제 지표)

Method: Path, 설명, Web 연결
--------: ------, ------, ----------
GET: /api/fred/series/catalog, 시리즈 카탈로그 목록, ✅ 연결됨
GET: /api/fred/series/subscriptions, 구독 목록, ✅ 연결됨
POST: /api/fred/series/subscriptions, 구독 추가, ✅ 연결됨
DELETE: /api/fred/series/subscriptions, 구독 해지, ✅ 연결됨
GET: /api/fred/series/search, FRED 외부 API 검색, ✅ 연결됨
GET: /api/fred/series/info, 시리즈 상세 정보 조회, ✅ 연결됨
GET: /api/fred/series/observations, 관측치 조회, ✅ 연결됨
POST: /api/fred/series/catalog/sync, 카탈로그 동기화, ✅ 연결됨
