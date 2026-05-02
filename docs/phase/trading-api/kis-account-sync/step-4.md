# Step 4 — code-reviewer

## Goal
구현 결과를 보안, 회귀, 재무정합성 중심으로 리뷰하고 수정 권고를 확정한다.

## Required Inputs
- Step 2/3 결과물
- `docs/phase/trading-api/kis-account-sync/spec.md`

## Review Checklist
1. Security
- KIS credential/token이 로그/응답에 노출되지 않는가
- 외부 API 오류 메시지에 민감정보가 포함되지 않는가
- 인증/인가 실패 매핑이 일관적인가

2. Regression
- 기존 account/position API 계약과 충돌이 없는가
- `AccountSource` 추가로 클라이언트 호환성 문제가 없는가
- 예외 처리 변경이 전역 핸들러 규칙을 깨지 않는가

3. Financial Consistency
- 금액/수익률 계산이 전부 `BigDecimal`인가
- 반올림/스케일 정책이 코드 전반에서 일치하는가
- KIS-LOCAL 정합성 비교 규칙이 명확하고 감사 가능한가

4. Test Adequacy
- 정상/실패/경계 케이스 테스트가 충분한가
- flakiness 가능성이 있는 테스트가 있는가

## Expected Outputs
- severity 포함 리뷰 이슈 목록 (Critical/Major/Minor)
- 즉시 수정 필요 항목과 후속 개선 항목 분리

## Verification Commands
- `cd trading-api && ./gradlew test --tests "*KisAccount*"`
- `cd trading-api && ./gradlew test --tests "*Account*"`

## Done Criteria
- 리뷰 결과가 재현 가능한 근거(파일/라인/테스트)와 함께 정리됨
- 차단 이슈 유무가 명확함

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
