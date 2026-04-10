realtime-quote-pubsub (Phase 4 선행 작업)
작성일: 2026-04-10
상태: 구현 중

## 목적
trading-api 주문/포지션 기반으로 종목을 자동 구독 → 실시간 시세를 Redis Pub/Sub으로 발행
→ trading-api LOCAL 체결 엔진의 실시간 매칭 트리거

## 핵심 결정사항

### WebSocket TR ID
H0STCNT0(실시간 체결가)만 사용. H0STASP0(호가) 구독 불필요.
이유: H0STCNT0 메시지에 ASKP1(매도호가1), BIDP1(매수호가1)이 이미 포함됨.
KisProperties.trIds = ["H0STCNT0"] 으로 변경.

### 파싱 대상 필드 (H0STCNT0 ^구분)
인덱스 0  MKSC_SHRN_ISCD  종목코드
인덱스 2  STCK_PRPR       현재가 (시장가 체결가)
인덱스 8  STCK_HGPR       고가
인덱스 9  STCK_LWPR       저가
인덱스 10 ASKP1           매도호가1 (지정가 매수 조건 판단)
인덱스 11 BIDP1           매수호가1 (지정가 매도 조건 판단)
인덱스 12 CNTG_VOL        체결거래량
나머지 전일대비/누적통계 등은 버림.

### 수요 기반 자동 구독 (Demand-driven Subscription)
trading-api가 주문/포지션 생성 시 collector-api에 구독 요청
→ WebSocket 자동 구독, 시세 수신 시작
주문/포지션 모두 종료 시 구독 해제 요청
→ WebSocket 해제, 슬롯 반환

구독 우선순위 (WebSocket 41슬롯 제한)
  1순위: 활성 주문/포지션 보유 종목 (자동 관리, 필수)
  2순위: 사용자 수동 관심종목 (슬롯 남을 때만, 모니터링 전용)
  슬롯 부족 시: 2순위 종목은 대기 또는 REST polling fallback

### 내부 API (trading-api → collector-api)
POST   /api/internal/subscriptions/{ticker}   구독 요청
DELETE /api/internal/subscriptions/{ticker}   구독 해제
인증: 내부 서비스간 통신, 외부 노출 없음

### Redis 시세 키 구조
key: quote:{ticker}  (예: quote:005930, quote:BTC-KRW)
type: Hash
fields:
  price      현재가 (BigDecimal string)
  askp1      매도호가1
  bidp1      매수호가1
  high       고가
  low        저가
  volume     체결거래량
  updatedAt  epoch ms (stale 판단: 60초 초과 시 시장가 주문 거부)
TTL: 300초 (시세 수신 중단 시 자연 만료)

### Redis Pub/Sub 채널
channel: quote:{ticker} (ticker별 동적 생성)
message: {"ticker":"005930","price":"75000","askp1":"75100","bidp1":"74900","updatedAt":1234567890123}
trading-api 구독 시점: PENDING 주문 보유 종목만 동적 구독/해제

## 구현 컴포넌트

### domain/kis/KisQuoteEvent.kt (완료)
H0STCNT0 파싱 결과 Value Object
fields: ticker, price, askp1, bidp1, high, low, volume, receivedAt

### application/kis/pipeline/KisRawEventParser.kt
H0STCNT0 raw 메시지 파싱 → KisQuoteEvent?
파싱 불가(호가/시스템메시지/필드부족/숫자오류) → null 반환

### infra/redis/QuoteRedisPublisher.kt
saveQuote(event): Redis Hash 저장 + TTL 300초
publishQuote(event): Redis Pub/Sub channel quote:{ticker} 발행

### application/kis/pipeline/RawEventPipeline.kt (변경)
publish(source, payload):
  1. KisRawEventParser.parse(payload) → KisQuoteEvent?
  2. QuoteRedisPublisher.saveQuote(event)
  3. QuoteRedisPublisher.publishQuote(event)

### presentation/internal/InternalSubscriptionController.kt
POST   /api/internal/subscriptions/{ticker}
DELETE /api/internal/subscriptions/{ticker}
→ KisWebSocketCollector.emit()으로 구독/해제

## 작업 분해
| # | 작업                                           | 상태   |
|---|------------------------------------------------|--------|
| 1 | KisQuoteEvent Value Object                     | 완료   |
| 2 | KisRawEventParserTest + KisRawEventParser      | 완료   |
| 3 | QuoteRedisPublisher (Hash 저장 + Pub/Sub)      | 완료   |
| 4 | RawEventPipeline.publish() 구현                | 완료   |
| 5 | KisProperties trIds H0STASP0 제거              | 완료   |
| 6 | InternalSubscriptionController                 | 완료   |
| 7 | 빌드 검증 (./gradlew compileKotlin)            | 완료   |