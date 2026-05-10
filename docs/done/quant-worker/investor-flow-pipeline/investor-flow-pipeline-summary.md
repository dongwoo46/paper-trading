# investor-flow-pipeline — 완료 요약

완료일: 2026-05-11
브랜치: feature/quant-worker-investor-flow-pipeline

---

## 구현 목적

국내 주식시장 수급 데이터(투자자별 매매동향 · 공매도 · 프로그램매매 · 외국인보유현황)를 pykrx를 통해 일별 수집하여 PostgreSQL에 적재하고, 퀀트 전략 팩터 계산에 사용할 수 있도록 REST API를 제공한다.
기관 세부 7개(금융투자·보험·투신·사모·은행·기타금융·연기금) 포함 외국인/개인 전 구간을 Decimal 타입으로 안전하게 처리한다.

---

## 신규 파일 목록

- `backend/quant-worker/src/collectors/investor_flow_collector.py` — pykrx 기반 4개 데이터셋 수집기
- `backend/quant-worker/src/repositories/investor_flow_repository.py` — ON CONFLICT upsert, market 필터 지원 리포지토리
- `backend/quant-worker/src/application/investor_flow_fetch_service.py` — 영업일 산출, 데이터셋별 독립 예외 격리 서비스
- `backend/quant-worker/src/jobs/investor_flow_schedule.py` — 19:00 KST KOSPI/KOSDAQ 분리 배치 스케줄러
- `backend/quant-worker/src/migrations/V1__create_investor_flow_tables.sql` — 4개 테이블 DDL
- `backend/quant-worker/tests/collectors/test_investor_flow_collector.py`
- `backend/quant-worker/tests/repositories/test_investor_flow_repository.py`
- `backend/quant-worker/tests/application/test_investor_flow_fetch_service.py`
- `backend/quant-worker/tests/jobs/test_investor_flow_schedule.py`
- `backend/quant-worker/tests/interfaces/test_investor_flow_api.py`
- `backend/quant-worker/tests/interfaces/test_investor_flow_migration_sql.py`

---

## 수정 파일 목록

- `backend/quant-worker/src/interfaces/api/app.py` — REST API 엔드포인트 4개 추가 및 investor_flow_schedule 라이프사이클 통합

---

## 테이블 정의 (4개)

| 테이블명 | 설명 | UNIQUE 키 |
|---|---|---|
| `investor_flow` | 투자자별 매매동향 (외국인/기관합계/기관세부7/개인) | (trade_date, symbol, market) |
| `short_selling` | 공매도 거래량·금액·비율·잔고 | (trade_date, symbol, market) |
| `program_trading` | 차익/비차익 프로그램매매 매수·매도·순매수 | (trade_date, symbol, market) |
| `foreign_holding` | 외국인 보유한도·보유수량·소진율 | (trade_date, symbol, market) |

금액 컬럼: `NUMERIC(20,0)` (정수 원화), 비율 컬럼: `NUMERIC(8,4)`

---

## REST API 엔드포인트 (4개)

| 메서드 | 경로 | 설명 | 쿼리 파라미터 |
|---|---|---|---|
| GET | `/investor-flow/{symbol}` | 외국인·기관(세부7)·개인 매매동향 | start_date, end_date, market |
| GET | `/short-selling/{symbol}` | 공매도 거래량·금액·잔고 | start_date, end_date, market |
| GET | `/program-trading/{symbol}` | 차익/비차익 프로그램매매 | start_date, end_date, market |
| GET | `/foreign-holding/{symbol}` | 외국인 보유한도·소진율 | start_date, end_date, market |

금액 필드는 JSON 응답에서 string 직렬화 (float 정밀도 손실 방지).

---

## 배치 정의 (2개)

| batch_id | market | 실행 시각 | 요일 | 활성화 방법 |
|---|---|---|---|---|
| `investor_flow_kospi` | KOSPI | 19:00 KST | 월~금 | `INVESTOR_FLOW_SCHEDULE_ENABLED=true` |
| `investor_flow_kosdaq` | KOSDAQ | 19:00 KST | 월~금 | `INVESTOR_FLOW_SCHEDULE_ENABLED=true` |

크론 오버라이드: `INVESTOR_FLOW_CRON_KOSPI`, `INVESTOR_FLOW_CRON_KOSDAQ` (5-field cron 형식)
재시도 정책: `batch_schedule.py`의 `compute_retry_delay_seconds` 재사용 (지수 백오프)

---

## 핵심 트레이드오프 결정 3가지

1. **4개 데이터셋 독립 예외 격리**: `InvestorFlowFetchService.execute()`에서 각 데이터셋(investor_flow, short_selling, program_trading, foreign_holding)을 개별 try/except로 감싸 한 데이터셋 수집 실패가 다른 데이터셋 적재를 차단하지 않도록 설계.
   - 대안: 전체 롤백 트랜잭션 — 데이터 일관성은 높지만, 하나의 pykrx API 오류로 전체가 누락되는 리스크 허용 불가.

2. **KOSPI/KOSDAQ 배치 분리**: 단일 배치에서 두 시장을 순차 처리하는 대신, 배치를 2개(`investor_flow_kospi`, `investor_flow_kosdaq`)로 분리하여 시장별 독립 재시도 및 개별 Slack 알림 지원.
   - 대안: 하나의 배치에서 루프 처리 — 운영 가시성 저하, 부분 실패 시 재시도 단위가 시장 전체가 되는 문제.

3. **float 경유 없는 Decimal 처리**: pykrx에서 반환되는 pandas DataFrame 수치를 numpy/float 캐스팅 없이 `str()` → `Decimal()` 변환 파이프라인으로 처리. DB `NUMERIC(20,0)` 컬럼과 1:1 정합성 보장.
   - 대안: float 중간 변환 허용 — 금액 컬럼 반올림 오차 발생 가능, 재무 안전 규칙 위반.

---

## 테스트 결과

- 전체 테스트: 229/229 통과 (pytest)
- 린트: ruff check all passed
- 컴파일: py_compile 4개 파일 모두 통과