# 시험 증거 — multi-broker-kis-toss 2단계 (변경분 QA, 2회차)

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 기능 | multi-broker-kis-toss 2단계 — TOSS_LIVE 추가, TOSS_LIVE·UPBIT_LIVE 주문 501 선행 거부 |
| 테스터 | `devkit:tester` (작성자와 독립) |
| 날짜 | 2026-11-15 (KST) |
| 대상 | 커밋 `52db67b`(기준 `e2904c8`)의 `backend/trading-api` 13개 파일 |
| 근거 | `steps/step-2.md` §4 완료 기준 · `review/qa.md` 1회차 Q1~Q5 · `review/panel.md` P-01·P-02·P-08·P-09·P-10 |
| 방법 | devkit `test-design`. 저장소 코드는 바꾸지 않음. 돌연변이는 격리 사본(`scratchpad/qa2/trading-api`, `build`·`.gradle` 제외 rsync)에서만 |

## 1. 실행 기록

| 명령 | 위치 | exit | 결과 |
|---|---|---|---|
| 작성자 결과 집계(`build/test-results/test/*.xml` 파싱) | worktree | — | 58개 파일 · 438건 · 실패 0 · 오류 0 · 건너뜀 0. 시각 `2026-11-14T18:52:30Z`~`18:53:07Z`(03:52~03:53 KST), 커밋 04:13 KST |
| 신선도: `find src -newer <가장 이른 XML>` | worktree | 0 | 결과 XML 이후 바뀐 소스 0개 |
| 빠진 클래스: `@Test`·`@ParameterizedTest` 있는 소스 파일 목록 ↔ XML 클래스 목록 `comm -3` | worktree | 0 | 양쪽 58개, 차이 0줄. 안 돈 테스트 클래스 없음 |
| 대상 케이스 판정(XML 안 이름 필터) | worktree | — | 새 케이스 37건 전부 통과. `OrderControllerIntegrationTest` 10/0/0(TOSS_LIVE 4 + UPBIT_LIVE 1 포함, Docker 있는 작성자 환경에서 실행된 결과) |
| 돌연변이 N1: `TradingMode` 에 `NEW_LIVE` 추가 + 메인 전수 `when` 5곳과 `Account.validate` 에 분기 추가(테스트 표는 그대로). 스크립트 `scratchpad/mutate_newmode.py` | 사본 | — | 사본과 worktree 차이 = 변형한 메인 파일 5개뿐(`diff -rq`) |
| `./gradlew test --offline --tests 'com.papertrading.api.domain.entity.account.AccountTest'` | 사본(N1 적용) | 1 | 57건 중 2건 RED: `모드별_허용_계좌타입_표의_키가_TradingMode_전체와_같다`(키 집합 불일치) · `모드별로_허용된_계좌타입만_생성할_수_있다[NEW_LIVE]`(`Key NEW_LIVE is missing in the map`). 컴파일은 통과 — 모든 테스트 소스가 새 모드로 컴파일됨 |
| worktree 무결성: `git status --porcelain` · `revert-probe.js` | worktree | 0 | `backend/` 변경 0. 원본 소스는 읽기만 함 |
| 통합 테스트 재실행 | — | 안 함 | Docker 필요, 요청대로 안 함. 통합 판정은 작성자 XML 만 근거 |
| 전체 스위트 재실행 | — | 안 함 | 요청대로 안 함 |

## 2. 기준·지적별 판정

판정: «증거 있음(실행)» = 이번 또는 1회차에 돌연변이를 돌려 RED 를 봄 · «증거 있음(읽기)» = 단언이 틀린 구현을 잡는 경로를 코드로 따라갔으나 되돌림 실행은 안 함 · «부족» · «확인 못 함».

| ID | 기준 또는 지적 | 테스트 file:line | 단언 | 판정 | 등급 | 근거 조항 |
|---|---|---|---|---|---|---|
| K1 | 완료 기준 1: 모드 누락 표가 `TradingMode.entries` 전체를 덮음 | `ExecutionProcessorTest.kt:261`·`:267` · `OrderCommandServiceTest.kt:392`·`:398` · `RedisMarketQuoteAdapterTest.kt:32` · `AccountTest.kt:149`(+ 행렬 `모드별로_허용된_계좌타입만_생성할_수_있다`) | 표 키 집합 `== TradingMode.entries.toSet()` · `@EnumSource(TradingMode::class)` 로 모드마다 `getValue` | 증거 있음(실행). 계좌 표는 이번 N1 에서 RED 2건. 주문·체결·시세 표는 1회차 M2 에서 RED(같은 키 집합 단언, 이번 커밋 판으로는 재실행 안 함). 앞단에 컴파일 장벽도 있음 — 전수 `when` 5곳(`OrderCommandService` 2 · `ExecutionProcessor` 2 · `RedisMarketQuoteAdapter` 1), 1회차 M1 에서 컴파일 실패 확인 | 없음 | `label:mode-table-exhaustive` |
| K2 | 완료 기준 2: TOSS_LIVE·UPBIT_LIVE 주문 501, 주문·원장·잠금·구독 변화 없음 | 단위: `OrderCommandServiceTest.kt:283`(지정가 매수) · `:297`(시장가) · `:311`(매도) · `:324`(IOC·FOK) · `:341`·`:358`(자동 청산). 통합: `OrderControllerIntegrationTest.kt:322`·`:329`·`:336`·`:343`·`:357`(공통 단언 `:299`) | 단위: 예외 타입 · `errorCode` · `HttpStatus.NOT_IMPLEMENTED` · 예수금 1,000,000/잠금 0 · `orderRepository.save`·원장 저장·포지션 조회·구독·시세·KIS 실행기 0회(`:266`). 통합: HTTP 501 · `$.code` · 주문 수·원장 수·예수금·잠금 예수금·포지션 잠금 수량 전후 같음 · 구독 포트 상호작용 0 | 단위 증거 있음(실행): 1회차 M3(거부 호출 삭제 → 7건 RED) · M5(거부를 수량 잠금 뒤로 → RED). mockk 엄격 모드라 거부 전에 다른 저장소를 부르면 다른 예외로 실패하는 이중 장치. 통합은 작성자 XML 통과만 — 이번에 직접 실행 못 함 | 없음 | `label:reject-before-side-effect` |
| K3 | 완료 기준 3: 기준선 대비 새 실패 0 | 전체 스위트 | 실패·오류 합계 | 증거 있음(작성자 결과 집계). 실패 0·오류 0·건너뜀 0, 안 돈 클래스 0, 결과 이후 바뀐 소스 0 | 없음 | `label:suite-green-fresh` |
| Q1 | 1회차 QA: 매도 체결 거부가 체결 저장·포지션 차감 뒤 → 롤백 의존 | `ExecutionProcessorTest.kt:223`(공통 단언 `:212`) | 예외 · `errorCode` · 예수금 그대로 · `order.filledQuantity` 0 · `executionRepository.save`·포지션 조회·저장·원자 감소·원장·정산 저장 0회 | 증거 있음(읽기). 거부가 `processFill` 진입부(계좌 조회 직후, `applyExecution` 전)로 옮겨짐. 옛 구현은 `executionRepository.save` 뒤에 던지므로 `verify(exactly = 0)` 이 깨지고, `applyExecution` 이 `filledQuantity` 를 바꾸므로 그 단언도 깨짐. 되돌림 실행은 안 함(클래스 1개 제한) | 없음 | `label:reject-before-side-effect` |
| Q2 | 1회차 QA: 매수 체결은 거부 없이 통과 | `ExecutionProcessorTest.kt:241` | 예외 · 예수금 그대로 · `filledQuantity` 0 · 같은 부작용 0회 | 증거 있음(읽기). 거부가 매수 분기에 없으면 예외가 안 나 `assertThrows` 가 깨짐 | 없음 | `label:gate-skipped-on-some-path` |
| Q3 | 1회차 QA: 자동 청산 501 을 3번 재시도 | 없음 | — | 부족. 코드·테스트 모두 그대로(기존 재시도 정책, 이번 범위에서 손대지 않음) | nit | `label:redundant-work-per-item` |
| Q4 | 1회차 QA: 프런트 501 문구·TOSS_LIVE 타입 | 없음(백엔드 범위 밖) | — | 확인 못 함(범위 밖, DEC-02 사람 확인 몫) | nit | `label:client-branch-on-status-change` |
| Q5 | 1회차 QA: 계좌 표가 `AccountType` 만 전수 강제 | `AccountTest.kt:149` · 행렬 테스트 | 위 K1 과 같음 | 증거 있음(실행). N1 에서 RED 2건 — 1회차 M2 에서 51/0 통과하던 빈틈이 닫힘 | 없음 | `label:mode-table-exhaustive` |
| P-01 | 패널: 체결 거부 위치·매수 누락·«상태 변화 없음» 테스트 부재 | `ExecutionProcessorTest.kt:223`·`:241`·`:267`(표의 `REJECTED` 분기도 `verifyNoFillSideEffects`) | Q1·Q2 와 같음 | 증거 있음(읽기). Q1·Q2 참고 | 없음 | `label:reject-before-side-effect` |
| P-02 | 패널: 계좌 표 키가 `AccountType` · 이름·주석이 사실과 다름 | `AccountTest.kt:139-175` | 키 = `TradingMode`, 값 = 허용 계좌 타입 집합 | 코드 쪽 증거 있음(실행, N1). 이름·주석이 이제 실제 단언과 맞음. step-2 §6 기록 문구는 확인 안 함 | 없음 | `label:mode-table-exhaustive` |
| P-08 | 패널: `position.lockedQuantity == 0` 단언이 깨질 경로 없음 | `OrderCommandServiceTest.kt:340`(주석) · `:341` | 그 단언은 삭제됨. 대신 `positionRepository.findByAccountIdAndTickerWithLock` 0회 | 증거 있음(읽기). 수량 잠금 경로는 이 조회 하나뿐(`OrderCommandService.kt:86-94` `lockSellQuantityIfNeeded`), `createAutoExitSellOrder`(`:224`)도 `placeOrder` 를 탐 — 조회 0회면 잠금 불가 | 없음 | `label:test-asserts-nothing` |
| P-09 | 패널: 기대값 `if (routing == …) 1 else 0` · 거부 목록이 표와 따로 | `OrderCommandServiceTest.kt:384-389`(`expectedCalls` 전수 `when`) · `:432`(표에서 파생) · `ExecutionProcessorTest.kt:306` | 라우팅 값마다 저장·KIS 실행기 호출 수 명시 | 고쳐짐(읽기). 새 라우팅 값은 `when` 컴파일 에러, 거부 모드 목록은 두 클래스 모두 표 하나에서 파생 | 없음 | `label:failure-or-absence-reported-as-value` |
| P-10 | 패널: `for` 반복 단언이 첫 실패 뒤를 가리고 메시지에 모드 없음 | `OrderCommandServiceTest.kt:283-358` · `ExecutionProcessorTest.kt:223-267`(매개변수화, 이름 `{0}`) · 남은 반복: `RedisMarketQuoteAdapterTest.kt:32-41` · `AccountTest.kt` 행렬(계좌 타입 반복) | 매개변수화 + 메시지에 `$mode` | 대부분 고쳐짐. 남은 두 반복은 메시지에 모드(와 계좌 타입)가 있어 어디서 깨졌는지는 보이나 첫 실패가 뒤 모드를 가림 | nit | `label:gate-message-missing-next-step` |

## 3. 빠진 엣지

| ID | 엣지 | 현재 | 판정 | 등급 | 근거 조항 |
|---|---|---|---|---|---|
| E1 | 새 모드 추가 시 표 테스트가 실패하나 | 컴파일 장벽(전수 `when` 5곳) + 표 4종 키 집합 단언 | 증거 있음 — K1 | 없음 | `label:mode-table-exhaustive` |
| E2 | 거부 시 자금·포지션 상태 그대로인가 | 단위: 예수금·잠금 예수금·포지션 조회 0회(주문), 예수금·`filledQuantity`·저장 0회(체결). 통합: 예수금·잠금·포지션 잠금 수량·주문·원장 수 | 증거 있음 — K2, Q1. 통합의 UPBIT_LIVE 는 지정가 매수 1종뿐(시장가·매도·IOC 는 단위로만) | nit | `label:reject-before-side-effect` |
| E3 | 같은 `idempotencyKey` 재요청 — 변경 전에 접수된 UPBIT_LIVE 주문은 거부보다 먼저 기존 주문을 돌려줌(`OrderCommandService.kt:52-53`) | 저장소 테스트 없음. 1회차 탐침 P3(저장소 밖)만 | 부족. 순서가 바뀌면(거부를 멱등 확인 앞으로) 이미 접수된 주문의 재요청이 501 로 바뀌어도 아무 테스트도 안 깨짐 | nit | `label:idempotency-path-untested` |
| E4 | 변경 전에 남은 UPBIT_LIVE 대기 주문의 취소·GTD 만료가 잠긴 자금을 푸는가 | 저장소 테스트 없음. 1회차 탐침 P1·P2(저장소 밖)만. `cancelOrder` 는 이번에 안 바뀜 | 부족. 나중에 취소 경로에 같은 거부를 넣으면 자금이 영영 잠기는데 막는 테스트가 없음 | nit | `label:locked-funds-release-untested` |
| E5 | `fillLocal` 경로의 체결 거부 | 체결 거부 테스트는 `fillExternal` 만. 둘 다 같은 `processFill` 호출 | 증거 있음(읽기) — 같은 진입부를 탐 | nit | `label:gate-skipped-on-some-path` |
| E6 | 400 과 501 의 우선순위(비활성 계좌·지정가 가격 누락 + 거부 모드) | 저장소 테스트 없음. 1회차 탐침 P4·P5 만 | 부족. 400 범주 정렬은 step-3(DEC-02) 범위라 지금은 기록만 | nit | `label:error-precedence-untested` |
| E7 | `AccountSource.TOSS` 추가 | 참조 0곳 — `AccountResponse.kt:18`·`:34` 는 `LOCAL` 고정, 테스트 0건 | 확인 못 함(의도). 동작 없는 값이라 위험은 없으나 TOSS_LIVE 계좌 응답이 `source=LOCAL` 로 나가는지 정할 사람 확인 필요 | nit | `label:unused-enum-value` |

## 4. 실패·리스크

- 발견 실패: 없음. 이번 돌연변이 RED 2건은 의도한 탐지(테스트가 새 모드를 잡음).
- 미커버: 통합 테스트 직접 재실행(Docker), 주문·체결·시세 표의 이번 커밋 판 돌연변이 재실행(클래스 1개 제한 — 1회차 M2 결과로 대신함), Q1·Q2·P-08 되돌림 실행.
- 리스크(테스트 밖, 기록만): `application.yaml:19` `ddl-auto: create` — 테스트는 `create-drop` 이라 새 enum 값이 들어간 스키마로 늘 새로 만들어짐. 운영 DB 의 `trading_mode` 제약 갱신은 이 테스트들로 보이지 않음(패널 P-04 · RISK-04 몫).

## 5. 판정

- 완료 기준 3개 모두 증거 있음(K2 의 통합 부분은 작성자 결과만).
- 1회차 QA·패널의 테스트 지적: Q1·Q2·Q5·P-01·P-02·P-08·P-09 해소, P-10 일부 남음(nit), Q3·Q4 범위 밖·그대로(nit).
- 막는 지적 0건 · should-fix 0건 · nit 8건(Q3·Q4·P-10·E2·E3·E4·E6·E7).

<!-- devkit:reviewed-head 6387778d31cf46ee9f2046212a48b4625268e29a -->

---

# step 3 시험 보강 — S3Q-01·02·03 (격리 사본 사전 검증 뒤 본 트리 적용·재실행, §9 첫 줄)

## 6. 메타데이터

| 항목 | 값 |
|---|---|
| 대상 | 커밋 `7111818`(step 3 에러 응답 정렬)의 리뷰 `review/step3-panel.md` S3Q-01·02·03 |
| 테스터 | `devkit:tester` |
| 날짜 | 2026-11-15 15:09~15:20 (KST) |
| 상태 | 시험 코드는 본 트리에 아직 없음. tester 쓰기 범위 게이트가 `backend/**/src/test/**` 쓰기를 거부(허용 = `tests/**`·`docs/**`) → 격리 사본(레포 밖 scratchpad 의 `copy/trading-api`, `build`·`.gradle`·`.kotlin` 제외 rsync)에 작성·실행. 적용용 패치 `step3-test-reinforcement.patch`(sha256 `e444fdf1…c69c`, 추가 120줄·삭제 0줄) |
| 제품 코드 | 사본 `src/main` 과 본 트리 `src/main` 은 `diff -rq` 차이 0(돌연변이 복구 뒤) · 본 트리 `git status --short backend/` 빈 출력 |

## 7. 실행 기록

| 명령 | 위치 | exit | 결과 |
|---|---|---|---|
| `./gradlew test --tests OrderCommandServiceTest --tests GlobalExceptionHandlerTest --tests OrderControllerIntegrationTest` | 사본(최종 판) | 0 | XML 집계: `OrderCommandServiceTest` 37/실패 0/오류 0/건너뜀 0 · `OrderControllerIntegrationTest` 17/0/0/0(Testcontainers, Docker 29.5.2) · `GlobalExceptionHandlerTest` 7/0/0/0. 시각 06:19:37~06:19:55Z |
| 돌연변이 M-01: `OrderCommandService.cancelOrder` 소유 검사(404)와 상태 검사(409) 순서 뒤집기 | 사본 | — | 사본 diff 는 그 두 문장 이동뿐 |
| `./gradlew test --tests OrderCommandServiceTest` | 사본(M-01) | 1 | 37건 중 2건 RED: 새 시험 `[FILLED]`·`[CANCELLED]` — `expected OrderNotFoundException but was OrderNotCancelableException`. 기존 35건은 초록 그대로(리뷰가 본 «못 잡음» 재현) |
| 돌연변이 M-02: `GlobalExceptionHandler` 의 `@ExceptionHandler(InternalErrorException::class)`·`@ExceptionHandler(NotImplementedException::class)` 주석 처리 | 사본 | — | 두 줄만 바뀜 |
| `./gradlew test --tests GlobalExceptionHandlerTest` | 사본(M-02) | 1 | 7건 중 3건 RED, 전부 새 MVC 경유 시험: K 기반 `$.code expected SOME_DEFECT but was INTERNAL_SERVER_ERROR` · K 하위 `expected LOCKED_DEPOSIT_INSUFFICIENT but was INTERNAL_SERVER_ERROR` · I 하위 `Status expected 501 but was 500`. 기존 직접 호출 4건은 초록 그대로. 두 매핑을 한 번에 지웠으나 K 시험은 code, I 시험은 상태로 갈려 실패해 매핑별 탐지가 구분됨 |
| 복구: `cp *.orig` → `cmp` 본 트리 원본 일치 · `diff -rq src/main` | 사본 | 0 | 두 파일 모두 원본과 같음 |
| `node …/devkit/0.95.0/tools/revert-probe.js` | 본 트리 | 0 | 흔적 출력 없음 |
| `./gradlew ktlintCheck detekt --continue` | 사본(최종 판) | 0 | 위반 0. `verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main` 은 사본이 git 저장소가 아니라 못 돌림 — 본 트리 적용 뒤 실행 필요 |
| `git apply --check --numstat <패치>` | 본 트리 | 0 | 세 파일 26·51·43줄 추가, 삭제 0. 커밋 게이트(삭제된 단언 줄만 봄)에 걸릴 줄 없음 |
| 전체 스위트 | — | 안 함 | 요청대로(CI 몫) |

## 8. 항목별 판정

| ID | 추가한 시험 | 단언 | 판정 |
|---|---|---|---|
| S3Q-01 | `OrderCommandServiceTest` `다른 계좌의 취소 불가 상태 주문 취소도 409 가 아니라 404 ORDER_NOT_FOUND` (`@EnumSource` FILLED·CANCELLED) | `OrderNotFoundException` · code `ORDER_NOT_FOUND` · 404 · 상태 그대로 · 계좌 잠금 조회 0회 · 원장 저장 0회 | 증거 있음(실행) — M-01 RED 2건 |
| S3Q-02 | `GlobalExceptionHandlerTest` MVC 경유 3건(`MockMvcBuilders.standaloneSetup` + `setControllerAdvice`, 시험 전용 중첩 컨트롤러). 레포 MockMvc 시험은 전부 `@WebMvcTest`+`@Import(GlobalExceptionHandler)` 라 실제 컨트롤러가 필요 → 범주 예외를 직접 던질 수 있게 standalone 선택, DSL(`mockMvc.get{}`)은 레포 관례 그대로 | K 기반: 500 · `$.status` 500 · `$.code` SOME_DEFECT · 고정 메시지(내부 메시지 미노출) · K 하위: 500 · 자기 code · I 하위: 501 · `$.status` · 자기 code | 증거 있음(실행) — M-02 RED 3건 |
| S3Q-03 | `OrderControllerIntegrationTest` `expectOrderError` 에 전후 발자국 비교 추가(400 2건·409 3건) · 다른 계좌 취소 404(양쪽 계좌 발자국 + 주문 PENDING) · 재취소 409(첫 취소 뒤 발자국 + CANCELLED) | 주문 행 수 · 원장 행 수 · 가용 예수금 · 잠금 예수금 · 포지션 수량 · 포지션 잠금 수량(금액·수량은 `compareTo`, 포지션 없음은 null 그대로) | 증거 있음(실행, 초록만). 불변 단언 자체의 돌연변이는 안 돌림(요청 범위 밖) |

## 9. 남은 것

- 본 트리 적용과 재실행(2026-11-15, 메인 세션): `git apply` 로 패치 적용(시험 파일 3개, 추가 120줄·삭제 0줄). 세 시험 클래스 실행 exit 0, XML 집계 `OrderCommandServiceTest` 37 · `GlobalExceptionHandlerTest` 7 · `OrderControllerIntegrationTest` 17, 실패·오류·건너뜀 0. `ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=origin/main` exit 0.
- 의도적으로 안 넣은 것: `LOCAL 시장가 매수 주문 — stale 시세면 400`·`LOCAL 미보유 종목 매도 시 400` 의 불변 단언. ktlint 기준선이 줄 번호로 묶여 있어, 기존 위반(통합 시험 파일 23건) 위쪽에 줄을 넣으면 기준선이 어긋나 `ktlintCheck` 가 빨개짐(사본에서 실측). 넣으려면 `ktlintGenerateBaseline` 재생성이 필요하고 그건 설정 파일 변경이라 메인 세션 판단 몫.
- 발견한 제품 결함: 없음. 400·409·404 거부 7경로 모두 주문·원장·자금·포지션이 요청 전과 같음(특히 `INSUFFICIENT_AVAILABLE_DEPOSIT` 은 주문 저장 뒤 실패하지만 트랜잭션 롤백으로 주문 행이 남지 않음).
