# Step 4: 테스트
Assigned agent: test-engineer

## Working Directory
.worktrees/quant-ai-service-role-refactor

## Goal
Step 2~3에서 변경된 파일 범위에 대한 단위 테스트를 작성하고 실행하여 핵심 기능을 검증한다.
전체 테스트 수트 실행 금지 — 변경 파일 범위만 대상.

## Files to Read

### quant-research 신규 파일 (테스트 대상)
- `backend/quant-research/src/infrastructure/db.py`
- `backend/quant-research/src/infrastructure/migration_runner.py`
- `backend/quant-research/src/chart_analysis/infrastructure/ohlcv_repository.py`
- `backend/quant-research/src/chart_analysis/infrastructure/chart_analysis_repository.py`
- `backend/quant-research/src/chart_analysis/infrastructure/analysis_request_queue_repository.py`
- `backend/quant-research/src/chart_analysis/application/precompute_pipeline_service.py`
- `backend/quant-research/src/chart_analysis/application/popular_symbols_refresh_service.py`
- `backend/quant-research/src/interfaces/api/results_router.py`
- `backend/quant-research/src/interfaces/api/app.py`

### quant-ai 변경 파일 (기존 테스트 삭제 확인)
- `backend/quant-ai/src/chart_analysis/domain/ports.py`
- `backend/quant-ai/src/chart_analysis/interfaces/chart_analysis_router.py`
- `backend/quant-ai/src/interfaces/api/app.py`

### 기존 테스트 참조
- `backend/quant-research/tests/unit/chart_analysis/infrastructure/test_confidence_scorer.py`
- `backend/quant-ai/tests/unit/chart_analysis/application/test_generate_report_service.py`

## Tasks

### Task 1: quant-research 신규 단위 테스트 작성

테스트 위치: `backend/quant-research/tests/unit/`

#### 1-1. `test_ohlcv_repository.py`
경로: `backend/quant-research/tests/unit/chart_analysis/infrastructure/test_ohlcv_repository.py`

테스트 케이스:
- `test_find_window_daily_returns_candles`: `connect_fn`을 mock하여 DB row 반환 시 `Candle` 목록 변환 검증
- `test_find_window_invalid_window_raises`: 잘못된 window → `ValueError`
- `test_find_window_invalid_interval_raises`: 잘못된 interval → `ValueError`
- `test_find_window_max_has_no_date_filter`: MAX window → SQL에 `from_date` 없음 검증

#### 1-2. `test_chart_analysis_repository.py`
경로: `backend/quant-research/tests/unit/chart_analysis/infrastructure/test_chart_analysis_repository.py`

테스트 케이스:
- `test_upsert_executes_sql`: mock `connect_fn`으로 `upsert()` 호출 시 SQL 실행 확인
- `test_find_by_symbol_returns_results`: mock cursor가 1개 row 반환 시 `ChartAnalysisResult` 목록 길이 1
- `test_find_one_returns_none_when_no_row`: cursor `fetchone()` → None 시 `find_one()` → None

#### 1-3. `test_analysis_request_queue_repository.py`
경로: `backend/quant-research/tests/unit/chart_analysis/infrastructure/test_analysis_request_queue_repository.py`

테스트 케이스:
- `test_enqueue_executes_upsert_sql`: mock으로 `enqueue()` → SQL 실행 확인
- `test_fetch_pending_returns_queue_items`: mock row → `QueueItem` 목록 변환 검증
- `test_mark_processed_updates_status`: mock으로 `mark_processed(id, "processed")` → UPDATE SQL 확인

#### 1-4. `test_precompute_pipeline_service.py`
경로: `backend/quant-research/tests/unit/chart_analysis/application/test_precompute_pipeline_service.py`

테스트 케이스:
- `test_run_for_symbol_skips_when_too_few_candles`: `ohlcv_repo.find_window` → 빈 list → skipped=1
- `test_run_for_symbol_skips_when_hash_unchanged`: 동일 hash → 수치 재계산 스킵 + `chart_repo.upsert` 1회
- `test_run_for_symbol_enqueues_popular`: `is_popular=True`, 해시 변경 시 `queue_repo.enqueue` 호출 확인
- `test_run_for_symbol_no_enqueue_when_not_popular`: `is_popular=False` → `queue_repo.enqueue` 미호출
- `test_run_for_market_calls_notify_after_all_symbols`: `run_for_market()` 완료 후 `slack_notifier.notify_batch_completed` 1회 호출

#### 1-5. `test_popular_symbols_refresh_service.py`
경로: `backend/quant-research/tests/unit/chart_analysis/application/test_popular_symbols_refresh_service.py`

테스트 케이스:
- `test_refresh_returns_zero_when_no_metrics`: metrics 없으면 0 반환
- `test_refresh_selects_top_n`: 5개 metrics, top_n=3 → 3개만 `replace_all`에 전달
- `test_refresh_uses_z_score_ranking`: market_cap이 높은 종목이 top에 선택됨을 검증

#### 1-6. `test_results_router.py`
경로: `backend/quant-research/tests/unit/interfaces/api/test_results_router.py`

`TestClient`(또는 `httpx.AsyncClient`) 사용. `app.dependency_overrides`로 `get_chart_analysis_repo` 교체.

테스트 케이스:
- `test_get_results_returns_empty_when_no_data`: mock repo → `find_by_symbol` → [] → 응답 `{"symbol": ..., "analyses": []}`
- `test_get_results_returns_analyses`: mock repo → `find_by_symbol` → [ChartAnalysisResult] → 응답에 `analyses` 1개
- `test_get_symbols_returns_symbol_list`: mock DB query → 종목 목록 반환 검증

### Task 2: quant-ai 변경 파일 대상 테스트 보강

#### 2-1. `test_chart_analysis_router_no_analyze_endpoint.py`
경로: `backend/quant-ai/tests/integration/chart_analysis/test_chart_analysis_router_no_analyze_endpoint.py`

테스트 케이스:
- `test_post_chart_analysis_symbol_not_found`: `POST /chart-analysis/{symbol}` → 404 또는 405 (라우트 제거됨)
- `test_post_request_llm_report_still_works`: `POST /chart-analysis/request-llm-report` → 정상 응답 (라우트 유지 확인)
- `test_admin_symbols_not_found`: `GET /admin/symbols` → 404 (라우트 제거됨)
- `test_admin_run_analysis_not_found`: `POST /admin/run-analysis/TEST` → 404 (라우트 제거됨)

### Task 3: 테스트 실행

```
# quant-research 신규 테스트
cd backend/quant-research
python -m pytest tests/unit/chart_analysis/infrastructure/test_ohlcv_repository.py -v
python -m pytest tests/unit/chart_analysis/infrastructure/test_chart_analysis_repository.py -v
python -m pytest tests/unit/chart_analysis/infrastructure/test_analysis_request_queue_repository.py -v
python -m pytest tests/unit/chart_analysis/application/test_precompute_pipeline_service.py -v
python -m pytest tests/unit/chart_analysis/application/test_popular_symbols_refresh_service.py -v
python -m pytest tests/unit/interfaces/api/test_results_router.py -v

# quant-ai 변경 파일 테스트
cd backend/quant-ai
python -m pytest tests/integration/chart_analysis/test_chart_analysis_router_no_analyze_endpoint.py -v
```

## 규칙

- 변경된 파일 범위만 테스트 (전체 수트 실행 금지)
- DB 연결 없는 순수 단위 테스트: `connect_fn` mock 처리
- 금융 수치 검증 시 `Decimal` 사용, float 비교 금지
- 각 테스트 함수는 독립 실행 가능하게 작성 (공유 상태 없음)
- TestClient는 `httpx` 또는 `fastapi.testclient.TestClient` 사용

## Agent Return Protocol

When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <paths relative to worktree root>
- Test result: <N passed, M failed — list failing tests if any>
- Blockers: <none | description>
---
