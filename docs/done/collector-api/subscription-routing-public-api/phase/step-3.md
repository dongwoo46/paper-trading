# Step 3: Testing and QA Verification
Assigned agent: test-engineer

## Working Directory
.worktrees/collector-api-subscription-routing-public-api

## Files to Read
- docs/phase/collector-api/subscription-routing-public-api/spec.md
- docs/phase/collector-api/subscription-routing-public-api/step-2.md
- backend/collector-api/src/test/** (step-2 변경 테스트)

## Tasks
1. 신규/변경 테스트가 spec contract를 충분히 검증하는지 리뷰한다.
2. 실패 케이스(mode/channel/symbol invalid)와 멱등 케이스를 확인한다.
3. 컴파일/테스트를 실행하고 결과를 정리한다.

## Acceptance Criteria
- contract mismatch 없음
- 핵심 테스트(성공/실패/멱등) 통과
- 빌드/컴파일 통과

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
