# Portfolio Snapshot Service Summary

- Project: trading-api
- Phase: portfolio-snapshot-service
- Completed date: 2026-05-08
- Branch: feature/trading-api-portfolio-snapshot-service

## Scope Delivered
- DailyBalance/PortfolioSnapshot 도메인 및 마이그레이션 추가
- 배치 커맨드 서비스 구현 (upsert, BigDecimal 계산)
- 조회 QueryService 및 PortfolioSnapshotController/API 계약 구현
- 에러 계약 구현: INVALID_DATE_RANGE, INVALID_BUSINESS_DATE, SNAPSHOT_ALREADY_RUNNING, SNAPSHOT_COMPUTE_FAILED
- SnapshotJobService 기반 원자성 보장 및 중복 실행 방지(run table + partial unique index)
- 행동 기반 원자성 롤백 통합 테스트 추가 (실패 시 partial commit 없음 검증)

## Validation
- compileKotlin 통과
- 타깃 테스트 통과
  - Daily/Portfolio command/query/controller tests
  - SnapshotJobService conflict/failure tests
  - SnapshotJobServiceAtomicRollbackIntegrationTest

## Key Files
- src/main/resources/db/migration/V4__portfolio_snapshot_service.sql
- src/main/resources/db/migration/V5__snapshot_job_runs.sql
- src/main/kotlin/com/papertrading/api/application/portfolio/*
- src/main/kotlin/com/papertrading/api/presentation/controller/PortfolioSnapshotController.kt
- src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt
- src/test/kotlin/com/papertrading/api/application/portfolio/*
- src/test/kotlin/com/papertrading/api/presentation/controller/PortfolioSnapshotControllerTest.kt

## PR Status
- Pending (PR not created yet)