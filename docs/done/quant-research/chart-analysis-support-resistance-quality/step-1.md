# Step 1: Support/Resistance Quality Diagnosis and Plan
Assigned agent: Quant Planner

## Working Directory
.worktrees/quant-research-chart-analysis-support-resistance-quality

## Feature
종목 분석 시 3M, 6M, 1Y, 2Y, MAX 지지·저항 차트 분석 신뢰도 개선

**Goal**: 장기 window에서 지지·저항 레벨이 실제 차트 맥락과 맞지 않는 문제를 진단하고, 정량적으로 검증 가능한 개선 spec 및 후속 implementation/test/review step 파일을 생성한다.

## Files to Read
- CODEX.md
- docs/TODO.md (P0 항목 chart-analysis-support-resistance-quality 전체 읽기)
- backend/quant-research/src/chart_analysis/infrastructure/support_resistance_finder.py
- backend/quant-research/src/chart_analysis/infrastructure/confidence_scorer.py
- backend/quant-research/src/chart_analysis/infrastructure/trend_classifier.py
- backend/quant-research/src/chart_analysis/infrastructure/indicator_calculator.py
- backend/quant-research/src/chart_analysis/infrastructure/ohlcv_repository.py
- backend/quant-research/src/chart_analysis/application/precompute_pipeline_service.py
- backend/quant-research/src/chart_analysis/domain/chart_snapshot.py
- backend/quant-research/src/chart_analysis/domain/chart_analysis_result.py
- backend/quant-research/src/chart_analysis/domain/value_objects.py
- backend/quant-research/tests/unit/chart_analysis/infrastructure/test_ohlcv_repository.py
- backend/quant-ai/tests/unit/chart_analysis/infrastructure/test_support_resistance_finder.py
- backend/quant-ai/tests/fixtures/chart_analysis/support_resistance/sample_uptrend_3m.json
- backend/quant-ai/tests/fixtures/chart_analysis/support_resistance/sample_sideways_6m.json

## Open Questions
(Planner Pass A가 코드베이스를 읽은 뒤 질문 목록과 추천 옵션을 생성한다. 여기에 미리 확정하지 않음.)

## Confirmed Design Choices
(사용자 Q&A 완료 후 확정된 결정만 기록. Planner Pass B가 직접 채운다.)

## Tasks
1. 현재 지지·저항 산출 흐름을 window별로 추적한다: OHLCV 조회 봉 수, interval, peak 탐지, ATR/클러스터링, 정렬/상위 레벨 선택, confidence score 반영 여부.
2. 3M/6M/1Y/2Y/MAX에서 품질이 떨어질 수 있는 원인을 코드 근거로 분류한다.
3. 사용자에게 확인해야 할 설계 질문을 구조화한다. 각 질문은 다음을 반드시 포함한다:
   - 기능/개념 설명: 이 선택이 실제 차트 분석 품질에 어떤 영향을 주는지
   - 최소 3개 이상의 선택지: 보수적/균형/공격적 또는 단순/중간/고급 방향
   - 각 선택지의 장점, 단점, 구현 난이도, 검증 방법
   - planner의 추천안 1개와 추천 이유
   - 최종 결정은 사용자가 선택한다는 명시
4. 사용자 답변 후 `spec.md`를 작성한다.
5. 후속 step 파일을 생성한다:
   - `step-2.md`: support/resistance 알고리즘 및 confidence 개선, TDD 포함
   - `step-3.md`: fixture/acceptance dataset 보강 및 회귀 테스트
   - `step-4.md`: targeted QA 실행
   - `step-5.md`: code/quant review

## Acceptance Criteria
- `spec.md` 생성: 문제 원인, window별 정책, 알고리즘 개선안, 검증 기준, non-goals 포함
- `step-2.md` ~ `step-5.md` 생성: 각 step에 Working Directory, Files to Read, Tasks, Acceptance Criteria 포함
- 장기 window 품질 검증 기준이 정량적으로 표현되어야 함
- 금융 계산은 float 기반 금액 산출로 확장하지 말고, 가격 레벨 최종 출력은 기존 Decimal 계약을 유지해야 함
- 각 step 파일 끝에 "## Agent Return Protocol" 포함
- 모든 docs 경로는 main repo root 기준

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <paths relative to worktree root>
- Test result: N/A (planner step)
- Blockers: <none | description>
---
