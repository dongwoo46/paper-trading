# Step 3: 테스트 및 QA 검증
Assigned agent: test-engineer

## Goal
구현 결과가 기능 요구사항, 회귀 안정성, 빌드 품질을 만족하는지 검증한다.

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/front/portfolio-chart/spec.md
- docs/phase/front/portfolio-chart/step-2.md
- step 2에서 변경된 파일 전체

## Tasks
- 기능 검증:
  - 일별 평가금액 시리즈가 날짜 오름차순으로 렌더되는지 확인.
  - KOSPI 대비 수익률 계산이 기준일 0%, 이후 누적수익률 규칙을 따르는지 확인.
  - 벤치마크 데이터 누락 시 warning/대체 UI가 노출되는지 확인.
- 회귀 검증:
  - account-dashboard 및 기존 네비게이션이 정상 동작하는지 smoke 테스트.
- 품질 검증:
  - feature-scope 테스트 + 프런트 빌드 실행.

## Acceptance Criteria
```bash
cd frontend/trading-web
npm run test -- portfolio-chart
npm run test -- account
npm run build
```
- 모든 테스트 통과.
- 수동 QA 체크리스트(로딩/에러/빈 상태) 결과를 리포트에 포함.

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
