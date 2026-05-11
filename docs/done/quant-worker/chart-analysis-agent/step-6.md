# Step 6: 사전계산 파이프라인 + 큐 처리 + APScheduler

Assigned agent: quant-dev

## Working Directory
.worktrees/quant-worker-chart-analysis-agent

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/spec.md (§7 Precompute Pipeline)
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/decisions.md (§6, §7, §8, §9)
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/src/jobs/batch_schedule.py (기존 APScheduler 패턴)
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/src/jobs/catalog_daily_fetch_job.py (수집 잡 체인 진입점)
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/scripts/fetch_daily_from_catalog.py
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/src/chart_analysis/ (Step 2-5 산출물)

## Open Questions
없음.

## Confirmed Design Choices
- 트리거: 데이터 수집 잡 완료 → 수치 분석 → LLM 체인 (decisions §6)
- 콘텐트 해시 비교로 무변동 윈도우 LLM 재호출 스킵 (decisions §7)
- TOP 300 인기 종목 주 1회 갱신 (월요일 04:00 KST) (decisions §8)
- 인기 종목 점수: `0.5*z(market_cap) + 0.5*z(60d_avg_amount)` (spec §7-3)
- 큐 처리는 운영자가 수동 실행: `python -m scripts.process_llm_request_queue` (decisions §9)
- APScheduler를 FastAPI lifespan에 통합 (기존 `src/interfaces/api/app.py` 패턴)
- **Slack 알림**: `PrecomputePipelineService.run_for_market()` 완료 시 배치 결과(성공/실패 종목 수) Slack 알림 (Step 5에서 구현된 `SlackWebhookNotifier` 주입)

## Tasks

### Substep 6-1: `PrecomputePipelineService` (Application)
1. (TEST FIRST) `tests/unit/chart_analysis/application/test_precompute_pipeline_service.py`
   - 시나리오 1: snapshot_hash 동일 → 수치/LLM 모두 스킵, `numeric_computed_at`만 갱신
   - 시나리오 2: snapshot_hash 다름 + TOP300 → 수치 + LLM 호출
   - 시나리오 3: snapshot_hash 다름 + 비인기 → 수치만, LLM 스킵
   - 시나리오 4: 일부 윈도우 OHLCV 데이터 부족 → 해당 윈도우 스킵 + 경고 로그
   - 시나리오 5: `run_for_market` 완료 → `slack_notifier.notify_batch_completed(market, success, failed)` 1회 호출 검증
   - 시나리오 6: `run_for_market` 중 전체 실패 → Slack 알림 전송 검증
2. `src/chart_analysis/application/precompute_pipeline_service.py`
   - 생성자: 모든 도메인 포트 + 5개 calculator + `SlackWebhookNotifier` 주입
   - 메서드:
     - `run_for_symbol(symbol, is_popular: bool)` — 7 윈도우 순회
     - `run_for_market(market: 'KRX'|'US')` — 시장별 종목 리스트 순회 → 완료 후 `notify_batch_completed(market, success_count, failed_count)` 호출
   - 윈도우별 로직:
     1. `OhlcvRepository.find_window(symbol, window, interval)` → candles
     2. `IndicatorCalculator.compute(candles)` → indicators
     3. `ChartSnapshot` 생성 → `compute_hash()`
     4. 기존 row hash 비교 → 동일 시 `numeric_computed_at`만 update + LLM 스킵
     5. 다름 시: `SupportResistanceFinder`, `PatternDetector`, `TrendClassifier`, `ConfidenceScorer` 호출 → `ChartAnalysisResult` 조립
     6. `is_popular=True` 일 때만 `LlmReportGenerator.generate(...)` 호출 → result.with_report
     7. `ChartAnalysisRepository.upsert(result)`

### Substep 6-2: 시장별 트리거 함수
1. (TEST FIRST) `tests/unit/chart_analysis/application/test_market_pipeline_triggers.py`
   - `run_krx_pipeline()` → KRX symbol list 조회 → `PrecomputePipelineService.run_for_market('KRX')`
   - `run_us_pipeline()` → US symbol list 조회 → `run_for_market('US')`
2. `src/chart_analysis/application/market_pipeline_triggers.py`
   - 함수: `run_krx_chart_analysis_pipeline()`, `run_us_chart_analysis_pipeline()`, `run_weekly_chart_analysis_pipeline()`
   - 종목 리스트는 기존 `postgres_symbol_catalog` 활용
   - `popular_symbols` 조회로 is_popular flag 결정

### Substep 6-3: `PopularSymbolsRefreshService` (주 1회)
1. (TEST FIRST) `tests/unit/chart_analysis/application/test_popular_symbols_refresh_service.py`
   - z-score 정규화 + 가중치 0.5/0.5 합산 → score 산출
   - 미장 + 국장 통합 TOP 300 (`CHART_ANALYSIS_POPULAR_TOP_N` 환경변수)
   - TRUNCATE + INSERT 트랜잭션 (구버전 row 완전 교체)
   - 미장:국장 비율은 동적 (실제 점수 기반)
2. `src/chart_analysis/application/popular_symbols_refresh_service.py`
   - 클래스: `PopularSymbolsRefreshService`
   - 메서드: `refresh() -> int` (반환: 삽입된 row 수)
   - 의존성: market_cap 조회 + 60d 평균 거래대금 조회 헬퍼

### Substep 6-4: APScheduler 통합
1. (TEST FIRST) `tests/unit/chart_analysis/jobs/test_chart_analysis_schedule.py`
   - `start_chart_analysis_scheduler()` 호출 시 4개 잡 등록 확인:
     - KRX 차트 분석 (KRX 수집 잡 완료 후)
     - 미장 차트 분석 (yfinance 수집 잡 완료 후)
     - 주봉 차트 분석 (주봉 수집 잡 완료 후)
     - 인기 종목 갱신 (CRON: 월요일 04:00 KST)
   - shutdown 정상 동작
2. `src/jobs/chart_analysis_schedule.py`
   - 함수: `start_chart_analysis_scheduler() -> BackgroundScheduler`, `stop_chart_analysis_scheduler(scheduler)`
   - APScheduler `EVENT_JOB_EXECUTED` 리스너로 수집 잡 → 차트 분석 잡 chain
   - CronTrigger("0 4 * * 1") for popular refresh
3. `src/interfaces/api/app.py` lifespan 수정:
   - `start_chart_analysis_scheduler` 시작 / 종료 등록

### Substep 6-5: 큐 처리 스크립트 (운영자 수동)
1. (TEST FIRST) `tests/unit/chart_analysis/scripts/test_process_llm_request_queue.py`
   - `--limit N` 인자 파싱
   - pending → processing → completed/failed 상태 전이
   - LLM 호출 실패 시 `status='failed'`, processed_at 기록
   - `chart_analysis_result.llm_report` 업데이트
2. `backend/quant-worker/scripts/process_llm_request_queue.py`
   - main 함수 + `run(limit: int)` 분리
   - DI: 환경변수로 모든 어댑터 생성
   - 로깅: 처리 시작/완료/실패 명시
3. README 또는 docstring에 실행 명령 표기: `python -m scripts.process_llm_request_queue --limit 50`

### Substep 6-6: 환경변수 + requirements 최종 점검
1. `requirements.txt` 추가 확인: `apscheduler==3.10.4`
2. spec §13 환경변수 모두 `.env.example` 또는 docker-compose에 반영 (선택, 운영자 가이드)
3. `python -m py_compile` 모든 새 파일 통과
4. 단위 테스트 100% 통과

## Acceptance Criteria
- 시장별(국장/미장/주봉) 파이프라인 트리거 3개 동작
- 콘텐트 해시 비교로 무변동 윈도우 LLM 스킵 단위 테스트 입증
- 인기 종목 TOP 300 갱신 잡 동작 (TRUNCATE+INSERT)
- APScheduler 4개 잡 등록 확인
- 큐 처리 스크립트 단위 테스트 통과
- `python -m py_compile` 통과
- 커밋 메시지 한국어

## Agent Return Protocol
---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <한두 문장>
- Files modified: <목록>
- Test result: <pytest 결과>
- Blockers: <none | description>
---
