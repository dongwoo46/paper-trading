# Step 5: FastAPI + Application Services + SSE

Assigned agent: quant-dev

## Working Directory
.worktrees/quant-worker-chart-analysis-agent

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/spec.md (§5 Output Schema, §9 SSE)
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/decisions.md (§1, §2, §9, §10)
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/src/interfaces/api/app.py (기존 FastAPI 앱 패턴)
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/src/chart_analysis/domain/ (Step 2)
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/src/chart_analysis/infrastructure/ (Step 3, 4)

## Open Questions
없음.

## Confirmed Design Choices
- 3개 엔드포인트 (decisions §1, §9):
  - `POST /chart-analysis/{symbol}` → 수치 분석 7 윈도우 (DB hit 즉시 응답)
  - `POST /chart-analysis/{symbol}/report` → SSE 비동기 LLM 리포트
  - `POST /chart-analysis/request-llm-report` → 큐 등록
- 기존 `src/interfaces/api/app.py`에 router 포함 (별도 `chart_analysis_router.py` 모듈로 분리, `app.include_router(chart_analysis_router)`)
- DTO는 Pydantic `BaseModel` 사용, Decimal → string 직렬화
- SSE: `sse_starlette.EventSourceResponse` 또는 FastAPI `StreamingResponse(media_type="text/event-stream")`
- Redis Job 락(snapshot_hash) 사용해 동시 중복 요청 dedup (decisions §10)
- **Slack 알림**: `SlackWebhookNotifier` 인프라 어댑터 — LLM 리포트 생성 실패 시 호출. 환경변수: `SLACK_WEBHOOK_URL`, `SLACK_NOTIFICATIONS_ENABLED` (기존 quant-worker 패턴 동일)
- 디렉토리:
  - `backend/quant-worker/src/chart_analysis/application/`
  - `backend/quant-worker/src/chart_analysis/interfaces/`
  - `backend/quant-worker/src/chart_analysis/infrastructure/slack_notifier.py`

## Tasks

### Substep 5-1: DTO (Pydantic) — `interfaces/dto.py`
1. (TEST FIRST) `tests/unit/chart_analysis/interfaces/test_dto.py`
   - 도메인 `ChartAnalysisResult` → `AnalysisResponseDTO` 변환 (Decimal → str)
   - DTO 직렬화/역직렬화 결정성
2. `src/chart_analysis/interfaces/dto.py`
   - `AnalysisSummaryDTO`, `LevelsDTO`, `TrendDTO`, `PatternDTO`, `IndicatorSignalDTO`, `VolumeDTO`, `ReportStatusDTO`, `AnalysisWindowDTO`, `ChartAnalysisResponseDTO`
   - `RequestLlmReportRequestDTO`, `RequestLlmReportResponseDTO`
   - 변환 헬퍼: `from_domain(result: ChartAnalysisResult) -> AnalysisWindowDTO`

### Substep 5-2: `AnalyzeChartService` (Application)
1. (TEST FIRST) `tests/unit/chart_analysis/application/test_analyze_chart_service.py`
   - fake `ChartAnalysisRepository` 주입 → 7 windows 정렬 반환
   - DB row 없는 종목 요청 → `ChartAnalysisResponseDTO(analyses=[])` 또는 404 (spec 결정: 빈 배열 + symbol 그대로)
   - `report.status` 매핑: `llm_report != null` → "available", 큐 등록됨 → "pending", 그 외 → "none"
2. `src/chart_analysis/application/analyze_chart_service.py`
   - 생성자 주입: `ChartAnalysisRepository`, `AnalysisRequestQueueRepository`
   - 메서드: `get_analyses(symbol) -> ChartAnalysisResponseDTO`
   - 큐 상태 lookup으로 pending 매핑

### Substep 5-3: `GenerateReportService` (SSE)
1. (TEST FIRST) `tests/unit/chart_analysis/application/test_generate_report_service.py`
   - fake `ChartAnalysisRepository`, `LlmReportGenerator`, `RedisJobStore`, `SlackWebhookNotifier` 주입
   - 시나리오 1: DB에 LLM 리포트 존재 → `status: running` → `report` 이벤트 → `end`
   - 시나리오 2: 리포트 없음 + popular → 즉시 LLM 호출 → 결과 저장 → 이벤트 push
   - 시나리오 3: 리포트 없음 + 비인기 → `status: none` → 사용자가 별도 큐 등록
   - 시나리오 4: 동시 동일 snapshot_hash → 락 보유자만 LLM 호출, 다른 요청은 대기 후 결과 반환
   - 시나리오 5: LLM 호출 실패(예외) → 룰 템플릿 폴백 후 Slack 알림 전송 검증 (`slack_notifier.notify` 1회 호출)
2. `src/chart_analysis/application/generate_report_service.py`
   - 생성자: `ChartAnalysisRepository`, `LlmReportGenerator`, `RedisJobStore`, `popular_symbols` 조회 어댑터, `SlackWebhookNotifier`
   - 메서드: `async def stream(symbol) -> AsyncIterator[SseEvent]` (yield event_type + data)
   - 락: `redis_job_store.acquire_lock(snapshot_hash, ttl=600)` → `try/finally release_lock`
   - LLM 성공 시: `slack_notifier.notify_analysis_success(symbol, window, source='llm_primary')` 호출
   - LLM 예외 발생 시: 룰 템플릿 폴백 + `slack_notifier.notify_analysis_failure(symbol, window, error)` 호출 (SLACK_NOTIFICATIONS_ENABLED=false 면 no-op)

### Substep 5-4: Endpoint — `POST /chart-analysis/{symbol}` (수치 응답)
1. (TEST FIRST) `tests/integration/chart_analysis/test_chart_analysis_endpoint.py`
   - `httpx.AsyncClient + ASGI transport` 사용
   - 정상: 200 + JSON 스키마 검증 (analyses 배열 길이 7, 각 window 필드 존재)
   - 빈 symbol: 400
   - DB row 0건: 200 + `analyses=[]`
2. `src/chart_analysis/interfaces/chart_analysis_router.py`
   - `APIRouter(prefix="/chart-analysis", tags=["chart_analysis"])`
   - 핸들러: `analyze_chart(symbol: str)` — `AnalyzeChartService.get_analyses` 호출 → DTO 반환
   - Dependency Injection: FastAPI `Depends(...)` 또는 모듈 레벨 factory

### Substep 5-5: Endpoint — `POST /chart-analysis/{symbol}/report` (SSE)
1. (TEST FIRST) `tests/integration/chart_analysis/test_chart_analysis_sse.py`
   - SSE 응답 라인 파싱 → 이벤트 순서 검증 (`status` → `report` → `end`)
   - 리포트 부재 시 `status: none` → `end`
2. router에 핸들러 추가:
   - `report_chart(symbol: str)` → `StreamingResponse(generator, media_type="text/event-stream")`
   - generator는 `GenerateReportService.stream(symbol)` yield → `event: <type>\ndata: <json>\n\n` 포맷

### Substep 5-6: Endpoint — `POST /chart-analysis/request-llm-report`
1. (TEST FIRST) `tests/integration/chart_analysis/test_request_llm_report_endpoint.py`
   - 새 요청 → 201 + queue_position
   - 중복 요청 (같은 symbol/window/interval, status=pending) → 200 + requested_count 증가
   - 잘못된 window 값 → 400
2. router에 핸들러 추가:
   - 입력 DTO: `RequestLlmReportRequestDTO(symbol, window, interval)`
   - `AnalysisRequestQueueRepository.enqueue(...)` 호출
   - 응답: `{ "status": "queued", "queue_position": int, "estimated_wait": "24h" }`

### Substep 5-8: `SlackWebhookNotifier` 어댑터
1. (TEST FIRST) `tests/unit/chart_analysis/infrastructure/test_slack_notifier.py`
   - `httpx.AsyncClient` mock 주입
   - `SLACK_NOTIFICATIONS_ENABLED=false` → webhook POST 호출 안 함 (no-op)
   - `notify_analysis_failure(symbol, window, error)` → POST body에 symbol/window/error 포함
   - `notify_analysis_success(symbol, window, source)` → POST body에 symbol/window/source(llm_primary|rule_template) 포함
   - `notify_batch_completed(market, success_count, failed_count)` → POST body에 결과 요약 포함
   - HTTP 오류(4xx/5xx) → 예외 비전파 (로그만)
2. `src/chart_analysis/infrastructure/slack_notifier.py`
   - 클래스: `SlackWebhookNotifier`
   - 환경변수: `SLACK_WEBHOOK_URL`, `SLACK_NOTIFICATIONS_ENABLED` (기존 quant-worker 패턴)
   - 메서드: `notify_analysis_failure(symbol, window, error)`, `notify_analysis_success(symbol, window, source)`, `notify_batch_completed(market, success_count, failed_count)`
   - 실패 시 로그만 기록, 예외 전파 금지 (알림 실패가 메인 플로우를 막으면 안 됨)
   - `src/chart_analysis/infrastructure/__init__.py`에 re-export 추가

### Substep 5-7: FastAPI 통합 + lifespan
1. `src/interfaces/api/app.py` 수정:
   - `from src.chart_analysis.interfaces.chart_analysis_router import router as chart_analysis_router`
   - `app.include_router(chart_analysis_router)`
2. (TEST FIRST) `tests/integration/chart_analysis/test_app_includes_router.py`
   - `app.routes` 에서 `/chart-analysis/{symbol}` 존재 확인
3. `python -m py_compile src/interfaces/api/app.py` 통과

## Acceptance Criteria
- 3 엔드포인트 통합 테스트 통과 (httpx ASGI)
- DTO 단위 테스트 통과
- AnalyzeChartService / GenerateReportService 단위 테스트 통과
- SSE 응답이 `text/event-stream` Content-Type + 올바른 이벤트 시퀀스
- 동시 중복 요청 dedup (Redis 락) 단위 테스트로 입증
- 모든 응답 JSON에서 가격/confidence string 직렬화
- `python -m py_compile` 통과
- 커밋 메시지 한국어

## Agent Return Protocol
---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <한두 문장>
- Files modified: <목록>
- Test result: <pytest 결과>
- Blockers: <none | description>
---
