# Step 4: 영속화 + LLM 어댑터 + Redis (Persistence + LLM Adapter)

Assigned agent: quant-dev

## Working Directory
.worktrees/quant-worker-chart-analysis-agent

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/spec.md (§6 LLM, §8 DB Schema, §9 SSE)
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/decisions.md (§5, §10, §12)
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/src/repositories/market_daily_ohlcv_repository.py (기존 repository 패턴)
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/src/repositories/market_weekly_ohlcv_repository.py
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/src/migrations/V1__create_investor_flow_tables.sql (마이그레이션 패턴)
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/src/chart_analysis/domain/ (Step 2 산출물)

## Open Questions
없음.

## Confirmed Design Choices
- 3 신규 테이블: `chart_analysis_result`, `analysis_request_queue`, `popular_symbols` (decisions §12)
- 마이그레이션 파일: `src/migrations/V2__create_chart_analysis_tables.sql`
- LLM provider: LangChain Ollama (`qwen2.5:7b`, base_url=$OLLAMA_BASE_URL) — decisions §5
- LangChain `PydanticOutputParser` 사용, 5섹션 스키마 — decisions §5
- 폴백: 룰 기반 템플릿 (모델 폴백 미사용) — decisions §5
- 13초 LLM 타임아웃 + 1회 스키마 위반 재시도 — decisions §5
- Redis Job 저장 TTL 600s, 캐시 TTL 3600s — decisions §10
- 모든 수치 필드 NUMERIC, 도메인 ↔ DB 변환 시 Decimal 보존 — decisions §18
- `psycopg` 연결은 기존 패턴 따름 (`src.catalog.postgres_symbol_catalog.connect`)
- 디렉토리: `backend/quant-worker/src/chart_analysis/infrastructure/`

## Tasks

### Substep 4-1: DB 마이그레이션 (V2)
1. `src/migrations/V2__create_chart_analysis_tables.sql` 작성:
   - `chart_analysis_result` (PK `(symbol, window, interval)`, JSONB 컬럼 6개, NUMERIC(4,3) confidence)
   - `analysis_request_queue` (BIGSERIAL PK, 부분 UNIQUE 인덱스)
   - `popular_symbols` (PK `symbol`, score NUMERIC(10,6))
   - 인덱스: `idx_chart_analysis_symbol`, `idx_chart_analysis_numeric_computed_at`, `idx_request_queue_status_requested_at`
2. 마이그레이션 적용 헬퍼는 기존 패턴(`docker exec postgres psql ...`) 또는 `scripts/apply_migrations.py` 신규 시 작성
3. (TEST FIRST) `tests/integration/chart_analysis/test_migration_v2.py` (마커 `integration`): 테이블 3개 존재 + 컬럼 시그니처 검증

### Substep 4-2: `OhlcvRepository` 어댑터
1. (TEST FIRST) `tests/unit/chart_analysis/infrastructure/test_ohlcv_repository.py`
   - psycopg cursor mock → window→날짜 변환 검증 (1M=30일, 3M=90일, 6M=180일, 1Y=365일, 2Y=730일, MAX=NULL)
   - interval=D → `market_daily_ohlcv`, W → `market_weekly_ohlcv` 분기
   - DB row → list[Candle] (Decimal 변환)
2. `src/chart_analysis/infrastructure/ohlcv_repository.py`
   - 클래스: `PostgresOhlcvRepository(OhlcvRepository)`
   - 메서드: `find_window(symbol, window, interval) -> list[Candle]`
   - 윈도우 → from_date 매핑 상수 + 쿼리

### Substep 4-3: `ChartAnalysisRepository` 어댑터
1. (TEST FIRST) `tests/unit/chart_analysis/infrastructure/test_chart_analysis_repository.py`
   - `upsert(result)` → INSERT ... ON CONFLICT UPDATE (PK 충돌 시 갱신)
   - JSONB 직렬화: Decimal → str (커스텀 json encoder)
   - `find_by_symbol(symbol)` → 7 windows 반환 순서: 1M-D, 3M-D, 6M-D, 1Y-D, 1Y-W, 2Y-W, MAX-W
2. `src/chart_analysis/infrastructure/chart_analysis_repository.py`
   - 클래스: `PostgresChartAnalysisRepository(ChartAnalysisRepository)`
   - 커스텀 `DecimalJSONEncoder` (Decimal → str)
   - 모든 메서드: spec §11-3 Ports 시그니처와 일치

### Substep 4-4: `AnalysisRequestQueueRepository` 어댑터
1. (TEST FIRST) `tests/unit/chart_analysis/infrastructure/test_request_queue_repository.py`
   - 동일 (symbol, window, interval) 재요청 → INSERT 대신 `requested_count += 1`
   - `fetch_pending(limit)` → status='pending' FIFO 정렬
   - `mark_processed(id, status='completed'|'failed')`
2. `src/chart_analysis/infrastructure/analysis_request_queue_repository.py`
   - 클래스: `PostgresAnalysisRequestQueueRepository`
   - SQL: `INSERT ... ON CONFLICT (symbol, window, interval) WHERE status IN ('pending','processing') DO UPDATE SET requested_count = requested_count + 1`

### Substep 4-5: `RuleTemplateReportGenerator` (폴백)
1. (TEST FIRST) `tests/unit/chart_analysis/infrastructure/test_rule_template_report_generator.py`
   - 입력 수치 분석 → 5섹션 모두 비어있지 않음
   - 출력 `NarrativeReport.source = RULE_TEMPLATE`
   - 결정론적 (같은 입력 → 같은 출력)
   - 단정 어조 ("한다") 검증, 절대 "확실히/100%" 어휘 없음
2. `src/chart_analysis/infrastructure/rule_template_report_generator.py`
   - 클래스: `RuleTemplateReportGenerator(LlmReportGenerator)`
   - 한국어 템플릿 5개 (trend/levels/entry/signal/risk)
   - 가격/지표 값 string.format 으로 치환

### Substep 4-6: `LangChainOllamaReportGenerator` (메인)
1. (TEST FIRST) `tests/unit/chart_analysis/infrastructure/test_langchain_ollama_report_generator.py`
   - `langchain_ollama.ChatOllama`을 mock으로 주입
   - 정상 응답 (Pydantic 스키마 일치) → `NarrativeReport(source=LLM_PRIMARY)`
   - 스키마 위반 → 1회 재시도 → 여전히 실패 → 룰 템플릿 폴백 호출
   - 타임아웃 → 룰 템플릿 폴백 호출
2. `src/chart_analysis/infrastructure/langchain_ollama_report_generator.py`
   - 클래스: `LangChainOllamaReportGenerator(LlmReportGenerator)`
   - 의존성: 폴백 generator 주입 (`RuleTemplateReportGenerator`)
   - 컴포넌트: `ChatOllama` + `PydanticOutputParser(NarrativeReportSchema)` + `ChatPromptTemplate`
   - `NarrativeReportSchema(BaseModel)`: 5섹션 string 필드 (Pydantic — 도메인 `NarrativeReport`와 분리, 어댑터 내부에서만 사용)
   - 프롬프트:
     - system: 한국어 평문 ~한다 어조, 단정 금지, 초보자 친화, 500-800자, 5섹션 JSON 출력
     - human: 수치 분석 JSON dump (모든 Decimal string)
   - 환경변수: `CHART_ANALYSIS_LLM_MODEL`, `OLLAMA_BASE_URL`, `CHART_ANALYSIS_LLM_INNER_TIMEOUT_S`

### Substep 4-7: `RedisJobStore` (SSE Job 상태)
1. (TEST FIRST) `tests/unit/chart_analysis/infrastructure/test_redis_job_store.py`
   - `redis.Redis` mock 주입
   - `set_status(job_id, stage)` → TTL 적용
   - `get_status(job_id)` → 없으면 None
   - `acquire_lock(snapshot_hash, ttl)` → SETNX 동작
2. `src/chart_analysis/infrastructure/redis_job_store.py`
   - 클래스: `RedisJobStore`
   - 메서드: `set_status`, `get_status`, `acquire_lock(snapshot_hash, ttl) -> bool`, `release_lock`
   - 환경변수: `REDIS_HOST`, `REDIS_PORT`, `CHART_ANALYSIS_JOB_TTL_S`

### Substep 4-8: 모듈 초기화 + 검증
1. `src/chart_analysis/infrastructure/__init__.py` 확장 (Step 3 + Step 4 어댑터 re-export)
2. `requirements.txt` 추가 확인: `langchain==0.3.7`, `langchain-ollama==0.2.0`, `langchain-core==0.3.15`, `pydantic==2.9.2`, `redis==5.1.1`, `sqlalchemy==2.0.36` (선택)
3. `python -m py_compile` 모든 새 파일
4. 단위 테스트 100% 통과

## Acceptance Criteria
- 마이그레이션 V2 실행 후 3 테이블 + 인덱스 존재
- 4개 repository(OHLCV/result/queue + 마이그레이션 자체) + 2개 LLM generator + 1 Redis store 구현
- LLM 어댑터 타임아웃 시 룰 템플릿 폴백 동작 단위 테스트로 입증
- 도메인 Port와 시그니처 100% 호환
- 모든 가격/confidence Decimal 처리 (DB ↔ 도메인 양방향)
- `python -m py_compile` 통과, 단위 테스트 통과
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
