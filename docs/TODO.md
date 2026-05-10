# TODO — Paper Trading

Orchestrator가 읽어 다음 개발 대상을 선택하는 작업 목록.
`$orchestrate` 실행 시 idle 상태면 이 파일의 미완료 항목을 제안한다.

형식: `- [ ] 설명 | project: {프로젝트} | phase: {phase명} | priority: P{n}`
완료 시: `- [x]` 로 변경 + `| done: YYYY-MM-DD | pr: #{n}` → TODO-DONE.md로 이동

---

## 보류 정책
- `strategy-execution`은 전략 도메인 리빌딩(저장/버전/성과기록 체계) 이후 재개.
- TimesFM/Kronos/Mirofish는 백테스팅 엔진 + 팩터 파이프라인 완성 이후 진행.
- 커뮤니티·폴리마켓 등 대안데이터는 핵심 전략 레이어 완성 이후 진행.

---

## trading-api

### P1 — 전략 실행 기반

- [ ] 전략 실행 모드 설계 결정 | project: trading-api | phase: strategy-execution-mode-design | priority: P1
  - ⚠️ strategy-execution 선행 설계 — 결정 전까지 자동 주문 구현 금지

  **설계 결정 완료 (2026-05-10 논의)**

  - ✅ 실행 모드 3개 전부 지원: `KIS_LIVE` / `KIS_PAPER` / `LOCAL`
  - ✅ 모드는 전략 생성 시 지정 (전략 내장 방식) — 전략 단위 독립 제어
  - ✅ 글로벌 킬스위치 필요 — 전체 KIS_LIVE 주문 긴급 차단용

  **데이터 저장 전략 결정 (2026-05-10 논의)**

  - ✅ 잔고 / 포지션: KIS API 실시간 호출, 우리 DB에 저장하지 않음 (drift 방지)
  - ✅ 주문 (Order): 주문 생성 시점에 우리 DB에 저장 (KIS_LIVE/KIS_PAPER/LOCAL 공통)
  - ✅ 체결 (Execution): KIS WebSocket 체결 통보 수신 → 우리 DB에 저장 (kis-execution-ws 기존 흐름 활용)
  - ✅ 전략 ID를 Order / Execution에 연결 — 전략별 성과 추적, 세금 집계, 거래 일지 귀속용

  **⚠️ 논의 필요 (구현 전 결정 필수)**
  - [ ] 논의: 전략 활성화/비활성화 제어를 UI에서 할지, API로만 할지
  - [ ] 논의: 글로벌 킬스위치 저장 위치 (DB + Redis 캐시 vs DB만)
  - 위 논의 완료 후 strategy-execution phase 설계 시작

- [ ] 전략 실행 서비스 (장투·단투 통합) | project: trading-api | phase: strategy-execution | priority: P1
  - ⚠️ 보류: 전략 실행 모드 설계 결정 + 전략 도메인 리빌딩 이후 진행
  - 장투(Swing/Position) / 단투(Day/Scalping) 전략 타입 구분 지원
  - 리빌딩 범위: 전략 저장/버전 관리, 전략별 손익 기록, 실주문 성과 vs 백테스트 성과 분리 기록
  - StrategyCommandService: 전략 활성화/비활성화, 글로벌 킬스위치
  - 시그널(OrderSignal) 수신 → 자동 주문 생성
  - 전략 성과 스냅샷 기록 (StrategyPerformanceSnapshot)

---

## quant-worker — 데이터 수집

### P1 — 핵심 데이터 (전략 생성의 입력)

- [x] 국내 수급 데이터 수집 파이프라인 (외국인/기관/개인/공매도) | project: quant-worker | phase: investor-flow-pipeline | priority: P1 | done: 2026-05-11

- [ ] 뉴스 수집 파이프라인 | project: quant-worker | phase: news-pipeline | priority: P1
  - 데이터 소스: Naver 뉴스 RSS / Google News / 연합뉴스
  - 종목별 뉴스 태깅 + 감성 점수(positive/negative/neutral) 자동 분류
  - LLM 기반 요약 + 핵심 키워드 추출
  - 수집 주기: 장중 30분마다, 장외 1회

- [ ] 공시 분석 파이프라인 (DART) | project: quant-worker | phase: dart-pipeline | priority: P1
  - DART OpenAPI 연동: 실적발표·분기보고서·주요사항 보고서
  - 공시 유형 분류 + 종목 태깅
  - 실적 서프라이즈 계산 (예측 대비 실제 EPS/매출)
  - 공시 발생 → 단기 시그널 트리거 연동

### P2 — 분석 강화 데이터

- [ ] 미국 주식 수급 데이터 수집 파이프라인 | project: quant-worker | phase: us-market-flow-pipeline | priority: P2
  - 공매도 잔고 (Short Interest): FINRA
  - 기관 보유 변화 (Institutional Ownership): SEC 13F
  - 다크풀 거래량 (Dark Pool Volume): FINRA off-exchange
  - ETF 자금 유출입: yfinance 기반
  - ⚠️ 설계 전 논의 필요: 수집 대상 종목 범위 (S&P500 / NASDAQ100 / 전체)

- [ ] 기업 펀더멘탈 데이터 수집 | project: quant-worker | phase: fundamental-pipeline | priority: P2
  - 국내: DART 재무제표 파싱 (PER/PBR/ROE/부채비율/영업이익률)
  - 미국: yfinance/Yahoo Finance 재무 데이터
  - 분기별 업데이트, 종목별 히스토리 적재
  - 밸류에이션 팩터 계산 (PER 분위, PBR 분위)

- [ ] 이벤트 캘린더 수집 | project: quant-worker | phase: event-calendar-pipeline | priority: P2
  - 국내: 실적발표 일정, 배당락일, 주주총회
  - 미국: FOMC 일정, CPI/PPI/고용지표 발표일, 어닝시즌
  - 이벤트 D-day 기준 시그널 트리거 연동

- [ ] 거시경제 분석 고도화 | project: quant-worker | phase: macro-factor-pipeline | priority: P2
  - 기존 FRED 수집 확장: 금리·환율·경기선행지수·공포지수(VIX)
  - 거시 팩터 정규화 + 시계열 DB 적재
  - 경기 사이클 판단 로직 (확장/수축/회복/침체)

### P4 — 대안 데이터

- [ ] 커뮤니티 감성 분석 | project: quant-worker | phase: community-sentiment | priority: P4
  - 네이버 종목토론실, Reddit r/Korea·r/stocks, X(Twitter)
  - 종목별 감성 점수 시계열 적재
  - 감성 급등 → 과열·공포 시그널 연동

- [ ] 폴리마켓 데이터 연동 | project: quant-worker | phase: polymarket-pipeline | priority: P4
  - 이벤트 예측 확률 수집 (금리결정·선거·경제지표)
  - 확률 변화 → 거시 이벤트 리스크 입력값으로 활용

---

## quant-worker — 전략·백테스팅

### P1 — 전략의 뼈대

- [ ] 백테스팅 엔진 기본 구조 | project: quant-worker | phase: backtest-engine | priority: P1
  - Python 기반 벡터화 백테스팅 (vectorbt 또는 자체 구현)
  - 일봉 OHLCV + 수급 + 팩터 데이터 소비
  - 성과 지표 계산: Sharpe / MDD / 연환산 수익률 / Calmar / 승률
  - 장투·단투 전략 타입별 분리 검증

- [ ] 알파 팩터 파이프라인 | project: quant-worker | phase: alpha-pipeline | priority: P1
  - 팩터 계산: 수급(외국인 순매수 모멘텀) / 기술적(RSI·BB·MACD 신호) / 밸류(PER·PBR 분위) / 거시(경기선행지수)
  - 팩터 정규화 (Z-score, 극단값 처리)
  - 팩터 IC (Information Coefficient) 분석
  - 팩터 → 백테스팅 엔진 연동

### P2 — 예측 모델

- [ ] 시계열 예측 모델 통합 (TimesFM / Kronos / MiroFish) | project: quant-worker | phase: timeseries-forecast | priority: P2
  - Google TimesFM 또는 Amazon Chronos 로컬 추론
  - MiroFish 오프라인 모델 통합 (ref: github.com/nikmcfly/MiroFish-Offline)
  - 종가·거래량 단기 예측 (1~5일)
  - 3개 모델 예측 앙상블 → 신뢰도 가중 합산
  - 예측 결과 → 전략 신호 입력값으로 활용

### P3 — AI 에이전트 전략

- [ ] 차트 분석 AI 에이전트 (차트매매용) | project: quant-worker | phase: chart-analysis-agent | priority: P3
  - 목적: 지표 표시(UI)가 아니라 현재 차트 기반 매수·매도 진입점·청산점 판단
  - 지지선·저항선 자동 탐지 (스윙 고점·저점 기반)
  - 캔들 패턴 인식 (망치형·십자형·장악형 등) → 반전/지속 신호 분류
  - 추세 판단 (상승추세·하락추세·횡보) + 추세 강도 (ADX)
  - LLM이 차트 상태를 분석 → "현재 지지선 X, 진입 권고가 Y, 손절 Z" 형태 자연어 리포트 출력
  - 분석 결과 → 전략 생성 에이전트 입력값으로 연동

- [ ] 전략 생성 AI 에이전트 | project: quant-worker | phase: strategy-gen-agent | priority: P3
  - 수집된 모든 데이터(수급·뉴스·공시·펀더멘탈·거시) 종합
  - LLM 기반 장투·단투 전략 자동 생성
  - 전략 파라미터(진입·청산·손절 조건) 명세 출력

- [ ] 전략 검증 자동화 | project: quant-worker | phase: strategy-validation | priority: P3
  - 생성된 전략 → 백테스팅 엔진 자동 연동
  - 통계적 유의성 검증 (p-value, 샘플 수 충분성)
  - 과최적화(overfitting) 탐지: Walk-forward / 몬테카를로

- [ ] 전략 반박 에이전트 (Devil's Advocate) | project: quant-worker | phase: strategy-critique-agent | priority: P3
  - 생성된 전략의 약점 자동 비판
  - 반박 근거: 시장 체제 변화, 팩터 붕괴 위험, 수수료·슬리피지 영향
  - 리스크 시나리오 자동 생성

- [ ] 여러 AI 전략 비교 | project: quant-worker | phase: multi-ai-strategy | priority: P3
  - GPT-4o / Claude / Gemini 병렬 전략 생성
  - 전략별 백테스트 성과 비교 테이블 자동 생성
  - 앙상블 전략 합성

- [ ] GraphDB + RAG 주식-뉴스 관계 종목추천 | project: quant-worker | phase: graphdb-rag-recommendation | priority: P3
  - Neo4j 그래프DB로 종목↔뉴스↔기업↔산업↔인물 관계 모델링
  - 노드: 종목, 기업, 뉴스, 공시, 섹터 / 엣지: 언급·영향·경쟁·공급망 관계
  - RAG 파이프라인: 사용자 질의 → 그래프 탐색 → 관련 뉴스·공시 컨텍스트 검색 → LLM 종목 추천 근거 생성
  - 뉴스·공시 파이프라인 완성 선행 필요

- [ ] 논문 기반 팩터 라이브러리 | project: quant-worker | phase: paper-factor-library | priority: P3
  - arXiv·SSRN 퀀트 논문에서 검증된 팩터 추출
  - 팩터 DB 구축 및 백테스팅 엔진 연동
  - 국내 시장 적용 가능성 검증

---

## quant-worker — 실행 판단

### P3 — 신호 생성

- [ ] 종목 추천 엔진 | project: quant-worker | phase: stock-recommendation | priority: P3
  - 전 레이어(수급·뉴스·펀더멘탈·거시·예측) 통합 신호
  - 종목별 점수 산정 + 랭킹
  - 장투 후보 / 단투 후보 분리 추천

### P4 — 실행 연동

- [ ] 실행 판단 에이전트 | project: quant-worker | phase: execution-agent | priority: P4
  - 종목 추천 신호 → 포지션 사이징 (Kelly Criterion / 고정 비율)
  - 최대 손실 한도 / 섹터 집중도 리스크 체크
  - trading-api OrderSignal 생성 → 자동 주문 연동
  - ⚠️ trading-api strategy-execution 완성 선행 필요

---

## trading-web (front)

### P1 — 완료

- [x] 통합 차트 고도화 (거래량/보조지표/수급/드로잉) | project: front | phase: market-unified-indicators-ui | priority: P1 | done: 2026-05-11

### P1 — 전략 성과 추적

- [ ] 전략 수익률/손실률 추적 대시보드 | project: front+trading-api | phase: strategy-performance-dashboard | priority: P1
  - trading-api: 전략별 실현·미실현 손익 실시간 집계 API
  - 일별·누적 수익률 차트 (전략 단위 분리)
  - 백테스트 성과 vs 실주문 성과 비교 시각화
  - MDD·샤프·승률·평균 손익비 자동 계산
  - 전략 실행 서비스 완성 선행 필요

### P2 — 전략 대시보드

- [ ] 내 포폴 분석 고도화 | project: front | phase: portfolio-analysis-ui | priority: P2
  - 섹터별·전략별 손익 분해
  - 리스크 지표 시각화 (MDD, 변동성, 샤프)
  - 백테스트 성과 vs 실주문 성과 비교 차트

- [ ] 전략 관리 대시보드 | project: front | phase: strategy-dashboard | priority: P2
  - 전략 목록·상태(활성/비활성)·성과 요약
  - 전략별 진입·청산 이력 타임라인
  - 킬스위치 UI

### P3 — AI 인사이트 UI

- [ ] 뉴스·공시 피드 UI | project: front | phase: news-disclosure-feed | priority: P3
  - 종목 선택 시 관련 뉴스·공시 실시간 피드
  - 감성 점수 색상 표시

- [ ] AI 전략 리포트 UI | project: front | phase: ai-strategy-report | priority: P3
  - 전략 생성·반박·비교 결과 대시보드
  - 종목 추천 랭킹 테이블
