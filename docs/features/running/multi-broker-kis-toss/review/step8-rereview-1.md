# 코드 리뷰 결과 — multi-broker-kis-toss step 8 재리뷰 1 (2회차)

> 재리뷰 모드: 지난 회차 막는 지적(C-01) 판정 + 고침 커밋이 새로 깬 것만 봄. 범위 `91f03b1..338a5ad`(커밋 338a5ad 하나). 읽기 전용.

---

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | `.worktrees/multi-broker-kis-toss-phase2` 커밋 338a5ad — characterization/kis 시험 2파일, step-8.md, TODO.md, review 기록 3파일 |
| 지난 회차 | review/step8-code-review.md (1회차, 막는 지적 C-01) |
| 기준 | `rule-pointer --list`(고침 커밋이 바꾼 파일) 그대로: rules/dev-team/tests.md(TT) · rules/dev-team/workflow.md(WF) · rules/common/outputs.md(O) · 레포 rules/tests.md(PTT1) · always:dev-team |
| 프로필 | consumer (`profile:consumer` 행) |
| 리뷰어 | code-reviewer (구현자와 다른 컨텍스트) |
| 날짜 | 2026-11-16 |

---

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 표준 · 유지보수성 · 의도(커밋 `변경:` 블록과 diff 대조 — 맞음) |
| 수정 지킴 확인 | 건너뜀(시험·문서만 바뀐 diff, 되돌릴 비시험 소스 없음) |
| 발견 | 2개 (막음 0 · 후속 후보 2, 둘 다 nit) |
| 머지 판정 | `review-verdict <이 파일> --previous step8-code-review.md` 첫 줄 그대로 |
| 한 줄 | C-01 고침: 체결 통지 시험이 웹소켓 전송 대역의 `onMessage(원문)` 로 들어가 실제 배선(파서→체결 서비스)을 지나고, D27 이 이름을 바꾸는 세 타입은 시험에서 0건 |

- 시험 실행: 안 함(작성자 증거 사용 — 338a5ad 에서 `./gradlew test --rerun --offline --tests 'com.papertrading.api.characterization.kis.*'` exit 0, 3+3+4건 실패 0. 탐침 샌드박스는 Docker 소켓 접근 불가라 Testcontainers 시험 실행 확인 불가).
- 뮤테이션 렌즈: 해당 없음(바뀐 main 소스 0).
- 읽기 범위 밖에서 연 파일(이유):
  - `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/kis/KisExecutionWebSocketManager.kt` 전체 — 새 입구 `onMessage` 가 실제 `handleRawMessage`→파서→체결 서비스를 지나는지, `connectMode` 공개 여부, 대역이 진짜 전송기를 대신하는지 확인(C-01 판정 근거).
  - `backend/trading-api/src/main/resources/application.yaml` grep `execution-ws-enabled` — 기동 때 `start()` 가 mock·대역에 닿는지 확인(R2-02).
  - `KisOrderRestClient.kt`·`KisExecutionNoticeParser.kt` grep `ORNO|ODNO` — 고침 커밋이 TODO.md 에 새로 적은 줄 번호 대조.
  - `KisExecutionWebSocketManagerTest.kt` grep `Transport` — 새 대역이 기존 대역을 다시 만들었는지(재사용) 확인.
  - characterization/kis 나머지 시험 파일 `import com.papertrading.api` 줄 — step-8.md 에 새로 쓴 대조표 주장 확인(R2-01).
  - development-plan.md 는 허용 줄 378-379 를 보려고 370-385 를 함께 출력함(판정엔 378-379 만 씀).

---

## 1-1. 프로필별 검토 절

### consumer 프로필

- 1. 규칙 준수 — 대조한 조항 출처: rule-pointer --list(모델이 따로 고른 규칙 없음), 고침 커밋 줄만. TT7(새 대역은 웹소켓 전송 `KisExecutionWebSocketTransport`, 새 mock 은 승인키를 KIS 에서 받는 `KisTokenManager` — 둘 다 프로세스 밖 경계, 지킴) · TT9(단언은 DB 행·발행 이벤트 그대로, 고치기 전과 같은 값 — 지킴) · TT44(새 `verify` 없음, 승인키 stub 은 호출 횟수를 단언하지 않음 — 지킴) · TT16(R2-02) · TT24~TT26(고정 규칙과 맞게 이름 바뀌는 타입 의존을 없앰 — 지킴) · PTT1(Testcontainers 유지 — 지킴) · WF(step-8 §검증 표에 고친 뒤 재실행 행 추가, TODO 항목 형식이 이웃 항목과 같음 — 지킴. 대조표 개수 틀림은 R2-01) · always:dev-team C12(새 주석 L92·L353, 대역 L140 은 명사형. 바뀐 KDoc L52 `쓰지 않는다` 는 1회차 C-07 이 이미 L50-52 로 넘긴 같은 줄 — 새 지적 안 함).
- 2. 사이드 이펙트 — main 소스 변경 0. 대역 설정 `KisFakeExecutionWebSocketTransportConfig` 는 `@TestConfiguration` 이라 다른 시험 컨텍스트 스캔에 안 들어가고 `@Import` 한 (c) 만 `@Primary` 로 진짜 `SpringKisExecutionWebSocketTransport` 를 대신함. `KisExecutionWebSocketManagerTest` 는 mockk 전송기를 쓰고 공유 대역이 없어 재발명 아님. `connectMode` 반복 호출은 상태 맵만 바꾸고 재연결 예약이 없음.
- 3. 버그·장애 위험 — 시험만 바뀌어 운영 경로 위험 없음. 고치기 전의 `requireNotNull(notice)` 가 빠져 원문이 안 읽히면 `handleRawMessage`(KisExecutionWebSocketManager.kt:227-229)가 조용히 넘어가지만, 세 시험 모두 바로 다음 단언(주문 FILLED/PARTIAL·체결 1건)에서 빨개지므로 빠져나가는 결함은 없음(진단 문구만 약해짐 — 지적 안 함). 환경 변수에 따른 기동 실패는 R2-02.

---

## 2. 발견 (findings)

| ID | file:line | 렌즈 | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|---|
| R2-01 | docs/features/running/multi-broker-kis-toss/steps/step-8.md:95 | 표준 | label:doc-claim-contradicts-code | C-01 확인 표 «(고친 뒤)» 행이 `열거형 7종` 이라 적었으나 시험 파일이 쓰는 열거형은 9종(`AccountType`·`MarketType`·`NotificationEventType`·`OrderCondition`·`OrderSide`·`OrderStatus`·`OrderType`·`TradingMode`·`TransactionType`, KisExecutionNoticeBoundaryCharacterizationTest.kt:8-16). 표가 «전수 = 각 파일의 import 줄»이라 밝혔으므로 개수가 틀림. 9종 모두 §10.3 이 바꾸는 `AccountSource` 가 아니라 결론(유지)은 그대로 | nit | 이번 수정 | 아니오 | 높음 | — | `grep -c "^import com.papertrading.api.domain.enums" backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisExecutionNoticeBoundaryCharacterizationTest.kt` |
| R2-02 | backend/trading-api/src/test/kotlin/com/papertrading/api/characterization/kis/KisExecutionNoticeBoundaryCharacterizationTest.kt:77 | 정확성 | TT16 | 고침이 `KisTokenManager` 를 `@MockitoBean` 으로 바꾸고 승인키 stub 은 `@BeforeEach`(L93-97)에서만 함. 시험이 `trading.kis.execution-ws-enabled` 를 고정하지 않아(application.yaml:45 `${KIS_EXECUTION_WS_ENABLED:false}`, 시험 자원에 덮어쓰기 없음) 실행 셸에 `KIS_EXECUTION_WS_ENABLED=true` 가 있으면 기동 때 `start()`(KisExecutionWebSocketManager.kt:141-146)가 stub 전 mock 으로 승인키 null 을 받아 `KisExecutionWebSocketConnectionRequest` 생성에서 NPE → 컨텍스트 기동 실패로 3건 모두 오류. 같은 모양이 이미 `KisTokenManager` 를 mock 하는 (a)(b) 에도 있음(1회차 C-05 와 같은 환경 변수 의존 계열). `@SpringBootTest(properties = ["trading.kis.execution-ws-enabled=false"])` 로 세 클래스에 고정하면 됨 | nit | 이번 수정 | 아니오 | 중간 | — | — |

---

## 2-1. 재리뷰 — 지난 지적 판정

| 지난 ID | 판정 | 근거 |
|---|---|---|
| C-01 | 고침 | 1. 이름 의존 제거: KisExecutionNoticeBoundaryCharacterizationTest.kt·KisFakeExecutionWebSocketTransport.kt 의 `import com.papertrading.api` 줄에 `KisExecutionNoticeService`·`KisExecutionNotice`·`KisExecutionNoticeParser` 0건(characterization/kis 전체 grep 0건). 계획서 development-plan.md:378-379 가 바꾸는 것은 이 세 타입과 `KisExecutionWebSocketManager.kt` 의 «입력 타입 교체»뿐이고, 시험이 쓰는 `connectMode(mode)`(KisExecutionWebSocketManager.kt:148)·`KisExecutionWebSocketTransport`·`KisExecutionWebSocketConnectionRequest.onMessage: (String) -> Unit`(L24, L31-33)은 그 변경 대상이 아님. 관리자 생성자는 스프링 주입이라 시험과 무관. 2. 배선 통과: 시험 `deliver`(L354-357)가 `connectMode` → 대역이 받은 요청의 `onMessage(raw)` → 실제 `handleRawMessage`(L162, L227-229) → 파서 → 체결 서비스로 들어감(지목한 구조 고침 그대로). 같은 원문을 두 번 넣는 재수신 시험도 이제 원문 파싱부터 두 번 지남. 3. 단언 값 불변(diff 상 단언 줄 변경 0), 작성자 재실행 exit 0 |

---

## 3. 범위 밖 (안 본 것)

- 1회차 후속 후보 C-02~C-10 (원장으로 넘김, 이번 회차 판정 대상 아님).
- 시험 실제 실행(작성자 증거 사용, Docker 소켓 접근 불가).
- `KisTokenManager` 실제 동작(R2-02 의 고치기 전 비교는 기동 경로까지만 읽음).

<!-- devkit:reviewed-head 338a5addc733170f0daff65ef2de92585d469963 -->

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f73746570382d636f64652d7265766965772e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2239316630336231643033343931613534633235383063383963613964633464343662326166393239227d2c2232223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2233333861356164646337333331373066306461666636356566326465393235383564343639393633227d7d7d -->
