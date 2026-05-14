# Step 2: 도메인 계층 구현 (Domain Layer)

Assigned agent: quant-dev

## Working Directory
.worktrees/quant-worker-chart-analysis-agent

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/spec.md (§11 DDD Model Summary)
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/decisions.md (§13 DDD 모델, §18 금융 안전)

## Open Questions
없음 (Pass A에서 모두 확정됨).

## Confirmed Design Choices
- Aggregate Roots 2개: `ChartSnapshot` (입력), `ChartAnalysisResult` (출력) — decisions §13
- Value Objects: Candle, IndicatorSet, TrendAnalysis, LevelSet, TradePlan, CandlePattern, IndicatorSignal, VolumeAnalysis, Recommendation, NarrativeReport — decisions §13
- Domain Ports (Protocol): OhlcvRepository, ChartAnalysisRepository, AnalysisRequestQueueRepository, LlmReportGenerator — decisions §13
- 모든 가격/confidence 필드 `Decimal` 강제 — decisions §18, financial safety
- 도메인 계층은 LangChain/SQLAlchemy/pandas 직접 import 금지 (의존성 역전)
- 디렉토리: `backend/quant-worker/src/chart_analysis/domain/` — decisions §14

## Tasks

### Substep 2-1: Value Objects 정의
1. (TEST FIRST) `tests/unit/chart_analysis/domain/test_value_objects.py` 작성
   - 각 VO 생성 시 `Decimal`이 아닌 가격 입력 시 `TypeError` 또는 `ValueError`
   - `Recommendation.grade`가 5단계 enum
   - `TrendAnalysis.direction`/`strength` enum 검증
   - `NarrativeReport`의 5섹션 필드 모두 비어있지 않음 validator
2. `src/chart_analysis/domain/value_objects.py` 작성
   - `Candle`, `IndicatorSet`, `TrendAnalysis`, `LevelSet`, `TradePlan`, `CandlePattern`, `IndicatorSignal`, `VolumeAnalysis`, `Recommendation`, `NarrativeReport`
   - 모두 `@dataclass(frozen=True)` 또는 Pydantic `BaseModel`(frozen) 중 일관성 있게 채택 (권고: 도메인은 dataclass, LLM 출력만 Pydantic)
   - Enum: `Direction(UPTREND/DOWNTREND/SIDEWAYS)`, `Strength(WEAK/MEDIUM/STRONG)`, `Grade(STRONG_BUY/BUY/HOLD/SELL/STRONG_SELL)`, `PatternType(DOJI/HAMMER/INVERTED_HAMMER/BULLISH_ENGULFING/BEARISH_ENGULFING/MORNING_STAR/EVENING_STAR)`, `ReportSource(LLM_PRIMARY/RULE_TEMPLATE/NONE)`

### Substep 2-2: Aggregate Root — `ChartSnapshot`
1. (TEST FIRST) `tests/unit/chart_analysis/domain/test_chart_snapshot.py`
   - 식별자 `(symbol, window, interval)` 유효성 (window ∈ {1M,3M,6M,1Y,2Y,MAX}, interval ∈ {D,W})
   - `compute_hash()` 결정성 (같은 입력 → 같은 해시), 입력 변동 → 다른 해시
   - 빈 candles 입력 시 `ValueError`
2. `src/chart_analysis/domain/chart_snapshot.py`
   - 필드: symbol, window, interval, candles: list[Candle], indicators: IndicatorSet
   - 메서드: `compute_hash() -> str` (sha256 of normalized json — Decimal → str)
   - 메서드: `last_close() -> Decimal`, `length() -> int`

### Substep 2-3: Aggregate Root — `ChartAnalysisResult`
1. (TEST FIRST) `tests/unit/chart_analysis/domain/test_chart_analysis_result.py`
   - 동일 식별자로 다른 분석 결과 생성 시 동일 PK
   - `with_report(NarrativeReport)` 메서드는 새로운 인스턴스 반환 (immutable)
   - `report_source` 기본값 `NONE`
2. `src/chart_analysis/domain/chart_analysis_result.py`
   - 필드: symbol, window, interval, snapshot_hash, trend: TrendAnalysis, levels: LevelSet, trade_plan: TradePlan, patterns: list[CandlePattern], indicator_signals: list[IndicatorSignal], volume_analysis: VolumeAnalysis, recommendation: Recommendation, report: NarrativeReport | None, report_source: ReportSource, numeric_computed_at, llm_computed_at
   - 메서드: `with_report(report)`, `to_response_dict()` (직렬화 헬퍼 — 모든 Decimal → str)

### Substep 2-4: Domain Ports (Protocol)
1. (TEST FIRST) `tests/unit/chart_analysis/domain/test_ports_signature.py`
   - 각 Protocol을 inmem fake로 구현해 인터페이스 호환 검증
2. `src/chart_analysis/domain/ports.py`
   - `class OhlcvRepository(Protocol)`: `find_window(symbol, window, interval) -> list[Candle]`
   - `class ChartAnalysisRepository(Protocol)`: `upsert(result)`, `find_by_symbol(symbol) -> list[ChartAnalysisResult]`, `find_one(symbol, window, interval) -> ChartAnalysisResult | None`
   - `class AnalysisRequestQueueRepository(Protocol)`: `enqueue(symbol, window, interval)`, `fetch_pending(limit) -> list[QueueItem]`, `mark_processed(id, status)`
   - `class LlmReportGenerator(Protocol)`: `generate(snapshot, result) -> NarrativeReport`
   - 도메인 서비스 포트 (선택, 인프라로 옮길 수 있음):
     - `IndicatorCalculator(Protocol)`, `SupportResistanceFinder(Protocol)`, `PatternDetector(Protocol)`, `TrendClassifier(Protocol)`, `ConfidenceScorer(Protocol)`

### Substep 2-5: 모듈 초기화 + 검증
1. `src/chart_analysis/__init__.py`, `src/chart_analysis/domain/__init__.py` 작성 (public re-export)
2. `python -m py_compile` 모든 새 파일에 대해 성공 확인
3. 단위 테스트 100% 통과 확인

## Acceptance Criteria
- 모든 VO/Aggregate 단위 테스트 통과 (TDD: Red → Green 순서 commit 이력)
- 도메인 계층 코드에서 `langchain`, `sqlalchemy`, `pandas`, `pandas_ta`, `scipy`, `redis` import 0건
- 모든 가격/confidence 필드 `Decimal` 타입, 검증 테스트 포함
- `ChartSnapshot.compute_hash()` 결정성 입증
- 5개 enum 정확히 정의 (Direction, Strength, Grade, PatternType, ReportSource)
- `python -m py_compile src/chart_analysis/domain/*.py` 성공
- 커밋 메시지 한국어 (예: "feat(chart-analysis): 도메인 VO 및 Aggregate 추가")

## Agent Return Protocol
완료 시 다음 형식으로 응답:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <한두 문장 — "도메인 계층 구현 완료. VO 10개, Aggregate 2개, Port 5개 정의.">
- Files modified: <변경된 파일 경로 목록>
- Test result: <pytest 결과 요약 (passed/failed/skipped)>
- Blockers: <none | description>
---
