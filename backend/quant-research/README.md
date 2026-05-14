# quant-research

기술 분석 수치 계산 + DB 저장 + 배치 스케줄링 담당 서비스.

- OHLCV 수집 결과를 기반으로 지표/패턴/추세 계산
- 계산 결과를 `chart_analysis_result` 테이블에 저장
- LLM 보고서 생성 요청을 `analysis_request_queue`에 enqueue
- quant-ai가 큐를 소비하여 LLM 해석 생성

## 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/research/results/{symbol}` | 종목별 분석 결과 조회 |
| GET | `/research/symbols` | 분석된 종목 목록 조회 |
| POST | `/research/run/{symbol}` | 특정 종목 분석 즉시 실행 |
| GET | `/health` | 헬스체크 |

## 실행

```bash
uvicorn main:app --host 0.0.0.0 --port 8084
```

## 환경 변수

```
DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
```
