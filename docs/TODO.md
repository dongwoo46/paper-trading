# TODO — Paper Trading

Orchestrator가 읽어 다음 개발 대상을 선택하는 작업 목록.
`$orchestrate` 실행 시 idle 상태면 이 파일의 미완료 항목을 제안한다.

형식: `- [ ] 설명 | project: {프로젝트} | phase: {phase명} | priority: P{n}`
완료 시: `- [x]` 로 변경 + `| done: YYYY-MM-DD | pr: #{n}` → TODO-DONE.md로 이동

---

## 우선순위 기준

- **P1**: 사용자에게 빠르게 보여줄 수 있거나, 전체 시스템의 필수 기반
- **P2**: P1 완성 후 품질·깊이 강화
- **P3**: AI 에이전트 고도화 레이어 (P1·P2 선행 필요)
- **P4**: 대안데이터·복잡한 인프라 (시간 여유 있을 때)

---

## 보류 정책

- `strategy-execution`은 전략 도메인 리빌딩 이후 재개.
- TimesFM/Kronos/MiroFish는 백테스팅 엔진 완성 이후 진행.
- GraphDB RAG·논문 팩터는 핵심 레이어 완성 이후 진행.

---

## quant-ai / quant-research 아키텍처 정리 ⚠️ 최우선

### P0 — 서비스 역할 재정립 (현재 구조 잘못됨)

- [x] 종목 분석 시 3M, 6M, 1Y, 2Y, MAX 지지·저항 차트 분석 신뢰도 개선 | project: quant-research | phase: chart-analysis-support-resistance-quality | priority: P0 | done: 2026-05-19 | pr: #36
  - 증상: 장기 window(3M/6M/1Y/2Y/MAX)에서 지지·저항 레벨이 실제 차트 맥락과 맞지 않아 분석 품질이 떨어짐
  - 조사 항목: window별 봉 수/interval 매핑, peak 탐지 파라미터, ATR 기반 클러스터링 폭, 최근성/터치 횟수/거래량 가중치, 장기 추세 구간 분리 여부
  - 목표: 장기 구간에서도 주요 지지·저항 레벨이 시각적으로 납득 가능하고, confidence score와 자연어 근거가 계산 품질을 반영하도록 개선

- [x] quant-research / quant-ai 역할 분리 리팩터 | project: quant-ai | phase: service-role-refactor | priority: P0 | done: 2026-05-14
  - **문제**: quant-ai가 분석 파이프라인을 오케스트레이션하고 quant-research에 위임하는 구조 — 역할 역전
  - **올바른 역할**:
    - `quant-research`: 기술분석 계산(지표/패턴/추세/지지저항) + DB 저장 + 결과·종목 조회 엔드포인트
    - `quant-ai`: research가 저장한 분석 결과를 Ollama LLM에 보내 초보 투자자도 이해할 수 있는 자연어 해석 생성 (이유·근거·시사점) — LLM 프로세스만 담당
  - **필수 파이프라인 흐름** (순서 고정, 생략 불가):
    - `quant-research` 분석 완료 → 즉시 `quant-ai` 자연어 해석 트리거 → 결과 DB 저장
    - 수동 트리거(프론트) / 배치 스케줄 모두 동일 흐름
  - **변경 범위**:
    - quant-research에 추가: `POST /research/run/{symbol}`, `GET /research/results/{symbol}`, `GET /research/symbols`
    - quant-research에 이전: `PostgresOhlcvRepository`, `PostgresChartAnalysisRepository`, `PrecomputePipelineService`
    - quant-ai에서 제거: `/chart-analysis/{symbol}`, `/admin/run-analysis/{symbol}`, `/admin/symbols`
    - quant-ai 유지: LLM 자연어 해석 엔드포인트, SSE 스트림, 분석 요청 큐
    - 프론트엔드: `runChartAnalysis`, `fetchChartAnalysis`, `fetchAnalyzedSymbols` → `api-research`로 변경

- [x] Python 서비스 DB 스키마 자동 생성 | project: quant-research | phase: service-role-refactor | priority: P0 | done: 2026-05-14
  - Python 서비스가 사용하는 테이블은 Python 서비스가 직접 생성 (Kotlin Flyway 의존 금지)
  - quant-research / quant-ai lifespan 시작 시 `migrations/*.sql` 순서대로 자동 실행
  - `CREATE TABLE IF NOT EXISTS` 사용으로 멱등성 보장
  - 대상 테이블: `chart_analysis_result`, `analysis_request_queue`, `popular_symbols`

- [x] pykrx 일봉·주봉 OHLCV 저장 오류 조사 | project: quant-collector | phase: pykrx-ohlcv-fix | priority: P0
  - 증상: pykrx provider로 수집 시 데이터가 DB에 제대로 저장되지 않음
  - 조사 항목: pykrx API 응답 컬럼명 매핑, upsert 로직, market/source 값 일치 여부
  - 검증: 수집 후 `market_daily_ohlcv`, `market_weekly_ohlcv` 직접 조회로 확인

- [x] 프론트엔드 차트분석 페이지 리팩터 | project: front | phase: chart-analysis-frontend-refactor | priority: P0 | done: 2026-05-14
  - 백엔드 역할 분리(service-role-refactor) 완료 후 진행
  - **API 엔드포인트 변경**:
    - `fetchChartAnalysis` (결과 조회) → `api-research`
    - `runChartAnalysis` (분석 실행) → `api-research`
    - `fetchAnalyzedSymbols` (종목 목록) → `api-research`
    - LLM 자연어 해석 스트림 → `api-ai` 유지
  - **UX 흐름 반영**:
    - 분석 실행(research) → 자동으로 ai 자연어 해석까지 연결 (한 버튼으로)
    - 결과 패널에 수치 분석(research) + 자연어 해설(ai) 함께 표시
    - 자연어 해설은 SSE 스트림으로 실시간 타이핑 표시
  - `chartAnalysisApi.ts` base URL 및 타입 정리

---

## trading-api

### P0 — 포지션 청산 트리거 모델 리팩터링 ⚠️ 최우선

- [x] PositionExitTrigger 단일 조건 모델 리팩터링 | project: trading-api | phase: position-exit-trigger-model-refactor | priority: P0 | done: 2026-05-19
  - 현재 구조 문제: 포지션당 trigger row 1개에 `stopLossPercent` / `takeProfitPercent`, `stopLossState` / `takeProfitState`가 같이 있어 트리거별 주문·상태·비율·멱등성 관리가 흐려짐
  - 목표 모델: `PositionExitTrigger`를 청산 조건 1개를 나타내는 엔티티로 변경
    - `triggerType`: `STOP_LOSS` / `TAKE_PROFIT`
    - `triggerPercent`: 조건 퍼센트
    - `triggerPrice`: 평균매수가 기준으로 등록 시 계산된 발동 가격
    - `exitRatioPercent`: 발동 시 매도할 포지션 비율
    - `state`: 단일 상태(`ARMED`, `TRIGGERED`, `CANCELED`, `FAILED`, `SKIPPED`)
  - Evaluator: trigger type별 가격 비교 방향만 담당하고, 발동 후보 `TriggerDecision` 목록을 반환하도록 변경
  - Orchestrator: 한 tick에서 여러 트리거가 동시에 조건을 만족하면 `positionId + triggerType` 기준으로 그룹핑하고, 같은 그룹은 `exitRatioPercent`를 합산해 하나의 자동 매도 주문으로 생성
  - 주문 멱등성: `triggerId + entity version` 기반 idempotency key 사용, 같은 tick에서 묶인 주문은 `orderGroupId`로 추적
  - 동시성 유지: trigger row와 position row 비관적 락, 수동 청산/매도 잠금 충돌 시 `SKIPPED` + `skipReason` 기록
  - 변경 범위: entity/migration/repository/querydsl/command/query/orchestrator/evaluator/order DTO/API/tests 전체 갱신

### P1

- [ ] 전략 실행 모드 설계 결정 | project: trading-api | phase: strategy-execution-mode-design | priority: P1
  - ✅ 실행 모드 3개 지원 결정: `KIS_LIVE` / `KIS_PAPER` / `LOCAL`
  - ✅ 전략 단위 독립 제어, 글로벌 킬스위치 필요
  - ✅ 주문·체결 DB 저장, 전략 ID 연결
  - ⚠️ 논의 필요: 전략 활성화/비활성화 UI vs API 제어
  - ⚠️ 논의 필요: 킬스위치 저장 위치 (DB+Redis vs DB만)

- [ ] 전략 실행 서비스 (장투·단투 통합) | project: trading-api | phase: strategy-execution | priority: P1
  - ⚠️ 보류: 전략 실행 모드 설계 결정 이후 진행
  - 장투(Swing/Position) / 단투(Day/Scalping) 전략 타입 구분
  - 전략 저장/버전 관리, 전략별 손익 기록
  - StrategyCommandService: 활성화/비활성화, 글로벌 킬스위치
  - 시그널(OrderSignal) 수신 → 자동 주문 생성

---

## quant-worker — 데이터 수집

### P1 — 빠르게 사용자에게 보여줄 수 있는 데이터

- [x] 국내 수급 데이터 수집 (외국인/기관/개인/공매도) | project: quant-worker | phase: investor-flow-pipeline | priority: P1 | done: 2026-05-11

- [ ] 뉴스 수집 파이프라인 | project: quant-worker | phase: news-pipeline | priority: P1
  - 데이터 소스: Naver 뉴스 RSS / Google News / 연합뉴스
  - 종목별 뉴스 태깅 + LLM 감성 분류 (positive/negative/neutral)
  - 핵심 키워드 추출 + 요약
  - 수집 주기: 장중 30분, 장외 1회

- [ ] 공시 분석 파이프라인 (DART) | project: quant-worker | phase: dart-pipeline | priority: P1
  - DART OpenAPI: 실적발표·분기보고서·주요사항 보고서
  - 공시 유형 분류 + 종목 태깅
  - 실적 서프라이즈 계산 (예측 대비 실제 EPS/매출)
  - 단기 시그널 트리거 연동

- [ ] 기업 펀더멘탈 데이터 수집 | project: quant-worker | phase: fundamental-pipeline | priority: P1
  - 국내: DART 재무제표 파싱 (PER/PBR/ROE/부채비율/영업이익률)
  - 미국: yfinance 재무 데이터
  - 분기별 업데이트, 밸류에이션 분위 계산

### P2 — 분석 강화 데이터

- [ ] 이벤트 캘린더 수집 | project: quant-worker | phase: event-calendar-pipeline | priority: P2
  - 국내: 실적발표 일정, 배당락일, 주주총회
  - 미국: FOMC, CPI/PPI/고용지표 발표일, 어닝시즌
  - 이벤트 D-day 기준 시그널 트리거 연동

- [ ] 거시경제 분석 고도화 | project: quant-worker | phase: macro-factor-pipeline | priority: P2
  - 기존 FRED 확장: 금리·환율·경기선행지수·VIX
  - 거시 팩터 정규화 + 시계열 DB 적재
  - 경기 사이클 판단 로직 (확장/수축/회복/침체)

- [ ] 미국 주식 수급 데이터 수집 파이프라인 | project: quant-worker | phase: us-market-flow-pipeline | priority: P2
  - 공매도 잔고 (FINRA), 기관 보유 변화 (SEC 13F)
  - 다크풀 거래량, ETF 자금 유출입
  - ⚠️ 설계 전 논의: 수집 종목 범위 (S&P500 / NASDAQ100 / 전체)

### P4 — 대안 데이터

- [ ] 커뮤니티 감성 분석 | project: quant-worker | phase: community-sentiment | priority: P4
  - 네이버 종목토론실, Reddit, X(Twitter) 감성 점수 시계열

- [ ] 폴리마켓 데이터 연동 | project: quant-worker | phase: polymarket-pipeline | priority: P4
  - 이벤트 예측 확률 수집 → 거시 리스크 입력값

---

## quant-worker — 전략·백테스팅

### P1 — 전략의 뼈대

- [ ] 장투 전략 생성 (Swing/Position) | project: quant-worker | phase: longterm-strategy-gen | priority: P1
  - 전략 타입: 추세추종 (MA 크로스오버) / 모멘텀 (상대강도) / 밸류 (PER·PBR 저평가) / 수급 모멘텀
  - 진입 조건: 골든크로스 + 외국인·기관 순매수 지속 + PER 저평가
  - 청산 조건: 데드크로스 / 목표 수익률 도달 / 손절 -8%
  - 보유 기간: 수 주 ~ 수 개월
  - 출력: 전략 파라미터 명세 + 진입·청산 규칙 자연어 설명

- [ ] 단투 전략 생성 (Day/Scalping) | project: quant-worker | phase: shortterm-strategy-gen | priority: P1
  - 전략 타입: 브레이크아웃 / RSI 과매도 반등 / 거래량 스파이크 / VWAP 이탈
  - 진입 조건: 전일 고점 돌파 + 거래량 전일 대비 200% 이상
  - 청산 조건: 목표가 도달 / 장 마감 전 청산 / 손절 -3%
  - 보유 기간: 당일 ~ 수일
  - 출력: 전략 파라미터 명세 + 진입·청산 규칙 자연어 설명

- [x] 백테스팅 엔진 기본 구조 | project: quant-worker | phase: backtest-engine | priority: P1 | done: 2026-08-16
  - Python 기반 벡터화 백테스팅 (vectorbt 또는 자체 구현)
  - 일봉 OHLCV + 수급 + 팩터 데이터 소비
  - 성과 지표: Sharpe / MDD / 연환산 수익률 / Calmar / 승률
  - 장투·단투 전략 타입별 분리 검증

- [ ] 멀티팩터 AI 전략 DSL 변인 확장 | project: quant-worker | phase: backtest-multifactor-dsl-expansion | priority: P1
  - 백테스트 엔진 기본 구조 완료 후 진행
  - local AI가 생성하는 전략 DSL에서 가격/기술지표 외 변인 지원 확장
  - 대상 변인: 수급, 펀더멘탈, 거시경제, 뉴스 감성, 공시, 이벤트 캘린더, 논문 기반 팩터
  - 각 변인별 point-in-time 데이터 계약과 look-ahead bias 방지 규칙 정의
  - LEAN custom data export/loader 구현
  - 변인별 fixture + golden backtest regression 추가

- [ ] 알파 팩터 파이프라인 | project: quant-worker | phase: alpha-pipeline | priority: P1
  - 팩터 계산: 수급 모멘텀 / 기술적 신호 / 밸류(PER·PBR 분위) / 거시
  - 팩터 정규화 (Z-score), IC(Information Coefficient) 분석
  - 팩터 → 백테스팅 엔진 연동

### P1 — 사용자에게 빠르게 보여주는 에이전트

- [x] 차트 분석 AI 에이전트 (차트매매용) | project: quant-worker | phase: chart-analysis-agent | priority: P1 | done: 2026-05-12

- [ ] 수급·펀더멘탈 기반 종목 추천 엔진 | project: quant-worker | phase: stock-recommendation | priority: P1
  - 외국인·기관 순매수 상위 종목 스크리닝 (데이터 이미 있음)
  - 펀더멘탈 필터: PER 저평가 + ROE 개선 종목
  - 장투 후보 / 단투 후보 분리 랭킹
  - 초보자도 이해 가능한 추천 근거 자연어 출력

- [ ] 연관 종목 상승 예측 (파생 종목 추천) | project: quant-worker | phase: ripple-stock-recommendation | priority: P1
  - A 종목 급등 시 → 연관 수혜 종목 자동 탐지
  - 관계 유형: 동일 섹터 peer / 공급망(upstream·downstream) / 원자재 수혜
  - 예: 반도체 장비주 급등 → 관련 소재·부품주 수혜 후보 추천
  - 섹터-종목 관계 테이블 + 상관계수 기반 구현 (GraphDB 없이 먼저 구현)
  - 향후 GraphDB RAG로 고도화 예정

### P1 — 리스크 관리

- [ ] 리스크 관리 에이전트 | project: quant-worker | phase: risk-management-agent | priority: P1
  - 포지션별 VaR (Value at Risk) 계산
  - 포트폴리오 최대 손실 한도 모니터링 (MDD 실시간 추적)
  - 섹터 집중도 경고 (단일 섹터 30% 초과 시 알림)
  - 상관계수 기반 리스크 분산도 평가
  - 손절 조건 위반 시 → 자동 청산 시그널 생성

- [ ] 헤징 전략 에이전트 | project: quant-worker | phase: hedging-agent | priority: P1
  - 보유 포지션 기반 헤징 전략 자동 추천
  - 인버스 ETF / 섹터 역상관 종목 헤징 제안
  - 변동성 급등 시 (VIX 스파이크) → 헤징 비중 조정 권고
  - 장투 포지션 대 단투 헤징 비율 계산
  - 출력: "현재 리스크 노출 X%, 헤징 권고 Y 종목 Z% 비중"

### P2 — 예측 모델

- [ ] 시계열 예측 모델 통합 (TimesFM / Kronos / MiroFish) | project: quant-worker | phase: timeseries-forecast | priority: P2
  - Google TimesFM 또는 Amazon Chronos 로컬 추론
  - MiroFish 오프라인 모델 통합 (ref: github.com/nikmcfly/MiroFish-Offline)
  - 종가·거래량 단기 예측 (1~5일)
  - 3개 모델 예측 앙상블 → 신뢰도 가중 합산
  - 예측 결과 → 전략 신호 입력값으로 활용

### P3 — LangGraph 에이전트 워크플로우

- [ ] LangGraph + LangChain 에이전트 워크플로우 구축 | project: quant-worker | phase: langgraph-agent-workflow | priority: P3
  - LangGraph로 전체 퀀트 에이전트 파이프라인 오케스트레이션
  - 노드: 데이터수집 → 분석(차트·뉴스·펀더멘탈·거시) → 전략생성 → 백테스팅 → 반박 → 수정 → 승인
  - Reflexion 패턴: 전략생성 → 검증 → 반박 → 수정 루프
  - conditional edge: Sharpe > 1.5 && 반박 에이전트 승인 → 실행 / 미달 → 재생성
  - 분석 에이전트 병렬 실행, 장투·단투 분리 그래프
  - 선행 필요: 백테스팅 엔진, 알파 팩터, 뉴스·공시 파이프라인

### P3 — AI 에이전트 고도화

- [ ] 전략 생성 AI 에이전트 | project: quant-worker | phase: strategy-gen-agent | priority: P3
  - 수집된 모든 데이터(수급·뉴스·공시·펀더멘탈·거시) 종합
  - LLM 기반 장투·단투 전략 자동 생성
  - 전략 파라미터(진입·청산·손절 조건) 명세 출력

- [ ] 전략 검증 자동화 | project: quant-worker | phase: strategy-validation | priority: P3
  - 생성된 전략 → 백테스팅 엔진 자동 연동
  - 통계적 유의성 검증, Walk-forward / 몬테카를로 과최적화 탐지

- [ ] 전략 반박 에이전트 (Devil's Advocate) | project: quant-worker | phase: strategy-critique-agent | priority: P3
  - 생성된 전략 약점 자동 비판
  - 반박 근거: 시장 체제 변화, 팩터 붕괴 위험, 수수료·슬리피지
  - 리스크 시나리오 자동 생성

- [ ] 여러 AI 전략 비교 | project: quant-worker | phase: multi-ai-strategy | priority: P3
  - GPT-4o / Claude / Gemini 병렬 전략 생성
  - 전략별 백테스트 성과 비교 + 앙상블 합성

### P4 — 고급 인프라

- [ ] GraphDB + RAG 주식-뉴스 관계 종목추천 | project: quant-worker | phase: graphdb-rag-recommendation | priority: P4
  - Neo4j 그래프DB: 종목↔뉴스↔기업↔산업↔인물 관계 모델링
  - RAG: 사용자 질의 → 그래프 탐색 → 컨텍스트 검색 → LLM 추천 근거 생성
  - 연관 종목 파생 추천의 고도화 버전 (P1 구현 후 마이그레이션)

- [ ] 논문 기반 팩터 라이브러리 | project: quant-worker | phase: paper-factor-library | priority: P4
  - arXiv·SSRN 퀀트 논문 팩터 추출 → 팩터 DB 구축
  - 국내 시장 적용 가능성 검증

### P4 — 실행 연동

- [ ] 실행 판단 에이전트 | project: quant-worker | phase: execution-agent | priority: P4
  - 종목 추천 신호 → 포지션 사이징 (Kelly Criterion)
  - 리스크 체크 → trading-api 자동 주문 연동
  - ⚠️ trading-api strategy-execution 완성 선행 필요

---

## 인프라 — Railway MVP 배포

### P1

- [ ] Railway MVP 배포 | project: infra | phase: railway-mvp-deploy | priority: P1

  **배포 대상 (3개 서비스)**
  - quant-worker (FastAPI) — 백테스트 + 차트분석 API 서버
  - trading-web (React/Vite) — 사용자 UI
  - PostgreSQL (Railway 애드온) — OHLCV + 수급 데이터

  **MVP에서 사용자가 할 수 있는 것**
  - 종목 검색 + 차트 조회 (캔들/보조지표/수급 패인)
  - 전략 조건 입력 → 백테스트 실행 → 성과 확인 (수익률/MDD/샤프/승률)
  - 차트 AI 분석 버튼 → 지지선·저항선·진입가·손절가 자연어 리포트

  **배포 전 완성 필요 항목**
  - [ ] 백테스팅 엔진 FastAPI 엔드포인트 (`POST /backtest`)
  - [ ] 차트 분석 AI FastAPI 엔드포인트 (`POST /chart-analysis`)
  - [ ] 백테스트 결과 UI (전략 입력 폼 + 수익률 곡선 + 매매 내역 테이블)
  - [ ] 차트 분석 결과 UI (AI 리포트 패널 — 차트 옆에 표시)

  **Railway 설정**
  - [ ] quant-worker Dockerfile 작성
  - [ ] trading-web Dockerfile 작성 (Nginx static)
  - [ ] 환경변수: `DATABASE_URL`, `OPENAI_API_KEY`, `VITE_API_BASE_URL`
  - [ ] Railway PostgreSQL → quant-worker DB 마이그레이션 (Alembic)
  - [ ] OHLCV 초기 데이터 적재 (pykrx 수집 스크립트 1회 실행)

  **PostHog 연동 (사용자 행동 분석)**
  - [ ] PostHog SDK 설치 (trading-web)
  - [ ] 주요 이벤트 트래킹: 백테스트 실행, 차트 분석 요청, 종목 검색
  - [ ] 환경변수: `VITE_POSTHOG_KEY`, `VITE_POSTHOG_HOST`

  **Sentry 연동 (에러 모니터링)**
  - [ ] Sentry SDK 설치 (trading-web + quant-worker)
  - [ ] 프론트 에러 자동 수집 + 소스맵 업로드
  - [ ] FastAPI 에러 자동 수집 (sentry-sdk[fastapi])
  - [ ] 환경변수: `VITE_SENTRY_DSN`, `SENTRY_DSN`

---

## trading-web (front)

### P1 — 완료

- [x] 통합 차트 고도화 (거래량/보조지표/수급/드로잉) | project: front | phase: market-unified-indicators-ui | priority: P1 | done: 2026-05-11

### P1 — 빠르게 사용자에게 보여줄 UI

- [ ] 뉴스·공시 피드 UI | project: front | phase: news-disclosure-feed | priority: P1
  - 종목 선택 시 관련 뉴스·공시 실시간 피드
  - 감성 점수 색상 표시 (긍정/부정/중립)
  - 뉴스 수집 파이프라인 완성 선행 필요

- [ ] 내 포폴 분석 고도화 | project: front | phase: portfolio-analysis-ui | priority: P1
  - 섹터별·전략별 손익 분해
  - 리스크 지표 시각화 (MDD, 변동성, 샤프)
  - 리스크 관리 에이전트 경고 알림 표시

- [ ] 전략 수익률/손실률 추적 대시보드 | project: front+trading-api | phase: strategy-performance-dashboard | priority: P1
  - 전략별 실현·미실현 손익 실시간 집계
  - 일별·누적 수익률 차트, 백테스트 vs 실주문 비교
  - MDD·샤프·승률·평균 손익비 자동 계산
  - 전략 실행 서비스 완성 선행 필요

### P2 — 전략 관리

- [ ] 전략 관리 대시보드 | project: front | phase: strategy-dashboard | priority: P2
  - 전략 목록·상태(활성/비활성)·성과 요약
  - 전략별 진입·청산 이력 타임라인
  - 킬스위치 UI

### P3 — AI 인사이트 UI

- [ ] AI 전략 리포트 UI | project: front | phase: ai-strategy-report | priority: P3
  - 전략 생성·반박·비교 결과 대시보드
  - 종목 추천 랭킹 + 연관 파생 종목 테이블
  - 리스크 관리 에이전트 헤징 권고 표시
