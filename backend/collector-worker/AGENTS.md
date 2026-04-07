@../../AGENTS.md

---

# collector-worker 서비스 규칙

> 공통 규칙은 루트 `AGENTS.md`를 참조한다. 이 파일은 collector-worker 전용 규칙이다.

## 서비스 개요

- **역할**: 일별 시세 데이터 수집 워커 — collector-api의 요청을 받아 yfinance, pykrx 등 외부 라이브러리로 데이터를 수집하고 PostgreSQL에 적재
- **언어/기술**: Python 3.11+ + FastAPI + SQLAlchemy
- **검증 명령**: `python -m py_compile {파일}` 또는 `uvicorn api_main:app`

## 디렉토리 구조

```
collector-worker/
├── src/              # 소스 코드
│   ├── fetch/        # 데이터 수집 로직 (yfinance, pykrx 어댑터)
│   └── db/           # DB 연결, upsert 로직
├── scripts/          # 수동 실행 스크립트
├── main.py           # 배치 실행 진입점
└── api_main.py       # FastAPI 서버 진입점
```

## Python 코딩 규칙

- 타입 힌트를 반드시 사용한다 (`def fetch(...) -> list[OhlcvRow]:`)
- 클래스보다 함수형 스타일 선호 (단, 상태가 필요하면 dataclass 사용)
- 외부 라이브러리(yfinance, pykrx) 호출은 반드시 try-except로 감싼다
- DB upsert는 중복 적재 방지를 위해 ON CONFLICT DO UPDATE 사용

## 데이터 수집 규칙

- 수집 단위: 종목별 일별 OHLCV (open, high, low, close, volume)
- 증분 수집(incremental): catalog의 watermark 기준으로 마지막 수집일 이후 데이터만 가져온다
- 전체 수집(full): 2010년부터 현재까지 전체 수집 (최초 1회)
- collector-api에서 HTTP 요청으로 수집을 트리거한다

## .agents 폴더 관리

- 기능 완료 시 `.agents/feature/{날짜}-{기능명}.md` 생성
- API 추가/수정 시 `.agents/feature/README.md` 즉시 갱신
- 버그/장애 발생 시 `.agents/rule/` 에 재발 방지 기록