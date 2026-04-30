# Step 1: Feature Spec and API Spec
Assigned agent: Service Planner

## Working Directory
.worktrees/collector-api-ws-reconnect-stability

## Context
KIS WebSocket이 끊겼을 때 자동 재연결이 없어 시세 수신이 중단되는 운영 리스크를 해결한다.
REST fallback은 이번 scope 밖 (API 제한 복잡도 + 우선순위 라우팅은 subscription-routing phase에서).
이번 목표: heartbeat 감지 → exponential backoff 재연결 → 재연결 시 구독 자동 복구 → 연결 상태 health 엔드포인트.

## Files to Read
- CLAUDE.md (C:\Users\dw\Desktop\paper-trading\CLAUDE.md)
- backend/collector-api/src/main/kotlin/com/papertrading/collector/infra/kis/source/ws/KisWebSocketCollector.kt
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/runtime/ (전체)
- backend/collector-api/src/main/kotlin/com/papertrading/collector/application/kis/subscription/ (전체)

## Tasks
1. 현재 KisWebSocketCollector의 연결 생명주기 분석 — 끊김을 어떻게 감지하는지, 재연결 로직 유무 확인
2. 다음을 설계한다:
   - **Heartbeat / pong timeout 감지**: KIS WS ping-pong 프로토콜 or 수신 없음 timeout
   - **Exponential backoff 재연결**: 초기 1s, 배수 2, 상한 30s, 최대 시도 무제한
   - **재연결 시 자동 재구독**: `kis:ws:{mode}` Redis Set의 모든 종목 재신청
   - **연결 상태 노출**: `GET /api/kis/ws/health` — 모드별 { status: CONNECTED|DISCONNECTED|RECONNECTING, lastConnectedAt, reconnectAttempts }
3. 상태 머신 정의: CONNECTED → DISCONNECTED → RECONNECTING → CONNECTED
4. 설정값 externalize: backoff 초기값/배수/상한, heartbeat timeout → application.yml
5. spec.md 작성
6. step-2.md ~ step-5.md 생성 (구체적 클래스명, 메서드 시그니처, 파일 경로 포함)

## Acceptance Criteria
- spec.md 생성 (상태 머신 다이어그램, backoff 파라미터, health API 스펙 포함)
- step-2.md ~ step-5.md 생성, 각각 "## Agent Return Protocol" 섹션 포함
- step 파일 내 모든 doc 경로는 main repo root 기준 (worktree 경로 아님)
- 사용자 승인 수령

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
