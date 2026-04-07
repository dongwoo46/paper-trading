# FRED 저빈도 증분 수집 모드 전환

## 목적
- FRED API 과호출을 줄이고, 1시간 주기로 최신 데이터만 증분 수집한다.

## 변경 범위
- `FredObservationIngestionService`
  - 시리즈별 `max(observation_date)` 조회
  - 다음 수집 시작일을 `latest + 1 day`로 설정하여 증분 요청

- `FredClient`
  - `observation_start` 파라미터 지원
  - 최초 수집은 `sort_order=desc`(최신 우선)
  - 증분 수집은 `sort_order=asc`

- 기본 설정 변경 (`application.yaml`, `FredProperties`)
  - `polling-interval-millis`: `3600000` (1시간)
  - `polling-limit-per-series`: `60`
  - `polling-request-interval-millis`: `1500`

## 검증
- 실행: `.\gradlew.bat compileKotlin`
- 결과: 성공
