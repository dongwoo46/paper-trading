# Feature README (Latest API/Web Integration)

Last updated: 2026-04-06

## Rules
- If backend API is added/changed, update this file immediately.
- If web API wiring is added/changed, update this file immediately.
- This file must keep only latest state.

## Backend API Index

### KIS
- `GET/POST/DELETE /api/kis/ws/subscriptions`
- `GET/POST/DELETE /api/kis/rest/watchlist`
- `GET /api/kis/rest/watchlist/price`
- `GET /api/symbols/kr`
- `GET /api/kis/symbols/catalog`
- `GET/POST/DELETE /api/kis/symbols/subscriptions`

### Upbit
- `GET /api/upbit/markets/catalog`
- `GET /api/upbit/markets/search`
- `GET /api/upbit/markets/selections`
- `GET/POST/DELETE /api/upbit/markets/subscriptions`
- `POST /api/upbit/markets/selections`
- `DELETE /api/upbit/markets/selections`
- `POST /api/upbit/markets/catalog/sync`

### pykrx
- `GET /api/pykrx/symbols/catalog`
- `GET /api/pykrx/symbols/search`
- `GET /api/pykrx/symbols/selections`
- `GET/POST/DELETE /api/pykrx/symbols/subscriptions`
- `POST /api/pykrx/symbols/selections`
- `DELETE /api/pykrx/symbols/selections`
- `POST /api/pykrx/symbols/catalog/sync`
- `POST /api/pykrx/symbols/collection-status`

### yfinance
- `GET /api/yfinance/symbols/catalog`
- `GET /api/yfinance/symbols/search`
- `GET /api/yfinance/symbols/selections`
- `GET/POST/DELETE /api/yfinance/symbols/subscriptions`
- `POST /api/yfinance/symbols/selections`
- `DELETE /api/yfinance/symbols/selections`
- `POST /api/yfinance/symbols/collection-status`

### FRED
- `GET /api/fred/series/catalog`
- `GET /api/fred/series/search`
- `GET /api/fred/series/selections`
- `GET/POST/DELETE /api/fred/series/subscriptions`
- `POST /api/fred/series/selections`
- `DELETE /api/fred/series/selections`
- `POST /api/fred/series/catalog/sync`
- `GET /api/fred/series/observations`
- `GET /api/fred/series/info`

### Daily OHLCV
- `GET /api/pykrx/ohlcv/symbols`
- `GET /api/yfinance/ohlcv/symbols`
- `GET /api/pykrx/ohlcv/daily`
- `GET /api/yfinance/ohlcv/daily`

## Web Integration Status (`frontend/trading-web`)

### Connected
- KIS
  - `GET /api/kis/ws/subscriptions`
  - `GET /api/kis/rest/watchlist`
  - `GET /api/kis/rest/watchlist/price`
  - `GET /api/kis/symbols/catalog`
  - `GET/POST/DELETE /api/kis/symbols/subscriptions`
  - `GET /api/symbols/kr`
  - Screen: `features/kis-management/ui/KisPanel.tsx`
- Upbit
  - `GET /api/upbit/markets/catalog`
  - `GET /api/upbit/markets/search`
  - `GET/POST/DELETE /api/upbit/markets/subscriptions`
  - `POST /api/upbit/markets/catalog/sync`
  - Screen: `features/upbit-management/ui/UpbitPanel.tsx`
- pykrx/yfinance
  - `GET /api/{source}/symbols/catalog`
  - `GET /api/{source}/symbols/search`
  - `GET/POST/DELETE /api/{source}/symbols/subscriptions`
  - `POST /api/{source}/symbols/collection-status`
  - `POST /api/pykrx/symbols/catalog/sync`
  - `GET /api/{source}/ohlcv/symbols`
  - `GET /api/{source}/ohlcv/daily`
  - Screen: `features/catalog-management/ui/SymbolCatalogPanel.tsx`
- FRED
  - `GET /api/fred/series/catalog`
  - `GET /api/fred/series/search`
  - `GET/POST/DELETE /api/fred/series/subscriptions`
  - `POST /api/fred/series/catalog/sync`
  - `GET /api/fred/series/observations`
  - `GET /api/fred/series/info`
  - Screen: `features/fred-management/ui/FredPanel.tsx`

### Remaining (not wired in web)
- none

## Runtime Jobs
- FRED observation polling (scheduled)
  - Job: `FredObservationScheduler`
  - Stores selected FRED series observations into `fred_series_observation`
  - Incremental mode: fetches only data after latest stored `observation_date` per series
  - Config:
    - `collector.source.fred.polling-enabled`
    - `collector.source.fred.polling-initial-delay-millis`
    - `collector.source.fred.polling-interval-millis`
    - `collector.source.fred.polling-limit-per-series`
    - `collector.source.fred.polling-request-interval-millis`
    - `collector.source.fred.polling-max-retries`
