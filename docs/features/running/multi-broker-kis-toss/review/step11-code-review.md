# 코드 리뷰 결과 — multi-broker-kis-toss step 11 (KIS 주문 공통 포트·접수기·실패 분류 공용화)

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | `.worktrees/multi-broker-kis-toss-phase2` 미커밋 변경 중 `backend/trading-api` 아래(`git diff` + 추적 안 된 새 파일). `tech-plan-approval.md`·`docs/` 폴더는 제외 |
| 기준 | `rule-pointer --list <변경 파일>` 목록 그대로 — dev-team architecture·backend-api·common·events·failure-handling·tests, 레포 `rules/backend-api.md`(PTB)·`data-access.md`(PTD)·`failure-handling.md`(PTF)·`tests.md`(PTT), always:dev-team·always:arch |
| 프로필 | consumer |
| 리뷰어 | code-reviewer(구현자와 독립, 읽기 전용) |
| 날짜 | 2026-11-21 |

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 표준 · 유지보수성 · 보안 · 성능 · 회귀 시험 · 의도 |
| 수정 지킴 확인 | 확인 불가(미커밋 — `guards-fix --base` 가 커밋 범위를 재는데 변경이 커밋되지 않아 범위가 빔) |
| 뮤테이션 | 확인 불가(뮤테이션) — `mutation-run.js --base main` 이 샌드박스에서 임시 파일 생성 EPERM 으로 중단. 대상 후보: `KisFailureClassifier.kt`(분기 ≥ 3) |
| 발견 | 10개 (막음 1 · 후속 후보 9) |
| 머지 판정 | 머지 판정: 불가 — 막는 지적 1건 · 후속 후보 9건 |
| 한 줄 | 계층·기동 검사·롤백 경계·문구는 옛 실행기와 같음. 막는 것은 하나 — 주문 경로의 «결과를 모르는 실패»(타임아웃·5xx·ORNO 없는 rt_cd 0)가 롤백 + «잠시 후 다시 시도» 응답으로 나가 이중 실주문 위험(RISK-15)을 키움(C-01, 고침은 응답 계약이라 사람 승인 필요) |

## 1-1. consumer 프로필

- 1. 규칙 준수 — 대조한 조항 출처: rule-pointer --list. 대조: PTB7·PTB8(계층·포트 자리: `BrokerOrderSubmitter`·`OrderCommandService` 의 infrastructure import 0, `KisOrderAdapter` → `application.order.port` 만 — 위반 없음), PTB9(트랜잭션 경계 유스케이스), AE4·AE8·AE9·AE11·AE13·AE19·AE20(App-Impact 근거는 step-11 §5 13번 기록), AE21·CS14, FH1, PTF3, TT 계열. 어긴 것: C-01(PTF3·FH1), C-03(AE11·AE9), C-07(AE21), C-09(AE8·AE19).
- 2. 사이드 이펙트 — 바뀐 응답: 주문 접수 실패 500·400 → 400 KIS_ORDER_REJECTED / 409 KIS_ACCOUNT_NOT_LINKED / 503 KIS_*(D28 대로). 호출처 `OrderController.placeOrder`, `AutoExitOrderPlacementService`(REQUIRES_NEW) → `PositionExitTriggerOrchestrator.tryCreateGroupedAutoExitOrderWithRetry`(`catch (ex: Exception)` 재시도) — 예외 타입이 바뀌어도 잡는 쪽이 `Exception` 이라 분기 변화 없음. trading-web 주문 호출부 0(구현 기록 확인). Slack 은 성공 때만, 옛 글자와 같음(특성화 시험 175·210·246·269행 단언). `docs/TODO.md:123` 이 지운 파일을 가리킴(C-08).
- 3. 버그·장애 위험 — 트랜잭션: `BrokerOrderSubmitter.submit` 은 `@Transactional`(REQUIRED)로 `placeOrder` 트랜잭션에 참여, 던지는 예외가 모두 `RuntimeException` 계열(`ServiceUnavailableException`·`ApiDomainException`·`InternalErrorException`)이라 롤백 규칙은 옛 실행기와 같음. 다만 이를 실DB로 보인 시험은 없음(C-04). 기동 검사: `routeOf` 전수 when 과 `KisOrderAdapter.supportedModes` 가 같은 두 모드라 통과, KIS 빈에 조건부 등록(`@ConditionalOn*`) 없음 — 앱 기동 위험 못 찾음. 조용한 반환: C-07. 외부 호출 불확정: C-01·C-02.

## 2. 발견 (findings)

| ID | file:line | 렌즈 | 근거 조항 | 무엇이 틀렸나 | 등급 | 영향 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| C-01 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisFailureClassifier.kt:31 | 정확성 | PTF3 | 주문 경로에서 결과를 모르는 실패 셋 — 읽기 타임아웃(`KisTimeoutException`), KIS HTTP 5xx(`HttpServerErrorException` → `KisUnavailableException`, :29-31), rt_cd 0 인데 ORNO 없음(`IllegalArgumentException` → `KisUnavailableException`, :56, 이건 KIS 가 접수를 확정한 응답) — 이 모두 주문 행·멱등 키·잠금을 롤백하고, 핸들러가 «잠시 후 다시 시도해 주세요»(`GlobalExceptionHandler.kt:122-123`)로 답함. 같은 `idempotencyKey` 로 다시 보내면 `findByAccountIdAndIdempotencyKey` 가 비어 KIS 에 새 주문이 나감. 자동 청산은 `PositionExitTriggerOrchestrator.kt:170` `catch (ex: Exception)` 로 바로 재접수. 롤백 자체는 DEC-08 A(2026-11-16)로 유지가 정해졌으나, 옛 응답(500 «서버 오류», ORNO 없음은 400 BAD_REQUEST)과 달리 이번에 재시도를 권하는 문구가 붙었고 ORNO 없음 → 503 은 구현자 가정(step-11 §8). 롤백 자체(기존·DEC-08)는 이 행의 대상이 아니고, 이 행은 이번에 붙은 재시도 안내와 ORNO 없음 503 만 봄 | blocker | 버그·장애 | 이 변경 | 아니오 | 중간 | 주문 경로의 불확정 실패(읽기 타임아웃·HTTP 5xx·ORNO 없는 rt_cd 0)는 잔고와 같은 code·문구를 쓰지 말고 재시도를 권하지 않는 주문 전용 응답(«접수 여부를 확인한 뒤 다시 주문»)으로 분류 — 새 code·문구는 응답 계약이라 사람 승인 뒤 | — |
| C-02 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisFailureClassifier.kt:29 | 정확성 | label:output-format-assumption-wrong | KIS 가 오류 본문(`rt_cd`·`msg_cd`·`msg1`)을 HTTP 4xx/5xx 로 줄 때 RestTemplate 기본 오류 처리기가 `HttpStatusCodeException` 을 던지고, `transportFailure` 는 본문을 안 보고 KIS_UNAVAILABLE 로 보냄. 공개 사례상 EGW00201(초당 거래건수)·토큰 만료가 HTTP 500 으로 오면 S9-02 의 한도 판정(:23-26)과 인증 판정이 잔고·주문 모두 실제로는 안 먹음. 시험은 200 본문만 꾸밈(`KisAccountBalanceAdapterTest`·`KisOrderAdapterTest` `stubResponse`). 상태는 둘 다 503 이라 응답 상태는 같고 code 만 틀림. KIS 실제 상태 코드는 네트워크 차단으로 확인 불가 | should-fix | 동작 무관 | 이 변경 | 아니오 | 중간 | `HttpStatusCodeException.getResponseBodyAs(Map)` 의 `msg_cd`·`msg1` 을 같은 분류기로 넘김(KisOrderRestClient 안 고치고 분류기에서) | — |
| C-03 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/BrokerOrderSubmitter.kt:64 | 표준 | AE11 | 새 파일이 로그하고 다시 던짐(`log.error(it)` 뒤 `throw it`). 503 은 핸들러가 또 `log.warn(ex)`(`GlobalExceptionHandler.kt:39`)해 한 실패가 스택 둘로 남고, 400 주문 거부·한도·타임아웃처럼 사람이 지금 움직일 일이 아닌 것도 error 로 남음(AE9 → warning). 구현자가 AE9 만 후속으로 적음(step-11 §8), AE11 중복 로그는 기록 없음 | should-fix | 동작 무관 | 이 변경 | 아니오 | 높음 | 접수기는 맥락만 붙여 던지고 핸들러가 code 별 레벨로 한 번 로그, 또는 옛 글자 유지가 사람 결정이면 그 결정을 기록 | — |
| C-04 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/OrderCommandServiceTest.kt | 회귀 시험 | label:test-only-pure-layer-not-wiring | «접수 실패 시 주문·원장·잠금 롤백, 동작 불변»(step-11 §6 Done)을 보인 시험이 목 단위 시험(`KIS 접수 실패 예외는 바꾸지 않고 그대로 올린다`)뿐. 실DB 특성화 시험(`KisOrderIntakeBoundaryCharacterizationTest` 141·181·216·251행)은 성공 경로만 봄. 접수기에 `@Transactional` 이 빠지거나 예외를 삼켜도 지금 시험은 다 초록 | should-fix | 동작 무관 | 이 변경 | 아니오 | 높음 | 특성화 클래스 옆에 KIS 거부·타임아웃 뒤 주문 행·`buy-lock` 원장 0건 시험 하나 | — |
| C-05 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt:128 | 유지보수성 | label:duplicated-logic-diverges | KIS_ORDER_REJECTED 고정 문구가 두 곳에 서로 다르게 있음 — 예외 `KisOrderRejectedException.kt:10` «KIS가 주문을 받지 않았습니다.», 핸들러 `fixedDomainMessage` «…주문 내용과 계좌 상태를 확인해 주세요.». 예외 메시지는 원문을 받지 않는 고정값이라 핸들러 덮어쓰기가 막는 것이 없고, 문구를 고칠 때 한쪽만 바뀜 | nit | 동작 무관 | 이 변경 | 아니오 | 높음 | 예외 메시지 하나에 문구를 두고 `fixedDomainMessage` 제거 | — |
| C-06 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/BrokerOrderSubmitter.kt:73 | 유지보수성 | label:doc-claim-contradicts-code | 주석 «모드 분기의 유일한 자리» 와 달리 같은 모드 판정이 `OrderCommandService.kt:183-188`(`rejectIfBrokerOrderNotSupported`)과 `:191-199`(`submitExternalOrderIfNeeded`)에도 있음. TOSS_LIVE 주문을 열 때 세 곳을 함께 고쳐야 하고 하나만 고치면 501 또는 내부 오류로 갈림 | nit | 동작 무관 | 이 변경 | 아니오 | 높음 | 주석을 사실대로 고치거나 OrderCommandService 의 두 when 이 접수기의 route 를 묻게 함 | — |
| C-07 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/BrokerOrderSubmitter.kt:51 | 정확성 | AE21 | `order.id ?: return`·`order.account.id ?: return` 이 로그 없이 돌아와, 그 경우 브로커 접수 없이 주문이 PENDING 으로 커밋되고 호출자는 성공으로 읽음. 지금 유일한 호출자 `OrderCommandService.kt:73` 가 `requireNotNull(order.id)` 를 먼저 해 도달 불가라 막지 않음. 새 public `@Service` 라 다른 호출자가 생기면 실주문이 조용히 빠짐. 지금은 도달 불가라 영향은 동작 무관 | should-fix | 동작 무관 | 이 변경 | 아니오 | 높음 | `requireNotNull`/`InternalErrorException` 으로 바꿈 | — |
| C-08 | docs/TODO.md:123 | 유지보수성 | label:stale-doc-after-change | P1 후속 `kis-order-cancel-timeout` 의 근거 위치가 이번에 지운 `KisLiveOrderExecutor.kt:30-42`. 그 결함은 이제 `BrokerOrderSubmitter.kt:54-66`·`KisOrderAdapter.kt:24-25` 에 있음 | nit | 동작 무관 | 이 변경 | 아니오 | 높음 | — | — |
| C-09 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt:65 | 보안 | AE8 | 건드린 파일인데(AE19) `handleIllegalArgument`·`handleIllegalState`·`handleTypeMismatch`·`handleNoSuchElement` 가 `ex.message` 를 응답에 그대로 실음(:59·:65·:71·:92). 주문 경로 `requireNotNull` 메시지와 `KisTokenManager` 의 `ExternalServiceResponseException`(«…mode=live», 409)이 응답으로 나감. 이번 diff 는 trailing comma 만 바꿈 | should-fix | 동작 무관 | 기존 | 아니오 | 높음 | 응답 변경(AE20)이라 사람 결정 뒤 별 작업 | — |
| C-10 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisFailureClassifier.kt:40 | 정확성 | label:duplicated-logic-diverges | 잔고 분류는 여전히 rt_cd 1 을 인증 실패로 봄. 같은 파일 :61 주석은 «주문 거부의 흔한 rt_cd 가 1» 이라 rt_cd 1 이 일반 실패 코드임을 인정해, 잔고 쪽 rt_cd 1 업무 오류는 «KIS 키 설정을 확인해 주세요»(503 KIS_AUTH_FAILED)로 오안내됨. 판정 순서는 step 9 결정이라 기존 | nit | 동작 무관 | 기존 | 아니오 | 중간 | 인증 판정을 msg_cd(EGW00121·EGW00123 등)로 좁힘 — 사람 결정 | — |

## 3. 중점 질문 답

1. 롤백·멱등성 — 옛 실행기와 같음. 경계 `@Transactional`(REQUIRED, 호출 트랜잭션 참여), `runCatching … onFailure { throw it }`, 조기 반환 둘이 같은 모양. 바뀐 것은 던지는 예외 타입뿐이고 모두 RuntimeException 이라 롤백 표시가 같음. 남은 위험은 C-01(불확정 실패의 롤백 + 재시도 안내)·C-07(조용한 반환)·C-04(시험 공백).
2. 실패 분류 — 500 으로 새는 길: `restModeOf` 의 InternalErrorException(도달 불가), `else -> e` 로 나가는 Redis 오류(기존). 응답에 새는 KIS 원문은 못 찾음(KIS_* 는 code 고정 문구, 원문은 cause). 오분류: C-02(HTTP 오류 본문 무시), `IllegalArgumentException` 전부를 KIS_UNAVAILABLE 로 보내 설정 오류(빈 base URL 등 «URI is not absolute»)도 외부 장애로 보이는 점은 구현자가 §8 가정으로 기록해 따로 행을 두지 않음.
3. 기동 검사 — 결함 못 찾음. KIS 빈에 조건부 등록 없음, `routeOf` 와 `supportedModes` 가 같은 두 모드.
4. 로그·Slack 문구 — 성공 로그·Slack 은 옛 글자에서 모드 이름만 치환(특성화 시험 단언). 실패 로그는 글자 같지만 스택이 붙고 `reason=` 값이 KIS 원문 대신 분류된 고정 문구가 됨(원문은 스택의 cause). 중복 로그는 C-03.
5. 계층 의존 — application → infrastructure import 0, infrastructure → application 은 `order.port` 만. 위반 없음.

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f7374657031312d636f64652d7265766965772e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2237373330366339623538343932656164613263313832346434656537323566623237303239346437227d7d7d -->
