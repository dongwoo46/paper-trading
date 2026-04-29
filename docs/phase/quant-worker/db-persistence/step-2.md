# step-2: 청킹 upsert + 파일 잔재 제거 (Green)

## 목표
step-1 테스트를 모두 Green으로 만든다.

---

## 수정 파일 1: `src/repositories/market_daily_ohlcv_repository.py`

### 변경 사항
- `upsert_daily_rows(frame, context, chunk_size=500)` — chunk_size 파라미터 추가
- `_executemany()` → `_execute_chunks()` 로 교체
  - `rows`를 `chunk_size` 단위로 분할
  - 청크마다 `cursor.executemany(query, chunk)` 실행 후 `connection.commit()`
  - 반환값: 총 삽입/업데이트된 행 수 (int)

### 시그니처
```
def upsert_daily_rows(
    self,
    frame: pd.DataFrame,
    context: OhlcvUpsertContext,
    chunk_size: int = 500,
) -> int
```

---

## 수정 파일 2: `src/jobs/catalog_daily_fetch_job.py`

### 변경 사항
- `FetchResult.output_path: Path | None` → 제거
- `FetchResult.rows_inserted: int` → 추가 (기존 `rows` 필드 대체 또는 rename)
- `run_for_yfinance()` — `save_csv()` 호출 제거, output_root 파라미터 제거
- `run_for_pykrx()` — `save_csv()` 호출 제거, output_root 파라미터 제거
- `to_summary_frame()` — `output_path` 컬럼 제거, `rows_inserted` 컬럼 반영

### 시그니처
```
def run_for_yfinance(
    self,
    symbols: list[CatalogSymbol],
    window: FetchWindow,
) -> list[FetchResult]

def run_for_pykrx(
    self,
    symbols: list[CatalogSymbol],
    window: FetchWindow,
    adjusted: bool,
) -> list[FetchResult]
```

---

## 수정 파일 3: `src/application/daily_fetch_service.py`

### 변경 사항
- `DailyFetchOptions.output_root` 제거
- `execute()` 내 `output_root` 사용 코드 제거
- `execute()` 내 `summary.to_csv()` 제거
- `execute()` 반환 dict: `summary_path` 제거, `total_rows_inserted: int` 추가
- `job.run_for_yfinance()`, `job.run_for_pykrx()` 호출부에서 `output_root` 인자 제거

---

## 수정 파일 4: `src/interfaces/api/app.py`

### 변경 사항
- `CollectDailyRequest.output_root` 제거
- `CollectDailyResponse.summary_path` 제거, `total_rows_inserted: int` 추가
- `collect_daily()` — `DailyFetchOptions` 생성 시 `output_root` 인자 제거

---

## 완료 조건
- `python -m pytest tests/ -v` 전체 Green
- `python -m py_compile src/repositories/market_daily_ohlcv_repository.py`
- `python -m py_compile src/jobs/catalog_daily_fetch_job.py`
- `python -m py_compile src/application/daily_fetch_service.py`
- `python -m py_compile src/interfaces/api/app.py`
