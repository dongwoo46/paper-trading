@../../AGENTS.md
@../AGENTS.md

# collector-worker

일별 시세 데이터 수집 워커. collector-api HTTP 요청으로 트리거, yfinance/pykrx로 수집 후 PostgreSQL 적재.
Python 3.11+ / FastAPI / SQLAlchemy
검증: uvicorn api_main:app
