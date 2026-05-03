# Step 1: DDD Aggregate 경계 재설계 및 실행 단계 정의
Assigned agent: service-planner

## Working Directory
.worktrees/trading-api-ddd-aggregate-fix

## Files to Read
- CODEX.md
- docs/TODO.md
- docs/phase/trading-api/ddd-aggregate-fix/index.json
- backend/trading-api/graphify-out/graph.json

## Tasks
1. `ddd-aggregate-fix` 요구사항을 구조화하고, 범위/비범위를 명확히 정리한다.
2. trading-api 도메인 모델에서 Aggregate Root/Entity 경계 위반 지점을 식별한다.
3. 다음 목표를 만족하는 설계를 작성한다.
   - 내부 Entity Repository 제거
   - Aggregate Root Repository(`OrderRepository`, `AccountRepository`) 경유 접근
   - Aggregate 간 직접 객체 참조 제거 및 ID 참조로 전환
4. `docs/phase/trading-api/ddd-aggregate-fix/spec.md` 작성.
5. `step-2.md`~`step-N.md` 생성 (구현, 테스트, 리뷰, 정리 단계 포함).
   - 각 step 파일에 구체적인 변경 파일 경로, 클래스/메서드 단위 작업, 검증 명령 포함
   - 각 step 파일에 `## Agent Return Protocol` 포함
   - docs 경로는 항상 메인 리포 루트 기준으로 기재

## Acceptance Criteria
- `spec.md` 생성 완료
- `step-2.md`~`step-N.md` 생성 완료
- 각 step 파일의 작업 지시가 실행 가능한 수준으로 구체화됨
- 각 step 파일에 `## Agent Return Protocol` 섹션이 존재함

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
