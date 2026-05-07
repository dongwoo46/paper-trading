# Step 4: Testing and QA Verification
Assigned agent: test-engineer

## Working Directory
.worktrees/trading-api-portfolio-snapshot-service/backend/trading-api

## Files to Read
- CODEX.md
- docs/phase/trading-api/portfolio-snapshot-service/spec.md
- docs/phase/trading-api/portfolio-snapshot-service/step-2.md
- docs/phase/trading-api/portfolio-snapshot-service/step-3.md
- docs/phase/trading-api/portfolio-snapshot-service/index.json

## Tasks
1. 이번 phase 범위의 타깃 테스트 목록을 정의한다.
2. 신규/수정 테스트를 실행하고 실패 시 원인을 분류한다(테스트 코드/구현 코드/환경).
3. API 계약과 금액 계산(BigDecimal) 준수 여부를 검증한다.
4. 발견된 리스크를 재현 가능한 형태로 기록한다.

## Acceptance Criteria
- 타깃 테스트가 전부 통과하거나, 실패 시 재현 가능한 실패 목록이 제공된다.
- 회귀 위험(금액 계산, 멱등성, 날짜 필터)이 점검된다.

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
