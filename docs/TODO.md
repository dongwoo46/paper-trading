# TODO — Paper Trading

Orchestrator가 읽어 다음 개발 대상을 선택하는 작업 목록.
`$orchestrate` 실행 시 idle 상태면 이 파일의 미완료 항목을 제안한다.

형식: `- [ ] 설명 | project: {프로젝트} | phase: {phase명} | priority: P{n}`
완료 시: `- [x]` 로 변경 + `| done: YYYY-MM-DD | pr: #{n}` → TODO-DONE.md로 이동

---

## 보류 정책
- 전략/퀀트 기반 자동매수 관련 개발은 후순위 보류.
- `strategy-execution`은 전략 도메인 리빌딩(저장/버전/성과기록 체계) 이후 재개.
- `quant-worker` 리서치성 작업(백테스트/알파 파이프라인)은 collector-api/trading-api 안정화 이후 진행.

---

## trading-api

### P1 — 운영 안정성

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

- [ ] 전략 실행 서비스 | project: trading-api | phase: strategy-execution | priority: P1
  - ⚠️ 보류: 전략 실행 모드 설계 결정 + 전략 도메인 리빌딩 이후 진행 (자동 주문 로직 선개발 금지)
  - 리빌딩 범위: 전략 저장/버전 관리, 전략별 손익 기록, 실주문 성과 vs 백테스트 성과 분리 기록
  - StrategyCommandService: 전략 활성화/비활성화
  - 시그널(OrderSignal) 수신 → 자동 주문 생성
  - 전략 성과 스냅샷 기록 (StrategyPerformanceSnapshot)

---

## quant-worker

### P1 — 운영 안정성

- [x] 국내 수급 데이터 수집 파이프라인 (외국인/기관/개인) | project: quant-worker | phase: investor-flow-pipeline | priority: P1 | done: 2026-05-11
  - 외국인/기관/개인 순매수(일자별) 수집기 추가
  - 수급 데이터 저장 모델/리포지토리 및 배치 적재 구현
  - collector-api 조회 API에서 사용할 수 있도록 적재 계약 정리

---

## trading-web (front)

### P1 — 운영 편의

- [ ] 통합 차트 고도화 (거래량/보조지표/수급) | project: front | phase: market-unified-indicators-ui | priority: P1
  - 거래량 패널, 볼린저 밴드(BB), RSI, MACD 표시 및 토글 UI 추가
  - 포인트(크로스헤어) 기준 OHLCV + 보조지표 값 동시 표시
  - 외국인/기관/개인 순매수 시계열 패널 추가 (collector-api 수급 API 연동)

---

## quant-worker (미국 시장)

### P2 — 미장 데이터 수집

- [ ] 미국 주식 수급 데이터 수집 파이프라인 | project: quant-worker | phase: us-market-flow-pipeline | priority: P2
  - 국내와 달리 외국인/기관/개인 구분 데이터 없음 — 아래 대체 수급 지표 수집
  - 공매도 잔고 (Short Interest): FINRA 데이터, 종목별 잔고수량·비율·전일대비
  - 기관 보유 변화 (Institutional Ownership): SEC 13F 분기 기관 보유량 변화
  - 다크풀 거래량 (Dark Pool Volume): FINRA off-exchange 거래 비율
  - ETF 자금 유출입: yfinance 등 기반 주요 ETF 일별 자금 흐름
  - 데이터 소스: FINRA API (공매도/다크풀 무료), SEC EDGAR (13F), yfinance (ETF)
  - ⚠️ 설계 전 논의 필요: 수집 대상 종목 범위 (S&P500 / NASDAQ100 / 전체)

---

## quant-worker (퀀트 리서치)

### P2 — MVP 이후

- [ ] 백테스팅 엔진 기본 구조 | project: quant-worker | phase: backtest-engine | priority: P2
  - Python 기반 벡터화 백테스팅 (vectorbt 또는 자체 구현)
  - 일봉 OHLCV + FRED 데이터 소비
  - 성과 지표 계산 (Sharpe, MDD, 연환산 수익률, 최대낙폭)

- [ ] 알파 팩터 파이프라인 | project: quant-worker | phase: alpha-pipeline | priority: P2
  - 팩터 계산 → 정규화 → 백테스팅 연계
  - 팩터 IC (Information Coefficient) 분석