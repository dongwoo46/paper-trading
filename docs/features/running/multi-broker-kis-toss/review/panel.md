# 합동 리뷰 — multi-broker-kis-toss 2단계

- 대상: 미커밋 변경(기준 `e2904c8`), `backend/trading-api` 코드 11개 + 새 파일 2개
- 리뷰어: 코드(`code-reviewer`) · 명세(`spec-reviewer`) · QA(`tester`) · 테스트 품질 렌즈(`domain-expert`). 4명 모두 결과 회수, 빠진 렌즈 없음
- 건수: 코드 4 · 명세 4 · QA 5 · 테스트 품질 5 = 18건 → 중복 병합 후 11건
- 작성자 증거: `./gradlew test --continue` 58개 파일, 413개, 실패 0 (`build/test-results/test/*.xml`)

## Findings

| ID | file:line | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | 근거 조항 | 리뷰어 |
|---|---|---|---|---|---|---|---|
| P-01 | ExecutionProcessor.kt:170 | 미지원 모드(TOSS_LIVE·UPBIT_LIVE) 체결 거부가 매도 분기 안, 체결 저장(L123)·포지션 차감(L154) 뒤에만 있고 매수 분기엔 없음. 롤백에만 기대며 «상태 변화 없음»을 증명하는 테스트도 없음. 현재 진입점이 없어 도달 불가 | should-fix | 이 변경 | 아니오 | label:gate-skipped-on-some-path | 코드(C-01)+QA(Q1,Q2)+테스트(A5) |
| P-02 | AccountTest.kt:139-161 | 계좌 표의 키가 `AccountType`이라 새 `TradingMode`를 넣어도 표를 안 고친 채 통과. 테스트 이름·주석·step-2 §6 기록이 «TradingMode.entries 비교»라고 사실과 다르게 적음 | should-fix | 이 변경 | 아니오 | TT21 | 테스트(A1)+명세(R-03)+QA(Q5) |
| P-03 | OrderController 주문 POST · trading-web `orderApi.ts:9` | UPBIT_LIVE 주문 응답이 201→501로 바뀌었는데 AE20 호출부 점검 결과와 기록이 없음 | should-fix | 이 변경 | 아니오 | AE20 | 명세(R-01) |
| P-04 | steps/step-2.md §3 | 선행 조건 RISK-04(운영 ddl-auto) 확인 기록 없음 | should-fix | 이 변경 | 아니오 | label:precondition-check-missing | 명세(R-02) |
| P-05 | OrderCommandService.kt:173·181·184, ExecutionProcessor.kt:170, BrokerApiExceptions.kt:5 | 새 주석이 종결어미 문장 | nit | 이 변경 | 아니오 | C12 | 코드(C-02) |
| P-06 | PositionExitTriggerOrchestrator.kt:170 | 결정적 501 거부를 3회 재시도(새 트랜잭션·계좌 잠금 3번) 후 FAILED | nit | 기존 | 아니오 | label:redundant-work-per-item | 코드(C-03)+QA(Q3) |
| P-07 | trading-web `entities/order/model/types.ts:6` | 프런트 모드 타입에 TOSS_LIVE 없음, 501은 범용 오류로 원문 JSON 표시 | nit | 범위 밖 | 아니오 | AE20 | 코드(C-04)+QA(Q4) |
| P-08 | OrderCommandServiceTest.kt:352 | `position.lockedQuantity == 0` 단언은 깨질 경로가 없음 | nit | 이 변경 | 아니오 | label:test-asserts-nothing | 테스트(A2) |
| P-09 | OrderCommandServiceTest.kt:405-413 | 기대값을 `if (routing == …) 1 else 0`로 정해 새 라우팅 값이 조용한 기본값이 됨. 거부 모드 목록(232)이 표와 따로 있음 | nit | 이 변경 | 아니오 | label:failure-or-absence-reported-as-value | 테스트(A3) |
| P-10 | OrderCommandServiceTest.kt:282-375·393-418, ExecutionProcessorTest.kt:238-264 | 모드를 `for`로 돌며 단언해 첫 실패가 나머지를 가리고, 메시지에 모드가 없음 | nit | 이 변경 | 아니오 | label:gate-message-missing-next-step | 테스트(A4) |
| P-11 | state.md §3 | 예외 범주 J·K를 미룬 사실이 변경 이력에 없음 | nit | 이 변경 | 아니오 | label:stale-doc-after-change | 명세(R-04) |

## 충돌

없음. 같은 자리에 상반된 판정은 없었다.

## 리뷰어별 확인 불가

- 코드: 샌드박스가 Gradle 캐시 쓰기를 막아 회귀 시험 렌즈를 못 돌림. 뮤테이션 렌즈는 미커밋이라 대상 0
- QA: Docker 소켓이 없어 통합 테스트 재실행 못 함(작성자 결과로 대체). 대상 단위 테스트 74건과 돌연변이 5종은 실행, 모두 RED

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f70616e656c2e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2265323930346338623436333832623966633630353633353937343766353535333865633366353033227d7d7d -->
