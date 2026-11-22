## Project Overview

Paper trading (LOCAL/KIS) + algorithmic trading platform.
Market data collection → strategy execution → order/execution/settlement → portfolio analysis.

- trading-api: orders, executions, accounts, positions, settlement — Kotlin/Spring Boot 3/JPA/PostgreSQL/Redis
- collector-api: real-time quote collection, Redis publishing — Kotlin/Spring Boot 3/Redis
- quant-worker: daily OHLCV collection, backtesting, AI quant strategies — Python 3.11/FastAPI/SQLAlchemy/LangChain
- trading-web: operations dashboard UI — React/TypeScript/Vite

## Frontend Design System

- trading-web의 공식 디자인 시스템은 shadcn/ui Base Nova다.

Data flow: KIS WebSocket → collector-api → Redis Pub/Sub → trading-api (matching engine)

## Build Verification Commands

```
trading-api:      cd backend/trading-api && ./gradlew compileKotlin
collector-api:    cd backend/collector-api && ./gradlew compileKotlin
quant-worker:     python -m py_compile {file}
trading-web:      cd frontend/trading-web && npm run build
```

@AGENTS.md
