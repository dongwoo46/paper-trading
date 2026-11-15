# 코드 리뷰 결과 — multi-broker-kis-toss step 8 (KIS 경계 특성화 시험)

> 범위: 변경분(사람 선택 없음, 기본값). 커밋 범위 `f42063f..91f03b1`(커밋 91f03b1 하나). 읽기 전용 리뷰.

---

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | `.worktrees/multi-broker-kis-toss-phase2` 커밋 91f03b1 — characterization/kis 시험 4파일, steps/step-8.md, state.md 한 줄 |
| 기준 | `rule-pointer --list` 결과 그대로: rules/dev-team/tests.md(TT, 가이드 tests-how.md) · rules/dev-team/workflow.md(WF, docs 파일에 지목됨) · 레포 rules/tests.md(PTT1) · always:dev-team(C12 주석, 시험 먼저, KST) |
| 프로필 | consumer (`profile:consumer` 행) |
| 리뷰어 | code-reviewer (구현자와 다른 컨텍스트) |
| 날짜 | 2026-11-16 |

---

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 표준 · 유지보수성 · 의도(state.md 한 줄 — 커밋 `변경:` 블록과 맞음) |
| 수정 지킴 확인 | 건너뜀(시험·문서만 바뀐 diff — 되돌릴 비시험 소스 없음, 의뢰대로 실행 안 함) |
| 발견 | 10개 (막음 1 · 후속 후보 9) |
| 머지 판정 | `node tools/review-verdict.js <이 파일>` 첫 줄 그대로(아래 보고 참조) |
| 한 줄 | (a)(b)는 밖으로 보이는 결과에 잘 묶였으나, (c) 체결 통지 시험이 D27(step 13)이 이름을 바꾸는 `KisExecutionNoticeService`·`KisExecutionNotice` 를 직접 불러 step 13에서 고치지 않고는 컴파일되지 않음 |

- 뮤테이션 렌즈: 해당 없음(바뀐 main 소스 0).
- 재사용 점검: `reuse-inventory --target` 4파일 모두 재생성 의심 0건.
- 시험 실행: 하지 않음(작성자 증거 사용 — 메인 재실행 exit 0, 3+3+4건 실패 0. 탐침 샌드박스는 Docker 소켓 접근 불가라 Testcontainers 시험 실행 확인 불가).
- `skill-session open code-review`: 샌드박스 EPERM 으로 실패(홈 폴더 쓰기 막힘) — 기록만 함.

maker 확인 사항 판정:
- 주문 응답 fixture 키 `ORNO`: «지금 코드 동작 고정»이라는 목적에는 맞음(`KisOrderRestClient.kt:64` 가 `output.ORNO` 를 읽음). 다만 키를 시험 대상 코드에서 베껴 TT48에 걸림 → C-03(시험을 고치지 않고도 ODNO 결함 수정에 견디는 방법 포함).
- `asOf` 자리표시자: 적절. `asOf` 값만 치환하고 모양·UTC 오프셋·1분 창은 따로 단언(`KisAccountBalanceBoundaryCharacterizationTest.kt:220-231`). null·누락·숫자로 바뀌면 치환이 안 돼 글자 비교가 깨지므로 빈틈 없음. D22→D23 에서 KST `LocalDateTime` 을 UTC 로 잘못 되돌리면(9시간 어긋남·+09:00) 이 단언이 잡음.
- `@Suppress("LongParameterList")`: 사유 주석 있고 baseline 안 늘림. 지적 없음.

---

## 1-1. 프로필별 검토 절

### consumer 프로필

- 1. 규칙 준수 — 대조한 조항 출처: rule-pointer --list(모델이 따로 고른 규칙 없음). 대조한 조항: TT7(대역은 `KisTokenManager`·`CollectorSubscriptionPort`·KIS HTTP(MockRestServiceServer)뿐 — 지킴) · TT9(a·b 단언은 HTTP 본문·KIS 요청·DB 행에 묶임 — 지킴, Slack 은 C-04) · TT44(C-02) · TT48(C-03, C-09) · TT16(체결 시각은 원문 고정이고 파서가 `Asia/Seoul` 명시 `KisExecutionNoticeParser.kt:95-98` — 지킴, 환경 변수 의존은 C-05) · TT17(C-06) · TT53(C-10) · TT20·TT21(감사 전용 조항, 특성화 시험의 묶음 단언·문장형 이름은 지적 안 함) · TT24~TT26(step-8 §4 고정 규칙이 TT24 방향과 맞음) · TT33·TT49(해당 없음 — 소스 변경 0) · PTT1(Testcontainers PostgreSQL·Redis — 지킴) · WF9(C-08) · C12(C-07) · always 시간 KST(asOf UTC 오프셋 단언은 지금 동작 고정이고 step-8 §8에 AE20 필요로 기록됨 — 지적 안 함). 금액은 모두 문자열 `BigDecimal` + `isEqualByComparingTo` 또는 JSON 글자 비교로 단언, 부동소수 없음.
- 2. 사이드 이펙트 — main 소스 변경 0(`git diff --stat f42063f..91f03b1`: 시험 4 + 문서 2). 시험이 공유 `RestTemplate` 빈의 requestFactory 를 바꾸고 `@AfterEach` 에서 되돌림. `KisBoundaryEventCapture` 는 `@TestConfiguration` 으로만 등록돼 다른 시험 컨텍스트 스캔에 안 들어감. 리팩터링 내성: (a)(b)는 D22~D26 이 지우거나 바꾸는 타입(`KisAccountBalancePort`·`KisBalanceSnapshot`·실행기 둘)을 import 하지 않고, 묶인 `KisTokenManager`·`KisOrderRestClient` 는 계획서 §10.3 «건드리지 않음»(DEC-07, D26) — 컴파일 유지 예상. (c)는 C-01. PR #53(`fix/trading-api-money-state` 93cbd18) 소스 diff 는 금액 열 scale 확대와 계산 정리라 `isEqualByComparingTo` 단언은 값이 같으면 통과 예상(돌려 보진 않음).
- 3. 버그·장애 위험 — 시험만 추가라 운영 경로 위험 없음. 시험 신뢰성 위험은 C-05(환경 변수에 따라 Slack 전송이 같은 mock 서버로 감), C-06(DB 정리 없음).

---

## 2. 발견 (findings)

| ID | file:line | 렌즈 | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|---|
| C-01 | backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisExecutionNoticeBoundaryCharacterizationTest.kt:3 | 유지보수성 | label:test-only-pure-layer-not-wiring | (c) 시험이 입구를 `KisExecutionNoticeService`(L3·L62·L110·L177·L218)와 `KisExecutionNotice` 반환 타입(L19·L342)으로 직접 잡음. 계획서 §10.3(development-plan.md:378-379)과 D27 이 step 13 에서 이 서비스를 `BrokerExecutionNoticeService` 로 이름을 바꾸고 `KisExecutionNotice` 를 `BrokerExecutionNotice` 로 바꾸므로, step 13 에서 이 파일을 고치지 않으면 컴파일이 안 됨 → D21·step-8 §4(L66) «step 9~14 에서 고치지 않는다»가 구조상 지켜질 수 없음. 또 실제 배선(`KisExecutionWebSocketManager.handleRawMessage` 의 파싱→처리, D27 이 바꾸는 곳)을 건너뛰어, step 13 에서 그 배선의 변환이 틀려도 이 시험은 못 잡음 | blocker | 이 변경 | 아니오 | 높음 | (c) 입구를 웹소켓 전송 경계로 옮김: `KisExecutionWebSocketTransport` 대역이 받은 요청의 `onMessage(원문)` 을 부르고(`KisExecutionWebSocketManager.connectMode` 공개, `getWebSocketApprovalKey` 는 토큰 관리자 mock 으로 고정), 시험 파일에서 `KisExecutionNoticeService`·`KisExecutionNotice`·파서 이름을 없앰 | `grep -n -e KisExecutionNoticeService -e "KisExecutionNotice$" -e "): KisExecutionNotice" backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisExecutionNoticeBoundaryCharacterizationTest.kt && sed -n 378,379p docs/features/running/multi-broker-kis-toss/development-plan.md` |
| C-02 | backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisAccountBalanceBoundaryCharacterizationTest.kt:140 | 표준 | TT44 | `verify(kisTokenManager).getToken(..)` + `verifyNoMoreInteractions(kisTokenManager)`(L140-141, L169-170)는 프로세스 안 협력자의 호출 횟수 단언. 쓰인 토큰은 이미 `authorization` 헤더 기대값(L115, L150)이 증명하므로 지키는 동작이 없고, D22 어댑터 개편이 토큰을 한 번 더 읽거나 캐시하면 KIS 요청이 같아도 빨개짐(고정 규칙상 고칠 수 없음). 두 줄씩 지우면 됨 | should-fix | 이 변경 | 아니오 | 높음 | — | — |
| C-03 | backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisOrderIntakeBoundaryCharacterizationTest.kt:286 | 정확성 | TT48 | KIS 주문 응답 fixture 의 `output` 키 `ORNO` 를 KIS 응답 스키마가 아니라 시험 대상 코드(`KisOrderRestClient.kt:64`)에서 베낌. 같은 클라이언트도 조회는 `ODNO` 를 씀(`KisOrderRestClient.kt:112,130`) → 실제 응답이 `ODNO` 면 운영에서는 접수가 실패하는데 시험은 초록(TT48 Why 와 같은 모양). 지금 동작 고정과 함께 가려면 fixture `output` 에 KIS 스키마 키(`ODNO`·`KRX_FWDG_ORD_ORGNO`·`ORD_TMD`)와 `ORNO` 를 같은 값으로 함께 둠 → 지금 코드도, TODO 의 ODNO 결함 수정 뒤 코드도 시험을 안 고치고 통과 | should-fix | 이 변경 | 아니오 | 중간 | — | — |
| C-04 | backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisBoundaryEventCapture.kt:15 | 정확성 | TT9 | Slack 문구를 발행 시점 `@EventListener` 로 잡음. 실제 전송은 `SlackNotificationEventHandler.kt:21-22` 의 `AFTER_COMMIT` + `@Async` 를 거쳐 `NotificationSender.send` 로 나감 → D26 `BrokerOrderSubmitter` 가 트랜잭션 밖에서 발행하게 바뀌면 전송 처리기가 안 불려 Slack 이 끊겨도 이 시험은 초록. 경계 값은 `NotificationSender.send(message)` 쪽 | nit | 이 변경 | 아니오 | 중간 | — | — |
| C-05 | backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisOrderIntakeBoundaryCharacterizationTest.kt:130 | 정확성 | TT16 | MockRestServiceServer 를 공유 `RestTemplate` 빈에 묶는데 `SlackWebhookNotifier.kt:20` 도 같은 빈을 씀. 시험이 Slack 설정을 고정하지 않아 실행 셸에 `TRADING_NOTIFICATION_SLACK_ENABLED=true`·`SLACK_WEBHOOK_URL` 이 있으면(application.yaml:78-79) 비동기 Slack 전송이 mock 서버에 «예상 밖 요청»으로 들어가 `kisServer.verify()` 가 타이밍 따라 깨지고, (c)에서는 실제 웹훅으로 나감. `@SpringBootTest(properties)` 에 Slack 끔을 고정하면 됨 | nit | 이 변경 | 아니오 | 중간 | — | — |
| C-06 | backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisExecutionNoticeBoundaryCharacterizationTest.kt:91 | 표준 | TT17 | 세 클래스 모두 시험 사이 DB 정리(rollback·truncate)가 없고 고유 이름·외부 주문번호로만 격리. `seedFeePolicies` 는 «정책이 하나라도 있으면 건너뜀»이라, 다른 경로(시드·다른 정책)가 먼저 행을 넣으면 KIS 정책이 안 심겨 수수료 0 으로 빨개지고 원인이 가려짐. KIS_PAPER·KIS_LIVE 정책 존재로 조건을 좁히거나 클래스 단위 정리 | nit | 이 변경 | 아니오 | 중간 | — | — |
| C-07 | backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisAccountBalanceBoundaryCharacterizationTest.kt:225 | 표준 | C12 | 주석이 문장 종결(`본다`·`호출한다`·`단언한다`·`통과시킨다`·`둔다`·`심는다`·`모은다`) — 같은 꼴이 KisAccountBalance L46-48, KisOrderIntake L51·L54, KisExecutionNotice L50-52·L88, KisBoundaryEventCapture L10. 명사형(`봄`·`막음`)으로 | nit | 이 변경 | 아니오 | 높음 | — | — |
| C-08 | docs/features/running/multi-broker-kis-toss/steps/step-8.md:24 | 표준 | WF9 | 메타데이터 Status 가 템플릿 자리표시자 `todo / in progress / done / blocked` 그대로(state.md 는 in progress, step-7 은 채움) → doc-quality 가 첫 낱말 todo 로 읽음. §7(L107) «커밋 전(메인이 확인 뒤 커밋)»은 91f03b1 커밋 뒤 낡음 | nit | 이 변경 | 아니오 | 높음 | — | — |
| C-09 | backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisExecutionNoticeBoundaryCharacterizationTest.kt:331 | 정확성 | TT48 | 체결 통지 원문이 `ODNO=..^CCLD_NO=..` 키=값 꼴로, 파서(`KisExecutionNoticeParser.kt:57-77`)가 기대하는 모양에서 만든 것. KIS H0STCNI0·H0STCNI9 실시간 통보는 암호화된 위치 기반 `^` 값으로 알고 있어 실제 통지가 이 파서를 지나는지 의심(오프라인이라 KIS 문서 대조 못 함). 시험은 지금 동작 고정이라 그대로 두고, RISK-16 수동 확인 때 함께 대조할 대상 | nit | 기존 | 아니오 | 낮음 | — | — |
| C-10 | backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisOrderIntakeBoundaryCharacterizationTest.kt:328 | 정확성 | TT53 | KIS 주문 접수의 멱등 재요청(같은 `idempotencyKey` 로 두 번 POST → KIS 호출 1번, 원장·잠금 1벌, `OrderCommandService.kt:58-59`)이 고정 안 됨. D26 이 같은 서비스의 생성자와 KIS 분기를 바꾸고 이 파일은 이후 고칠 수 없으므로, 필요하면 별도 시험 파일로 | nit | 범위 밖 | 아니오 | 중간 | — | — |

---

## 3. 범위 밖 (안 본 것)

- PR #53 병합 뒤 이 시험들의 실제 통과 여부(소스 diff 만 읽음, 실행 안 함).
- KIS 공식 문서 대조(주문 응답 `ODNO`, 실시간 체결 통보 형식) — 오프라인.
- 기존 KIS 시험 24건·관련 시험 10클래스의 내용(step-8 §6 표는 maker 기록 그대로 인용).
- 전체 시험·린트 재실행(작성자 증거 사용, 의뢰대로 안 함).

<!-- devkit:reviewed-head 91f03b1d03491a54c2580c89ca9dc4d46b2af929 -->

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f73746570382d636f64652d7265766965772e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2239316630336231643033343931613534633235383063383963613964633464343662326166393239227d7d7d -->
