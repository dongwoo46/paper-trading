# Chart Analysis Support/Resistance Quality

## Core Feature
3M, 6M, 1Y, 2Y, MAX 장기 window의 지지·저항 분석 품질을 높이기 위해 window별 탐지 정책, 내부 점수화, endpoint 후보, 회귀 fixture를 도입했다.

## Considerations
- 외부 API 응답은 기존 Decimal `LevelSet` 계약을 유지했다.
- SR 품질은 confidence 신뢰도만 보정하고, BUY/SELL 방향성 grade를 단독으로 바꾸지 않도록 분리했다.
- synthetic/property 테스트는 이번 phase 범위에서 제외하고 future enhancement로 남겼다.
- `quant-ai`에 남아 있는 동일 분석 유틸과 테스트도 compatibility 유지를 위해 함께 갱신했다.

## Trade-offs
- Window별 정책(B)을 선택해 단일 전역 파라미터보다 장기/단기 차이를 반영했다.
- Composite ranking(B)을 선택해 최근성만이 아니라 touch count, rejection, volume, proximity를 함께 반영했다.
- 내부 `ScoredLevel`(B)을 선택해 API schema 변경 없이 랭킹 품질을 개선했다.
- MAX는 전체 이력을 유지하되 오래된 touch를 decay(B)해 stale level 지배를 줄였다.

## Implementation Approach
- `support_resistance_finder.py`: window/interval별 `SupportResistancePolicy`, 내부 `ScoredLevel`, endpoint 후보, composite score, MAX recency decay 추가.
- `precompute_pipeline_service.py`: finder에 window/interval/last_close 전달, trade plan에서 correct-side nearest support/resistance 선택.
- `confidence_scorer.py`: SR quality/trade-plan validity를 bounded confidence adjustment로 반영하되 directional grade mapping에서는 제외.
- `ohlcv_repository.py`: invalid interval 검증 순서를 기존 contract에 맞게 복구.
- Tests/fixtures: monotonic uptrend, role flip, long weekly, MAX-like history fixture와 ranked top-K/zone tolerance/correct-side regression 추가.

## Workflow
OHLCV window 조회 -> indicator/trend/pattern 계산 -> window-specific SR 후보 탐지 -> scored ranking -> Decimal LevelSet 반환 -> correct-side trade plan 선택 -> SR 품질 기반 confidence 보정.

## Key APIs
Public API schema changes: none.

Internal contract changes:
- `SupportResistanceFinder.find(candles, atr, *, window=None, interval=None, last_close=None)`
- `ConfidenceScorer.score(..., *, levels=None, trade_plan=None, last_close=None)`

## DB
No DB schema changes.

## Verification
- `python -m pytest tests\unit\chart_analysis\infrastructure\test_support_resistance_finder.py -q` -> 26 passed
- `python -m pytest tests\unit\chart_analysis\infrastructure\test_confidence_scorer.py -q` -> 13 passed
- `python -m pytest tests\unit\chart_analysis\application\test_precompute_pipeline_service.py -q` -> 5 passed, 1 warning
- `python -m pytest tests\unit\chart_analysis\infrastructure\test_ohlcv_repository.py -v --tb=short` -> 4 passed
- `python -m py_compile` on changed Python files -> passed
- `git diff --check` -> passed
- Code/quant review -> PASS after SR confidence grade side-effect rework

## Residual Risk
Broader `tests\unit\chart_analysis` runs still have unrelated existing failures outside this phase scope:
- `quant-ai`: 51 failures in legacy report/service/domain/protocol/import/class-identity/script tests.
- `quant-research`: 1 failure in `test_chart_analysis_repository.py` fixture row shape mismatch.

## Completed / PR
2026-05-19 / #36
