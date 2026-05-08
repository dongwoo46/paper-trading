# market-indicators-api 완료 요약

## 완료일
2026-05-09

## 구현 범위
- `GET /api/market/indicators/{symbol}` API 구현 (BB/RSI/MACD 조합 조회)
- interval 지원: `1m`, `5m`, `10m`, `1d`, `1w`
- period 지원: `limit` 또는 `from~to` (둘 다 미입력 시 기본 `limit=200`)
- 에러 계약 정렬: `400/404/422` + 안정 코드
- 응답 스키마 정렬: `range`, `series`, `meta(missingPolicy,warnings)`
- 주봉(`1w`) 조회 시 전용 소스 부재/빈 결과 fallback (daily compose)
- `1d/1w`에서 `YYYY-MM-DD` 파싱 허용

## 스펙 준수 요약
- intervals: 준수 (`1m/5m/10m/1d/1w`)
- period modes: 준수 (`limit`, `from~to`, default `limit=200`)
- indicators: 준수 (`bb`, `rsi`, `macd`, multi-select)
- missing policy: 준수 (`null_until_window_ready`)

## 검증 결과
- `cd backend/collector-api && ./gradlew.bat test` -> BUILD SUCCESSFUL
- `cd backend/collector-api && ./gradlew.bat compileKotlin` -> BUILD SUCCESSFUL
- 2차 코드리뷰 PASS (must-fix 해소 확인)

## 알려진 제한/잔여 리스크
- non-blocking 테스트 갭 1건: `1w + limit` fallback 분기 전용 테스트 보강 필요

