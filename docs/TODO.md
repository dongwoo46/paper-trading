# TODO — Paper Trading

Orchestrator가 읽어 다음 개발 대상을 선택하는 작업 목록.
`/orchestrate` 실행 시 idle 상태면 이 파일의 미완료 항목을 제안한다.

형식: `- [ ] 설명 | project: {프로젝트} | phase: {phase명} | priority: P{n}`
완료 시: `- [x]` 로 변경 + `| done: YYYY-MM-DD | pr: #{n}`

---

## trading-api

### P0 — MVP 필수

- [x] Position 애플리케이션 서비스 | project: trading-api | phase: position-service | priority: P0 | done: 2026-04-24 | pr: #4
  - PositionCommandService (포지션 생성·업데이트·청산)
  - PositionQueryService (포지션 조회, 평균단가, 평가손익 계산)
  - PositionResponseDto + PositionController 응답 연결
  - 체결(Execution) 이벤트 → 포지션 자동 업데이트

- [ ] T+2 정산 처리 서비스 | project: trading-api | phase: settlement-service | priority: P0
  - SettlementCommandService: PendingSettlement → Settlement 체결
  - T+2 체결일 도래 시 예수금 반영 + AccountLedger 기록
  - 정산 스케줄러 (매일 장 마감 후 실행)

### P1 — 운영 안정성

- [ ] KIS LIVE 주문 연동 완성 | project: trading-api | phase: kis-live-order | priority: P1
  - KisLiveOrderExecutor 실주문 흐름 E2E 검증
  - 주문 거부/에러 응답 처리 (에러 코드 → 도메인 예외 매핑)
  - KIS 토큰 갱신 로직 안정화

- [ ] 전략 실행 서비스 | project: trading-api | phase: strategy-execution | priority: P1
  - StrategyCommandService: 전략 활성화/비활성화
  - 시그널(OrderSignal) 수신 → 자동 주문 생성
  - 전략 성과 스냅샷 기록 (StrategyPerformanceSnapshot)

### P2 — 고도화

- [ ] 세금 정산 자동화 | project: trading-api | phase: tax-summary | priority: P2
- [ ] 거래 일지 자동 생성 | project: trading-api | phase: trading-journal | priority: P2

### 완료
- [x] 계좌 관리 (AccountCommandService, AccountQueryService, 입출금 원장)
- [x] 주문 생성·체결·취소 (OrderCommandService, LocalMatchingEngine, KIS 실행기)
- [x] Redis 시세 구독 → LocalMatchingEngine 트리거 (QuoteEventListener)
- [x] 리스크 정책 (RiskPolicyService, RiskPolicyController)
- [x] 미결제 정산 조회 (PendingSettlementQueryService, PendingSettlementController)

---

## collector-api

### P1 — 운영 안정성

- [ ] REST 폴링 fallback 안정화 | project: collector-api | phase: rest-polling-fallback | priority: P1
  - WebSocket 연결 끊김 시 REST 폴링으로 자동 전환
  - 재연결 backoff 전략 구체화 및 테스트

- [ ] 구독 상태 모니터링 API | project: collector-api | phase: subscription-monitor | priority: P1
  - GET /api/subscriptions/status — 현재 구독 종목·연결 상태 조회
  - 운영 대시보드 연동용

### 완료
- [x] KIS WebSocket 시세 수집 + Redis Pub/Sub 발행 (RawEventPipeline)
- [x] FRED 거시지표 수집 및 조회
- [x] 일봉 OHLCV 조회 API (MarketDailyOhlcvQueryService)
- [x] 내부 구독 요청 API (POST /api/internal/subscriptions/{ticker})

---

## quant-worker

### P0 — MVP 필수

- [ ] PostgreSQL 직접 적재 연동 | project: quant-worker | phase: db-persistence | priority: P0
  - 현재 파일 기반(data/) 출력 → PostgreSQL market_daily_ohlcv 직접 적재로 전환
  - SQLAlchemy + collector-api DB 연결 설정
  - Flyway 마이그레이션 또는 collector-api 통해 적재 방식 결정

### P1 — 운영 안정성

- [ ] 배치 스케줄 자동화 | project: quant-worker | phase: batch-schedule | priority: P1
  - 매 거래일 장 마감 후 자동 수집 스케줄 (KST 17:00)
  - 수집 실패 시 알림 + 재시도 로직

### 완료
- [x] pykrx 일봉 수집기 (pykrx_daily_collector.py)
- [x] yfinance 일봉 수집기 (yfinance_daily_collector.py)
- [x] POST /collect/daily FastAPI 엔드포인트 (app.py)

---

## trading-web (front)

### P0 — MVP 필수

- [ ] 계좌·포지션 대시보드 | project: front | phase: account-dashboard | priority: P0
  - 계좌 목록 (예수금, 평가금액, 수익률)
  - 포지션 테이블 (종목, 수량, 평균단가, 현재가, 평가손익)
  - trading-api 연동 (GET /accounts, GET /positions)

- [ ] 주문 관리 UI | project: front | phase: order-management-ui | priority: P0
  - 주문 생성 폼 (종목 검색, 수량/가격 입력, 매수/매도, 모드 선택)
  - 주문 내역 테이블 (상태별 필터, 취소 버튼)
  - trading-api 연동 (POST /orders, GET /orders, DELETE /orders/{id})

### P1 — 운영 편의

- [ ] 실시간 체결 알림 | project: front | phase: realtime-execution | priority: P1
  - WebSocket 또는 SSE로 체결 이벤트 수신
  - 체결 토스트 알림

- [ ] 포트폴리오 수익률 차트 | project: front | phase: portfolio-chart | priority: P1
  - 일별 평가금액 추이 (DailyBalance 기반)
  - 벤치마크(KOSPI) 대비 수익률

### 완료
- [x] 실시간 시세 페이지 (RealtimePage — KIS WebSocket 연동)
- [x] 일봉 차트 페이지 (HistoricalPage — OHLCV 조회)
- [x] 매크로 지표 페이지 (MacroPage — FRED 연동)
- [x] 구독 관리 (KisPanel, CatalogManagement)

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
