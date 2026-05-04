# Step 1: Feature Spec and API Spec
Assigned agent: Service Planner

## Working Directory
.worktrees/collector-api-subscription-routing

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/PRD.md
- docs/TODO.md
- docs/phase/collector-api/subscription-routing/index.json
- backend/collector-api/graphify-out/graph.json

## Tasks
1. 아래 요구사항을 기준으로 collector-api 구독 라우팅 정책 spec을 작성한다.
   - 전략 관련 종목이 최우선이다.
   - 전략 관련 종목은 가능한 한 항상 WebSocket(WS)으로 유지한다.
   - WS가 전략 종목으로 가득 차 새 전략 종목을 WS에 넣을 수 없을 때만 REST로 fallback한다.
   - 사람이 수동 등록한 종목은 후순위이며, WS가 꽉 차면 REST overflow로 보낼 수 있다.
   - 사람이 수동 등록한 종목은 WS/REST 모두 꽉 차면 드롭(미등록) 허용이다.
   - 사람이 종목을 즐겨찾기할 수 있는 기능이 필요하다.
2. 우선순위/용량 정책을 명시한다.
   - source: STRATEGY vs MANUAL
   - channel: WS vs REST
   - overflow/승격 규칙, eviction 규칙
3. 내부 API를 설계한다.
   - 수동 종목 등록/해제 API
   - 즐겨찾기 등록/해제/조회 API
   - 전략 종목 등록/해제 API(향후 strategy-execution 자동 연동 가능)
   - 현재 라우팅 상태 조회 API
4. 상태 모델/저장 모델을 설계한다.
   - 구독 요청 엔티티(출처, 우선순위, 즐겨찾기 여부, 만료/드롭 사유)
   - WS 슬롯/REST 슬롯 관리 모델
5. 실패/경계 케이스를 설계한다.
   - WS/REST 포화 시 처리
   - 전략 종목 우선 보장을 위한 재배치
   - 중복 등록/동시성/멱등성
6. `spec.md` 작성 후 `step-2.md` ~ `step-N.md`를 생성한다.
   - 구현 범위를 작게 나누고, 각 step에 실제 파일 경로/클래스/테스트 대상을 구체적으로 적는다.
   - 각 step 파일 끝에 `## Agent Return Protocol` 섹션을 반드시 포함한다.

## Acceptance Criteria
- `spec.md`가 생성되고, 라우팅 우선순위/수용량/포화 정책이 명확하다.
- `step-2.md` 이상 실행 가능한 구현 단계 파일이 생성된다.
- 전략 최우선 + 수동 등록 후순위 + 드롭 허용 정책이 명시된다.
- 즐겨찾기 기능 요구가 API/모델에 반영된다.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
