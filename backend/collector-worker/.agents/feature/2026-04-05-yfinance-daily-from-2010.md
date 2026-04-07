# Title
yfinance 일봉 수집 (2010-01-01 ~ 현재)

# Date
2026-04-05

# Owner
Codex

# Goal
`yfinance`에서 특정 종목의 일봉(OHLCV)을 `2010-01-01`부터 현재까지 수집하고 CSV로 저장한다.

# Scope
- 포함:
  - yfinance 일봉 조회 모듈
  - CLI 실행 스크립트
  - 공통 컬럼 스키마 정규화
- 제외:
  - DB 적재
  - 스케줄러 연동
  - 멀티 심볼 배치

# Design (Clean Architecture boundary)
- `src/collectors/yfinance_daily_collector.py`
  - 수집 요청 모델(`DailyCollectRequest`)
  - 수집기(`YFinanceDailyCollector`)
  - 정규화 책임(출력 스키마)
- `scripts/fetch_yfinance_daily.py`
  - 입력 파싱/실행 orchestration
  - 파일 저장 경로 결정

# SRP checklist
- 수집 로직과 CLI 입력 처리 분리: Yes
- 정규화 책임을 Collector 내부 단일 메서드로 집중: Yes
- 저장 책임(`save_csv`) 분리: Yes

# Readability checklist
- 명확한 클래스/함수명 사용: Yes
- 짧은 함수 단위 유지: Yes
- guard clause(`start_date <= end_date`) 적용: Yes

# Test plan
- 수동 테스트:
  - `python scripts/fetch_yfinance_daily.py --symbol AAPL`
  - `python scripts/fetch_yfinance_daily.py --symbol MSFT --start 2015-01-01 --end 2020-12-31`
- 검증 포인트:
  - CSV 생성 여부
  - 컬럼 스키마/정렬 확인
  - 빈 데이터 응답 시 빈 CSV 처리

# Review result
- 구조/책임 분리 기준 충족
- 다음 확장:
  - DB upsert repository
  - retry/backoff 정책 주입
