# Step 3 — 테스트 및 QA 검증

- 담당: test-engineer
- 상태: pending
- 전제: step-2 완료 (모든 파일 구현 완료) 확인 후 진행

---

## 검증 목표

investor-flow-pipeline의 4개 데이터셋 수집·저장·조회 흐름이 spec.md와 일치하며, 재무 안전 규칙(Decimal) 및 멱등성(upsert) 요건을 충족하는지 검증한다.

---

## 테스트 Scope

### 단위 테스트 (Unit Tests)

외부 의존성(pykrx, DB)은 전부 Mock 처리.

| 테스트 파일 | 검증 대상 |
|-------------|-----------|
| `tests/collectors/test_investor_flow_collector.py` | 컬럼 매핑, Decimal 변환, 빈 DataFrame 처리 |
| `tests/repositories/test_investor_flow_repository.py` | upsert SQL 구조, find 파라미터 바인딩, Decimal 직렬화 |
| `tests/application/test_investor_flow_fetch_service.py` | 영업일 산출, 수집-저장 흐름, 부분 실패 처리 |
| `tests/jobs/test_investor_flow_schedule.py` | 배치 정의 개수, 스케줄 파라미터, 활성화 플래그 |
| `tests/interfaces/test_investor_flow_api.py` | HTTP 상태코드, 응답 필드 타입, 입력 검증 |

### 통합 테스트 (Integration Tests — 선택적)

실제 DB가 필요한 경우 환경변수 `TEST_DB_URL` 설정 후 진행.

| 검증 항목 | 방법 |
|-----------|------|
| upsert 멱등성 | 동일 레코드 2회 삽입 후 행 수 확인 (1개만 존재해야 함) |
| Decimal 정밀도 | 큰 금액(10조 이상) 저장 후 조회값 일치 확인 |
| 인덱스 활용 | EXPLAIN ANALYZE로 (symbol, trade_date) 인덱스 사용 확인 |

---

## 검증 기준 (Acceptance Criteria)

### AC-1: Decimal 안전성

- investor_flow, short_selling, program_trading, foreign_holding의 모든 금액 컬럼이 Python 레이어에서 Decimal 타입임
- float 타입이 어느 단계에서도 경유되지 않음
- JSON 응답에서 금액 필드가 string 타입으로 직렬화됨 (float/number 타입 아님)

검증 방법:
- `test_investor_flow_collector.py`에서 반환 딕셔너리의 금액 필드 `isinstance(val, Decimal)` 확인
- `test_investor_flow_api.py`에서 응답 JSON의 금액 필드 `isinstance(val, str)` 확인

### AC-2: 멱등성 (upsert)

- 동일 (trade_date, symbol, market) 레코드를 2회 upsert 시 행 수가 증가하지 않음
- 두 번째 upsert는 기존 행의 값을 최신 값으로 갱신함

검증 방법:
- Mock DB에서 ON CONFLICT DO UPDATE SQL이 execute에 전달됨을 확인
- (통합 테스트 시) 실제 DB에서 2회 insert 후 COUNT(*) = 1 확인

### AC-3: 빈 데이터 안전성

- pykrx가 빈 DataFrame 반환 시 collector가 예외 없이 빈 목록 반환
- 배치가 빈 목록을 받아도 정상 완료 처리 (NOOP 결과)
- API 엔드포인트는 데이터 없을 때 빈 배열 [] 반환 (404 아님)

### AC-4: API 입력 검증

- symbol이 공백 문자열일 때 400 응답
- from > to일 때 400 응답
- limit이 1000 초과 시 1000으로 클램핑 (오류 아님)
- limit이 0 이하 시 1로 클램핑

### AC-5: 배치 정의 정확성

- investor_flow_schedule이 반환하는 배치 정의가 정확히 2개 (kospi, kosdaq)
- 각 정의의 cron_hour = 19, cron_day_of_week = "mon-fri"
- `INVESTOR_FLOW_SCHEDULE_ENABLED=false` 시 scheduler가 None 반환
- `INVESTOR_FLOW_SCHEDULE_ENABLED=true` 시 APScheduler 인스턴스 반환 (APScheduler import 가능 환경)

### AC-6: 컬럼명 매핑

- pykrx DataFrame의 한글/영문 컬럼명이 spec.md의 표준 컬럼명으로 정확히 매핑됨
- 누락 컬럼 없이 전체 컬럼이 딕셔너리에 포함됨

---

## 실행 커맨드

```
# 전체 테스트 실행
cd backend/quant-worker && pytest tests/ -v --tb=short

# 특정 파일만
pytest tests/collectors/test_investor_flow_collector.py -v
pytest tests/repositories/test_investor_flow_repository.py -v
pytest tests/application/test_investor_flow_fetch_service.py -v
pytest tests/jobs/test_investor_flow_schedule.py -v
pytest tests/interfaces/test_investor_flow_api.py -v

# 컴파일 검증
python -m py_compile src/collectors/investor_flow_collector.py
python -m py_compile src/repositories/investor_flow_repository.py
python -m py_compile src/application/investor_flow_fetch_service.py
python -m py_compile src/jobs/investor_flow_schedule.py

# 린트
ruff check src/collectors/investor_flow_collector.py
ruff check src/repositories/investor_flow_repository.py
ruff check src/application/investor_flow_fetch_service.py
ruff check src/jobs/investor_flow_schedule.py
```

---

## 완료 기준

- 모든 단위 테스트 Green (0 failed)
- AC-1~AC-6 전부 통과
- 컴파일 오류 없음
- 린트 경고 없음
- step-3 완료 후 index.json의 step 3 status를 completed로 갱신
