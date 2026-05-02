# Step 5 — cleanup

## Goal
phase 산출물을 정리하고 상태 문서/TODO를 갱신해 PR 준비를 완료한다.

## Required Inputs
- Step 2~4 completion reports
- 변경 파일 목록, 테스트 결과

## Tasks
1. Summary 문서화
- phase 구현 요약, 주요 설계 결정, 테스트 결과를 `summary.md`에 정리

2. 상태 갱신
- `docs/phase/trading-api/kis-account-sync/index.json` 업데이트
  - step 상태, current_step, blockers, updated 반영
- 오케스트레이터 state 문서가 있다면 규칙에 맞게 동기화

3. TODO 갱신
- 완료 기준 충족 시 `docs/TODO.md`의 해당 항목 `[x]` 처리
- `done: YYYY-MM-DD | pr: #{n}` 메타데이터 반영 (PR 번호 없으면 임시값 금지, 보류 사유 명시)

4. PR 준비
- PR 본문 초안 작성
  - 배경/변경사항/테스트/리스크/롤백 플랜
- 필요 시 체크리스트 포함(보안, 재무정합성, 모니터링)

## Verification Commands
- `git status --short`
- `git diff -- docs/phase/trading-api/kis-account-sync docs/TODO.md`

## Done Criteria
- summary + state + TODO + PR 초안까지 일관되게 정리됨
- 누락/불일치 없는 최종 인수인계 가능 상태

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to worktree root>
- Test result: <passed N/N | failed N — list failing cases> (if applicable)
- Blockers: <none | description>
---
