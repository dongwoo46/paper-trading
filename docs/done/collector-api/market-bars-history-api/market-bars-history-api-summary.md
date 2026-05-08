# market-bars-history-api 완료 요약

## 개요

차트용 1m/5m/10m 바 히스토리 조회 REST API 구현.
Frontend가 Redis에 직접 접근하지 않고 collector-api를 통해 OHLCV 바 히스토리를 조회할 수 있다.

## 구현된 엔드포인트

`GET /api/market/bars/{symbol}?interval=1m|5m|10m&limit={n}`

## 주요 구현 내용

- **도메인 모델**: `MarketBar` — 모든 가격/금액 필드 BigDecimal
- **포트 인터페이스**: `MarketBarRepository.findBars(symbol, interval, limit)`
- **Redis 구현**: `MarketBarRedisRepository`
  - 1m: `LRANGE bars:1m:{symbol} -limit -1` 직접 조회
  - 5m/10m: 1m 바 조회 후 5분/10분 버킷 경계로 그룹 집계
  - vwap = tradeValue / volume (volume=0 → BigDecimal.ZERO)
- **서비스**: `MarketBarQueryService` — interval 검증, limit 클램프 [1..100]
- **컨트롤러**: `MarketBarController` — 빈 결과 → 404, 잘못된 interval → 400

## 신규 파일 목록

- `domain/marketbar/MarketBar.kt`
- `application/marketbar/port/MarketBarRepository.kt`
- `application/marketbar/service/MarketBarQueryService.kt`
- `infra/redis/MarketBarRedisRepository.kt`
- `presentation/marketbar/dto/MarketBarResponse.kt`
- `presentation/marketbar/MarketBarController.kt`
- (테스트) `MarketBarQueryServiceTest`, `MarketBarRedisRepositoryTest`, `MarketBarControllerTest`

## 검증 결과

- `./gradlew compileKotlin` PASS
- `./gradlew test` PASS — 13/13 통과 (회귀 없음)

## 완료일

2026-05-08
