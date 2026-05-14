# Step 3: 서비스 역할 이전 구현
Assigned agent: fullstack-dev

## Working Directory
.worktrees/quant-ai-service-role-refactor

## Goal
quant-research에 기술분석 파이프라인 전체(저장소, 배치, 엔드포인트)를 추가하고,
quant-ai에서 동일 기능을 제거하여 역할을 완전 분리한다.

## Success Criteria
- [Step A] quant-research 신규 파일 추가 → verify: `python -m py_compile` 통과
- [Step B] quant-research 라우터 등록 확인 → verify: FastAPI 앱 시작 시 `/research/results/{symbol}`, `/research/symbols`, `/research/run/{symbol}` 라우트 존재
- [Step C] quant-ai 파일 삭제 및 참조 정리 → verify: `python -m py_compile backend/quant-ai/src/interfaces/api/app.py` 통과
- [Step D] quant-ai 테스트 파일 정리 → verify: 삭제된 파일을 import하는 테스트 파일 없음

## Files to Read

### quant-ai (이전할 원본)
- `backend/quant-ai/src/infrastructure/db.py`
- `backend/quant-ai/src/chart_analysis/infrastructure/ohlcv_repository.py`
- `backend/quant-ai/src/chart_analysis/infrastructure/chart_analysis_repository.py`
- `backend/quant-ai/src/chart_analysis/infrastructure/analysis_request_queue_repository.py`
- `backend/quant-ai/src/chart_analysis/application/precompute_pipeline_service.py`
- `backend/quant-ai/src/chart_analysis/application/market_pipeline_triggers.py`
- `backend/quant-ai/src/chart_analysis/application/popular_symbols_refresh_service.py`
- `backend/quant-ai/src/chart_analysis/application/analyze_chart_service.py`
- `backend/quant-ai/src/chart_analysis/domain/ports.py`
- `backend/quant-ai/src/chart_analysis/interfaces/chart_analysis_router.py`
- `backend/quant-ai/src/interfaces/api/app.py`
- `backend/quant-ai/src/jobs/chart_analysis_schedule.py`

### quant-research (기존 구조 파악)
- `backend/quant-research/src/interfaces/api/app.py`
- `backend/quant-research/src/interfaces/api/research_router.py`
- `backend/quant-research/src/chart_analysis/domain/ports.py`

### Step 2에서 생성된 파일 (선행 의존)
- `backend/quant-research/src/infrastructure/db.py`
- `backend/quant-research/src/infrastructure/migration_runner.py`
- `backend/quant-research/migrations/001_chart_analysis_result.sql`
- `backend/quant-research/migrations/002_analysis_request_queue.sql`
- `backend/quant-research/migrations/003_popular_symbols.sql`

## Tasks

---

### Part A: quant-research 신규 파일 추가

#### A-1: 도메인 포트 보완
경로: `backend/quant-research/src/chart_analysis/domain/ports.py`

현재 ports.py가 비어 있거나 계산기 포트만 있을 수 있다. 아래 포트를 추가한다:
- `OhlcvRepository(Protocol)`: `find_window(symbol, window, interval) -> list[Candle]`
- `ChartAnalysisRepository(Protocol)`: `upsert(result)`, `find_by_symbol(symbol)`, `find_one(symbol, window, interval)`
- `AnalysisRequestQueueRepository(Protocol)`: `enqueue(symbol, window, interval)`, `fetch_pending(limit)`, `mark_processed(id, status)`
- `QueueItem` 데이터 클래스: `id, symbol, window, interval, status` 필드
- 기존 계산기 포트(`IndicatorCalculator`, `SupportResistanceFinder`, `PatternDetector`, `TrendClassifier`, `ConfidenceScorer`)는 이미 있으면 유지, 없으면 추가 불필요

#### A-2: 인프라 저장소 이전
아래 파일을 quant-ai 원본을 참조하여 quant-research에 동일하게 생성:

- `backend/quant-research/src/chart_analysis/infrastructure/ohlcv_repository.py`
  - 클래스: `PostgresOhlcvRepository`
  - import 경로 수정: `from src.chart_analysis.domain.value_objects import Candle`
  - quant-ai 원본과 동일 로직

- `backend/quant-research/src/chart_analysis/infrastructure/chart_analysis_repository.py`
  - 클래스: `PostgresChartAnalysisRepository`, `DecimalJSONEncoder`
  - import 경로 수정: `from src.chart_analysis.domain.*`
  - quant-ai 원본과 동일 로직

- `backend/quant-research/src/chart_analysis/infrastructure/analysis_request_queue_repository.py`
  - 클래스: `PostgresAnalysisRequestQueueRepository`
  - import 경로 수정: `from src.chart_analysis.domain.ports import QueueItem`
  - quant-ai 원본과 동일 로직

#### A-3: Application 서비스 이전
아래 파일을 quant-research에 생성:

- `backend/quant-research/src/chart_analysis/application/__init__.py` (빈 파일)

- `backend/quant-research/src/chart_analysis/application/precompute_pipeline_service.py`
  - 클래스: `PrecomputePipelineService`
  - import 경로 수정: `from src.chart_analysis.domain.*`
  - `run_for_symbol(symbol, is_popular)`, `run_for_market(market, symbols, popular_set)`
  - 배치 완료 후 `analysis_request_queue` enqueue 추가:
    - `run_for_symbol` 내부에서 popular 종목의 경우 처리 완료 후 `queue_repo.enqueue(symbol, window, interval)` 호출
    - `PrecomputePipelineService.__init__`에 `queue_repo` 파라미터 추가
  - quant-ai 원본의 나머지 로직 동일

- `backend/quant-research/src/chart_analysis/application/popular_symbols_refresh_service.py`
  - 클래스: `PopularSymbolsRefreshService`
  - quant-ai 원본과 동일 로직, import 경로만 수정

- `backend/quant-research/src/chart_analysis/application/market_pipeline_triggers.py`
  - 함수: `run_krx_chart_analysis_pipeline()`, `run_us_chart_analysis_pipeline()`, `run_weekly_chart_analysis_pipeline()`
  - `_get_pipeline_service()` 내부에서 `PostgresAnalysisRequestQueueRepository`도 주입
  - import 경로 수정: `from src.*`
  - quant-ai 원본 로직 유지, ResearchClient 의존성 제거 (quant-research는 계산기를 직접 보유)
    - `_get_pipeline_service()` 내 계산기: `PandasTaIndicatorCalculator`, `MaAdxTrendClassifier`, `RuleBasedPatternDetector`, `ScipyPeakSupportResistanceFinder`, `WeightedRuleConfidenceScorer` 직접 인스턴스화

#### A-4: 배치 스케줄러 이전
- `backend/quant-research/src/jobs/__init__.py` (빈 파일)

- `backend/quant-research/src/jobs/chart_analysis_schedule.py`
  - 함수: `start_chart_analysis_scheduler()`, `stop_chart_analysis_scheduler(scheduler)`
  - import 경로 수정: `from src.chart_analysis.application.market_pipeline_triggers import ...`
  - `popular_symbols_refresh` 잡 포함 (4개 잡 동일)
  - quant-ai 원본과 동일 스케줄 설정

#### A-5: 결과 조회 라우터 신규 생성
경로: `backend/quant-research/src/interfaces/api/results_router.py`

- `results_router = APIRouter(prefix="/research")`
- 의존성 팩토리 함수 `get_chart_analysis_repo()`:
  - `load_db_config_from_env()` + `connect()` 조합
  - `PostgresChartAnalysisRepository(connect_fn)` 반환

엔드포인트 명세:

1. `GET /research/results/{symbol}`
   - 파라미터: `symbol: str` (path)
   - 처리: `chart_analysis_repo.find_by_symbol(symbol.strip().upper())`
   - 응답: `{"symbol": str, "analyses": [...]}`
   - 결과 없으면 `{"symbol": str, "analyses": []}` 반환 (404 아님)
   - 응답 DTO: `ChartAnalysisResultDTO` — 기존 quant-ai `ChartAnalysisResponseDTO`와 동일 구조

2. `GET /research/symbols`
   - 처리: SQL `SELECT symbol, COUNT(*) AS windows, MAX(numeric_computed_at) AS last_analyzed FROM chart_analysis_result GROUP BY symbol ORDER BY symbol ASC`
   - 응답: `{"symbols": [{"symbol": str, "windows": int, "last_analyzed": str|null}]}`

3. `POST /research/run/{symbol}`
   - 처리: `_get_pipeline_service().run_for_symbol(symbol, is_popular=True)` (수동 트리거)
   - 응답: `{"symbol": str, "success": int, "failed": int, "skipped": int}`
   - 개발/디버그용. 빈 symbol이면 422

#### A-6: quant-research app.py 업데이트
경로: `backend/quant-research/src/interfaces/api/app.py`

- lifespan에 스케줄러 시작/종료 추가:
  ```
  lifespan 시작: run_migrations(connect_fn) → start_chart_analysis_scheduler()
  lifespan 종료: stop_chart_analysis_scheduler(scheduler)
  ```
- `results_router` import 및 `app.include_router(results_router)` 추가
- 기존 `research_router` include 유지

---

### Part B: quant-ai 파일 삭제 및 참조 정리

#### B-1: 파일 삭제
아래 파일을 삭제한다:
- `backend/quant-ai/src/chart_analysis/infrastructure/ohlcv_repository.py`
- `backend/quant-ai/src/chart_analysis/infrastructure/chart_analysis_repository.py`
- `backend/quant-ai/src/chart_analysis/infrastructure/analysis_request_queue_repository.py`
- `backend/quant-ai/src/chart_analysis/application/precompute_pipeline_service.py`
- `backend/quant-ai/src/chart_analysis/application/market_pipeline_triggers.py`
- `backend/quant-ai/src/chart_analysis/application/popular_symbols_refresh_service.py`
- `backend/quant-ai/src/chart_analysis/application/analyze_chart_service.py`
- `backend/quant-ai/src/jobs/chart_analysis_schedule.py`

#### B-2: quant-ai ports.py 정리
경로: `backend/quant-ai/src/chart_analysis/domain/ports.py`

- 삭제할 포트: `OhlcvRepository`, `ChartAnalysisRepository`, `AnalysisRequestQueueRepository`, `IndicatorCalculator`, `SupportResistanceFinder`, `PatternDetector`, `TrendClassifier`, `ConfidenceScorer`
- 유지할 항목: `LlmReportGenerator`, `QueueItem`
- 남은 value_object import만 유지 (미사용 import 제거)

#### B-3: quant-ai chart_analysis_router.py 정리
경로: `backend/quant-ai/src/chart_analysis/interfaces/chart_analysis_router.py`

- 삭제할 엔드포인트: `POST /chart-analysis/{symbol}` (analyze_chart 핸들러)
- 삭제할 의존성 팩토리: `get_analyze_chart_service()`
- 삭제할 import: `AnalyzeChartService`, `ChartAnalysisResponseDTO` (쓰지 않게 됨)
- 유지할 엔드포인트: `POST /chart-analysis/request-llm-report`, `POST /chart-analysis/{symbol}/report`
- 유지할 의존성 팩토리: `get_generate_report_service()`, `get_queue_repository()`

#### B-4: quant-ai app.py 정리
경로: `backend/quant-ai/src/interfaces/api/app.py`

- 삭제: APScheduler lifespan 훅 (`start_chart_analysis_scheduler`, `stop_chart_analysis_scheduler` 호출)
- lifespan을 단순 pass 또는 `@asynccontextmanager` 없이 기본 FastAPI로 전환 (스케줄러 없음)
- 삭제: `GET /admin/symbols` 핸들러
- 삭제: `POST /admin/run-analysis/{symbol}` 핸들러
- `chart_analysis_router` include는 유지 (SSE 스트림, 큐 등록 엔드포인트가 남음)

#### B-5: quant-ai 테스트 파일 정리
아래 테스트 파일들은 삭제된 파일을 import하므로 함께 삭제:
- `backend/quant-ai/tests/unit/chart_analysis/application/test_analyze_chart_service.py`
- `backend/quant-ai/tests/unit/chart_analysis/application/test_market_pipeline_triggers.py`
- `backend/quant-ai/tests/unit/chart_analysis/application/test_precompute_pipeline_service.py`
- `backend/quant-ai/tests/unit/chart_analysis/application/test_popular_symbols_refresh_service.py`
- `backend/quant-ai/tests/unit/chart_analysis/infrastructure/test_chart_analysis_repository.py`
- `backend/quant-ai/tests/unit/chart_analysis/infrastructure/test_ohlcv_repository.py`
- `backend/quant-ai/tests/unit/chart_analysis/infrastructure/test_request_queue_repository.py`
- `backend/quant-ai/tests/unit/chart_analysis/jobs/test_chart_analysis_schedule.py`

---

### Part C: 컴파일 검증

```
python -m py_compile backend/quant-research/src/infrastructure/db.py
python -m py_compile backend/quant-research/src/infrastructure/migration_runner.py
python -m py_compile backend/quant-research/src/chart_analysis/infrastructure/ohlcv_repository.py
python -m py_compile backend/quant-research/src/chart_analysis/infrastructure/chart_analysis_repository.py
python -m py_compile backend/quant-research/src/chart_analysis/infrastructure/analysis_request_queue_repository.py
python -m py_compile backend/quant-research/src/chart_analysis/application/precompute_pipeline_service.py
python -m py_compile backend/quant-research/src/chart_analysis/application/market_pipeline_triggers.py
python -m py_compile backend/quant-research/src/chart_analysis/application/popular_symbols_refresh_service.py
python -m py_compile backend/quant-research/src/jobs/chart_analysis_schedule.py
python -m py_compile backend/quant-research/src/interfaces/api/results_router.py
python -m py_compile backend/quant-research/src/interfaces/api/app.py
python -m py_compile backend/quant-ai/src/chart_analysis/domain/ports.py
python -m py_compile backend/quant-ai/src/chart_analysis/interfaces/chart_analysis_router.py
python -m py_compile backend/quant-ai/src/interfaces/api/app.py
```

## 규칙

- 금액/수치 컬럼: `Decimal` 사용, `float` 절대 금지
- 환경변수로만 DB/서비스 접속 정보 읽기
- 최소 구현: 현재 역할 분리 목적 외 코드 추가 금지
- quant-research의 계산기는 직접 인스턴스화 (`ResearchClient` 경유 금지 — 자기 자신이 계산 서비스)
- 모든 import는 quant-research 기준 `src.*` 경로로 수정

## Agent Return Protocol

When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <paths relative to worktree root>
- Test result: <compile check results summary>
- Blockers: <none | description>
---
