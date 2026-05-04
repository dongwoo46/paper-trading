# Step 4: 코드 리뷰 및 계약 정합성 점검
Assigned agent: code-reviewer

## 목표
Tax Summary UI 변경사항을 리뷰하고 API 계약 정합성, 금액/세율 표시 정확성, 회귀 위험을 중점 점검한다.

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/CODEX.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/spec.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/step-2.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/step-3.md`
- Step 2/3 결과 코드 및 테스트 리포트
- `C:/Users/dw/Desktop/paper-trading/docs/done/trading-api/tax-summary/tax-summary-summary.md`

## 입력
- 구현 코드, 테스트 결과, 빌드 결과
- trading-api tax-summary API 계약

## 출력
- 리뷰 이슈 목록(심각도/근거/수정 제안)
- 승인 가능 여부 및 잔여 리스크

## Review Checklist
- API 계약 정합성
- 경로/파라미터(`accountId`, `taxYear`)와 응답 필드 이름이 spec 및 backend 구현과 일치하는가.
- `409 RUNNING`/`400`/`404` 오류 처리가 UX 요구와 일치하는가.

- 금액/세율 표시 정확성
- 금액/세율 문자열을 부정확한 재계산 없이 표시하는가.
- 자릿수/단위/부호 처리 및 null 처리 규칙이 일관적인가.
- 반올림/포맷 로직이 기존 대시보드 규칙과 충돌하지 않는가.

- 회귀 위험
- 라우팅/사이드바 변경이 기존 페이지 접근성을 해치지 않는가.
- Query key/invalidate 범위가 과도하거나 누락되어 stale 데이터 위험이 없는가.
- 로딩/에러 상태에서 중복 요청 및 버튼 다중 클릭 위험이 제어되는가.

- 테스트 적정성
- 재계산 성공/실패/중복 상태 테스트가 충분한가.
- 핵심 회귀 테스트(account-dashboard/order-management-ui/build)가 수행되었는가.

## 검증 명령
```bash
cd C:/Users/dw/Desktop/paper-trading/frontend/trading-web
npm run test -- tax-summary
npm run test -- AccountDashboardPage OrderPage
npm run build
```

## 완료 조건
- 주요 리뷰 포인트(API 계약/금액·세율/회귀 위험)가 모두 점검됨.
- Blocking 이슈가 있으면 재현 경로와 수정 권고가 명확히 작성됨.
- 승인 가능 시 잔여 리스크가 문서화됨.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Test result: <passed N/N | failed N — list failing cases> (if applicable)
- Blockers: <none | description>
---
