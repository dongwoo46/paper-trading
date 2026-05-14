# Step 2: DB 스키마 자동 생성
Assigned agent: fullstack-dev

## Working Directory
.worktrees/quant-ai-service-role-refactor

## Goal
quant-research 서비스가 시작될 때 필요한 DB 테이블 3종을 자동으로 멱등 생성한다.
`CREATE TABLE IF NOT EXISTS`를 사용하므로 재시작 시 중복 오류 없음.

## Success Criteria
- [Step 1] migrations/001, 002, 003 SQL 파일 생성 → verify: 파일 존재 확인
- [Step 2] quant-research lifespan에 migration runner 추가 → verify: `python -m py_compile` 통과
- [Step 3] 수동 실행으로 테이블 생성 확인 → verify: psql `\dt` 또는 쿼리로 테이블 존재 확인

## Files to Read
- `backend/quant-research/src/interfaces/api/app.py` — lifespan 훅 추가 위치
- `backend/quant-ai/src/infrastructure/db.py` — DB 연결 방식 참고 (psycopg 기반)
- `backend/quant-ai/src/chart_analysis/infrastructure/chart_analysis_repository.py` — 테이블 컬럼 참조
- `backend/quant-ai/src/chart_analysis/infrastructure/analysis_request_queue_repository.py` — 테이블 컬럼 참조

## Tasks

### Task 1: migrations 디렉터리 및 SQL 파일 생성

경로: `backend/quant-research/migrations/`

**001_chart_analysis_result.sql**
```
테이블: chart_analysis_result
PK: (symbol, window, interval)
컬럼: symbol VARCHAR(20), window VARCHAR(10), interval VARCHAR(5),
      snapshot_hash VARCHAR(64), recommendation VARCHAR(20), confidence NUMERIC(6,4),
      levels JSONB, trend JSONB, patterns JSONB, indicator_signals JSONB, volume_analysis JSONB,
      llm_report JSONB, llm_report_source VARCHAR(20),
      numeric_computed_at TIMESTAMPTZ, llm_computed_at TIMESTAMPTZ,
      created_at TIMESTAMPTZ DEFAULT NOW(), updated_at TIMESTAMPTZ DEFAULT NOW()
UNIQUE CONSTRAINT: (symbol, window, interval)
```

**002_analysis_request_queue.sql**
```
테이블: analysis_request_queue
PK: id SERIAL
컬럼: symbol VARCHAR(20), window VARCHAR(10), interval VARCHAR(5),
      status VARCHAR(20) DEFAULT 'pending',
      requested_count INT DEFAULT 1,
      requested_at TIMESTAMPTZ DEFAULT NOW(),
      processed_at TIMESTAMPTZ
PARTIAL UNIQUE INDEX: (symbol, window, interval) WHERE status IN ('pending', 'processing')
```

**003_popular_symbols.sql**
```
테이블: popular_symbols
PK: id SERIAL
컬럼: symbol VARCHAR(20) UNIQUE, market VARCHAR(10),
      rank INT, market_cap NUMERIC(20,2), avg_volume NUMERIC(20,2),
      score NUMERIC(10,6), updated_at TIMESTAMPTZ DEFAULT NOW()
```

모든 SQL 파일은 `CREATE TABLE IF NOT EXISTS` 사용. INDEX도 `CREATE INDEX IF NOT EXISTS` 사용.

### Task 2: DB 연결 모듈 추가

경로: `backend/quant-research/src/infrastructure/db.py`

- `backend/quant-ai/src/infrastructure/db.py` 내용과 동일한 구조로 작성
- `DbConfig` dataclass, `load_db_config_from_env()`, `connect(config)` 함수
- 환경변수: `PG_HOST`, `PG_PORT`, `PG_DATABASE`, `PG_USER`, `PG_PASSWORD`

### Task 3: Migration Runner 작성

경로: `backend/quant-research/src/infrastructure/migration_runner.py`

- 함수 시그니처: `run_migrations(connect_fn) -> None`
- `backend/quant-research/migrations/` 디렉터리에서 `*.sql` 파일을 파일명 오름차순으로 정렬하여 순서대로 실행
- 각 SQL 파일을 읽어 `conn.execute(sql)` 실행 후 `conn.commit()`
- 실행 성공/실패를 `logging.getLogger(__name__)` 로 기록

### Task 4: quant-research lifespan에 migration runner 연결

경로: `backend/quant-research/src/interfaces/api/app.py`

- `@asynccontextmanager async def lifespan(app: FastAPI)` 추가
- lifespan yield 이전에 `run_migrations(connect_fn)` 호출
- `connect_fn`은 `load_db_config_from_env()` + `connect()` 조합
- 기존 `research_router` include는 유지

### Task 5: 컴파일 검증

아래 명령어로 각 파일 구문 오류 없음을 확인:
```
python -m py_compile backend/quant-research/src/infrastructure/db.py
python -m py_compile backend/quant-research/src/infrastructure/migration_runner.py
python -m py_compile backend/quant-research/src/interfaces/api/app.py
```

## 규칙

- `CREATE TABLE IF NOT EXISTS` — 멱등성 필수
- `CREATE INDEX IF NOT EXISTS` — 멱등성 필수
- float 사용 금지, 금액 컬럼은 `NUMERIC`
- 환경변수로만 DB 접속 정보 읽기, 하드코딩 금지
- 코드 삽입 없이 설계 명세만 따라 최소 구현

## Agent Return Protocol

When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <paths relative to worktree root>
- Test result: N/A (infrastructure-only step — compile check only)
- Blockers: <none | description>
---
