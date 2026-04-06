# 일봉 OHLCV 저장 모델 및 조회 API

## 목적/배경
- pykrx/yfinance에서 수집한 일봉 데이터를 백테스트/차트용으로 저장 및 조회하기 위함.
- 프론트에서 심볼 목록 조회 후 상세 일봉 차트를 그릴 수 있는 API를 제공하기 위함.

## 변경 범위
- API:
  - `GET /api/pykrx/ohlcv/symbols`
  - `GET /api/yfinance/ohlcv/symbols`
  - `GET /api/pykrx/ohlcv/daily`
  - `GET /api/yfinance/ohlcv/daily`
- DB:
  - `market_daily_ohlcv` 테이블 생성
  - 컬럼: OHLCV, adjClose, provider, interval(기본 `1d`), isAdjusted, collectedAt
  - 고유키: `(source, symbol, trade_date)`
- 도메인/서비스:
  - `MarketDailyOhlcv` 엔티티
  - `MarketDailyOhlcvRepository` (심볼 집계/기간 조회)
  - `MarketDailyOhlcvQueryService` (목록/차트 조회)
  - pykrx/yfinance catalog에 `fetchedUntilDate`, `lastCollectedAt` 추적 필드 및 업데이트 API

## 검증
- 실행 명령: `.\gradlew.bat compileKotlin`
- 결과: 성공

## 후속 TODO
- worker 적재 시 upsert 정책(동일일자 재수집) 확정
- 차트 조회 API의 페이징/최대 바 수 제한 정책 추가
- 분봉 테이블 분리 필요 시 스키마 설계

