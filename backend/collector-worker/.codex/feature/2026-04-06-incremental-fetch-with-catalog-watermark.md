# Title
Catalog 워터마크 기반 증분 수집 및 상태 갱신

# Date
2026-04-06

# Owner
Codex

# Goal
각 종목의 `fetched_until_date` 이후 구간만 수집하고, 성공 시 `fetched_until_date`, `last_collected_at`를 갱신한다.

# Scope
- 포함:
  - catalog 조회 시 `fetched_until_date`, `last_collected_at` 로드
  - 종목별 증분 시작일 계산(`fetched_until_date + 1`)
  - 이미 최신이면 스킵
  - 성공/스킵 건 DB 상태 갱신
  - 실패 종목은 전체 중단 없이 계속 수행
- 제외:
  - 실패 재시도 큐

# Design (Clean Architecture boundary)
- `src/catalog/postgres_symbol_catalog.py`: 상태 조회/갱신
- `src/jobs/catalog_daily_fetch_job.py`: 증분 시작일 계산 및 per-symbol 실행
- `scripts/fetch_daily_from_catalog.py`: 수집 후 카탈로그 상태 동기화

# SRP checklist
- 상태 저장 책임을 repository에 집중: Yes
- 증분 계산 책임을 job에 집중: Yes
- 실행 orchestration은 script에서 유지: Yes

# Readability checklist
- 요약 CSV에 `success/skipped/error` 추가: Yes
- 성공/실패 카운트 출력: Yes

# Test plan
- 1차 실행 후 `fetched_until_date` 갱신 확인
- 2차 실행 시 기존 범위 재호출 없이 이후 구간/스킵 동작 확인

# Review result
- 다음 실행부터 워터마크 이후 구간만 수집 가능
