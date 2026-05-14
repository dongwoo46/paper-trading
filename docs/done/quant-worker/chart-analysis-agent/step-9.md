# Step 9: Cleanup + PR 생성 (Orchestrator 직접)

Assigned agent: orchestrator (직접 처리 — 서브에이전트 위임 금지)

## Working Directory
.worktrees/quant-worker-chart-analysis-agent → main repo root

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CLAUDE.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/spec.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/decisions.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/quant-worker/chart-analysis-agent/index.json
- C:/Users/dw/Desktop/paper-trading/docs/TODO.md
- C:/Users/dw/Desktop/paper-trading/docs/state.md

## Open Questions
없음.

## Confirmed Design Choices
- Cleanup 단계는 **orchestrator 직접 처리** (memory `feedback_cleanup_direct.md` — 서브에이전트 위임 금지)
- 전체 수트 실행은 본 단계에서 수행
- PR 생성: gh CLI 또는 수동 (커밋 메시지 한국어)
- 완료 후: phase 폴더를 `docs/done/quant-worker/chart-analysis-agent/` 로 이동, `docs/TODO.md` 체크박스 `[x]` 처리

## Tasks

### Substep 9-1: 전체 테스트 수트 실행
1. `cd backend/quant-worker && pytest -v --tb=short` (전체 단위 + 마커 없는 통합)
2. `pytest -v -m integration --tb=short` (선택, 로컬 Ollama 가용 시)
3. 실패 시 quant-dev 재호출 (orchestrator 판단)

### Substep 9-2: lint + build 검증
1. `python -m py_compile` — `find backend/quant-worker/src/chart_analysis -name "*.py" -exec python -m py_compile {} +`
2. (있을 경우) `ruff check backend/quant-worker/src/chart_analysis/`
3. FastAPI 앱 부팅 smoke: `uvicorn src.interfaces.api.app:app --port 18082` background → `/health` 200 → kill

### Substep 9-3: 문서 정리
1. 작성: `docs/done/quant-worker/chart-analysis-agent/chart-analysis-agent-summary.md`
   - 구현 요약 (엔드포인트 3개, 테이블 3개, 윈도우 7개, 패턴 6종)
   - 주요 설계 결정 (decisions.md 핵심 요약)
   - 환경변수 목록
   - 운영 매뉴얼 (큐 처리 스크립트 실행 명령)
   - 알려진 제한 (TOP 300 외 LLM 부재, 프론트 가짜 진행은 별도 작업)
2. `docs/phase/quant-worker/chart-analysis-agent/` → `docs/done/quant-worker/chart-analysis-agent/` 로 이동 (mv)
3. `docs/TODO.md` 에서 chart-analysis-agent 항목 `[ ]` → `[x]`
4. `docs/state.md` active feature 해제 (다음 feature 또는 idle)
5. `index.json` 의 status 를 `done` 으로 갱신 (이동 전)

### Substep 9-4: 커밋 + PR
1. 모든 변경 사항 정리 + `git status` 확인
2. 커밋 메시지(한국어):
   - 예: `feat(quant-worker): 차트 분석 AI 에이전트 구현 (7 윈도우, SSE, LLM 폴백)`
3. PR 생성:
   - 제목: `feat(quant-worker): 차트 분석 AI 에이전트 (chart-analysis-agent)`
   - 본문: spec.md 핵심 요약 + 변경 파일 그룹 + 테스트 결과 + 알려진 제한
   - base: `main` ← head: `feature/quant-worker-chart-analysis-agent`
4. PR URL을 보고서에 기록

### Substep 9-5: Worktree 정리
1. PR 머지 또는 검토 완료 후: `git worktree remove .worktrees/quant-worker-chart-analysis-agent`
2. (본 단계에서는 머지 전 PR만 생성 — worktree 정리는 머지 후로 보류)

## Acceptance Criteria
- 전체 수트 통과 (0 fail, 0 error)
- lint/build smoke 통과
- summary.md 작성 완료
- phase 폴더 이동 완료
- `docs/TODO.md` 체크 완료
- PR 생성 + URL 확보
- 커밋 메시지 한국어

## Agent Return Protocol
---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <한두 문장 — "차트 분석 AI 에이전트 phase 완료. PR #N 생성, 문서 이동, TODO 체크 완료.">
- Files modified: <목록>
- Test result: <전체 수트 결과 요약>
- Blockers: <none | description>
- PR URL: <https://...>
---
