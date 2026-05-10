# Investor Flow Pipeline — Feature Spec

## 기능 개요

한국 주식시장(KOSPI/KOSDAQ)의 투자자별 매매동향, 공매도, 프로그램 매매, 외국인 보유현황 데이터를 pykrx를 통해 일 단위로 수집하고 PostgreSQL에 적재하는 파이프라인이다. 수집된 데이터는 quant-worker REST API를 통해 외부 서비스(trading-api 등)에 제공된다.

---

## 데이터 소스 및 수집 전략

### 데이터 소스

- 라이브러리: pykrx
- 대상 시장: KR (KOSPI, KOSDAQ) 전 종목
- 수집 단위: 날짜 기준 시장 전체 일괄 조회 (종목별 순회 아님)
- 핵심 API
  - 투자자 매매동향: `get_market_trading_volume_by_investor(date, market, detail=True)`
  - 공매도: `get_market_short_selling(date, market)` 계열
  - 프로그램 매매: `get_market_program_trading_by_date(date, market)` 계열
  - 외국인 보유현황: `get_market_cap_by_date(date, market)` 또는 외국인 한도 전용 API

### 수집 전략

- KOSPI와 KOSDAQ을 별도 배치로 분리 실행 (market 파라미터 주입)
- 조회 결과는 Wide DataFrame 형태 (한 행 = 한 종목의 모든 투자자 컬럼)
- 전 종목 저장 (필터링 없음)
- 멱등 upsert: (trade_date, symbol, market) 복합 고유키 기준 ON CONFLICT DO UPDATE
- 수집 대상 날짜 기본값: 직전 영업일 (배치 실행 시점 자동 산출)

---

## 테이블 스키마

모든 금액 컬럼은 NUMERIC(20, 0) 타입을 사용하며, Python 레이어에서는 Decimal로 처리한다. float 변환 금지.

### 1. investor_flow

투자자별 매매동향 (외국인 / 기관 합계·세부 7개 / 개인)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 자동 생성 키 |
| trade_date | DATE | NOT NULL | 거래일 |
| symbol | VARCHAR(20) | NOT NULL | 종목코드 |
| market | VARCHAR(10) | NOT NULL | KOSPI / KOSDAQ |
| foreign_buy_volume | BIGINT | NOT NULL | 외국인 매수량 |
| foreign_sell_volume | BIGINT | NOT NULL | 외국인 매도량 |
| foreign_net_volume | BIGINT | NOT NULL | 외국인 순매수량 |
| foreign_buy_amount | NUMERIC(20,0) | NOT NULL | 외국인 매수금액 |
| foreign_sell_amount | NUMERIC(20,0) | NOT NULL | 외국인 매도금액 |
| foreign_net_amount | NUMERIC(20,0) | NOT NULL | 외국인 순매수금액 |
| institution_buy_volume | BIGINT | NOT NULL | 기관 합계 매수량 |
| institution_sell_volume | BIGINT | NOT NULL | 기관 합계 매도량 |
| institution_net_volume | BIGINT | NOT NULL | 기관 합계 순매수량 |
| institution_buy_amount | NUMERIC(20,0) | NOT NULL | 기관 합계 매수금액 |
| institution_sell_amount | NUMERIC(20,0) | NOT NULL | 기관 합계 매도금액 |
| institution_net_amount | NUMERIC(20,0) | NOT NULL | 기관 합계 순매수금액 |
| finance_invest_buy_volume | BIGINT | NOT NULL | 금융투자 매수량 |
| finance_invest_sell_volume | BIGINT | NOT NULL | 금융투자 매도량 |
| finance_invest_net_volume | BIGINT | NOT NULL | 금융투자 순매수량 |
| finance_invest_buy_amount | NUMERIC(20,0) | NOT NULL | 금융투자 매수금액 |
| finance_invest_sell_amount | NUMERIC(20,0) | NOT NULL | 금융투자 매도금액 |
| finance_invest_net_amount | NUMERIC(20,0) | NOT NULL | 금융투자 순매수금액 |
| insurance_buy_volume | BIGINT | NOT NULL | 보험 매수량 |
| insurance_sell_volume | BIGINT | NOT NULL | 보험 매도량 |
| insurance_net_volume | BIGINT | NOT NULL | 보험 순매수량 |
| insurance_buy_amount | NUMERIC(20,0) | NOT NULL | 보험 매수금액 |
| insurance_sell_amount | NUMERIC(20,0) | NOT NULL | 보험 매도금액 |
| insurance_net_amount | NUMERIC(20,0) | NOT NULL | 보험 순매수금액 |
| trust_buy_volume | BIGINT | NOT NULL | 투신 매수량 |
| trust_sell_volume | BIGINT | NOT NULL | 투신 매도량 |
| trust_net_volume | BIGINT | NOT NULL | 투신 순매수량 |
| trust_buy_amount | NUMERIC(20,0) | NOT NULL | 투신 매수금액 |
| trust_sell_amount | NUMERIC(20,0) | NOT NULL | 투신 매도금액 |
| trust_net_amount | NUMERIC(20,0) | NOT NULL | 투신 순매수금액 |
| private_equity_buy_volume | BIGINT | NOT NULL | 사모 매수량 |
| private_equity_sell_volume | BIGINT | NOT NULL | 사모 매도량 |
| private_equity_net_volume | BIGINT | NOT NULL | 사모 순매수량 |
| private_equity_buy_amount | NUMERIC(20,0) | NOT NULL | 사모 매수금액 |
| private_equity_sell_amount | NUMERIC(20,0) | NOT NULL | 사모 매도금액 |
| private_equity_net_amount | NUMERIC(20,0) | NOT NULL | 사모 순매수금액 |
| bank_buy_volume | BIGINT | NOT NULL | 은행 매수량 |
| bank_sell_volume | BIGINT | NOT NULL | 은행 매도량 |
| bank_net_volume | BIGINT | NOT NULL | 은행 순매수량 |
| bank_buy_amount | NUMERIC(20,0) | NOT NULL | 은행 매수금액 |
| bank_sell_amount | NUMERIC(20,0) | NOT NULL | 은행 매도금액 |
| bank_net_amount | NUMERIC(20,0) | NOT NULL | 은행 순매수금액 |
| other_finance_buy_volume | BIGINT | NOT NULL | 기타금융 매수량 |
| other_finance_sell_volume | BIGINT | NOT NULL | 기타금융 매도량 |
| other_finance_net_volume | BIGINT | NOT NULL | 기타금융 순매수량 |
| other_finance_buy_amount | NUMERIC(20,0) | NOT NULL | 기타금융 매수금액 |
| other_finance_sell_amount | NUMERIC(20,0) | NOT NULL | 기타금융 매도금액 |
| other_finance_net_amount | NUMERIC(20,0) | NOT NULL | 기타금융 순매수금액 |
| pension_buy_volume | BIGINT | NOT NULL | 연기금 매수량 |
| pension_sell_volume | BIGINT | NOT NULL | 연기금 매도량 |
| pension_net_volume | BIGINT | NOT NULL | 연기금 순매수량 |
| pension_buy_amount | NUMERIC(20,0) | NOT NULL | 연기금 매수금액 |
| pension_sell_amount | NUMERIC(20,0) | NOT NULL | 연기금 매도금액 |
| pension_net_amount | NUMERIC(20,0) | NOT NULL | 연기금 순매수금액 |
| individual_buy_volume | BIGINT | NOT NULL | 개인 매수량 |
| individual_sell_volume | BIGINT | NOT NULL | 개인 매도량 |
| individual_net_volume | BIGINT | NOT NULL | 개인 순매수량 |
| individual_buy_amount | NUMERIC(20,0) | NOT NULL | 개인 매수금액 |
| individual_sell_amount | NUMERIC(20,0) | NOT NULL | 개인 매도금액 |
| individual_net_amount | NUMERIC(20,0) | NOT NULL | 개인 순매수금액 |
| collected_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW() | 수집 시각 |

고유 제약: UNIQUE (trade_date, symbol, market)

인덱스:
- (symbol, trade_date DESC) — 종목별 시계열 조회
- (trade_date, market) — 날짜·시장별 일괄 조회

### 2. short_selling

공매도 거래 현황

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 자동 생성 키 |
| trade_date | DATE | NOT NULL | 거래일 |
| symbol | VARCHAR(20) | NOT NULL | 종목코드 |
| market | VARCHAR(10) | NOT NULL | KOSPI / KOSDAQ |
| short_sell_volume | BIGINT | NOT NULL | 공매도 거래량 |
| short_sell_amount | NUMERIC(20,0) | NOT NULL | 공매도 거래금액 |
| short_sell_ratio | NUMERIC(8,4) | NOT NULL | 공매도 비율 (%) |
| short_balance_volume | BIGINT | NOT NULL | 공매도 잔고수량 |
| short_balance_amount | NUMERIC(20,0) | NOT NULL | 공매도 잔고금액 |
| collected_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW() | 수집 시각 |

고유 제약: UNIQUE (trade_date, symbol, market)

인덱스:
- (symbol, trade_date DESC)
- (trade_date, market)

### 3. program_trading

프로그램 매매 현황 (차익/비차익)

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 자동 생성 키 |
| trade_date | DATE | NOT NULL | 거래일 |
| symbol | VARCHAR(20) | NOT NULL | 종목코드 |
| market | VARCHAR(10) | NOT NULL | KOSPI / KOSDAQ |
| arb_buy_volume | BIGINT | NOT NULL | 차익 매수량 |
| arb_sell_volume | BIGINT | NOT NULL | 차익 매도량 |
| arb_net_volume | BIGINT | NOT NULL | 차익 순매수량 |
| arb_buy_amount | NUMERIC(20,0) | NOT NULL | 차익 매수금액 |
| arb_sell_amount | NUMERIC(20,0) | NOT NULL | 차익 매도금액 |
| arb_net_amount | NUMERIC(20,0) | NOT NULL | 차익 순매수금액 |
| non_arb_buy_volume | BIGINT | NOT NULL | 비차익 매수량 |
| non_arb_sell_volume | BIGINT | NOT NULL | 비차익 매도량 |
| non_arb_net_volume | BIGINT | NOT NULL | 비차익 순매수량 |
| non_arb_buy_amount | NUMERIC(20,0) | NOT NULL | 비차익 매수금액 |
| non_arb_sell_amount | NUMERIC(20,0) | NOT NULL | 비차익 매도금액 |
| non_arb_net_amount | NUMERIC(20,0) | NOT NULL | 비차익 순매수금액 |
| collected_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW() | 수집 시각 |

고유 제약: UNIQUE (trade_date, symbol, market)

인덱스:
- (symbol, trade_date DESC)
- (trade_date, market)

### 4. foreign_holding

외국인 보유 한도 및 소진율

| 컬럼명 | 타입 | 제약 | 설명 |
|--------|------|------|------|
| id | BIGSERIAL | PK | 자동 생성 키 |
| trade_date | DATE | NOT NULL | 기준일 |
| symbol | VARCHAR(20) | NOT NULL | 종목코드 |
| market | VARCHAR(10) | NOT NULL | KOSPI / KOSDAQ |
| holding_limit | BIGINT | NOT NULL | 보유한도 (주) |
| holding_volume | BIGINT | NOT NULL | 현재보유량 (주) |
| exhaustion_ratio | NUMERIC(8,4) | NOT NULL | 한도소진율 (%) |
| collected_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW() | 수집 시각 |

고유 제약: UNIQUE (trade_date, symbol, market)

인덱스:
- (symbol, trade_date DESC)
- (trade_date, market)

---

## 수집기 / 리포지토리 / 배치 책임 정의

### 수집기 (Collectors)

파일: `src/collectors/investor_flow_collector.py`

책임:
- pykrx API를 호출하여 날짜·시장 기준 DataFrame 반환
- Wide 형태의 DataFrame을 도메인 딕셔너리 목록으로 변환
- 금액 컬럼은 Decimal로 변환 (float 통과 금지)
- pykrx 응답 컬럼명과 내부 표준 컬럼명 간 매핑 처리
- 빈 DataFrame(거래 없는 날) 수신 시 빈 목록 반환 (예외 아님)
- 수집 가능 범위: 4개 데이터셋 각각 독립 메서드

클래스: `InvestorFlowCollector`
주요 메서드:
- `fetch_investor_flow(date: str, market: str) -> list[dict]`
- `fetch_short_selling(date: str, market: str) -> list[dict]`
- `fetch_program_trading(date: str, market: str) -> list[dict]`
- `fetch_foreign_holding(date: str, market: str) -> list[dict]`

### 리포지토리 (Repositories)

파일: `src/repositories/investor_flow_repository.py`

책임:
- 4개 테이블에 대한 upsert 연산 제공
- upsert 단위: (trade_date, symbol, market) 고유키 기준 ON CONFLICT DO UPDATE
- 삽입/갱신 행 수 반환
- DB 연결은 환경변수로부터 주입 (하드코딩 금지)
- 금액 컬럼은 Decimal → psycopg2 NUMERIC 자동 직렬화

클래스: `InvestorFlowRepository`
주요 메서드:
- `upsert_investor_flow(records: list[dict], conn) -> int`
- `upsert_short_selling(records: list[dict], conn) -> int`
- `upsert_program_trading(records: list[dict], conn) -> int`
- `upsert_foreign_holding(records: list[dict], conn) -> int`
- `find_investor_flow(symbol: str, from_date: date, to_date: date, limit: int, conn) -> list[dict]`
- `find_short_selling(symbol: str, from_date: date, to_date: date, limit: int, conn) -> list[dict]`
- `find_program_trading(symbol: str, from_date: date, to_date: date, limit: int, conn) -> list[dict]`
- `find_foreign_holding(symbol: str, from_date: date, to_date: date, limit: int, conn) -> list[dict]`

### 배치 스케줄러

파일: `src/jobs/investor_flow_schedule.py` (신규 — batch_schedule.py 수정 없음)

책임:
- APScheduler BackgroundScheduler를 독립적으로 관리
- KOSPI / KOSDAQ 2개 배치 정의 (market 파라미터 분리)
- 실행 시각: 19:00 KST 월-금
- 수집 날짜: 직전 영업일 자동 산출 (당일이 영업일이면 당일 허용, 아니면 이전 영업일)
- 재시도 정책: batch_schedule.py와 동일 (exponential backoff, max 4회)
- Slack 알림: batch_schedule.py 의 notify_slack 재사용
- 환경변수 `INVESTOR_FLOW_SCHEDULE_ENABLED`(default: false)로 활성화 제어
- 스케줄 오버라이드: `INVESTOR_FLOW_CRON_KOSPI`, `INVESTOR_FLOW_CRON_KOSDAQ` 환경변수

app.py 연동:
- lifespan 함수에서 `start_investor_flow_scheduler()` / `stop_investor_flow_scheduler()` 호출 추가

---

## REST API 엔드포인트 계약

모든 엔드포인트는 quant-worker FastAPI 앱 (`src/interfaces/api/app.py`)에 추가한다.

### GET /investor-flow/{symbol}

투자자별 매매동향 조회

파라미터:
- `symbol` (path): 종목코드 (대문자 정규화, 공백 제거)
- `from` (query, optional): 조회 시작일 (ISO 8601 date), 기본값: to - 365일
- `to` (query, optional): 조회 종료일 (ISO 8601 date), 기본값: 오늘
- `market` (query, optional): KOSPI / KOSDAQ, 기본값: 미지정 시 전체
- `limit` (query, optional): 최대 반환 행 수, 기본 250, 최대 1000

응답 필드 (배열):
- `trade_date`: string (ISO date)
- `symbol`: string
- `market`: string
- `foreign_net_volume`: integer
- `foreign_net_amount`: string (Decimal 직렬화)
- `institution_net_volume`: integer
- `institution_net_amount`: string
- `individual_net_volume`: integer
- `individual_net_amount`: string
- 세부 기관 7개의 net_volume, net_amount 포함 (finance_invest, insurance, trust, private_equity, bank, other_finance, pension)
- buy/sell 컬럼 전부 포함

응답 규칙:
- 금액 컬럼은 JSON string으로 직렬화 (float 금지, Decimal → str)
- 데이터 없으면 빈 배열 반환 (404 아님)
- symbol 공백이면 400

### GET /short-selling/{symbol}

공매도 현황 조회

파라미터: symbol(path), from(query), to(query), market(query), limit(query) — investor-flow와 동일 규칙

응답 필드 (배열):
- `trade_date`, `symbol`, `market`
- `short_sell_volume`, `short_sell_amount`(string), `short_sell_ratio`(string)
- `short_balance_volume`, `short_balance_amount`(string)

### GET /program-trading/{symbol}

프로그램 매매 현황 조회

파라미터: symbol(path), from(query), to(query), market(query), limit(query)

응답 필드 (배열):
- `trade_date`, `symbol`, `market`
- `arb_buy_volume`, `arb_sell_volume`, `arb_net_volume`, `arb_buy_amount`(string), `arb_sell_amount`(string), `arb_net_amount`(string)
- `non_arb_buy_volume`, `non_arb_sell_volume`, `non_arb_net_volume`, `non_arb_buy_amount`(string), `non_arb_sell_amount`(string), `non_arb_net_amount`(string)

### GET /foreign-holding/{symbol}

외국인 보유현황 조회

파라미터: symbol(path), from(query), to(query), market(query), limit(query)

응답 필드 (배열):
- `trade_date`, `symbol`, `market`
- `holding_limit`, `holding_volume`
- `exhaustion_ratio`(string)

### 공통 오류 응답

| HTTP 코드 | 조건 |
|-----------|------|
| 400 | symbol 공백, from > to |
| 500 | DB 접속 실패, 수집기 예외 |

---

## 트레이드오프 결정 사항

| 결정 | 이유 |
|------|------|
| Wide 테이블 구조 | 투자자 컬럼 수가 고정(~9그룹×6메트릭)되어 있고, 단일 쿼리로 전체 투자자 그룹 조회 가능. EAV 방식 대비 조인 비용 없음 |
| 날짜 기준 전체 일괄 조회 | pykrx의 효율적 조회 단위가 날짜×시장. 종목별 순회는 API 호출 수가 수천 배 |
| 전 종목 저장 | 어떤 종목이 퀀트 전략에 사용될지 사전에 알 수 없음. 사후 필터링이 사전 필터링보다 안전 |
| KOSPI/KOSDAQ 배치 분리 | pykrx market 파라미터가 단일 시장만 지원. 분리하면 장애 격리도 가능 |
| 별도 investor_flow_schedule.py | batch_schedule.py의 OHLCV 배치와 독립적으로 장애 격리. 기존 배치 수정 범위 최소화 |
| 19:00 KST 배치 실행 | 장 마감(15:30) 후 pykrx 데이터 안정화 시간 고려. 기존 kr_daily(18:30)와 30분 간격으로 DB 부하 분산 |
| 금액 컬럼 Decimal 처리 | 금융 데이터의 float 정밀도 손실 방지. CLAUDE.md 재무 안전 규칙 준수 |
| 멱등 upsert | 동일 날짜 재수집 시 데이터 중복 없이 최신 값으로 갱신 |
