# Step 4 — 코드 리뷰

- 담당: code-reviewer
- 상태: pending
- 전제: step-3 완료 (모든 테스트 Green) 확인 후 진행

---

## 리뷰 범위

아래 파일을 대상으로 리뷰를 수행한다.

- `backend/quant-worker/src/collectors/investor_flow_collector.py`
- `backend/quant-worker/src/repositories/investor_flow_repository.py`
- `backend/quant-worker/src/application/investor_flow_fetch_service.py`
- `backend/quant-worker/src/jobs/investor_flow_schedule.py`
- `backend/quant-worker/src/interfaces/api/app.py` (추가된 엔드포인트 부분)
- `backend/quant-worker/src/migrations/V{next}__create_investor_flow_tables.sql`
- `backend/quant-worker/tests/` (신규 테스트 파일 전부)

---

## 리뷰 체크리스트

### 1. 재무 안전성 (Decimal)

- [ ] 금액 컬럼이 Python 레이어에서 float을 경유하지 않음
  - pykrx DataFrame의 int64 → Decimal 직접 변환 (float 캐스팅 없음)
  - `Decimal(str(val))` 또는 `Decimal(int(val))` 방식 허용, `Decimal(float(val))` 금지
- [ ] 비율 컬럼(short_sell_ratio, exhaustion_ratio)도 Decimal 처리
- [ ] API 응답 JSON에서 금액/비율 필드가 string 타입으로 직렬화됨 (`str(decimal_val)` 또는 Pydantic 커스텀 직렬화)
- [ ] NUMERIC(20,0) 컬럼에 float 바인딩 없음 (psycopg2 파라미터 타입 확인)

### 2. 멱등성

- [ ] 4개 upsert 메서드 모두 ON CONFLICT DO UPDATE 구문 사용
- [ ] ON CONFLICT 대상 컬럼이 (trade_date, symbol, market) 3개 일치
- [ ] collected_at은 upsert 시 업데이트하지 않음 (최초 삽입 시각 보존) 또는 업데이트 여부 결정이 spec과 일치
- [ ] 동일 날짜 재실행 시 데이터 중복 없음

### 3. API 계약 일치

- [ ] 4개 엔드포인트 경로가 spec.md와 일치 (`/investor-flow/{symbol}` 등)
- [ ] 쿼리 파라미터명이 spec과 일치 (`from` alias 포함)
- [ ] limit 기본값 250, 최대 1000 클램핑 적용
- [ ] symbol 공백 → 400, from > to → 400 처리 존재
- [ ] 데이터 없을 때 200 + 빈 배열 반환 (404 아님)
- [ ] 응답 필드가 spec.md의 응답 필드 목록과 일치 (누락/추가 필드 없음)

### 4. 보안

- [ ] DB 연결 정보가 환경변수로 주입됨 (하드코딩 없음)
- [ ] pykrx 호출 결과가 로그에 평문으로 덤프되지 않음
- [ ] 스택 트레이스가 API 500 응답 body에 노출되지 않음 (str(exc)만 허용)
- [ ] Slack 알림 payload에 URL 외 비밀 정보 없음

### 5. 배치 스케줄러

- [ ] `INVESTOR_FLOW_SCHEDULE_ENABLED=false` 시 APScheduler import 없이 None 반환
- [ ] 2개 배치 정의: market=KOSPI, market=KOSDAQ
- [ ] cron_hour=19, cron_day_of_week="mon-fri"
- [ ] 환경변수 오버라이드 파싱 오류 시 기본값으로 폴백
- [ ] batch_schedule.py 기존 코드 수정 없음 (diff 확인)
- [ ] app.py 수정이 lifespan 함수 내부로 최소화됨

### 6. 오류 처리 및 복원력

- [ ] InvestorFlowFetchService에서 특정 데이터셋 수집 실패 시 나머지 데이터셋 계속 진행
- [ ] 빈 DataFrame 반환 시 예외 없이 처리
- [ ] pykrx 연결 타임아웃/오류 시 BatchOutcome.FAILED로 분류
- [ ] DB 연결 오류가 상위로 전파되어 배치 결과에 기록됨

### 7. 코드 품질

- [ ] 클래스·메서드명이 step-2.md 지시와 일치
- [ ] 단일 책임 원칙: Collector는 수집만, Repository는 영속화만, Service는 조합만
- [ ] 불필요한 전역 상태 없음
- [ ] `__init__.py`에 필요한 export가 추가됨
- [ ] ruff/flake8 경고 없음
- [ ] 미사용 import 없음

### 8. 테스트 품질

- [ ] 각 AC (AC-1~AC-6) 커버 여부 확인
- [ ] Mock 사용이 적절함 (pykrx, DB connection)
- [ ] 테스트 픽스처가 실제 pykrx 응답 컬럼명 기반으로 작성됨
- [ ] 엣지 케이스 커버: 빈 DataFrame, 큰 금액값, 공백 symbol

---

## Must-Fix 기준

아래 항목 중 하나라도 해당하면 구현을 반려하고 수정 요청한다.

- 금액 컬럼에 float 사용 (Decimal 미적용)
- upsert에 ON CONFLICT 누락 (INSERT OR REPLACE 등 비호환 방식)
- DB 연결 정보 하드코딩
- API 응답 금액 필드가 float/number 타입
- batch_schedule.py 기존 기능 변경
- API 400 검증 누락 (symbol 공백, from > to)
- 스택 트레이스 API 응답 노출

---

## 리뷰 완료 후

- Must-Fix 없으면 index.json의 step 4 status → completed 갱신
- Must-Fix 있으면 quant-dev에게 항목 전달 후 step-2 재실행 요청