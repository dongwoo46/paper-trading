Web: Remaining API Integration Completion

Goal
Connect all previously unlinked backend APIs in frontend/trading-web.
Keep README-driven UX structure and verify build success.

Scope
KIS (KisPanel)
Added unified catalog/subscription API wiring:
GET /api/kis/symbols/catalog
GET/POST/DELETE /api/kis/symbols/subscriptions
Added price API wiring:
GET /api/kis/rest/watchlist/price
Kept legacy list wiring for visibility:
GET /api/kis/ws/subscriptions
GET /api/kis/rest/watchlist

Upbit (UpbitPanel)
Switched operational flow to subscriptions API:
GET/POST/DELETE /api/upbit/markets/subscriptions
Added sync trigger:
POST /api/upbit/markets/catalog/sync
Kept catalog/search:
GET /api/upbit/markets/catalog
GET /api/upbit/markets/search

pykrx/yfinance (SymbolCatalogPanel)
Switched selection flow to subscriptions API:
GET/POST/DELETE /api/{source}/symbols/subscriptions
Added collection status update:
POST /api/{source}/symbols/collection-status
Added pykrx catalog sync:
POST /api/pykrx/symbols/catalog/sync
Added OHLCV query wiring:
GET /api/{source}/ohlcv/symbols
GET /api/{source}/ohlcv/daily

FRED (FredPanel)
Switched operation to subscriptions API:
GET/POST/DELETE /api/fred/series/subscriptions
Added sync/search/info/observations wiring:
POST /api/fred/series/catalog/sync
GET /api/fred/series/search
GET /api/fred/series/info
GET /api/fred/series/observations
Kept catalog listing:
GET /api/fred/series/catalog

Types/Pages
Added OHLCV frontend response types in entities/symbol/model/types.ts.
Updated role pages copy to reflect connected API coverage.

Verification
Command: npm run build (frontend/trading-web)
Result: success

Follow-up
If needed, replace legacy KIS WS/REST endpoints with unified endpoints only in UI to reduce duplication.
