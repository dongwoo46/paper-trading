기능: 주문/포지션 API (Phase 4)
상태: 완료

개요
LOCAL(내부 체결) / KIS_PAPER(모의투자) / KIS_LIVE(실거래) 3가지 모드 지원.
모드는 계좌(Account.tradingMode)에 따라 자동 결정.

구현 파일

application
- OrderCommandService: 주문 접수/취소 유스케이스. 멱등성, 공매도 가드, 예수금 잠금.
- OrderQueryService: 주문/체결/포지션 조회.
- LocalMatchingEngine: LOCAL 모드 체결 엔진. MARKET→price, LIMIT BUY→askp1≤limitPrice, LIMIT SELL→bidp1≥limitPrice.
- ExecutionProcessor: 체결 단일 트랜잭션. Order→Execution→Position→Account→AccountLedger.
- KisPaperOrderExecutor: KIS_PAPER 주문 접수(submit) + 체결 폴링(pollFills).
- KisLiveOrderExecutor: KIS_LIVE 주문 접수/취소.

infrastructure
- RedisMarketQuoteAdapter: Redis Hash quote:{ticker} 조회. 60초 stale 감지.
- QuoteEventListener: Redis Pub/Sub quote:* 구독 → LocalMatchingEngine 트리거.
- CollectorSubscriptionAdapter: collector-api 내부 API POST/DELETE /api/internal/subscriptions/{ticker}.
- KisTokenManager: Redis kis:token:{mode} 공유 토큰. collector-api와 공유, TTL=실제 만료.
- KisOrderRestClient: KIS 주문 접수/취소/체결 조회 REST 클라이언트.
- KisPaperPollingScheduler: 3초 주기 thin wrapper → KisPaperOrderExecutor.pollFills() 호출.
- LocalMatchingScheduler: 30초 주기 GTD 만료 주문 취소.
- RestTemplateConfig: connect 5s / read 10s timeout Bean.
- OrderRepositoryImpl: QueryDSL. findActiveLocalOrdersByTicker (fetch join), findPendingKisPaperOrders (fetch join), findExpiredOrders.

presentation
- OrderController: POST/GET/DELETE /api/v1/accounts/{id}/orders
- PositionController: GET /api/v1/accounts/{id}/positions

핵심 설계 결정

멱등성: idempotencyKey로 중복 주문 방지.
데드락 방지: Account 락 먼저 → Position 락 나중.
KIS 토큰: Redis kis:token:{mode} 공유 (하루 1회 발급 한도). TTL = 실제 만료, 버퍼 없음.
stale 시세: Redis 키 없거나 updatedAt > 60s → MARKET 주문 400.
공매도 가드: SELL 주문 시 포지션 없거나 수량 부족 → 400.
IOC/FOK: 즉시 체결 불가 시 CANCELLED 처리.
KisPaperPollingScheduler: thin wrapper (infra) → pollFills (application) 구조. 스케줄러는 @Scheduled만 담당.

테스트
- OrderCommandServiceTest: MockK 단위 테스트 5개
- PositionTest: lockQuantity/unlockQuantity 단위 테스트 6개
- OrderControllerIntegrationTest: Testcontainers(Postgres+Redis), @MockitoBean KisOrderRestClient/CollectorSubscriptionPort, @BeforeEach Redis 격리