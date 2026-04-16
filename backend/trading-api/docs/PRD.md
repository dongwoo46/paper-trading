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
