# Step 2: Implement Read-only Subscription Monitor UI (TDD)
Assigned agent: fullstack-dev

## Files to Read
- docs/phase/front/subscription-monitor-ui/spec.md
- docs/phase/front/subscription-monitor-ui/index.json
- frontend/trading-web/src/features/kis-management/ui/KisPanel.tsx
- frontend/trading-web/src/shared/api/index.ts

## Tasks
1. Test-first로 `/api/subscriptions/status` API client 및 모니터링 표시 로직 테스트를 작성한다.
2. `KisPanel`에 `구독 상태 모니터링 (읽기 전용)` 섹션을 추가한다.
3. 전역 슬롯/모드별 상태/심볼 목록을 spec 매핑대로 렌더링한다.
4. 기존 구독 제어 UI와 시각적으로 분리하고, monitor 섹션 내 제어 액션을 추가하지 않는다.
5. polling(기본 10초) + 수동 refresh(읽기 전용) 동작을 구현한다.

## Acceptance Criteria
- `/api/subscriptions/status` 응답이 UI에 매핑되어 표시된다.
- monitor 섹션에서 POST/DELETE 등 제어 요청이 발생하지 않는다.
- 기존 KIS 구독 제어 기능이 회귀 없이 동작한다.
- 테스트/빌드 통과.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
