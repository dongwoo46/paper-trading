# service-role-refactor — Phase Summary

**완료일**: 2026-05-14  
**브랜치**: feature/quant-ai-service-role-refactor  
**테스트**: 22 passed (quant-research 19, quant-ai 3)

## 문제

quant-ai가 기술분석 파이프라인 오케스트레이션 + DB 저장 + LLM 보고서 생성을 모두 담당하는 역할 역전 구조.
quant-research는 stateless HTTP 계산 서비스에 불과했음.

## 변경 내용

### quant-research (신규 추가)

| 범주 | 추가 내용 |
|------|-----------|
| DB 인프라 | `src/infrastructure/db.py`, `migration_runner.py` |
| Migrations | `001_chart_analysis_result.sql`, `002_analysis_request_queue.sql`, `003_popular_symbols.sql` |
| 저장소 | `PostgresOhlcvRepository`, `PostgresChartAnalysisRepository`, `PostgresAnalysisRequestQueueRepository` |
| 서비스 | `PrecomputePipelineService`, `PopularSymbolsRefreshService`, `market_pipeline_triggers` |
| 배치 | `APScheduler` (KRX/US/주봉 일배치) |
| API 엔드포인트 | `GET /research/results/{symbol}`, `GET /research/symbols`, `POST /research/run/{symbol}` |
| lifespan | 시작 시 migrations 자동 실행 + 스케줄러 시작 |

### quant-ai (제거/정리)

| 범주 | 제거 내용 |
|------|-----------|
| 파일 삭제 | `ohlcv_repository.py`, `chart_analysis_repository.py`, `analysis_request_queue_repository.py`, `precompute_pipeline_service.py`, `market_pipeline_triggers.py`, `popular_symbols_refresh_service.py`, `analyze_chart_service.py`, `chart_analysis_schedule.py` |
| 엔드포인트 제거 | `POST /chart-analysis/{symbol}`, `GET /admin/symbols`, `POST /admin/run-analysis/{symbol}` |
| ports.py 정리 | 계산기 포트 5종 제거, `LlmReportGenerator`만 유지 |
| APScheduler | lifespan에서 완전 제거 |
| 테스트 삭제 | 이전된 파일 대응 테스트 8개 삭제 |

## 아키텍처 결정

| 결정 | 선택 | 이유 |
|------|------|------|
| 수치 조회 엔드포인트 | quant-research로 완전 이전 | DB 소유권 일원화 |
| 배치 스케줄러 | quant-research로 이전 | 단일 책임 원칙 |
| LLM 트리거 | analysis_request_queue enqueue | 서비스 간 HTTP 의존 제거, 느슨한 결합 |
| 도메인 모델 | 서비스별 독립 유지 | 결합도 최소화 |
| DB 스키마 | Python lifespan 자동 생성 | Kotlin Flyway 의존 제거 |

## 코드 리뷰 Must Fix 처리

1. `popular_symbols_refresh_service.py` — float → Decimal z-score 재작성 완료
2. `test_results_router.py` — 빈 symbol 422 테스트 추가 완료
