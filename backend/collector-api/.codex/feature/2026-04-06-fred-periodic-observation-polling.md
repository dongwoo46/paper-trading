# FRED 주기 수집(관측치) 추가

## 목적
- 선택한 FRED 시리즈를 주기적으로 계속 가져와 DB에 적재한다.

## 변경 범위
- DB
  - `V12__create_fred_series_observation.sql`
  - 신규 테이블: `fred_series_observation`
    - 고유키: `(series_id, observation_date)`
    - 저장값: `raw_value`, `numeric_value`, `fetched_at` 등

- 서비스
  - `FredObservationIngestionService`
  - 선택된 시리즈(`enabled=true`)를 순회하여 FRED observations 조회 후 UPSERT

- 스케줄러
  - `FredObservationScheduler`
  - `@Scheduled`로 주기 실행

- 설정
  - `collector.source.fred.polling-enabled`
  - `collector.source.fred.polling-initial-delay-millis`
  - `collector.source.fred.polling-interval-millis`
  - `collector.source.fred.polling-limit-per-series`

## 검증
- 실행: `.\gradlew.bat compileKotlin`
- 결과: 성공

## 후속 TODO
- 저장된 관측치 조회 API 추가(필요 시)
- 시리즈별 마지막 수집일 기준 증분 수집 최적화
