# Step 5: Code Review and Phase Wrap Handoff
Assigned agent: code-reviewer

## Working Directory
`.worktrees/collector-api-realtime-market-features`

## Files to Read
- `CODEX.md`
- `docs/phase/collector-api/realtime-market-features/spec.md`
- `docs/phase/collector-api/realtime-market-features/step-2.md`
- `docs/phase/collector-api/realtime-market-features/step-3.md`
- `docs/phase/collector-api/realtime-market-features/step-4.md`
- All implementation/test files created in step-2~4

## Tasks
1. 변경사항을 버그/회귀/운영리스크 중심으로 리뷰한다.
2. Redis 키 충돌 가능성, TTL 누락, 분 경계 race 처리 누락 여부를 확인한다.
3. API 오류코드(400/404/409) 일관성을 확인한다.
4. 테스트 커버리지 갭을 명시하고 재작업 필요 시 우선순위를 제시한다.
5. 승인 가능하면 orchestrator가 summary/PR 단계로 이동할 수 있게 근거를 남긴다.

## Acceptance Criteria
```bash
cd .worktrees/collector-api-realtime-market-features/backend/collector-api && ./gradlew test
```

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
