# 3단계 명세 정합 리뷰

- 대상: 미커밋 변경(기준 HEAD a43538a) `backend/`와 새 파일 3개
- 명세: step-3.md §1·§2·§4·§5, development-plan.md(D5, D15 수정본, §3, §6 에러 정렬 행, RISK-05, DEC-02, §9), feature-spec.md 87행·RULE-03·04
- 규칙: rule-pointer가 짚은 레포 규칙 `rules/backend-api.md` 전체(PTB7). 그 규칙이 직접 열라고 한 팀 규칙 AE1·AE5·AE6·AE8·AE14·AE19·AE20
- 경로 줄임: `main/`은 `backend/trading-api/src/main/kotlin/com/papertrading/api/`
- 리뷰어: spec-reviewer. 읽기 전용 훅 때문에 리뷰어가 파일을 못 써서 오케스트레이터가 응답 본문을 옮김

## 1. 완료 기준

| 기준 | 판정 | 근거 |
|---|---|---|
| 1. 400으로 뭉치는 check, require 없음 | 충족 | 5-1 표 28곳이 전부 새 예외로 바뀜(부등호 방향까지 diff 대조). 남은 것은 §2가 대상 밖으로 정한 requireNotNull, 그리고 `OrderCommandService.kt:210`의 require 1건(시세 리스너 → 자동 청산 경로로만 닿고 HTTP 경로 없음). 핸들러 쪽은 S3S-01 |
| 2. 경로별 상태·code 단언, 전체 실패 0, ArchitectureRulesTest | 부분 | 28곳 모두 code 단언 있음. XML 집계 8개 클래스 154건 실패·오류·건너뜀 0, ArchitectureRulesTest 13건 통과. 전체 스위트 증거는 없음 → S3S-03 |
| 3. ktlintCheck, detekt, verifyLintBaselinesNotGrown | 기록상 충족 | §6 기록 기준. ktlint 기준선은 `line=` 값만 바뀜. detekt 기준선과 `archunit_store` 변경 0 |
| 4. AE20 목록, trading-web 점검 기록 | 충족 | §5-3. 표기 보완은 S3S-05 |

## 2. 범위, D15, DEC-02

- 범위: main 7파일은 대상 5개와 `common/exception/` 새 파일 2개. 시험 7파일은 바뀐 예외를 단언하는 곳뿐(RiskPolicyServiceTest는 `RiskPolicyService.kt:27`이 `Account.createRiskPolicy`를 불러서, AccountControllerIntegrationTest는 출금 초과 400→409 정정과 이중 비활성화 409 추가).
- D15 수정본: I·K 범주 예외는 Spring 무의존, 501·500 매핑은 `GlobalExceptionHandler.kt:24-41`, K는 고정 문구 응답과 error 로그(AE8). 동결 예외 그대로. J(503) 미사용으로 미추가.
- DEC-02: AE20 목록이 28곳 code를 모두 담음. trading-web이 부르는 변경 경로는 주문 등록·취소(`orderApi.ts:9, 23`)뿐이고 상태로 분기하는 곳(`TaxSummaryPage.tsx:17-32`, `PortfolioChartPage.tsx:46`)은 변경 경로가 아님. 프런트 수정 필요 없음.
- 내부 호출자: 바뀐 예외를 타입으로 잡던 곳 없음. 새 예외도 `RuntimeException`이라 롤백 동작 같음.
- feature-spec 87행: TOSS_LIVE 주문 501 거부 유지(통합 시험 5건 통과).

## 3. Findings

| ID | file:line | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 |
|---|---|---|---|---|---|---|
| S3S-01 | main/presentation/exception/GlobalExceptionHandler.kt:49-59 | AE19 | 이번 diff로 핸들러도 건드린 파일이 됐는데 `IllegalArgumentException`·`IllegalStateException`을 모두 400 `BAD_REQUEST`로 뭉치는 옛 매핑이 남음. AE19는 지금 정렬하거나 사람이 `API-Errors-Deferred`로 미루라고 함. 기록된 미룸 결정이 없었음. 같은 파일 43-47, 61-65의 묶음 code도 같은 결정 대상 | blocker | 기존 | 아니오 |
| S3S-02 | main/common/exception/CommonApiExceptions.kt:163-216 | PTB7 | 새 A·F 예외 15개가 동결 기반(`BadRequestException`·`ConflictException`, 상위 `ApiDomainException`이 `HttpStatus` 보유)을 상속해 HTTP 상태를 common에서 정함. domain인 Account가 그중 9개를 새로 참조. ArchUnit은 상속 간접 의존을 못 봄. step-3.md §5-1의 «새 예외는 Spring 무의존» 문장은 K 예외 2개에만 맞음 | should-fix | 이 변경 | 아니오 |
| S3S-03 | docs/features/running/multi-broker-kis-toss/steps/step-3.md:63 | label:stale-doc-after-change | 완료 기준 «전체 테스트 실패 0» 증거 없음. 관련 8개 클래스만 실행. 완료 체크 전 §6에 전체 결과 첨부 필요 | should-fix | 이 변경 | 아니오 |
| S3S-04 | docs/features/running/multi-broker-kis-toss/steps/step-3.md:193 | label:stale-doc-after-change | §8의 «limitPrice 0 이하 잠금 없이 접수(자금 안전 결함 후보)»는 사실과 다름. `Order.kt:184` `require(limitPrice == null \|\| limitPrice > 0)`가 잠금(:73)보다 먼저 막아 400으로 거부됨 | should-fix | 이 변경 | 아니오 |
| S3S-05 | docs/features/running/multi-broker-kis-toss/steps/step-3.md:133 | AE20 | 엔드포인트 목록에 계좌명 변경 `PATCH /api/v1/accounts/{id}`가 빠졌고 리스크 정책은 경로 `POST /api/v1/accounts/{accountId}/risk-policy`가 없음. «이미 활성화된 계좌 활성화»는 `activate()` 호출자가 없어 HTTP 응답 변화가 아님. 점검 결론은 그대로 | nit | 이 변경 | 아니오 |
| S3S-06 | docs/features/running/multi-broker-kis-toss/state.md:40 | label:stale-doc-after-change | 로드맵 3단계 상태가 `todo`인데 step-3.md는 `in progress` | nit | 이 변경 | 아니오 |
| S3S-07 | docs/features/running/trading-web-visual-redesign/progress.md:8 | label:stale-doc-after-change | 다른 기능 문서가 미커밋으로 바뀌어 있음. 이 단계 커밋에서 제외 필요 | nit | 범위 밖 | 아니오 |

## 4. 참고(판정 밖)

- code-review 몫으로 넘김: `GlobalExceptionHandler.kt:83-87`의 `handleException`이 예외를 로그 없이 500으로 돌려줌(AE21).

막는 지적 1건
