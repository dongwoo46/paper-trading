# 코드 리뷰 결과 — multi-broker-kis-toss step 11 (재리뷰 1 = 2회차)

범위: `변경분` — `.worktrees/multi-broker-kis-toss-phase2` 미커밋 변경(`git diff` + 추적 안 된 파일) 중 `backend/trading-api`, `development-plan.md`, `steps/step-11.md`, `docs/TODO.md`. 재리뷰 모드: 지난 막는 지적 판정 + 이번 수정(C-01·C-02·C-08)이 깬 것만.

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | 이번 고침이 건드린 파일: `KisFailureClassifier.kt` · `KisOrderResultUnknownException.kt`(새) · `GlobalExceptionHandler.kt` · `KisAccountBalanceAdapter.kt` · `KisApiException.kt` · 시험 `KisOrderAdapterTest` · `KisAccountBalanceAdapterTest` · `GlobalExceptionHandlerTest` · `KisHttpErrorFixtures.kt`(새), 문서 `development-plan.md` D28·§10.4 · `steps/step-11.md` · `docs/TODO.md:123` |
| 기준 | `rule-pointer --list` 목록 그대로(1회차와 같음): dev-team architecture · common · backend-api · failure-handling · tests, 레포 `rules/failure-handling.md`(PTF) · `backend-api.md`(PTB) · `tests.md`(PTT), always:dev-team · always:arch |
| 프로필 | consumer |
| 리뷰어 | code-reviewer(구현자와 독립, 읽기 전용) |
| 날짜 | 2026-11-21 |
| 범위 밖으로 연 파일 | `KisOrderRestClient.kt`·`KisTokenManager.kt`(주문 경로에서 분류기로 들어오는 예외 출처 확인), `PositionExitTriggerOrchestrator.kt:130-184`(C-01 의 자동 재접수 경로 확인), `RestTemplateConfig.kt`·`build.gradle`(HTTP 구현체 = Boot 3.5 기본 JDK HttpClient, 연결 타임아웃 예외 종류 확인), `Order.kt:152`(assignExternalOrderId) |

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 표준 · 유지보수성 · 보안 · 회귀 시험 |
| 수정 지킴 확인 | 확인 불가(미커밋) — `guards-fix` 범위가 커밋 기준이라 빔. 읽어서 본 것: 새 시험(`KisOrderAdapterTest` 결과 미확인 3건·HTTP 오류 6행·전송 6행, 잔고 HTTP 500 3건)은 1회차 코드에서 빨개질 단언을 가짐(읽기 판단, 측정 아님) |
| 작성자 시험 증거 | 보고: `./gradlew test --rerun --offline` exit 0, 1172건 실패·오류·건너뜀 0, 린트 exit 0. 전체 재실행 안 함 |
| 발견 | 3개 (막음 1 · 후속 후보 2) |
| 머지 판정 | `review-verdict.js <이 파일> --previous step11-code-review.md` 첫 줄: 머지 판정: 불가 — 막는 지적 1건 · 지난 지적 미해결 0건 · 후속 후보 2건 (재리뷰 · 2회차) |
| 한 줄 | C-01·C-02·C-08 은 고쳐짐. 이번 수정이 하나를 새로 막음 — R1-02 KIS 게이트웨이 «서버 에러» 본문이 붙은 5xx 가 400 주문 거부로 나가 재주문을 부름(C-02 본문 분류가 만든 꼴, 고침은 응답 계약이라 사람 결정). 후속: R1-01 주문 요청 전 토큰 발급 실패도 결과 미확인, R1-03 기존 자동 청산 재시도 |

## 1-1. consumer 프로필

- 1. 규칙 준수 — 대조 출처: rule-pointer --list. 대조: PTF3(불확정 결과는 재시도 전 확정), FH1, AE4·AE8·AE20(응답 계약 변경은 D28·§10.4 에 사람 결정으로 기록됨), CS14·AE21(새 catch: `kisErrorBody` 의 `JacksonException` 은 «본문 없음» 으로 내려 상태 규칙으로 분류 — 조용한 대체값 아님), C12(새 주석 명사형 한 줄), TT25·TT26(바뀐 단언에 `devkit:test-change` 표지 있음). 어긴 것: R1-02·R1-03(PTF3), R1-01(주석과 코드 불일치).
- 2. 사이드 이펙트 — 새 code `KIS_ORDER_RESULT_UNKNOWN`(503) 은 `ServiceUnavailableException` 하위라 `handleServiceUnavailable` → `kisServiceUnavailableMessage` 고정 문구로만 나감(시험 `GlobalExceptionHandlerTest` 177·189행). 호출처 `OrderController.placeOrder`, 자동 청산(`PositionExitTriggerOrchestrator.kt:170` `catch (ex: Exception)`) — 자동 청산은 새 예외를 구별하지 않고 재시도(R1-03). 잔고 경로: HTTP 오류 본문 분류가 새로 붙었고 읽기 타임아웃은 그대로 KIS_TIMEOUT, 본문 없는 HTTP 오류는 그대로 KIS_UNAVAILABLE — 회귀 못 찾음. 토스 경로: `TossTransportSupport` 는 같은 판정을 `TransportTimeouts` 로 옮긴 것뿐(동작 같음). 문서: D28·§10.4 문장이 코드 분기와 일치, TODO 123행 경로가 지금 줄(`BrokerOrderSubmitter.kt:54-66`·`KisOrderAdapter.kt:24-25`)과 일치.
- 3. 버그·장애 위험 — 500 누출: `orderFailure` 의 `else -> e` 로 가는 것은 Redis 오류·`ExternalServiceResponseException`(토큰 응답 null) 등 기존 그대로, 새 누출 없음. KIS 원문 노출: 새 예외 message 고정, 원문은 cause·로그만(`BrokerOrderSubmitter.kt:64` `reason=` 도 고정 문구). 고정 문구: 결과 미확인 문구는 재시도를 권하지 않고 주문 내역 확인을 안내 — 사람 결정 문구와 같음. 오분류: R1-01·R1-02. 연결 타임아웃: JDK HttpClient 는 `HttpConnectTimeoutException` 이라 KIS_UNAVAILABLE, 다른 구현체의 `SocketTimeoutException("Connect timed out")` 은 결과 미확인 — 사람 결정(SocketTimeoutException 전부)대로이고 안전 쪽.

## 2. 발견 (findings)

| ID | file:line | 렌즈 | 근거 조항 | 무엇이 틀렸나 | 등급 | 영향 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| R1-01 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisFailureClassifier.kt:73-74 | 정확성 | label:doc-claim-contradicts-code | 주석 «주문 경로의 IllegalArgumentException 은 rt_cd 0 인데 ORNO 없음» 이 사실과 다름. `placeOrder` 는 주문 요청 전에 `kisHeaders` → `KisTokenManager.getToken`(KisOrderRestClient.kt:140)을 부르고, 토큰 응답의 `access_token` null 은 `requireNotNull`(KisTokenManager.kt:80) IllegalArgumentException, 빈 base URL 은 RestTemplate 의 «URI is not absolute» IllegalArgumentException. 둘 다 이제 KIS_ORDER_RESULT_UNKNOWN. 같은 식으로 토큰 발급 호출의 읽기 타임아웃(ResourceAccessException + SocketTimeout/HttpTimeout)과 본문 없는 5xx 도 `orderTransportFailure`·`orderHttpFailure` 에서 결과 미확인이 됨(:86, :94). 주문 요청은 나가지도 않았는데 «KIS 주문 내역을 확인해 주세요» 로 답하고, 결과 미확인을 대사·알림 신호로 쓰면 토큰 장애 때마다 거짓 경보. 방향은 안전 쪽(재시도 권하지 않음)이라 돈 위험은 없음 | should-fix | 동작 무관 | 이번 수정 | 아니오 | 높음 | 토큰 발급을 분류 밖(주문 요청 앞)으로 떼어 내 결과 미확인 판정이 주문 POST 한 번에만 걸리게 — `KisOrderAdapter` 가 `KisOrderRestClient` 를 안 고치는 제약(D26)이면 최소한 주석을 사실대로 고치고 step-11 §8 에 «토큰 발급 실패도 결과 미확인» 을 가정으로 기록 | `KisOrderAdapterTest` 에 `every { tokenManager.getToken(any()) } throws ResourceAccessException("I/O", SocketTimeoutException("Read timed out"))` → `adapter.submit` 이 `KisOrderResultUnknownException`, `restTemplate.exchange` 0회(이 샌드박스는 Gradle 미실행) |
| R1-02 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisFailureClassifier.kt:82-84 | 정확성 | PTF3 | HTTP 5xx 라도 본문에 rt_cd≠0 만 있으면 `mapKisErrorCode` 로 가는데, 그 함수는 키워드가 없고 rt_cd 가 있으면 무조건 `KIS_ORDER_REJECTED`(KisApiException.kt:26). KIS 게이트웨이 오류 본문(공개 오류표 기준 EGW00001 «일시적 오류가 발생했습니다», EGW00002 «서버 에러가 발생 했습니다», rt_cd 1)이 500 으로 오면 400 KIS_ORDER_REJECTED «KIS가 주문을 받지 않았습니다. 주문 내용과 계좌 상태를 확인해 주세요» 로 답함. 서버 오류는 접수 여부를 확정하지 않으므로 사용자가 «거부됨» 을 믿고 다시 주문하면 이중 실주문(C-01 과 같은 부류). 1회차 코드에선 503 KIS_UNAVAILABLE 이었음. 시험의 5xx + 본문 행은 EGW00201·EGW00123(키워드로 잡히는 것)뿐이라 이 꼴이 빔. KIS 실제 상태·본문은 네트워크 차단으로 확인 불가 | should-fix | 버그·장애 | 이번 수정 | 아니오 | 중간 | 5xx 에서 본문으로 확정하는 것은 한도·인증처럼 «처리 안 함» 이 확실한 msg_cd 만으로 좁히고, 그 밖 5xx 본문은 결과 미확인 — 응답 계약이라 사람 결정 뒤(D28 문장 갱신) | `KisOrderAdapterTest.httpErrorFailures` 에 `Arguments.of(500, kisBody("1", "EGW00002", "서버 에러가 발생 했습니다."), KisOrderResultUnknownException::class.java)` → 지금 코드는 `KisOrderRejectedException` 이라 실패 |
| R1-03 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt:170 | 정확성 | PTF3 | 자동 청산 재시도가 `catch (ex: Exception)` 로 새 `KisOrderResultUnknownException` 도 같은 `orderGroupId` 로 최대 3회 다시 접수. 접수 실패 시 롤백(DEC-08)으로 주문 행이 없어 `findByAccountIdAndOrderGroupId` 확인도 비어, KIS 가 첫 요청을 받았으면 매도 실주문이 최대 3건. C-01 고침은 사람에게 보이는 문구만 바꿨고 사람 없이 도는 재시도는 그대로. 코드는 1회차 전부터 있던 것(이 diff 밖)이고 `docs/TODO.md` P1 `kis-order-cancel-timeout` 항목은 «우리 주문만 롤백» 만 적고 이 자동 재시도는 적지 않음 | should-fix | 버그·장애 | 기존 | 아니오 | 높음 | 결과 미확인 예외는 재시도에서 빼고 바로 실패 알림(사람 확인)으로 — `kis-order-cancel-timeout` 항목에 이 경로를 더해 같은 작업에서 | — |

## 2-1. 재리뷰 — 지난 지적 판정

| 지난 ID | 판정 | 근거 |
|---|---|---|
| C-01 | 고침 | `KisFailureClassifier.kt:89-95` 연결 거부·DNS·`HttpConnectTimeoutException` 만 KIS_UNAVAILABLE, 그 밖 `ResourceAccessException`(읽기 타임아웃 포함) → `KisOrderResultUnknownException`. `:86` 본문 없는 5xx → 결과 미확인. `:74` ORNO 없음(requireNotNull) → 결과 미확인. `:100` rt_cd 없음·본문 null → 결과 미확인. `:71` 그 밖 RestClientException → 결과 미확인. 응답 `GlobalExceptionHandler.kt:124` 재시도 안 권하는 고정 문구, 예외 message 고정(`KisOrderResultUnknownException.kt:10`). 시험 `KisOrderAdapterTest.kt:175-237`·`:261-293`, `GlobalExceptionHandlerTest.kt:177`·`:189`. 남은 꼴: R1-02(본문 있는 5xx), R1-03(자동 재시도, 기존) |
| C-02 | 고침 | `KisFailureClassifier.kt:41-42`(잔고)·`:80-85`(주문) `HttpStatusCodeException` 본문에 rt_cd≠0 이면 본문 분류, `:122-131` 빈 본문·HTML·깨진 JSON 은 null → 잔고 KIS_UNAVAILABLE(`:44`), 주문 5xx 결과 미확인·4xx KIS_UNAVAILABLE(`:86`). 시험 `KisAccountBalanceAdapterTest.kt:278-298`, `KisOrderAdapterTest.kt:286-293` |
| C-08 | 고침 | `docs/TODO.md:123` 이 `BrokerOrderSubmitter.kt:54-66`(runCatching 접수 블록)·`KisOrderAdapter.kt:24-25`(runCatching 분류) 를 가리키고 지금 줄과 일치 |

- C-03·C-04·C-05·C-06·C-07·C-09·C-10: 지시대로 판정 대상 아님(원장 몫). 상태만 확인 — 모두 코드 그대로(`BrokerOrderSubmitter.kt:51-52`·`:63-65`·`:73`, `GlobalExceptionHandler.kt:128-131`, `KisFailureClassifier.kt:53` rt_cd 1 인증 판정 그대로).
- 판정 계산: `review-verdict.js <이 파일> --previous step11-code-review.md`.

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f7374657031312d636f64652d7265766965772e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2237373330366339623538343932656164613263313832346434656537323566623237303239346437227d2c2232223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2237373330366339623538343932656164613263313832346434656537323566623237303239346437227d7d7d -->
