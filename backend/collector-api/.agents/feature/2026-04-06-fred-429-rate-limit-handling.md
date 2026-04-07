# FRED 429(Too Many Requests) 대응

## 목적
- FRED 주기 수집 중 `429 Too Many Requests`가 발생해 스케줄러가 실패하는 문제를 완화한다.

## 변경 범위
- `FredObservationIngestionService`
  - `429` 응답 시 재시도 + 백오프 추가
  - `Retry-After` 헤더가 있으면 우선 사용
  - 시리즈 간 요청 간격(`polling-request-interval-millis`) 추가

- `FredObservationScheduler`
  - 이전 실행이 끝나기 전에 다음 스케줄이 오면 건너뛰도록 중복 실행 방지(AtomicBoolean)

- 설정 추가
  - `collector.source.fred.polling-request-interval-millis` (기본 700ms)
  - `collector.source.fred.polling-max-retries` (기본 3)

## 검증
- 실행: `.\gradlew.bat compileKotlin`
- 결과: 성공

## 운영 가이드
- 429가 지속되면 아래 값을 더 보수적으로 조정:
  - `FRED_POLLING_REQUEST_INTERVAL_MILLIS=1000~2000`
  - `FRED_POLLING_INTERVAL_MILLIS` 증가
  - `FRED_POLLING_LIMIT_PER_SERIES` 축소
