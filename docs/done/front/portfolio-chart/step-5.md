# Step 5: 정리 및 PR 준비
Assigned agent: cleanup

## Goal
phase 결과를 정리하고 문서 상태를 완료 상태로 반영한 뒤 PR 생성 준비를 마친다.

## Files to Read
- CODEX.md
- docs/state.md
- docs/TODO.md
- docs/phase/front/portfolio-chart/index.json
- docs/phase/front/portfolio-chart/spec.md
- docs/phase/front/portfolio-chart/step-2.md
- docs/phase/front/portfolio-chart/step-3.md
- docs/phase/front/portfolio-chart/step-4.md

## Tasks
- 결과 문서화:
  - `docs/done/front/portfolio-chart/portfolio-chart-summary.md` 작성
  - 구현 범위, 테스트 결과, 리뷰 결과, 잔여 리스크를 요약
- 오케스트레이션 상태 갱신:
  - `docs/TODO.md`의 portfolio-chart 항목 `[x]` 처리 + 완료일/PR 번호 반영
  - `docs/state.md`를 idle 또는 다음 phase 지시 상태로 갱신
  - `docs/phase/front/portfolio-chart/index.json` step 상태 최종 반영
- PR 준비:
  - 변경 파일 점검
  - 커밋 메시지(한국어) 제안
  - PR 본문 초안(배경/변경점/검증/리스크) 작성

## Acceptance Criteria
```bash
git status --short
```
- summary 문서 작성 완료.
- TODO/state/index 상태 일관성 확보.
- PR 본문 초안 포함.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Test result: <passed N/N | failed N — list failing cases> (if applicable)
- Blockers: <none | description>
---
