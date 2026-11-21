# Trading 도메인 — 주문·체결·계좌·정산 (paper-trading)
<!-- devkit:pointer
path: ^backend/(trading-api/src/main/.+\.kt|quant-[a-z]+/src/.+\.py)$
why: 계좌·주문·체결·포지션·정산의 금액과 상태 전이를 다루는 코드(trading-api, 퀀트 금액 계산)
-->

> 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.

## 금융 안전

### 루트 CLAUDE.md

- Use `BigDecimal` for Kotlin/Java monetary calculations
- Use `Decimal` for Python monetary calculations
- Order/Execution/Settlement state transitions must be explicit
- Never mutate financial state implicitly
- All financial state changes must be auditable and event-traceable

### README.md 개발 원칙

- 금액 계산에 부동소수점을 쓰지 않습니다(Kotlin `BigDecimal`, Python `Decimal`, 화면은 문자열).
- 주문 처리 흐름은 멱등이고, 주문·체결·정산 상태 전이는 명시적이며 모두 추적 가능해야 합니다.

## 제품 요구 — 옛 `docs/PRD.md`

# PRD — Paper Trading System

## 목적
모의투자(로컬/KIS) + 실거래(KIS/Upbit) 를 통합 운영하는 알고리즘 트레이딩 플랫폼.
시장 데이터 수집 → 전략 실행 → 주문/체결/정산 → 포트폴리오 분석 전 과정을 커버한다.

## 서비스 구성

| 서비스 | 역할 |
| --- | --- |
| trading-api | 계좌·주문·체결·포지션·정산·전략 |
| collector-api | 시장 데이터 수집·구독 관리·Redis 시세 발행 |
| quant-worker | 일봉 OHLCV 배치 수집 (pykrx/yfinance) |
| trading-web | 운영 대시보드 UI |

## 핵심 기능 (MVP)
- 계좌 생성 및 예수금 관리 (입출금, 원장)
- 주문 생성/체결/취소 — LOCAL / KIS_PAPER / KIS_LIVE 3모드
- 실시간 시세 수집 (KIS WebSocket) → Redis Pub/Sub → 체결 엔진 트리거
- 일봉 OHLCV 수집 및 조회 (pykrx, yfinance, 2010~현재)
- 매크로 지표 수집 (FRED)
- 운영 대시보드 (구독 관리, OHLCV 차트, 매크로 조회)

## MVP 제외
- 전략 자동 실행 / 백테스트 엔진
- Upbit 실거래 어댑터
- DART/EDGAR 공시 수집
- 뉴스/감성 분석 파이프라인
- 분봉 데이터 수집
- 세금 정산 자동화
- 다중 사용자 / 권한 관리

## 서비스 간 데이터 흐름

```
KIS WebSocket
    └─→ collector-api (RawEventPipeline)
            ├─→ Redis Hash  quote:{ticker}     (최신 시세)
            └─→ Redis Pub/Sub  quote:{ticker}  (체결 트리거)
                    └─→ trading-api (QuoteEventListener → LocalMatchingEngine)

collector-api HTTP API
    └─→ quant-worker POST /collect/daily
            └─→ PostgreSQL market_daily_ohlcv  (일봉 적재)

trading-api → collector-api POST /api/internal/subscriptions/{ticker}
    (주문/포지션 생성 시 자동 구독 요청)
```

## 제품 요구 — 옛 `backend/trading-api/docs/PRD.md`

# PRD — trading-api

## 역할
계좌·주문·체결·포지션·정산·전략 실행 도메인 서비스.

## 아키텍처

### 디렉토리 구조
```
src/
└── main/kotlin/com/papertrading/trading/
    ├── domain/
    │   ├── account/        Account, AccountLedger, RiskPolicy, PendingSettlement
    │   ├── order/          Order, Execution, Settlement
    │   └── position/       Position
    ├── application/
    │   ├── account/        AccountCommandService, AccountQueryService, RiskPolicyService
    │   ├── order/          OrderCommandService, OrderQueryService, LocalMatchingEngine, ExecutionProcessor
    │   └── position/       PositionQueryService
    ├── infrastructure/
    │   ├── external/kis/   KisOrderRestClient, KisTokenManager, KisPaperOrderExecutor
    │   ├── redis/          RedisMarketQuoteAdapter, QuoteEventListener
    │   └── persistence/    JPA Repository 구현체
    └── interfaces/
        └── http/           OrderController, PositionController, AccountController
```

### 패턴
- Clean Architecture: presentation → application → domain ← infrastructure
- Strategy Pattern: OrderExecutionPort (LOCAL / KIS_PAPER / KIS_LIVE)
- Aggregate Root: Account, Order, Position (도메인 메서드로만 상태 변경)
- Pessimistic Lock: 입출금 (SELECT FOR UPDATE, timeout 3000ms)
- Optimistic Lock: Order (@Version)

### 데이터 흐름
```
HTTP 요청
  → OrderController
  → OrderCommandService (예수금 잠금, 멱등성 체크)
  → OrderExecutionPort.submit()
      ├── LOCAL: LocalMatchingEngine 즉시 체결 시도
      ├── KIS_PAPER: KisOrderRestClient → KIS REST API
      └── KIS_LIVE: KisOrderRestClient → KIS REST API

Redis Pub/Sub quote:{ticker}
  → QuoteEventListener
  → LocalMatchingEngine.tryMatch(ticker)
  → ExecutionProcessor (단일 트랜잭션: Order→Execution→Position→Account→Ledger)
```

## 핵심 기능 (MVP)
- 계좌 CRUD + 입출금 (멱등, 비관적 락)
- 주문 생성/취소/조회 (시장가/지정가, DAY/GTC/IOC/FOK/GTD)
- LOCAL 체결 엔진 (Redis Pub/Sub 트리거 + 30초 보조 스케줄러)
- KIS_PAPER 체결 (3초 폴링)
- 포지션 조회 (평균단가, 평가금액, 수익률)
- 리스크 정책 관리

## 미구현 (제외)
- 정산 손익 조회 API
- 포트폴리오 조회 API
- 전략 등록/실행 API
- Upbit 실거래 어댑터
- 분봉/틱 데이터 연동
- 세금 정산 자동화
