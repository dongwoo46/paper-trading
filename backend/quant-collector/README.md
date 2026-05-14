# quant-collector

데이터 수집 서비스. pykrx/yfinance로 OHLCV 및 투자자 수급 데이터를 수집하여 PostgreSQL에 저장한다.

## 실행

**API 서버 + 자동 배치 스케줄러 (APScheduler)**
```bash
uvicorn main:app --host 0.0.0.0 --port 8082
```
> APScheduler(OHLCV 배치, 투자자 수급 배치)는 FastAPI lifespan에 연결되어 있어 uvicorn 실행 시 자동으로 함께 시작된다.

개발 환경 (자동 재시작):
```bash
uvicorn main:app --host 0.0.0.0 --port 8082 --reload
```

**수동 1회 수집 (API 서버 없이)**
```bash
python main.py
```

## 의존성 설치

```bash
pip install -r requirements.txt
```

## 환경변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `PG_HOST` | `localhost` | PostgreSQL 호스트 |
| `PG_PORT` | `5432` | PostgreSQL 포트 |
| `PG_DATABASE` | `paper` | DB 이름 |
| `PG_USER` | `paper` | DB 유저 |
| `PG_PASSWORD` | `paper` | DB 비밀번호 |

## 주요 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/health` | 헬스 체크 |
| POST | `/collect/daily` | 일봉 OHLCV 수집 |
| POST | `/collect/weekly` | 주봉 OHLCV 수집 |
| GET | `/market/weekly/{symbol}` | 주봉 데이터 조회 |
| GET | `/investor-flow/{symbol}` | 투자자 수급 조회 |
| GET | `/short-selling/{symbol}` | 공매도 데이터 조회 |
| GET | `/program-trading/{symbol}` | 프로그램 매매 조회 |
| GET | `/foreign-holding/{symbol}` | 외국인 보유 조회 |
