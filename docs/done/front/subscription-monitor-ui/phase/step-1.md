# Step 1: Feature Spec and Step File Generation
Assigned agent: service-planner

## Working Directory
.worktrees/front-subscription-monitor-ui

## Files to Read
- CODEX.md
- docs/state.md
- docs/TODO.md
- docs/phase/front/subscription-monitor-ui/index.json
- C:/Users/dw/Desktop/paper-trading/frontend/trading-web/src/pages/realtime/ui/RealtimePage.tsx
- C:/Users/dw/Desktop/paper-trading/frontend/trading-web/src/features/kis-management/ui/KisPanel.tsx
- C:/Users/dw/Desktop/paper-trading/frontend/trading-web/src/shared/api/index.ts
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/subscriptions/SubscriptionStatusController.kt
- C:/Users/dw/Desktop/paper-trading/backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/subscriptions/dto/SubscriptionStatusResponse.kt

## Tasks
1. subscription-monitor-ui scope를 spec.md로 정리한다 (읽기 전용 모니터링, 제어 액션 없음).
2. 기존 RealtimePage > KisPanel 내 구독 제어 UI와 충돌 없이 섹션 분리 설계를 제시한다.
3. `/api/subscriptions/status` 응답 필드 기반 UI 데이터 매핑 표를 작성한다.
4. Step 2~5 문서를 생성한다.
   - step-2: fullstack-dev 구현(TDD)
   - step-3: test-engineer 검증
   - step-4: code-reviewer 리뷰
   - step-5: cleanup/summary
5. 각 step 파일 끝에 Agent Return Protocol을 포함한다.

## Acceptance Criteria
- docs/phase/front/subscription-monitor-ui/spec.md 생성
- docs/phase/front/subscription-monitor-ui/step-2.md ~ step-5.md 생성
- 구현 범위에서 읽기 전용 모니터링과 기존 제어 UI 경계가 명확히 정의됨

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
