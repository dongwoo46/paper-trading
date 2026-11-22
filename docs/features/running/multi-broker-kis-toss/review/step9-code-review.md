# 코드 리뷰 결과 — multi-broker-kis-toss step 9 (KIS 에러 정렬) 1회차

범위: `변경분`(사람 선택 기록 없음 → 기본값)

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | worktree `.worktrees/multi-broker-kis-toss-phase2`, HEAD `93fd840` 위 미커밋 변경 — backend/trading-api main 3 · 새 예외 6 · test 3 · baseline 2 · archunit_store 1 (tech-plan-approval.md·state.md 제외) |
| 기준 | `rule-pointer --list <변경 15파일>` 그대로 — dev-team `failure-handling.md`(FH1~FH6) · `common.md`(AE18·AE21·CS1~CS25·C12·DD1~DD6) · `tests.md`(TT*) · 레포 `rules/backend-api.md`(PTB1~PTB9, partial) · `rules/failure-handling.md`(PTF1~PTF6, partial) · `rules/tests.md`(PTT1) · `always:dev-team`. 스펙: step-9.md, development-plan §10.2 D28·§10.4 AE20 목록·D15 |
| 프로필 | consumer (`profile:consumer` 행 그대로) |
| 리뷰어 | code-reviewer (구현자와 다른 컨텍스트) |
| 날짜 | 2026-11-16 |

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 표준 · 유지보수성 · 보안 · 성능 · 회귀 시험 · 의도(step-9 §1 을 의도로 봄) |
| 수정 지킴 확인 | 확인 불가(미커밋 대상 — guards-fix 는 커밋 범위 기준, 메인 지정) |
| 뮤테이션 | 확인 불가(뮤테이션) — 어댑터가 TT49 대상(경계 코드, `ensureSuccess` 분기 3개 이상)이나 사본에서 `mutation-run.js --base HEAD` exit 4·목록 없음. 미커밋 변경이 범위 계산에서 빠진 것으로 보여 통과로 치지 않음 |
| 재사용 | `reuse-inventory --target` 4파일(어댑터·KisTimeout·KisUnavailable·KisAccountNotLinked) 재생성 의심 0건. 의미 중복 1건은 S9-01 |
| 발견 | 9개 (막음 0 · 후속 후보 9) |
| 머지 판정 | 머지 판정: 가능 — 막는 지적 0건 · 후속 후보 9건 (`review-verdict` 첫 줄, 초안 기준 — 저장 뒤 다시 계산) |
| 한 줄 | 범주(J 503·F-4 409)·고정 문구·옛 예외 삭제·응답 본문 원문 차단은 스펙대로입니다. 남는 것은 분류 정확도(타임아웃 과대 분류, 한도 분기가 rt_cd 1 에 가려짐)와 code 문자열 중복입니다 |
| 기타 | `skill-session.js open code-review` 는 샌드박스가 `~/.devkit` 쓰기를 막아 실패(EPERM). 시험은 돌리지 않고 작성자 증거(집중 72건·전체 1095건 실패 0, 린트 exit 0)를 씀 |

## 1-1. consumer 프로필 검토 절

- 1. 규칙 준수 — 대조 조항 출처는 rule-pointer --list 뿐(모델이 따로 고른 규칙 없음).
  - 어김: CS24(S9-03) · CS22(S9-05, 기존) · CS14(S9-07, 범위 밖) · C12(S9-08).
  - 지킴: AE21·CS14·CS21(새 catch 는 모두 다시 던짐, 좁은 타입) · PTB7(새 예외 6파일 Spring import 0, `ServiceUnavailableException` 상속. `KisAccountNotLinkedException` 의 `ConflictException` 상속은 토스 FLAG-12 와 같은 승인 선례) · PTB8 · TT1·TT25·TT26(바뀐 시험 3파일 모두 `devkit:test-change` 근거 줄) · TT33(Red 기록 step-9 §6) · TT7(목은 프로세스 밖 RestTemplate 만) · CS25 · CS8.
  - 해당 없음: FH1~FH6(diffChecks 4행 — 돈·주문 POST·웹훅·소비자·건별 루프 없음, 잔고 조회만) · PTF1·PTF2·PTF4~PTF6(원장·요율·구간·차감 없음) · PTF3(읽기 조회라 확정할 의도 없음) · PTT1(DB 어댑터 쿼리 시험 아님) · PTB1~PTB6·PTB9·PTB4(도메인 모델·금액 계산 변경 없음).
  - DD1(API 계약 변경은 사람 리뷰): 401·403·502·504 → 503·409 계약 변경은 DEC-05 A 사람 결정에 근거함.
  - always:dev-team 의 시간 줄: 어댑터 80행 `OffsetDateTime.now(ZoneOffset.UTC)` 는 KST 원칙과 어긋나나 안 바뀐 성공 응답 줄(D23 불변)이라 기록만 함.
- 2. 사이드 이펙트 — `fetchBalance` 호출부는 `KisAccountQueryService.getBalance` 하나, 그 호출부는 `KisAccountBalanceController` 하나. `ApiDomainException` 을 잡는 곳(SnapshotJobService·TaxSummaryCommandService)은 KIS 잔고 경로와 무관. 옛 클래스·옛 code(`KIS_UNAUTHORIZED` 등) 잔존 검색: src·config·trading-web·quant-worker 0건(`proxy.config.ts:25` 접두사만). archunit_store 4줄 삭제와 ktlint baseline 줄 이동(CommonApiExceptions 71~125행이 기존 클래스 상위 호출 줄과 일치, 순감 4)·detekt 3줄 삭제는 새 위반 흡수 없음. 참고: 옛 예외는 `handleApiDomain`(로그 없음)으로 갔고 새 예외는 `handleServiceUnavailable` 의 `log.warn(ex)` 를 거쳐 KIS 실패가 처음으로 로그에 남음(D28 «원문은 로그만» 대로). 응답 본문 원문 차단은 컨트롤러 시험이 단언함.
- 3. 버그·장애 위험 — 외부 호출 분류: S9-01·S9-02·S9-04·S9-05. 실패를 값으로 바꿔 넘김: S9-07(범위 밖). 트랜잭션·동시성·부하 변화 없음.
- 의도 렌즈 — step-9 §1 이 말한 변경(J 503·F-4 409·고정 문구·옛 예외와 기반 4개 삭제·archunit 해결 줄 제거)이 diff 와 맞음. §1 범위에 없는 `GlobalExceptionHandlerTest` 와 baseline 2파일은 시험·설정이라 동작 변경 아님(§5·§7 에 기록됨).

## 2. 발견 (findings)

| ID | file:line | 렌즈 | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|---|
| S9-01 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:64 | 정확성 | label:duplicated-logic-diverges | `ResourceAccessException` 을 원인 구분 없이 전부 `KisTimeoutException` 으로 바꿈. RestTemplate 은 연결 거부(`ConnectException`)·DNS 실패(`UnknownHostException`)·TLS 오류도 이 타입으로 던지므로, KIS 가 꺼져 있을 때 응답은 503 `KIS_TIMEOUT` «KIS 응답이 지연되고 있습니다»가 됨. D28 은 그 밖의 원격 실패를 `KIS_UNAVAILABLE` 로 두고, `KisTimeoutException` KDoc 도 «연결·읽기 타임아웃»이라 적음. 토스 쪽 `TossTransportSupport.transportFailure`(21~28행)는 원인 체인에서 `SocketTimeoutException`·`HttpTimeoutException` 일 때만 타임아웃으로 봄 → 같은 판정이 두 곳에서 다르게 남. 시험(어댑터 시험 156행)은 원인 없는 `ResourceAccessException("Read timed out")` 하나라 이 차이를 못 잡음 | should-fix | 이 변경 | 아니오 | 높음 | 원인 체인 타임아웃 판정(`causeChain`·`isTimeout`)을 토스·KIS 공용 전송 보조로 빼서 양쪽이 같은 판정을 쓰게 함. 연결 거부 원인 시험 1건 추가 | — |
| S9-02 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:99 | 정확성 | label:trigger-classifier-misfire | 새 `KIS_RATE_LIMITED` 분기는 `else` 안에만 있고, 그 앞 94행 `rtCd == "1"` 이 문구와 무관하게 인증 실패로 먼저 보냄. KIS 가 실패를 rt_cd `"1"` 로 주면(KIS 문서상 0 이외는 실패이고 공개 예시 대부분이 `"1"` — 네트워크 차단이라 이 세션에서 원문 확인 불가) «초당 거래건수를 초과하였습니다» 같은 한도 응답이 503 `KIS_AUTH_FAILED` «KIS 키 설정을 확인해 주세요»가 되어 운영자가 멀쩡한 키를 의심함. 한도 시험(어댑터 시험)은 실재 확인이 안 된 rt_cd `"9"` 로만 통과함. `mapKisErrorCode` 의 문구 순서(범위 밖 기록)와는 별개로, 어댑터 안의 분기 순서 문제임 | should-fix | 이 변경 | 아니오 | 중간 | 한도 문구 판정을 rt_cd `"1"` 판정보다 앞에 두거나, rt_cd 대신 KIS `msg_cd`(EGW00201 등)로 가름. rt_cd `"1"`+한도 문구 시험 추가 | — |
| S9-03 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt:118 | 표준 | CS24 | KIS code 5개가 예외 클래스(`KisAuthFailedException.kt:9` 등)와 핸들러 `when` 비교 문자열에 따로 적힌 문자열 리터럴임. 한쪽 오타는 컴파일을 통과하고 조용히 «외부 서비스를 일시적으로 사용할 수 없습니다»로 떨어짐. 지금은 컨트롤러 시험이 5개를 끝까지 단언해 잡지만, 규칙은 code 값을 상수·enum 으로 두라고 함(토스 code 도 같은 모양 — 기존) | should-fix | 이 변경 | 아니오 | 높음 | code 를 한 곳의 상수(예: 예외 companion `const val CODE`)로 두고 예외와 핸들러가 같이 참조 | — |
| S9-04 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:66 | 정확성 | label:trigger-classifier-misfire | `RestTemplateConfig` 가 기본 오류 처리기라, KIS 가 HTTP 4xx·5xx 로 답한 실패는 `HttpClientErrorException`·`HttpServerErrorException` 으로 이 catch 에 와서 본문의 rt_cd·msg1 을 보지 않고 전부 `KIS_UNAVAILABLE` 이 됨. 인증·권한·한도 분류(`ensureSuccess`)는 HTTP 200 본문에서만 돌아감. 토스는 `RestClientResponseException` 의 상태로 가름(`TossTokenIssuerImpl.errorFor`). 옛 코드도 같은 구조(502) | should-fix | 기존 | 아니오 | 중간 | `RestClientResponseException` 을 따로 잡아 상태·본문으로 `ensureSuccess` 와 같은 분류를 태움 | — |
| S9-05 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:61 | 정확성 | CS22 | `try` 본문이 `kisHeaders()` → `tokenManager.getToken()`(111행)까지 감쌈. 토큰 발급 실패도 잔고 호출 실패로 분류됨: 잘못된 앱 키로 토큰 엔드포인트가 401·403 → `KIS_UNAVAILABLE` «잠시 후 다시 시도», 토큰 엔드포인트 연결 실패 → `KIS_TIMEOUT`. `requireNotNull(response.accessToken)`(KisTokenManager 80행)은 `IllegalArgumentException` 이라 이 catch 를 지나 400 `BAD_REQUEST` 로 감. try 본문은 이 diff 가 안 바꿈 | should-fix | 기존 | 아니오 | 높음 | 헤더(토큰)를 `try` 밖에서 만들고 `try` 는 `exchange` 한 줄만 감쌈. 토큰 실패 분류는 KisTokenManager 정렬 때(D28 범위 밖) | — |
| S9-06 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:38 | 정확성 | label:missing-input-validation | KIS 잔고 경로가 계좌의 `tradingMode` 를 보지 않아, LOCAL·TOSS_LIVE 계좌 id 로 불러도 409 `KIS_ACCOUNT_NOT_LINKED` «externalAccountId 를 XXXXXXXX-XX 형식으로 등록해 주세요»가 나감. TOSS 계좌(accountSeq 숫자)에서 이 안내를 따르면 토스 연결이 깨짐(`TossAccountNotLinkedException`). 브로커 경로는 `NotBrokerAccountException` 으로 가름. 모드 검사 부재는 옛 코드부터 있었음 | should-fix | 기존 | 아니오 | 중간 | 서비스나 어댑터에서 KIS 모드가 아닌 계좌를 먼저 거부(브로커 경로와 같은 예외) | — |
| S9-07 | backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisAccountBalanceAdapter.kt:133 | 정확성 | CS14 | rt_cd `"0"` 성공 응답에서 `output2` 가 없거나 숫자가 깨지면 `decimal()` 이 `BigDecimal.ZERO` 로 채워 예수금·평가액 0 원을 정상 값으로 냄. 토스 쪽은 같은 경우 `TossResponseInvalidException` 으로 실패시킴(J-2). 성공 경로 불변(RULE-10·D23)이라 이 step 범위 밖 | should-fix | 범위 밖 | 아니오 | 높음 | 필수 필드 누락·변환 실패를 KIS 응답 해석 실패 예외로 바꿈(성공 응답 계약 변경이라 사람 결정 필요) | — |
| S9-08 | backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/KisAccountBalanceControllerIntegrationTest.kt:117 | 표준 | C12 | 주석이 문장 종결(«나오면 안 된다»)로 끝남. 규칙은 명사형 표지 한 줄 | nit | 이 변경 | 아니오 | 높음 | — | — |
| S9-09 | backend/trading-api/src/main/kotlin/com/papertrading/api/common/exception/KisAuthFailedException.kt:5 | 유지보수성 | label:doc-claim-contradicts-code | KDoc «KIS 원문과 계좌번호는 cause 에만 두고»(J 예외 5파일 같은 줄)가 코드와 다름 — 어댑터는 계좌번호를 어떤 cause 에도 넣지 않음(cause 는 msg1 만 담는 `KisApiException` 이거나, 쿼리를 뺀 URL 만 담는 `ResourceAccessException`). 뒤에 읽는 사람이 «로그의 cause 에 계좌번호가 있다»고 오해할 수 있음 | nit | 이 변경 | 아니오 | 중간 | «KIS 원문은 cause 에만, 계좌번호는 어디에도 싣지 않음» | — |

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f73746570392d636f64652d7265766965772e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2239336664383430373063663862353465346336333533626130303563616430663539343961643461227d7d7d -->
