# Step 7: 테스트 및 QA 검증 (Test Engineering)

Assigned agent: test-engineer

## Working Directory
.worktrees/quant-worker-chart-analysis-agent

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/backend/quant-worker/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/spec.md (§12 Test Strategy)
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/decisions.md (§16)
- Step 2-6에서 생성된 모든 테스트 파일 + 구현 파일

## Open Questions
없음.

## Confirmed Design Choices
- 테스트 범위: **현재 phase 변경 파일만** (memory `feedback_test_scope.md`)
- 전체 수트는 Step 9 cleanup 단계에서만 실행
- 통합 테스트는 `@pytest.mark.integration` 마커 (평소 CI skip)
- 골든 fixture는 Step 3에서 생성 완료 — 본 단계는 검증 + coverage

## Tasks

### Substep 7-1: Feature-scoped 단위 테스트 실행
1. 명령: `pytest tests/unit/chart_analysis/ -v --tb=short`
2. 검증:
   - 모든 단위 테스트 통과 (목표: 100%)
   - 실패/에러 0건
   - skip 사유 명시 (있다면)
3. 결과를 리포트로 정리

### Substep 7-2: 통합 테스트 실행 (마커 포함)
1. 명령: `pytest tests/integration/chart_analysis/ -v -m "not integration" --tb=short` (마커 없는 통합 — httpx ASGI 등)
2. 명령: `pytest tests/integration/chart_analysis/ -v -m integration --tb=short` (실제 Ollama 호출 — 로컬에서만)
3. 검증:
   - SSE 이벤트 시퀀스 확인 (status → report → end)
   - 3 엔드포인트 통합 테스트 통과
   - 마커 integration 테스트: 환경 미비 시 SKIP 허용 (CI 환경 변수 검사)

### Substep 7-3: Coverage 검사
1. 명령: `pytest tests/unit/chart_analysis/ tests/integration/chart_analysis/ -m "not integration" --cov=src.chart_analysis --cov-report=term-missing --cov-report=html:coverage_html_chart_analysis`
2. 검증:
   - 도메인 + 인프라 + 애플리케이션 coverage 목표 ≥ 80%
   - 미커버 라인 분석 → 의도된 경로(예: 에러 가드)인지 확인
3. 미달 시 추가 테스트 케이스 제안 (구현은 quant-dev 회신 — 본 단계에서 추가 구현 금지)

### Substep 7-4: 골든 fixture 무결성 검증
1. 모든 fixture 파일이 JSON 파싱 가능
2. fixture가 참조하는 expected 값과 현재 구현 결과 일치
3. fixture 재현 가능성 (날짜/심볼/seed 명시)

### Substep 7-5: 회귀 가드 (smoke)
1. `python -m py_compile` 모든 새 파일 통과 확인
2. lint (현재 프로젝트가 사용하는 linter 있으면 — `ruff` 또는 `black --check`) 적용
3. FastAPI 앱 부팅 smoke: `uvicorn src.interfaces.api.app:app --port 18082` 후 `/health` 200 응답 확인 (background process)

## Acceptance Criteria
- 단위 테스트 0 fail, 0 error
- 통합 테스트(non-integration marker) 0 fail
- coverage ≥ 80% (chart_analysis 모듈)
- 골든 fixture 모두 검증 통과
- FastAPI 앱 부팅 smoke 성공 + `/health` 200
- 테스트 결과 보고서 (어떤 파일 N개, 테스트 N개, 통과/실패/스킵, 누락 검출 영역) 제출
- 커밋 없음 (테스트만 실행, 새 fixture 추가 시 커밋)

## Agent Return Protocol
---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <한두 문장 — "단위 N개, 통합 N개 모두 통과. coverage X%.">
- Files modified: <목록 (보통 none)>
- Test result: <pytest 통계 요약>
- Blockers: <none | description>
---
