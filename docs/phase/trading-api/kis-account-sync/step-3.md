# Step 3 — test-engineer

## Goal
구현 결과를 feature-scope 기준으로 검증하고 회귀 가능성을 낮춘다.

## Required Inputs
- Step 2 결과 코드/테스트
- `docs/phase/trading-api/kis-account-sync/spec.md`

## Test Scope
- 서비스 단위 테스트: TR ID 분기, 정합성 계산, 예외 매핑
- API 통합/슬라이스 테스트: `GET /api/kis/account/balance` 응답 계약
- 직렬화 검증: 금액/수익률 필드 `BigDecimal` 표현 유지

## Required Checks
1. feature-scope 테스트 실행
- KIS account sync 관련 테스트만 선별 실행
2. 컴파일 검증
- `trading-api` 모듈 컴파일 성공 확인
3. 기본 회귀 스모크
- 계좌/포지션 조회 관련 기존 핵심 테스트 일부 재실행

## Verification Commands
- `cd trading-api && ./gradlew test --tests "*KisAccount*"`
- `cd trading-api && ./gradlew test --tests "*Account*Controller*"`
- `cd trading-api && ./gradlew compileKotlin`

## Expected Outputs
- 테스트 실행 요약(통과/실패)
- 실패 시 재현 가능한 케이스 목록

## Done Criteria
- feature-scope 테스트 전부 통과
- `compileKotlin` 성공
- 회귀 리스크 항목이 있으면 명시적으로 기록

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to worktree root>
- Test result: <passed N/N | failed N — list failing cases> (if applicable)
- Blockers: <none | description>
---
