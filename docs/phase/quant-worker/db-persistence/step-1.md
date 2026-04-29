# step-1: 테스트 작성 (Red)

## 목표
구현 전 테스트를 먼저 작성한다. 이 단계에서는 테스트가 실패(Red)해도 된다.

## 생성 파일

### `backend/quant-worker/tests/__init__.py` (빈 파일)
### `backend/quant-worker/tests/repositories/__init__.py` (빈 파일)
### `backend/quant-worker/tests/jobs/__init__.py` (빈 파일)
### `backend/quant-worker/tests/application/__init__.py` (빈 파일)

---

### `backend/quant-worker/tests/repositories/test_market_daily_ohlcv_repository.py`

**테스트 대상**: `MarketDailyOhlcvRepository`

검증 항목:
1. `upsert_daily_rows()` — 빈 DataFrame 전달 시 0 반환, DB 호출 없음
2. `upsert_daily_rows()` — 500행 초과(예: 1200행) 전달 시 청크(500/500/200) 단위로 분할 실행 확인
3. `upsert_daily_rows()` — 정확히 500행이면 1회 청크 실행
4. `_to_row()` — `adj_close=None` 또는 NaN 전달 시 `None` 반환
5. `_to_row()` — 정상 값 전달 시 tuple 순서 및 타입 검증

모킹: `connect()` 함수를 mock으로 교체해 실제 DB 연결 없이 테스트.
cursor mock으로 `executemany` 호출 횟수·인자 검증.

---

### `backend/quant-worker/tests/jobs/test_catalog_daily_fetch_job.py`

**테스트 대상**: `CatalogDailyFetchJob`

검증 항목:
1. `run_for_yfinance()` — `fetched_until_date`가 window.end_date 이후면 skip 처리, DB 호출 없음
2. `run_for_yfinance()` — 정상 수집 시 `ohlcv_repository.upsert_daily_rows()` 1회 호출, `save_csv()` 미호출
3. `run_for_yfinance()` — collector 예외 발생 시 `FetchResult.success=False`, `error` 메시지 포함
4. `run_for_pykrx()` — 동일 패턴 검증 (위 1~3 동일)
5. `FetchResult` — `output_path` 필드 없음, `rows_inserted: int` 필드 존재

모킹: `YFinanceDailyCollector`, `PykrxDailyCollector`, `MarketDailyOhlcvRepository` 전부 Mock.

---

### `backend/quant-worker/tests/application/test_daily_fetch_service.py`

**테스트 대상**: `daily_fetch_service.execute()`

검증 항목:
1. `start > end` 시 `ValueError` 발생
2. `provider="yfinance"` 시 `run_for_pykrx()` 미호출
3. `provider="pykrx"` 시 `run_for_yfinance()` 미호출
4. `provider="all"` 시 양쪽 모두 호출
5. 반환 dict에 `summary_path` 키 없음, `total_rows_inserted` 키 존재

모킹: `CatalogDailyFetchJob`, `PostgresSymbolCatalogRepository` Mock.

## 완료 조건
- 테스트 파일 3개 생성 완료
- `python -m pytest tests/ --collect-only` 실행 시 테스트 목록 수집 성공 (실행은 Red여도 무방)
