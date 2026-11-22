# PR-A1 합동 리뷰 (1회차)

- 범위: `e510634..fb23df3` (브랜치 fix/trading-api-money-state), 변경분 합동
- 리뷰어: 코드(code-reviewer) 7건, 정합성(spec-reviewer) 4건, QA(tester) 6건 → 중복 합친 뒤 14건
- 작성자 시험 증거: fb23df3 `./gradlew test ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=e510634 --rerun-tasks` BUILD SUCCESSFUL(418 시험)
- 사람 결정(2026-11-15 팝업 «메모리 반올림 제거»): 체결 금액은 수량×체결가 그대로, 평균가는 가중평균(표시용 4자리). 저장 열 소수 4자리 문제(P-05)는 별도 항목. 수량 초과 체결 거부·끝난 주문 체결 로그는 이번에 고침

| ID | file:line | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | 리뷰어 |
|---|---|---|---|---|---|---|---|
| P-01 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:148 | PTB4 | 평균가를 반올림된 누적 금액 ÷ 수량으로 구해 소수 수량 단일 체결도 평균가 ≠ 체결가(70000.1234 → 70000.1231) | blocker | 이 변경 | 예 | QA |
| P-02 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:144 | PTB4 | 이번 체결 금액을 4자리 HALF_UP 으로 반올림해 극소 수량이면 0 이 되어 confirmBuy 실패 | blocker | 이 변경 | 예 | QA |
| P-03 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:144 | PTB4 | 잠금은 반올림 없이, 체결 금액은 4자리 반올림이라 소수 수량 단일 체결도 잠금 부족으로 실패(단위 시험) | blocker | 이 변경 | 예 | QA |
| P-04 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:142 | TT33 | 새 수량·가격 > 0 검사를 찍는 시험이 없음 | should-fix | 이 변경 | 아니오 | 코드+정합성 |
| P-05 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:144 | PTB4 | 소수 수량을 나눠 체결하면 저장 자리수(소수 4자리) 끝수 합이 잠금을 0.0001 넘어 마지막 체결 실패 | should-fix | 기존 | 아니오 | QA |
| P-06 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:150 | PTB5 | 남은 수량을 넘는 체결을 받아 체결 수량이 주문 수량을 넘고 다른 주문 잠금을 씀 | should-fix | 기존 | 아니오 | 코드+QA |
| P-07 | docs/features/running/trading-api-money-state/development-plan.md:206 | label:plan-drift | 계획은 «동결 저장소 diff 0» 인데 1줄 삭제(사람 승인)·원장 저장 합침이 계획·step 문서에 기록 없음 | should-fix | 이 변경 | 아니오 | 코드+정합성 |
| P-08 | docs/features/running/trading-api-money-state/state.md:17 | label:progress-doc | state.md·step-1.md 가 «기준선 재생성이 다음»에 멈춤, 기록 칸 비어 있음 | should-fix | 이 변경 | 아니오 | 정합성 |
| P-09 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:155 | AE19 | DEC-07 1안의 `API-Errors-Deferred:` 기록이 커밋에 없음 | should-fix | 이 변경 | 아니오 | 정합성 |
| P-10 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:99 | label:silent-skip | FILLED·CANCELLED 주문에 새 체결 id 가 와도 로그 없이 return | nit | 기존 | 아니오 | QA |
| P-11 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:169 | label:silent-clamp | 해제액을 0 하한으로 깎을 때 잠금 초과 사용 사실이 기록 없이 묻힘 | nit | 기존 | 아니오 | 코드 |
| P-12 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:137 | C12 | KDoc 이 «~돌려준다» 종결어미 문장 | nit | 이 변경 | 아니오 | 코드 |
| P-13 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt:166 | label:comment-accuracy | KDoc 이 «취소 때»도 쓴다고 하나 취소 경로는 PR-A2 전까지 자체 식 | nit | 이 변경 | 아니오 | 코드 |
| P-14 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt:114 | TT7 | 새 시험이 내부 리포지토리를 목으로 바꿈(파일 기존 방식, C 갈래) | nit | 기존 | 아니오 | 코드 |

충돌: 없음. 원장 저장 합침은 코드·정합성 둘 다 «정당한 정리»로 판정.

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f74726164696e672d6170692d6d6f6e65792d73746174652f7265766965772f70616e656c2e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2266623233646633396166393135663036633837313363343439353336666235366134353235363934227d7d7d -->
