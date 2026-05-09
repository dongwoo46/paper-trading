# TODO — Paper Trading

Orchestrator가 읽어 다음 개발 대상을 선택하는 작업 목록.
`$orchestrate` 실행 시 idle 상태면 이 파일의 미완료 항목을 제안한다.

형식: `- [ ] 설명 | project: {프로젝트} | phase: {phase명} | priority: P{n}`
완료 시: `- [x]` 로 변경 + `| done: YYYY-MM-DD | pr: #{n}`

---

## 현재 실행 우선순위 (2026-05-03)

- [x] trading-api: DDD Aggregate 경계 수정 | phase: ddd-aggregate-fix | priority: P2 | done: 2026-05-03 | pr: #TBD
- [x] trading-api: 세금 정산 자동화 | phase: tax-summary | priority: P2 | done: 2026-05-03 | pr: #TBD
- [x] trading-api: 거래 일지 자동 생성 | phase: trading-journal | priority: P2 | done: 2026-05-03 | pr: #TBD
- [x] collector-api: 구독 우선순위 라우팅 | phase: subscription-routing | priority: P2 | done: 2026-05-04 | pr: #TBD

보류 정책
- 전략/퀀트 기반 자동매수 관련 개발은 후순위 보류.
- `strategy-execution`은 전략 도메인 리빌딩(저장/버전/성과기록 체계) 이후 재개.
- `quant-worker` 리서치성 작업(백테스트/알파 파이프라인)은 collector-api/trading-api 안정화 이후 진행.

---

## trading-api

### P0 — MVP 필수

- [x] Position 애플리케이션 서비스 | project: trading-api | phase: position-service | priority: P0 | done: 2026-04-24 | pr: #4
  - PositionCommandService (포지션 생성·업데이트·청산)
  - PositionQueryService (포지션 조회, 평균단가, 평가손익 계산)
  - PositionResponseDto + PositionController 응답 연결
  - 체결(Execution) 이벤트 → 포지션 자동 업데이트

- [x] T+2 정산 처리 서비스 | project: trading-api | phase: settlement-service | priority: P0 | done: 2026-04-28 | pr: #5
  - SettlementCommandService: PendingSettlement → Settlement 체결
  - T+2 체결일 도래 시 예수금 반영 + AccountLedger 기록
  - 정산 스케줄러 (매일 장 마감 후 실행)

### P1 — 운영 안정성

- [x] KIS 체결통보 WebSocket 연동 | project: trading-api | phase: kis-execution-ws | priority: P1 | done: 2026-05-02 | pr: #11
  - KIS WebSocket 체결통보 구독: H0STCNI9 (모의투자) / H0STCNI0 (실전)
  - 체결 이벤트 수신 → ExecutionProcessor.fill() → SSE 알림까지 연결
  - 기존 KisPaperPollingScheduler 제거 (WebSocket으로 대체)
  - 주문 거부/에러 응답 처리 (에러 코드 → 도메인 예외 매핑)
  - KIS 토큰 갱신 로직 안정화 (WebSocket 재인증 포함)

- [x] KIS 실계좌 데이터 조회 | project: trading-api | phase: kis-account-sync | priority: P1 | done: 2026-05-02 | pr: 미생성(phase cleanup 완료, PR 생성 대기)
  - KIS 잔고조회 API 연동 (TTTC8434R live / VTTC8434R paper) → 보유주식·평가금액·수익률
  - KisAccountQueryService: KIS 계좌 잔고 + 포지션 동기화
  - GET /api/kis/account/balance — 실계좌 잔고·평가손익 응답
  - trading-web 대시보드에 KIS 실계좌 잔고 반영 (KIS/LOCAL 계좌 구분 표시)

- [ ] 전략 실행 서비스 | project: trading-api | phase: strategy-execution | priority: P1
  - ⚠️ 보류: 전략 도메인 리빌딩 이후 진행 (자동 주문 로직 선개발 금지)
  - 리빌딩 범위: 전략 저장/버전 관리, 전략별 손익 기록, 실주문 성과 vs 백테스트 성과 분리 기록
  - StrategyCommandService: 전략 활성화/비활성화
  - 시그널(OrderSignal) 수신 → 자동 주문 생성
  - 전략 성과 스냅샷 기록 (StrategyPerformanceSnapshot)

- [x] 포지션 퍼센트 트리거 자동청산 (손절/익절) | project: trading-api | phase: position-trigger-exit | priority: P1 | done: 2026-05-08 | pr: #TBD
  - 진입가 대비 손절/익절 퍼센트 설정 저장 (계좌/포지션 단위)
  - 실시간 시세 기반 트리거 판정 (하락/상승 조건)
  - 트리거 충족 시 자동 매도 주문 실행
  - OCO 가드: 한쪽 체결 시 반대 트리거/대기주문 취소
  - 중복 실행 방지(idempotency key) + 재시도/오류 알림

- [x] Slack 알림 시스템 연동 | project: trading-api | phase: slack-notification | priority: P1 | done: 2026-05-04 | pr: #TBD
  - 체결/오류/리스크 이벤트 Slack Webhook 전송
  - 환경변수 기반 Webhook URL 관리 (비밀값 하드코딩 금지)
  - 실패 재시도/로깅 및 알림 비활성화 토글 지원

### P2 — 고도화

- [x] 거래 일지 서비스 구현 | project: trading-api | phase: trading-journal-service | priority: P2 | done: 2026-05-07 | pr: #20
  - TradingJournalCommandService: 일지 생성·수정
  - TradingJournalQueryService: 목록·상세 조회 (ticker/계좌 필터)
  - TradingJournalController + DTO 연결
  - ⚠️ 프론트 trading-journal-ui가 API 없이 완성된 상태 — 우선 연결 필요

- [x] 세금 정산 서비스 구현 | project: trading-api | phase: tax-summary-service | priority: P2 | done: 2026-05-07 | pr: #TBD
  - TaxSummaryCommandService: 연간 집계 생성·재계산
  - TaxSummaryQueryService: 계좌·연도별 조회
  - TaxSummaryController + DTO 연결
  - ⚠️ 프론트 tax-summary-ui가 API 없이 완성된 상태 — 우선 연결 필요

- [x] 포트폴리오 스냅샷 서비스 구현 | project: trading-api | phase: portfolio-snapshot-service | priority: P2 | done: 2026-05-08 | pr: pending
  - DailyBalanceCommandService: 장 마감 후 일별 잔고 집계·저장 (배치)
  - PortfolioSnapshotCommandService: 종목 비중 스냅샷 생성 (배치)
  - QueryService + Controller: 기간별 잔고 조회 API
  - ⚠️ 프론트 portfolio-chart가 API 없이 완성된 상태 — 우선 연결 필요

- [x] DDD Aggregate 경계 수정 | project: trading-api | phase: ddd-aggregate-fix | priority: P2 | done: 2026-05-03 | pr: #TBD
  - 내부 Entity Repository 제거: ExecutionRepository, SettlementRepository, RiskPolicyRepository, PendingSettlementRepository, OrderAmendmentRepository, SettlementExecutionRepository
  - 모든 접근을 Aggregate Root Repository(OrderRepository, AccountRepository)를 통하도록 변경
  - Aggregate 간 객체 직접 참조 → ID 참조로 변경 (Execution.order → orderId, Settlement.order → orderId)

- [x] 세금 정산 자동화 | project: trading-api | phase: tax-summary | priority: P2 | done: 2026-05-03 | pr: #TBD
- [x] 거래 일지 자동 생성 | project: trading-api | phase: trading-journal | priority: P2 | done: 2026-05-03 | pr: #TBD

- [x] T+2 정산 구조 정리 (ReceivableSettlement 제거) | project: trading-api | phase: settlement-cleanup | priority: P2 | done: 2026-05-09 | pr: #TBD
  - KIS 모드(KIS_PAPER/KIS_LIVE): ReceivableSettlement 생성 로직 제거 — 잔고/정산은 KIS가 처리
  - KIS 모드에서 우리 DB 예수금 변경 불필요 → ExecutionProcessor KIS 분기 단순화
  - ReceivableSettlement 엔티티·테이블·레포지토리 삭제
  - SettlementCommandService, SettlementProcessor, BusinessDayCalculator 삭제
  - Settlement 엔티티는 LOCAL 모드 P&L 기록용으로 유지
  - LOCAL 모드 매도 시 증권거래세(0.2%) 차감 누락 수정 (현재 tax=BigDecimal.ZERO 하드코딩)
  - TaxSummaryService: LOCAL 모드 집계용으로만 범위 한정 (KIS 모드 집계 제거)

### 완료
- [x] 계좌 관리 (AccountCommandService, AccountQueryService, 입출금 원장)
- [x] 주문 생성·체결·취소 (OrderCommandService, LocalMatchingEngine, KIS 실행기)
- [x] Redis 시세 구독 → LocalMatchingEngine 트리거 (QuoteEventListener)
- [x] 리스크 정책 (RiskPolicyService, RiskPolicyController)
- [x] 미결제 정산 조회 (PendingSettlementQueryService, PendingSettlementController)

---

## collector-api

### P1 — 운영 안정성

- [x] WS 재연결 안정화 | project: collector-api | phase: ws-reconnect-stability | priority: P1 | done: 2026-04-29 | pr: #8
  - heartbeat / pong timeout 감지
  - exponential backoff 재연결 (1s → 30s 상한)
  - 재연결 시 활성 구독 자동 복구
  - GET /api/kis/ws/health — 모드별 연결 상태 노출

- [x] 구독 우선순위 라우팅 | project: collector-api | phase: subscription-routing | priority: P2 | done: 2026-05-04 | pr: #TBD
  - 전략 종목 WS 우선 배정, 초과 시 REST overflow 자동화
  - WS 슬롯 해제 시 REST → WS 자동 승격
  - 내부 priority API (strategy-execution에서 호출)
  - ⚠️ strategy-execution phase 완료 후 설계할 것

- [x] 구독 상태 모니터링 API | project: collector-api | phase: subscription-monitor | priority: P1 | done: 2026-04-30 | pr: #9
  - GET /api/subscriptions/status — 현재 구독 종목·연결 상태 조회
  - 운영 대시보드 연동용

- [x] 구독 라우팅 외부 API 계약/구현 | project: collector-api | phase: subscription-routing-public-api | priority: P1 | done: 2026-05-05 | pr: #TBD
  - 목적: front/subscription-routing-ui 차단 해제용 외부 API 제공
  - 범위: favorites CRUD, strategy-priority symbol CRUD, routing status 조회 API
  - 계약 고정: request/response schema, error code, idempotency, mode/channel validation

- [x] 실시간 전략 판단용 market feature 생성 | project: collector-api | phase: realtime-market-features | priority: P1 | done: 2026-05-08 | pr: #TBD
  - 목적: WebSocket raw tick을 Redis에 장시간 누적하지 않고 전략이 바로 읽을 수 있는 feature snapshot 제공
  - Redis latest 유지: `latest:{symbol}` 현재가/호가 최신 상태 계속 갱신
  - Redis key 설계: `agg:1m:{symbol}:current`, `bars:1m:{symbol}`, `feature:{symbol}:1m`, `feature:{symbol}:5m`, `feature:{symbol}:10m`
  - tick 수신 시 `agg:1m:{symbol}:current`를 실시간 갱신 (open/high/low/close, volume, tradeValue, buyVolume, sellVolume, tickCount)
  - 분 롤오버(minute boundary) 시 기존 current bucket을 `bars:1m:{symbol}`에 append 후 새 1분 bucket 시작
  - `feature:{symbol}:1m`은 현재 시점 최근 1분 특징 snapshot으로 매 주기 덮어쓰기 저장
  - `feature:{symbol}:5m`, `feature:{symbol}:10m`은 최근 1분봉 5개/10개를 합산해 재계산 후 snapshot 갱신
  - feature 필드 표준화: open/high/low/close, returnRate, volume, tradeValue, vwap, buyVolume, sellVolume, tradeImbalance, tickCount, startedAt, updatedAt
  - `bars:1m:{symbol}`는 최근 필요 구간만 유지하도록 maxlen/TTL 정책 적용
  - raw tick은 Redis에 장기 보관하지 않으며, 필요 시 최근 30~60초 디버깅/장애 복구용 ring buffer만 둠
  - 원본 tick 장기 저장이 필요해지면 Redis가 아닌 별도 append-friendly 저장소(Kafka/ClickHouse/TimescaleDB 등)로 분리 검토

- [x] 차트용 1m/5m/10m 바 히스토리 조회 API | project: collector-api | phase: market-bars-history-api | priority: P1 | done: 2026-05-08 | pr: #TBD
  - 목적: frontend가 Redis 직접 접근 없이 시계열 바(1분/5분/10분)를 API로 조회
  - endpoint 예시: `GET /api/market/bars/{symbol}?interval=1m|5m|10m&limit={n}`
  - 응답: startedAt/open/high/low/close/volume/tradeValue/vwap/tickCount
  - 정책: invalid interval/limit 400, 데이터 없음 404, 상한 limit 강제

- [x] 보조지표 계산/조회 API | project: collector-api | phase: market-indicators-api | priority: P1 | done: 2026-05-09 | pr: #TBD
  - BB/RSI/MACD 서버 계산 및 조회 API 제공
  - interval(1m/5m/10m/1d/1w), 기간(limit/from~to), 다중 indicator 파라미터 지원
  - 차트용 응답 스키마 표준화(시점별 indicator 값, 결측 처리 규칙 포함)

- [x] 트레이딩 마이크로구조/상대강도 조회 API 확장 (뉴스/공시 제외) | project: collector-api | phase: market-microstructure-rs-api | priority: P1 | done: 2026-05-09 | pr: #TBD
  - 호가/잔량 기반 응답: best bid/ask, spread, bid-ask imbalance, depth summary
  - 체결강도/볼륨 기반 응답: buyVolume, sellVolume, tradeIntensity, vwap, rvol
  - 상대강도(RS) 응답: symbol vs benchmark/sector ratio 및 기간 수익률 상대값
  - 국내/해외 공통 조회 계약: session 구분(regular/pre/after), timezone 정규화, 결측 정책
  - 프론트 통합 차트가 바로 소비 가능한 단일 schema 제공

- [x] 마이크로구조 API 호가 WebSocket 적재/매핑 완성 | project: collector-api | phase: market-microstructure-orderbook-ws | priority: P1 | done: 2026-05-09 | pr: #28
  - KIS WebSocket 호가/호가잔량 이벤트 수집 채널 연결 및 파서/검증 추가
  - Redis 적재 키 표준화 (`quote:{symbol}`, `orderbook:{symbol}`) 및 TTL/결측 정책 확정
  - market microstructure API에서 bestBid/bestAsk/spread/depth 필드 실데이터 매핑
  - 기존 1d/1w 분기와 충돌 없이 intraday 마이크로구조 응답 안정성 검증
  - Step A: 호가 이벤트 ingest health check/metrics 추가 (수신량, 파싱 실패율, 지연)
  - Step B: Redis `orderbook:{symbol}` 스키마 고정 (topN depth, timestamp, source)
  - Step C: API 매핑 테스트 추가 (bestBid/bestAsk/spread/depth null 탈출 케이스)
  - Step D: `1d/1w` 요청 시 DB 소스 분기 강제 및 회귀 테스트 추가
  - Step E: RS 타임스탬프 정렬 검증(교집합 정렬 또는 timestamp join) 적용

### 완료
- [x] KIS WebSocket 시세 수집 + Redis Pub/Sub 발행 (RawEventPipeline)
- [x] FRED 거시지표 수집 및 조회
- [x] 일봉 OHLCV 조회 API (MarketDailyOhlcvQueryService)
- [x] 내부 구독 요청 API (POST /api/internal/subscriptions/{ticker})

---

## quant-worker

### P0 — MVP 필수

- [x] PostgreSQL 직접 적재 연동 | project: quant-worker | phase: db-persistence | priority: P0 | done: 2026-04-29 | pr: #8
  - 현재 파일 기반(data/) 출력 → PostgreSQL market_daily_ohlcv 직접 적재로 전환
  - SQLAlchemy + collector-api DB 연결 설정
  - Flyway 마이그레이션 또는 collector-api 통해 적재 방식 결정

### P1 — 운영 안정성

- [ ] 배치 스케줄 자동화 | project: quant-worker | phase: batch-schedule | priority: P1
  - 매 거래일 장 마감 후 자동 수집 스케줄 (KST 17:00)
  - 수집 실패 시 알림 + 재시도 로직

- [x] yfinance 주봉 수집 및 저장 | project: quant-worker | phase: weekly-ohlcv | priority: P1 | done: 2026-05-08 | pr: #TBD
  - yfinance `interval="1wk"` 주봉 데이터 수집 (ticker.history)
  - market_weekly_ohlcv 테이블 설계 및 SQLAlchemy 모델 추가
  - Flyway 마이그레이션 또는 collector-api DB 적재 방식 결정 (일봉과 동일 패턴)
  - POST /collect/weekly FastAPI 엔드포인트 추가
  - GET /market/weekly/{symbol} 조회 API 추가

- [ ] 국내 수급 데이터 수집 파이프라인 (외국인/기관/개인) | project: quant-worker | phase: investor-flow-pipeline | priority: P1
  - 외국인/기관/개인 순매수(일자별) 수집기 추가
  - 수급 데이터 저장 모델/리포지토리 및 배치 적재 구현
  - collector-api 조회 API에서 사용할 수 있도록 적재 계약 정리

- [x] 트레이딩 지표 원천 데이터 수집 확장 (뉴스/공시 제외) | project: quant-worker | phase: trading-indicator-source-pipeline | priority: P1 | done: 2026-05-09 | pr: #TBD
  - 국내: 호가 잔량/스프레드/체결강도/누적 거래대금(VWAP 원천)/벤치마크-섹터 RS 원천 적재
  - 해외: regular + pre/after OHLCV, bid/ask + spread + depth(가능 범위), VWAP/RVOL 원천 적재
  - 해외 수급 대체 지표: short interest, days-to-cover, shares outstanding/float 수집 파이프라인
  - 심볼 메타데이터 표준화: exchange, currency, timezone, market cap, free-float
  - collector-api 조회 API에서 바로 사용할 수 있도록 테이블/인덱스/파티션 전략 정리

### 완료
- [x] pykrx 일봉 수집기 (pykrx_daily_collector.py)
- [x] yfinance 일봉 수집기 (yfinance_daily_collector.py)
- [x] POST /collect/daily FastAPI 엔드포인트 (app.py)

---

## trading-web (front)

### P0 — MVP 필수

- [x] 계좌·포지션 대시보드 | project: front | phase: account-dashboard | priority: P0 | done: 2026-04-29 | pr: #7
  - 계좌 목록 (예수금, 평가금액, 수익률)
  - 포지션 테이블 (종목, 수량, 평균단가, 현재가, 평가손익)
  - trading-api 연동 (GET /accounts, GET /positions)

- [x] 주문 관리 UI | project: front | phase: order-management-ui | priority: P0 | done: 2026-04-29 | pr: #6
  - 주문 생성 폼 (종목 검색, 수량/가격 입력, 매수/매도, 모드 선택)
  - 주문 내역 테이블 (상태별 필터, 취소 버튼)
  - trading-api 연동 (POST /orders, GET /orders, DELETE /orders/{id})

### P1 — 운영 편의

- [x] 실시간 체결 알림 | project: front | phase: realtime-execution | priority: P1 | done: 2026-04-30 | pr: #10
  - WebSocket 또는 SSE로 체결 이벤트 수신
  - 체결 토스트 알림

- [x] 포트폴리오 수익률 차트 | project: front | phase: portfolio-chart | priority: P1 | done: 2026-05-03 | pr: #TBD
  - 일별 평가금액 추이 (DailyBalance 기반)
  - 벤치마크(KOSPI) 대비 수익률

- [x] 세금 요약 대시보드 UI | project: front | phase: tax-summary-ui | priority: P1 | done: 2026-05-04 | pr: #TBD
  - trading-api 세금 요약 조회/재계산 API 연동
  - 계좌/연도별 세금 요약 조회 및 수동 재계산 UX

- [x] 거래 일지 관리 UI | project: front | phase: trading-journal-ui | priority: P1 | done: 2026-05-04 | pr: #TBD
  - trading-api 거래 일지 생성/조회/수정 API 연동
  - 목록/상세/수정 플로우 및 ticker 필터 UX

- [x] 구독 상태 모니터링 대시보드 UI | project: front | phase: subscription-monitor-ui | priority: P1 | done: 2026-05-05 | pr: #TBD
  - 목표: **읽기 전용 모니터링 화면** (제어 액션 없음)
  - collector-api `GET /api/subscriptions/status` 연동
  - 표시 항목: `generatedAt`, `totalWsSlotUsed/Max`, mode별 `connectionStatus`, `reconnectAttempts`, `wsSymbols/restSymbols`, `wsSlotUsed/Max`
  - 기존 `RealtimePage > KisPanel`의 구독 제어 UI와 시각적으로 섹션 분리

- [x] 구독 라우팅 운영 UI | project: front | phase: subscription-routing-ui | priority: P1 | done: 2026-05-05 | pr: #TBD
  - 목표: **쓰기 중심 운영 화면** (수동 제어/정책 반영)
  - 필수 API: 수동 등록·해제, 즐겨찾기 토글/조회, 전략 종목 등록·해제, 라우팅 상태 조회
  - 운영 액션: 수동 등록·해제, 즐겨찾기 관리, 전략 우선 라우팅 반영 상태 확인
  - 선행조건: collector-api 외부 노출 API 스펙 확정(현재 내부 API/기존 KIS 구독 API와 경계 정리 필요)

- [x] 분봉 히스토리 차트 UI | project: front | phase: market-bars-chart-ui | priority: P1 | done: 2026-05-08 | pr: #25
  - collector-api `GET /api/market/bars/{symbol}?interval=1m|5m|10m&limit={n}` 연동
  - interval 탭 전환(1m/5m/10m), limit 슬라이더 또는 선택
  - OHLCV 바(캔들스틱 또는 라인) 차트 렌더링

- [ ] 통합 차트 고도화 (거래량/보조지표/수급) | project: front | phase: market-unified-indicators-ui | priority: P1
  - 거래량 패널, 볼린저 밴드(BB), RSI, MACD 표시 및 토글 UI 추가
  - 포인트(크로스헤어) 기준 OHLCV + 보조지표 값 동시 표시
  - 외국인/기관/개인 순매수 시계열 패널 추가 (collector-api 수급 API 연동)

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










