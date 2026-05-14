# service-role-refactor

## Core Feature

quant-ai / quant-research 서비스 역할을 명확히 분리한다.

- **quant-research**: 기술분석 수치 계산 + DB 저장 + 배치 스케줄링 + 결과 조회 담당
- **quant-ai**: LLM 자연어 해석(Ollama) 생성 + SSE 스트리밍 전용. `analysis_request_queue` 테이블을 소비하여 LLM 리포트를 생성한다.

## Considerations

- quant-ai에는 현재 배치 스케줄러(APScheduler), OHLCV/ChartAnalysis DB 조회, 파이프라인 오케스트레이션이 모두 존재하여 역할이 과중하다.
- `POST /chart-analysis/{symbol}` (수치 조회)가 quant-ai에 있으나 수치 데이터 소유권은 quant-research로 이전 후 해당 엔드포인트를 제거하고 quant-research의 `GET /research/results/{symbol}`로 대체한다.
- 배치 완료 후 LLM 트리거 연결은 DB 큐(`analysis_request_queue`) 경유 방식 — quant-research는 배치 완료 시 enqueue, quant-ai는 큐를 소비한다.
- 도메인 모델은 서비스별 독립 유지. quant-ai에서 계산기 포트(IndicatorCalculator 등 5종)를 삭제하고, 해당 역할은 quant-research 내부로 완전 이전한다.

## Trade-offs

| 결정 | 득 | 실 |
|---|---|---|
| 배치+DB를 quant-research로 이전 | 역할 명확, quant-ai 경량화 | quant-research 의존 추가 (psycopg, APScheduler) |
| DB 큐 경유 LLM 트리거 | 서비스 간 HTTP 의존 제거, 재시도 용이 | 큐 폴링 지연 발생 |
| 도메인 모델 독립 유지 | 결합도 최소화 | 양쪽에 유사 도메인 클래스 존재 |

## Implementation Approach

Step 2 (DB 스키마 자동 생성) → Step 3 (서비스 역할 이전 구현) 순서로 진행.

- Step 2: quant-research `lifespan`에서 `migrations/*.sql` 파일을 순서대로 실행하여 필요한 테이블을 `CREATE TABLE IF NOT EXISTS`로 멱등 생성
- Step 3: quant-research에 저장소/파이프라인/스케줄러/엔드포인트 추가; quant-ai에서 동일 코드 삭제

## Workflow

```
[quant-research]
  lifespan 시작 → migrations/*.sql 순서 실행 (Step 2)
  APScheduler → 배치 완료 → chart_analysis_result upsert + analysis_request_queue enqueue (Step 3)
  GET /research/results/{symbol} — 수치 결과 조회
  GET /research/symbols           — 분석 완료 종목 목록
  POST /research/run/{symbol}     — 수동 파이프라인 트리거 (개발/디버그용)

[quant-ai]
  큐 소비 루프: analysis_request_queue → LLM 생성 → chart_analysis_result.llm_report 갱신
  POST /chart-analysis/{symbol}/report  — SSE LLM 리포트 스트리밍 (유지)
  POST /chart-analysis/request-llm-report — 비인기 종목 큐 등록 (유지)
```

## API (추가/제거/이전 엔드포인트 명세)

### quant-research 추가

| Method | Path | 설명 |
|---|---|---|
| GET | `/research/results/{symbol}` | 종목의 7 윈도우 수치 분석 결과 반환 |
| GET | `/research/symbols` | 분석 완료 종목 목록 (symbol, windows, last_analyzed) |
| POST | `/research/run/{symbol}` | 단일 종목 파이프라인 수동 실행 (개발용) |

### quant-ai 제거

| Method | Path | 제거 이유 |
|---|---|---|
| POST | `/chart-analysis/{symbol}` | 수치 조회 역할 quant-research로 이전 |
| GET | `/admin/symbols` | 종목 목록 역할 quant-research로 이전 |
| POST | `/admin/run-analysis/{symbol}` | 파이프라인 트리거 역할 quant-research로 이전 |

### quant-ai 유지

| Method | Path | 설명 |
|---|---|---|
| POST | `/chart-analysis/{symbol}/report` | SSE LLM 리포트 스트리밍 |
| POST | `/chart-analysis/request-llm-report` | 비인기 종목 LLM 큐 등록 |

## DB (관련 테이블)

| 테이블 | 소유 서비스 | 설명 |
|---|---|---|
| `chart_analysis_result` | quant-research (쓰기), quant-ai (읽기/갱신) | 7 윈도우 수치 + LLM 리포트 |
| `analysis_request_queue` | quant-research (enqueue), quant-ai (소비) | LLM 생성 요청 큐 |
| `popular_symbols` | quant-research (쓰기+읽기) | 인기 종목 TOP N |
| `pykrx_symbol_catalog` | quant-worker (쓰기), quant-research (읽기) | KRX 종목 카탈로그 |
| `yfinance_symbol_catalog` | quant-worker (쓰기), quant-research (읽기) | US 종목 카탈로그 |

## 파일 이전 목록 (quant-ai → quant-research)

아래 파일들의 내용을 quant-research에 신규 생성한다. (quant-ai 원본은 Step 3에서 삭제)

| quant-ai 원본 경로 | quant-research 대상 경로 |
|---|---|
| `src/infrastructure/db.py` | `src/infrastructure/db.py` |
| `src/chart_analysis/infrastructure/ohlcv_repository.py` | `src/chart_analysis/infrastructure/ohlcv_repository.py` |
| `src/chart_analysis/infrastructure/chart_analysis_repository.py` | `src/chart_analysis/infrastructure/chart_analysis_repository.py` |
| `src/chart_analysis/infrastructure/analysis_request_queue_repository.py` | `src/chart_analysis/infrastructure/analysis_request_queue_repository.py` |
| `src/chart_analysis/application/precompute_pipeline_service.py` | `src/chart_analysis/application/precompute_pipeline_service.py` |
| `src/chart_analysis/application/market_pipeline_triggers.py` | `src/chart_analysis/application/market_pipeline_triggers.py` |
| `src/chart_analysis/application/popular_symbols_refresh_service.py` | `src/chart_analysis/application/popular_symbols_refresh_service.py` |
| `src/jobs/chart_analysis_schedule.py` | `src/jobs/chart_analysis_schedule.py` |

추가로 quant-research에 신규 생성 (quant-ai에 없음):
- `migrations/001_chart_analysis_result.sql`
- `migrations/002_analysis_request_queue.sql`
- `migrations/003_popular_symbols.sql`
- `src/interfaces/api/results_router.py` — GET /research/results/{symbol}, GET /research/symbols, POST /research/run/{symbol}

## 파일 삭제 목록 (quant-ai에서 제거)

| 경로 | 이유 |
|---|---|
| `src/chart_analysis/infrastructure/ohlcv_repository.py` | quant-research로 이전 |
| `src/chart_analysis/infrastructure/chart_analysis_repository.py` | quant-research로 이전 |
| `src/chart_analysis/infrastructure/analysis_request_queue_repository.py` | quant-research로 이전 |
| `src/chart_analysis/application/precompute_pipeline_service.py` | quant-research로 이전 |
| `src/chart_analysis/application/market_pipeline_triggers.py` | quant-research로 이전 |
| `src/chart_analysis/application/popular_symbols_refresh_service.py` | quant-research로 이전 |
| `src/jobs/chart_analysis_schedule.py` | quant-research로 이전 |
| `src/chart_analysis/application/analyze_chart_service.py` | 수치 조회 역할 제거 |

포트(ports.py) 정리:
- `src/chart_analysis/domain/ports.py`에서 제거할 포트: `OhlcvRepository`, `ChartAnalysisRepository`, `AnalysisRequestQueueRepository`, `IndicatorCalculator`, `SupportResistanceFinder`, `PatternDetector`, `TrendClassifier`, `ConfidenceScorer`
- 유지할 포트: `LlmReportGenerator`, `QueueItem`

app.py 정리:
- `src/interfaces/api/app.py`에서 제거: APScheduler lifespan 훅, `/admin/symbols`, `/admin/run-analysis/{symbol}` 엔드포인트
- `POST /chart-analysis/{symbol}` 라우트 제거 (chart_analysis_router.py에서)
