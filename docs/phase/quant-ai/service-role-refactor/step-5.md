# Step 5: 코드 리뷰
Assigned agent: code-reviewer

## Working Directory
.worktrees/quant-ai-service-role-refactor

## Goal
Step 2~4에서 변경된 파일 범위를 검토하여 보안, 정확성, 역할 분리 준수 여부를 확인한다.
전체 코드베이스 리뷰 금지 — 변경 파일 범위만 대상.

## Files to Read

### quant-research 신규 파일
- `backend/quant-research/src/infrastructure/db.py`
- `backend/quant-research/src/infrastructure/migration_runner.py`
- `backend/quant-research/migrations/001_chart_analysis_result.sql`
- `backend/quant-research/migrations/002_analysis_request_queue.sql`
- `backend/quant-research/migrations/003_popular_symbols.sql`
- `backend/quant-research/src/chart_analysis/domain/ports.py`
- `backend/quant-research/src/chart_analysis/infrastructure/ohlcv_repository.py`
- `backend/quant-research/src/chart_analysis/infrastructure/chart_analysis_repository.py`
- `backend/quant-research/src/chart_analysis/infrastructure/analysis_request_queue_repository.py`
- `backend/quant-research/src/chart_analysis/application/precompute_pipeline_service.py`
- `backend/quant-research/src/chart_analysis/application/market_pipeline_triggers.py`
- `backend/quant-research/src/chart_analysis/application/popular_symbols_refresh_service.py`
- `backend/quant-research/src/jobs/chart_analysis_schedule.py`
- `backend/quant-research/src/interfaces/api/results_router.py`
- `backend/quant-research/src/interfaces/api/app.py`

### quant-ai 변경 파일
- `backend/quant-ai/src/chart_analysis/domain/ports.py`
- `backend/quant-ai/src/chart_analysis/interfaces/chart_analysis_router.py`
- `backend/quant-ai/src/interfaces/api/app.py`

### 테스트 파일
- `backend/quant-research/tests/unit/chart_analysis/infrastructure/test_ohlcv_repository.py`
- `backend/quant-research/tests/unit/chart_analysis/infrastructure/test_chart_analysis_repository.py`
- `backend/quant-research/tests/unit/chart_analysis/infrastructure/test_analysis_request_queue_repository.py`
- `backend/quant-research/tests/unit/chart_analysis/application/test_precompute_pipeline_service.py`
- `backend/quant-research/tests/unit/chart_analysis/application/test_popular_symbols_refresh_service.py`
- `backend/quant-research/tests/unit/interfaces/api/test_results_router.py`
- `backend/quant-ai/tests/integration/chart_analysis/test_chart_analysis_router_no_analyze_endpoint.py`

## Review Checklist

### 1. 보안
- [ ] 환경변수로만 DB 접속 정보 읽기 (하드코딩 없음)
- [ ] 비밀 정보 로그 출력 없음
- [ ] SQL injection 위험 없음 (psycopg parameterized query 사용)

### 2. 금융 안전
- [ ] 금액/수치 처리에 `float` 사용 없음 (`Decimal` 또는 `NUMERIC` 사용)
- [ ] DB 컬럼 `NUMERIC` 타입 사용 확인 (금액 관련)
- [ ] 금액 비교 시 `Decimal` 사용

### 3. 역할 분리 준수
- [ ] quant-ai app.py에서 APScheduler 코드 완전 제거됨
- [ ] quant-ai에서 `POST /chart-analysis/{symbol}`, `GET /admin/symbols`, `POST /admin/run-analysis/{symbol}` 라우트 없음
- [ ] quant-ai ports.py에서 계산기 포트 제거됨 (`IndicatorCalculator` 등 5종)
- [ ] quant-research의 `market_pipeline_triggers.py`에서 `ResearchClient` 직접 사용 없음 (계산기 직접 인스턴스화)
- [ ] quant-research의 `precompute_pipeline_service.py`가 배치 완료 후 `analysis_request_queue`에 enqueue하는 코드 포함

### 4. 멱등성
- [ ] migrations SQL이 `CREATE TABLE IF NOT EXISTS` 사용
- [ ] migrations SQL index가 `CREATE INDEX IF NOT EXISTS` 사용
- [ ] `migration_runner.py`가 파일명 오름차순 정렬로 실행

### 5. API 정확성
- [ ] `GET /research/results/{symbol}` 응답에 빈 결과 시 404 아닌 200 반환 확인
- [ ] `GET /research/symbols` SQL이 `GROUP BY symbol` 사용
- [ ] `POST /research/run/{symbol}` 빈 symbol 처리 (422)

### 6. 테스트 품질
- [ ] DB 연결 없는 단위 테스트 (mock 사용)
- [ ] `test_run_for_symbol_enqueues_popular` — is_popular=True 시 enqueue 호출 확인
- [ ] `test_run_for_symbol_no_enqueue_when_not_popular` — is_popular=False 시 enqueue 미호출 확인
- [ ] quant-ai 삭제된 엔드포인트 404/405 검증 테스트 존재

## 출력 형식

아래 섹션을 포함하여 리뷰 결과를 작성:

### Must Fix (필수 수정)
각 항목: `[파일 경로] 문제 설명 — 수정 방법`

### Should Fix (권장 수정)
각 항목: `[파일 경로] 문제 설명 — 수정 방법`

### LGTM
특이사항 없으면 "모든 검토 항목 통과" 기재

## Agent Return Protocol

When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <none — review only>
- Test result: N/A (review step)
- Blockers: <none | Must Fix items if any>
---
