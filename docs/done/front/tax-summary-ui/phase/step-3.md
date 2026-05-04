# Step 3: 기능 검증 및 빌드 QA
Assigned agent: test-engineer

## 목표
Tax Summary UI feature-scope 테스트를 수행하고 `frontend/trading-web` 빌드까지 검증해 배포 전 품질을 확인한다.

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/CODEX.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/spec.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/front/tax-summary-ui/step-2.md`
- Step 2 산출 코드/테스트 파일 전체

## 입력
- Step 2 구현 결과
- Tax Summary UI 기능/에러/상태 시나리오

## 출력
- feature-scope 테스트 실행 결과
- 회귀 테스트 실행 결과
- production build 검증 결과
- 발견 이슈 목록(재현 절차 포함)

## Tasks
1. feature-scope 테스트 실행
- Tax Summary 관련 테스트 스위트를 우선 실행하고 실패 케이스를 분류한다.
- 재계산 UX(버튼 비활성화, 요청 후 상태 반영, 오류 메시지) 중심으로 검증한다.

2. 회귀 영향 범위 테스트 실행
- account-dashboard, order-management-ui 관련 핵심 테스트를 실행해 UI 공통 컴포넌트/라우팅 회귀를 확인한다.

3. 빌드 검증
- `frontend/trading-web` production build를 실행해 타입/번들 단계 오류 유무를 확인한다.

4. QA 리포트 작성
- 실패 시 재현 명령, 기대 결과, 실제 결과, 우선순위를 명시한다.

## 검증 명령
```bash
cd C:/Users/dw/Desktop/paper-trading/frontend/trading-web
npm run test -- tax-summary
npm run test -- AccountDashboardPage OrderPage
npm run build
```

## 완료 조건
- Tax Summary feature-scope 테스트 결과가 명확히 보고됨.
- 기존 핵심 UI 회귀 테스트 결과가 보고됨.
- `frontend/trading-web` 빌드 성공 또는 실패 원인이 명확히 기록됨.

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
