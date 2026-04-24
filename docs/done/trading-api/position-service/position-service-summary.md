# Position Service — 구현 완료 요약

## 완료 일자
2026-04-24

## 핵심 기능
포지션 전담 애플리케이션 서비스 계층 신설. Redis 실시간 시세 주입 + 평가손익 자동 갱신.

## 구현 범위

| 파일 | 유형 | 내용 |
|------|------|------|
| `application/position/PositionQueryService.kt` | 신규 | 보유 포지션 목록/단건 조회 + Redis 현재가 주입 |
| `application/position/PositionCommandService.kt` | 신규 | 시세 수신 시 ticker 기준 포지션 평가손익 일괄 갱신 |
| `application/position/result/PositionResult.kt` | 신규 | 현재가 포함 조회 결과 (application 계층 DTO) |
| `presentation/dto/position/PositionResponse.kt` | 신규 | 포지션 응답 DTO (dto/order/에서 분리) |
| `presentation/controller/PositionController.kt` | 수정 | OrderQueryService 의존 → PositionQueryService 전환 |
| `infrastructure/redis/QuoteEventListener.kt` | 수정 | 시세 수신 시 positionCommandService.updateCurrentPriceByTicker 추가 호출 |
| `infrastructure/persistence/PositionRepository.kt` | 수정 | findByTickerAndQuantityGreaterThan 메서드 추가 |
| `domain/model/Position.kt` | 수정 | idx_positions_ticker_qty 인덱스 추가 |
| `application/order/OrderQueryService.kt` | 수정 | listPositions/getPosition/PositionRepository 의존 제거 |
| `presentation/dto/order/OrderResponse.kt` | 수정 | PositionResponse 클래스 제거 |

## 주요 설계 결정

- **포지션 청산 상태**: 별도 status 컬럼 없음. `quantity=0` = 청산 상태. 조회 시 `quantity > 0` 필터로 자동 제외.
- **시세 저장 전략**: QuoteEventListener에서 DB 즉시 갱신(스냅샷) + 조회 시 Redis 현재가 override (이중 전략)
- **readOnly 트랜잭션에서 시세 주입**: `PositionQueryService`에서 `position.updatePrice()` 호출 후 `PositionResult`로 변환. `readOnly=true`이므로 Hibernate dirty checking 비활성 → DB flush 없음.
- **계좌 소유권 검증**: `findByAccountIdAndTicker(accountId, ticker)` 쿼리 조건으로 자동 보장.

## 트레이드오프

| 방식 | 선택 이유 |
|------|----------|
| DB 즉시 갱신 (QuoteEventListener) | 언제 조회해도 최신 평가손익 반환 가능. 고빈도 시 부하 있으나 ticker당 포지션 수 제한적 |
| 조회 시 Redis 주입 (PositionQueryService) | 응답 시점 최신 시세 보장. DB 주기적 갱신과 병행 |

## 워크플로우

```
Redis 시세 수신 → QuoteEventListener.onMessage
  → localMatchingEngine.tryMatchPendingOrders (체결 엔진)
  → positionCommandService.updateCurrentPriceByTicker (포지션 평가손익 갱신)

GET /api/v1/accounts/{accountId}/positions
  → PositionController.listPositions
    → positionQueryService.listPositionsWithCurrentPrice
      → positionRepository.findByAccountIdAndQuantityGreaterThan (quantity > 0)
      → marketQuotePort.getQuote(ticker) (Redis 현재가 주입)
      → PositionResult 조합 → PositionResponse 변환
```

## 주요 API

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/accounts/{accountId}/positions` | 보유 포지션 목록 (quantity > 0) |
| GET | `/api/v1/accounts/{accountId}/positions/{ticker}` | 단건 포지션 조회 (ticker 대소문자 무관) |

## 테스트 커버리지

| 테스트 클래스 | 유형 | 케이스 수 |
|--------------|------|-----------|
| `PositionTest` | 도메인 단위 | 11개 |
| `PositionQueryServiceTest` | 통합 (Testcontainers) | 5개 |
| `PositionCommandServiceTest` | 통합 (Testcontainers) | 3개 |
| `PositionControllerIntegrationTest` | E2E (Testcontainers + MockMvc) | 10개 |

## PR
2026-04-24 / TBD