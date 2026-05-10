# Step 2 — TDD 구현

- 담당: quant-dev
- 상태: pending
- 브랜치: feature/quant-worker-investor-flow-pipeline
- 워크트리: .worktrees/quant-worker-investor-flow-pipeline

---

## 전제 조건

- spec.md 숙지 후 시작
- 모든 구현은 TDD: 테스트 작성(Red) → 구현(Green) → 정리(Refactor)
- 금액 컬럼은 반드시 Decimal 사용 (float 금지)
- 각 substep 완료 후 `python -m py_compile {파일}` 컴파일 검증 수행

---

## Substep 정의

### Substep 2-1 — DB 마이그레이션 (테이블 생성)

작업 위치: `backend/quant-worker/`

파일: `src/migrations/V{next_version}__create_investor_flow_tables.sql`

처리:
- 4개 테이블 생성: investor_flow, short_selling, program_trading, foreign_holding
- 각 테이블의 UNIQUE 제약 (trade_date, symbol, market) 정의
- 인덱스 정의: (symbol, trade_date DESC), (trade_date, market)
- 금액 컬럼 타입: NUMERIC(20, 0)
- collected_at: TIMESTAMPTZ NOT NULL DEFAULT NOW()
- 테이블 이미 존재하면 무시 (CREATE TABLE IF NOT EXISTS)

검증: SQL 파일 구문 오류 없음 (psql --dry-run 또는 sqlfluff)

---

### Substep 2-2 — InvestorFlowCollector 구현

파일: `src/collectors/investor_flow_collector.py`

클래스: `InvestorFlowCollector`

메서드별 책임:
- `fetch_investor_flow(date: str, market: str) -> list[dict]`
  - pykrx `get_market_trading_volume_by_investor(date, date, market, detail=True)` 호출
  - DataFrame 컬럼을 spec.md의 표준 컬럼명으로 매핑
  - 금액 컬럼(원화): Decimal 변환 (float 경유 금지, pandas int64 → Decimal)
  - 빈 DataFrame 시 빈 목록 반환
- `fetch_short_selling(date: str, market: str) -> list[dict]`
  - pykrx 공매도 API 호출, 동일 변환 규칙
  - short_sell_ratio: Decimal 변환
- `fetch_program_trading(date: str, market: str) -> list[dict]`
  - pykrx 프로그램 매매 API 호출
- `fetch_foreign_holding(date: str, market: str) -> list[dict]`
  - pykrx 외국인 보유현황 API 호출
  - exhaustion_ratio: Decimal 변환

테스트 파일: `tests/collectors/test_investor_flow_collector.py`

테스트 대상:
- pykrx를 Mock 처리하여 DataFrame 픽스처 주입
- 정상 DataFrame → 딕셔너리 목록 변환 검증
- 금액 컬럼이 Decimal 타입인지 확인
- 빈 DataFrame → 빈 목록 반환 확인
- 컬럼명 매핑 정확성 확인 (pykrx 컬럼명 → 표준 컬럼명)

---

### Substep 2-3 — InvestorFlowRepository 구현

파일: `src/repositories/investor_flow_repository.py`

클래스: `InvestorFlowRepository`

메서드별 책임:
- `upsert_investor_flow(records: list[dict], conn) -> int`
  - INSERT ... ON CONFLICT (trade_date, symbol, market) DO UPDATE SET ...
  - 반환: upsert된 행 수
- `upsert_short_selling(records: list[dict], conn) -> int`
- `upsert_program_trading(records: list[dict], conn) -> int`
- `upsert_foreign_holding(records: list[dict], conn) -> int`
- `find_investor_flow(symbol, from_date, to_date, limit, conn) -> list[dict]`
  - ORDER BY trade_date ASC LIMIT limit
- `find_short_selling(symbol, from_date, to_date, limit, conn) -> list[dict]`
- `find_program_trading(symbol, from_date, to_date, limit, conn) -> list[dict]`
- `find_foreign_holding(symbol, from_date, to_date, limit, conn) -> list[dict]`

테스트 파일: `tests/repositories/test_investor_flow_repository.py`

테스트 대상:
- psycopg2 conn을 Mock 처리
- upsert 호출 시 execute 인자 검증 (SQL 문자열, 파라미터 바인딩)
- 중복 레코드 upsert 시 DO UPDATE 경로 확인
- find 메서드의 파라미터 바인딩 및 결과 변환 확인
- Decimal 컬럼이 변환 없이 그대로 전달되는지 확인

---

### Substep 2-4 — InvestorFlowFetchService 구현

파일: `src/application/investor_flow_fetch_service.py`

클래스: `InvestorFlowFetchService`

책임:
- Collector와 Repository를 조합하여 단일 날짜·시장의 4개 데이터셋을 수집하고 저장
- 직전 영업일 산출 로직 포함 (`_resolve_business_date(target_date: date | None) -> date`)
  - target_date가 None이면 직전 영업일 반환
  - 영업일 기준: 월-금, 공휴일 제외 없음 (단순 요일 기준으로 1차 구현)
- DB 연결은 환경변수로부터 생성 (하드코딩 금지)

메서드:
- `execute(date: date | None, market: str) -> dict`
  - 반환 필드: date, market, investor_flow_rows, short_selling_rows, program_trading_rows, foreign_holding_rows, elapsed_ms, errors

테스트 파일: `tests/application/test_investor_flow_fetch_service.py`

테스트 대상:
- Collector와 Repository를 Mock 처리
- date=None 시 직전 영업일 산출 확인
- 각 데이터셋 수집·저장 호출 순서 검증
- Collector 예외 시 errors 필드에 기록되고 나머지 데이터셋은 계속 진행
- 반환 딕셔너리의 모든 필드 존재 확인

---

### Substep 2-5 — investor_flow_schedule.py 구현

파일: `src/jobs/investor_flow_schedule.py`

책임:
- APScheduler BackgroundScheduler 독립 관리
- 2개 배치 정의 (kospi, kosdaq)
- 실행 시각: 19:00 KST 월-금
- 환경변수 `INVESTOR_FLOW_SCHEDULE_ENABLED` (기본: false)로 활성화
- 재시도 정책: batch_schedule.py의 `compute_retry_delay_seconds` 재사용
- Slack 알림: batch_schedule.py의 `notify_slack` 재사용 (BatchRunResult 포맷 호환)
- 스케줄 오버라이드 환경변수: `INVESTOR_FLOW_CRON_KOSPI`, `INVESTOR_FLOW_CRON_KOSDAQ`

외부 노출 함수:
- `start_investor_flow_scheduler() -> object | None`
- `stop_investor_flow_scheduler(scheduler: object | None) -> None`

app.py 수정 내용 (최소):
- lifespan 함수에서 investor_flow_scheduler 시작/종료 추가
- 기존 batch_scheduler 코드 변경 없음

테스트 파일: `tests/jobs/test_investor_flow_schedule.py`

테스트 대상:
- `INVESTOR_FLOW_SCHEDULE_ENABLED=false` 시 None 반환 확인
- build_investor_flow_batch_definitions() 가 2개 정의 반환 확인
- 각 배치 정의의 market, hour, day_of_week 값 확인
- 스케줄 오버라이드 환경변수 반영 확인

---

### Substep 2-6 — REST API 엔드포인트 추가

파일: `src/interfaces/api/app.py` (기존 파일 수정)

추가 내용:
- 4개 GET 엔드포인트: `/investor-flow/{symbol}`, `/short-selling/{symbol}`, `/program-trading/{symbol}`, `/foreign-holding/{symbol}`
- 각 엔드포인트의 파라미터 정의: symbol(path), from(query alias), to(query), market(query), limit(query)
- 입력 검증: symbol 공백 → 400, from > to → 400, limit 범위 클램핑(1~1000)
- 금액 컬럼 Decimal → str 직렬화 (float 금지)
- 데이터 없으면 빈 배열 반환 (404 아님)

Pydantic Response 모델 정의 (app.py 내부 또는 별도 schemas 파일):
- `InvestorFlowRecord`, `ShortSellingRecord`, `ProgramTradingRecord`, `ForeignHoldingRecord`
- 금액 필드 타입: str (Decimal 직렬화 보장)

테스트 파일: `tests/interfaces/test_investor_flow_api.py`

테스트 대상 (FastAPI TestClient 사용):
- symbol 공백 → 400 응답 확인
- from > to → 400 응답 확인
- 정상 요청 → 200 + 배열 응답 확인
- 데이터 없을 때 200 + 빈 배열 확인
- 금액 필드가 string 타입인지 확인 (float 아님)

---

## 완료 기준

- 모든 신규 파일 `python -m py_compile` 통과
- 테스트 파일 전부 실행: `pytest tests/ -v --tb=short`
- 모든 테스트 Green
- flake8 또는 ruff 경고 없음
