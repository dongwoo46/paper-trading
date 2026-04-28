@../CLAUDE.md

## quant-worker

일별 시세 데이터 수집 + 백테스팅 + AI 퀀트 전략 워커.
yfinance/pykrx로 수집 후 PostgreSQL 적재.
Python 3.11+ / FastAPI / SQLAlchemy / LangChain / LangGraph

### 모듈

- `src/collectors/` — yfinance/pykrx 일봉 OHLCV 수집
- `src/backtest/` — 백테스트 실행 엔진
- `src/strategy/` — AI 전략 생성 (LangChain/LangGraph)
- `src/signals/` — 실시간 주문 신호 생성
- `src/news/` — 뉴스·공시 수집 및 분석

빌드 검증: python -m py_compile {파일}
