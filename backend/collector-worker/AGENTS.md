@../../AGENTS.md
@../AGENTS.md

# collector-worker 서비스 규칙

일별 시세 데이터 수집 워커. collector-api HTTP 요청으로 트리거, yfinance/pykrx로 수집 후 PostgreSQL 적재.
Python 3.11+ / FastAPI / SQLAlchemy
검증: python -m py_compile {파일} 또는 uvicorn api_main:app

---

## 디렉토리 구조

```
collector-worker/
├── src/
│   ├── fetch/
│   └── db/
├── scripts/
├── main.py
└── api_main.py
```

---

## Python 코딩 규칙

- 타입 힌트 필수: def fetch(...) -> list[OhlcvRow]:
- 클래스보다 함수형 스타일 선호. 상태가 필요하면 dataclass 사용
- 외부 라이브러리(yfinance, pykrx) 호출은 반드시 try-except로 감싼다

---

## 데이터 수집 규칙

- 수집 단위: 종목별 일별 OHLCV (open, high, low, close, volume)
- 증분 수집: catalog의 watermark 기준으로 마지막 수집일 이후 데이터만
- 전체 수집: 2010년부터 현재까지 (최초 1회)
- DB upsert: 중복 적재 방지를 위해 ON CONFLICT DO UPDATE 사용
- 수집 실패 시 source, symbol, 날짜 범위 포함한 에러 로그 필수

---

## .agents 관리

- 기능 완료 시 .agents/feature/{기능명}.md 작성. 수정 시 동일 파일 업데이트
- API 추가/수정 시 .agents/feature/README.md 즉시 갱신
- 버그/장애 발생 시 .agents/rules/에 재발 방지 기록
