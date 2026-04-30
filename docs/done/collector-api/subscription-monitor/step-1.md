# Step 1: Feature Spec and API Spec
Assigned agent: Service Planner

## Working Directory
Main repo root: C:\Users\dw\Desktop\paper-trading
Worktree: C:\Users\dw\Desktop\paper-trading\.worktrees\collector-api-subscription-monitor

## Files to Read
- CLAUDE.md
- docs/ADR.md
- docs/PRD.md
- docs/TODO.md
- backend/collector-api/src/main/kotlin/com/dw/collectorapi/domain/ (explore structure)
- backend/collector-api/src/main/kotlin/com/dw/collectorapi/application/ (explore structure)
- backend/collector-api/src/main/kotlin/com/dw/collectorapi/infrastructure/ (explore structure)
- backend/collector-api/src/main/kotlin/com/dw/collectorapi/interfaces/ (explore structure)

## Feature Summary
**구독 상태 모니터링 API** (collector-api/subscription-monitor)

운영 대시보드에서 현재 KIS WebSocket 구독 중인 종목 목록과 연결 상태를 조회할 수 있는 API.

### 요구사항
- `GET /api/subscriptions/status` — 현재 구독 종목·연결 상태 조회
- 운영 대시보드 연동용 (trading-web에서 호출 예정)
- ws-reconnect-stability phase(PR #8)에서 구현된 heartbeat/health 인프라 위에 구축

## Tasks
1. collector-api 현재 구조 파악 (WS 구독 관리, 기존 헬스 엔드포인트 확인).
2. 구독 상태 모니터링에 필요한 도메인 모델 확정 (SubscriptionStatus VO 등).
3. API spec 설계: GET /api/subscriptions/status 응답 구조, 에러 케이스.
4. DDD 레이어 설계 (Domain, Application, Infrastructure, Interface).
5. spec.md 작성.
6. step-2.md ~ step-5.md 생성 (구체적 구현 지시 포함).
   - 각 step 파일의 "Files to Read" 는 main repo 절대 경로 사용.
   - fullstack-dev step에는 substeps(DDD Aggregate 1개 = substep 1개) 포함.
   - 모든 step 파일 끝에 "## Agent Return Protocol" 섹션 포함.

## Acceptance Criteria
- spec.md 생성됨.
- step-2.md ~ step-5.md 생성됨 (파일 경로, 클래스 시그니처, 핵심 규칙 포함).
- 각 step 파일에 "## Agent Return Protocol" 섹션 포함.
- 모든 docs 경로는 main repo root 기준 (worktree 내부 아님).
- 사용자 승인 불필요 (auto 모드).

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
