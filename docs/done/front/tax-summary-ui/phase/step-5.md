# Step 5: 정리 문서화 및 PR 준비
Assigned agent: cleanup

## 목표
Tax Summary UI phase 종료를 위해 summary 문서 작성, TODO/state/index 갱신, PR 준비 절차를 완료한다.

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/CODEX.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/spec.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/step-2.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/step-3.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/step-4.md`
- `C:/Users/dw/Desktop/paper-trading/docs/TODO.md`
- `C:/Users/dw/Desktop/paper-trading/docs/state.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/index.json`

## 입력
- 구현/테스트/리뷰 완료 결과
- phase 상태 정보와 TODO 현황

## 출력
- 완료 요약 문서 (`docs/done/front/tax-summary-ui/tax-summary-ui-summary.md`)
- 상태 파일 갱신 (`docs/TODO.md`, `docs/state.md`, `docs/phase/front/tax-summary-ui/index.json`)
- PR 본문 초안 및 체크리스트

## Tasks
1. Summary 문서 작성
- 구현 범위, API 연동 범위, UX 결정, 테스트/빌드 결과, 잔여 과제를 요약한다.

2. Orchestrator 상태 갱신
- `docs/phase/front/tax-summary-ui/index.json` step 상태와 phase 상태를 완료로 반영한다.
- `docs/TODO.md`의 `front: tax-summary-ui` 항목을 완료로 전환하고 날짜/PR 번호를 기록한다.
- `docs/state.md`에 다음 진행 phase가 반영되도록 상태를 갱신한다.

3. PR 준비
- 변경 파일 목록, 검증 명령/결과, 리뷰 포인트를 포함한 PR 본문 초안을 작성한다.
- PR 체크리스트(기능, 테스트, 문서, 리스크)를 정리한다.

## 검증 명령
```bash
cd C:/Users/dw/Desktop/paper-trading/frontend/trading-web
npm run test -- tax-summary
npm run build
```

## 완료 조건
- summary 문서가 생성되고 핵심 결과가 누락 없이 정리됨.
- TODO/state/index가 메인 레포 기준으로 일관되게 갱신됨.
- PR 생성에 바로 사용할 수 있는 본문 초안/체크리스트가 준비됨.

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
