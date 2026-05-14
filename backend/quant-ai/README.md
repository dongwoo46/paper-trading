# quant-ai

차트 분석 AI 서비스. LangChain/Ollama 기반 LLM 리포트 생성, SSE 스트리밍, Redis 큐 관리를 담당한다.
기술분석 계산은 quant-research 서비스에 위임한다.

## 실행

**API 서버 + 자동 배치 스케줄러 (APScheduler)**
```bash
uvicorn main:app --host 0.0.0.0 --port 8083
```
> APScheduler(차트분석 배치, 인기종목 갱신)는 FastAPI lifespan에 연결되어 있어 uvicorn 실행 시 자동으로 함께 시작된다.

개발 환경 (자동 재시작):
```bash
uvicorn main:app --host 0.0.0.0 --port 8083 --reload
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
| `RESEARCH_SERVICE_URL` | `http://quant-research:8084` | quant-research 서비스 URL |
| `OLLAMA_BASE_URL` | - | Ollama 서버 URL |

## 주요 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/health` | 헬스 체크 |
| POST | `/chart-analysis/{symbol}` | 7개 윈도우 수치 분석 즉시 반환 |
| POST | `/chart-analysis/{symbol}/report` | LLM 리포트 SSE 스트림 |
| POST | `/chart-analysis/request-llm-report` | 비인기 종목 LLM 요청 큐 등록 |

## 서비스 의존성

- **quant-research** (`RESEARCH_SERVICE_URL`): 기술분석 계산 (indicator, pattern, trend, support/resistance)
- **PostgreSQL**: 분석 결과 저장/조회
- **Redis**: 분석 요청 큐, 작업 상태 관리
- **Ollama**: LLM 리포트 생성
