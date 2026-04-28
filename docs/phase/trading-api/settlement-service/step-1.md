# Step 1: Feature Spec and API Spec
Assigned agent: Service Planner

## Working Directory
.worktrees/trading-api-settlement-service

## Files to Read
- CLAUDE.md
- docs/ADR.md
- docs/PRD.md
- docs/TODO.md
- backend/trading-api/graphify-out/graph.json

## Context
T+2 정산 처리 서비스 구현. 관련 도메인 모델은 이미 존재함:
- `Settlement`, `SettlementExecution`, `PendingSettlement` 엔티티 존재
- `SettlementRepository`, `SettlementExecutionRepository`, `SettlementRepository`, `PendingSettlementRepository` 존재
- `PendingSettlementQueryService` 이미 구현됨 (조회만)
- `SettlementCommandService`는 미구현

## Tasks
1. 기존 도메인 모델(Settlement, PendingSettlement, SettlementExecution) 분석
2. T+2 정산 흐름 설계: PendingSettlement → Settlement 체결 → 예수금 반영 → AccountLedger 기록
3. SettlementCommandService API 설계
4. 정산 스케줄러 설계 (매일 장 마감 후, KST 기준)
5. spec.md 작성
6. step-2.md ~ step-5.md 생성 (구체적인 구현 지시 포함)

## Acceptance Criteria
- spec.md 작성 완료
- step-2.md ~ step-5.md 생성 (파일 경로, 클래스 시그니처, 핵심 규칙 포함)
- 각 step 파일에 "## Agent Return Protocol" 섹션 포함
- 모든 docs 경로는 main repo root 기준

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
