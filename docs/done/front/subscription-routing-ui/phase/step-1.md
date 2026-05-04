# Step 1: Feature Spec and API Dependency Analysis
Assigned agent: service-planner

## Working Directory
.worktrees/front-subscription-routing-ui

## Files to Read
- CODEX.md
- docs/state.md
- docs/TODO.md
- docs/phase/front/subscription-routing-ui/index.json
- C:/Users/dw/Desktop/paper-trading/frontend/trading-web/src/pages/realtime/ui/RealtimePage.tsx
- C:/Users/dw/Desktop/paper-trading/frontend/trading-web/src/features/kis-management/ui/KisPanel.tsx
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/internal/InternalSubscriptionController.kt
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/kis/KisSymbolCatalogController.kt

## Tasks
1. subscription-routing-ui 목표를 spec.md로 구체화한다 (쓰기 중심 운영 UI).
2. 현재 외부 노출 API로 가능한 범위와 불가능 범위를 구분한다.
3. 부족한 API 스펙(수동 등록/해제, 즐겨찾기, 전략 종목, 라우팅 상태 조회)을 백엔드 선행 요구사항으로 명시한다.
4. Step 2~5 문서를 생성한다.
   - 백엔드 API 미확정 시 step-2를 blocked-safe 형태(구현 착수 조건 포함)로 작성
5. 각 step 파일 끝에 Agent Return Protocol을 포함한다.

## Acceptance Criteria
- docs/phase/front/subscription-routing-ui/spec.md 생성
- docs/phase/front/subscription-routing-ui/step-2.md ~ step-5.md 생성
- 선행조건/차단조건이 명확히 문서화됨

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
