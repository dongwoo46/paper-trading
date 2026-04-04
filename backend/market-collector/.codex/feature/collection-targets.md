# Feature: Collection Targets Checklist

Status values: `TODO`, `IN_PROGRESS`, `DONE`

## Realtime (WebSocket first)
- [ ] `TODO` Upbit WebSocket real-time ticker/trade/orderbook collection
- [ ] `IN_PROGRESS` KIS WebSocket real-time market data collection

## OHLCV
- [ ] `TODO` FinanceDataReader-based KR OHLCV collection
- [ ] `TODO` yfinance (Python worker)-based US OHLCV collection

## Macro (3 required)
- [ ] `TODO` FRED macro time-series collection
- [ ] `TODO` ECOS macro time-series collection
- [ ] `TODO` IMF macro time-series collection

## Filings (RAG inputs)
- [ ] `TODO` DART filings collection and document normalization
- [ ] `TODO` EDGAR filings collection and document normalization

## News and Sentiment
- [ ] `TODO` Finnhub news ingestion and sentiment pipeline

## Factor Baseline
- [ ] `TODO` FF Data Library factor data sync

## Additional Sources
- [ ] `TODO` GDELT event/news data collection
- [ ] `TODO` Reddit posts/comments data collection
- [ ] `TODO` FMP data collection

## Execution Rule
- Start work: set `TODO -> IN_PROGRESS`
- Complete work: set `DONE (YYYY-MM-DD, evidence: module/tests)`
- If blocked: add one `Blocker:` line directly under the item
