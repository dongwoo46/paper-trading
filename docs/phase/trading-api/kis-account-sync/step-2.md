# Step 2 — fullstack-dev (TDD)

## Goal
`trading-api/kis-account-sync` 기능을 TDD 순서로 구현하여 KIS 잔고조회 서비스와 API 엔드포인트를 동작시킨다.

## Required Inputs
- `docs/phase/trading-api/kis-account-sync/spec.md`
- 기존 trading-api 도메인/컨트롤러/adapter 코드

## Tasks (Must follow order)
1. 실패 테스트 작성
- `KisAccountQueryService` 단위 테스트:
  - LIVE -> `TTTC8434R`, PAPER -> `VTTC8434R` 분기 실패 테스트
  - `BigDecimal` 계산/매핑 검증 실패 테스트
  - 정합성 mismatch 계산 실패 테스트
- `GET /api/kis/account/balance` 컨트롤러 테스트:
  - 정상 200 응답 계약 테스트
  - 오류 매핑(400/401/403/502/504) 실패 테스트

2. 최소 구현
- 서비스/adapter/DTO/컨트롤러를 테스트 통과 최소 수준으로 구현
- `AccountSource` 노출 및 기본 하위호환 처리
- 로그/예외 매핑 추가

3. 리팩터링
- 중복 제거, 네이밍 정리, 매핑 코드 정돈
- 테스트 가독성 개선(테스트 픽스처 정리)

## Expected Outputs
- trading-api 코드 변경(서비스/컨트롤러/DTO/adapter/테스트)
- 테스트 통과 결과

## Verification Commands
- `cd trading-api && ./gradlew test --tests "*KisAccount*"`
- `cd trading-api && ./gradlew test --tests "*AccountBalance*Controller*"`

## Done Criteria
- 실패 테스트 -> 구현 -> 통과 순서가 커밋/작업 로그로 확인 가능
- 핵심 수치 필드가 `BigDecimal` 기반
- LIVE/PAPER TR ID 분기 테스트가 모두 통과

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
