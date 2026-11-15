# 코드 리뷰 결과 — multi-broker-kis-toss step 3 시험 보강(S3Q-01·02·03)

> 범위: `변경분` — 미커밋 시험 파일 3개의 추가 줄(`git diff -- backend/`, 추가 120줄·삭제 0, HEAD `7111818`). 제품 코드 변경 없음.

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | worktree `.worktrees/multi-broker-kis-toss` 미커밋 변경: `OrderCommandServiceTest.kt` · `OrderControllerIntegrationTest.kt` · `GlobalExceptionHandlerTest.kt` |
| 기준 | `rule-pointer --list <3파일>` 이 낸 목록 그대로: `rules/dev-team/tests.md`(TT1~TT56 중 31개) · 레포 `rules/tests.md`(PTT1) · `always:dev-team`(재사용·주석·시험 먼저·KST) |
| 프로필 | consumer (`profile:consumer` 행 그대로) |
| 리뷰어 | code-reviewer (구현자·tester 와 다른 컨텍스트) |
| 날짜 | 2026-11-15 |

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 표준 · 유지보수성 · 성능 · 의도 (근본성·회귀 시험은 해당 없음 — 버그 수정이 아니고 제품 변경이 없음) |
| 수정 지킴 확인 | 해당 없음 — 시험 아닌 파일 변경 0 (guards-fix 대상 없음) |
| 발견 | 5개 (막음 0 · 후속 후보 5) |
| 머지 판정 | 머지 판정: 가능 — 막는 지적 0건 · 후속 후보 5건 (`review-verdict.js` 첫 줄) |
| 한 줄 | S3Q-01·02 는 요구대로 메워졌고 막으려는 잘못된 구현(M-01·M-02)을 실제로 잡음. S3Q-03 은 501 밖 거부 시험 9개 중 7개를 덮었으나 2개가 남았고, 새 불변 단언은 RED 를 본 적이 없음 |

## 1-1. consumer 프로필 검토

- 1. 규칙 준수 — 대조한 조항 출처: rule-pointer --list (devkit `tests.md` + 레포 `rules/tests.md` + `always:dev-team`, 모델이 따로 고른 규칙 없음). 대조한 조항: TT53 · TT33 · TT7 · TT44 · TT48 · TT9 · TT2 · TT16 · TT17 · TT20 · TT21 · TT24 · TT25 · TT26 · TT31 · PTT1 · always(재사용·주석). 어긴 것: TT9(S3T-01) · TT33(S3T-02) · always 재사용(S3T-03) · TT21(S3T-05, 기존 줄). PTT1 — 통합 시험은 Testcontainers PostgreSQL·Redis(`OrderControllerIntegrationTest.kt:64-70`), 서비스 단위 시험의 리포지토리 목은 조항 예외(유스케이스 단위 시험)라 준수. TT44 — 새 `verify(exactly = 0) { accountRepository.findByIdWithLock(any()) }`(`OrderCommandServiceTest.kt:378`)는 같은 파일 기존 관례(425-431·493 «잠금 전 거부» 계약)와 같은 꼴이고, 엄격 목이라 호출되면 어차피 MockKException 으로 실패해 새 결합을 만들지 않음 → 지적 안 함.
- 2. 사이드 이펙트 — 시험 코드만 바뀜. `expectOrderError` 의 호출부 5곳(`OrderControllerIntegrationTest.kt:299·306·313·321·337`)은 모두 거부 경로(400 2건·409 3건)라 «거부 뒤 주문·원장·자금·포지션 불변» 단언이 붙는 것이 맞음: 루트 CLAUDE.md «Never mutate financial state implicitly» 와 같고, 409 `INSUFFICIENT_AVAILABLE_DEPOSIT` 은 주문 저장(`OrderCommandService.kt:70`) 뒤 잠금(`:73`)에서 실패해 `@Transactional`(`:56`) 롤백에 기대는데, 새 단언이 그 롤백을 처음으로 DB 값으로 못박음. 의미 변화는 «응답만 확인» → «응답 + 흔적 없음» 강화뿐이고 호출부가 기대하는 바와 어긋나는 곳 없음. 다만 같은 일을 하는 501 도우미와 갈라짐(S3T-03). `ThrowingController` 누출 위험: 없음으로 판정. (a) `TradingApiApplication.kt:10` 은 기본 `@SpringBootApplication` 스캔(사용자 정의 `@ComponentScan` 없음) (b) spring-boot-test 3.5.13 `TestTypeExcludeFilter` 바이트코드에서 `getEnclosingClassName` 으로 바깥 클래스를 거슬러 올라가 `@Testable`(JUnit 5 `@Test` 의 메타) 메서드가 있으면 제외함을 확인 → `@SpringBootTest`·`@WebMvcTest` 문맥 모두에서 `GlobalExceptionHandlerTest$ThrowingController` 는 스캔 제외 (c) ArchUnit 은 `ImportOption.DoNotIncludeTests`(`ArchitectureRulesTest.kt:28`). 남는 위험은 누군가 이 컨트롤러를 최상위 시험 클래스로 빼낼 때뿐(그때는 `/test/**` 경로가 앱 문맥에 등록됨).
- 3. 버그·장애 위험 — 시험 결정성: `LocalMatchingScheduler`(30초 주기)는 GTD 만료 주문만 취소하고 새 시험 주문은 전부 DAY, 발자국은 계좌별이라 다른 시험 행이 섞이지 않음(TT16·TT17 위반 없음). `open-in-view: false`·시험 클래스 비트랜잭션이라 `footprint()` 는 매번 새 조회. 조회 중복은 S3T-04.
- 요구별 판정
  - S3Q-01 — 충족. FILLED·CANCELLED 주문을 다른 계좌로 취소하면 404 를 요구해, 소유(`OrderCommandService.kt:266`)·상태(`:267`) 검사 순서를 뒤집은 M-01 에서 새 2건만 RED(작성자 증거 §7). REJECTED 는 `Order` 에 전이 메서드가 없어(`OrderStatus.REJECTED` 를 세우는 곳이 main 에 없음) 빠진 것이 맞음.
  - S3Q-02 — 충족. standalone MockMvc + `setControllerAdvice` 로 실제 `@ExceptionHandler` 해석을 거침. K 는 `$.code`(catch-all 은 `INTERNAL_SERVER_ERROR`, `GlobalExceptionHandler.kt:83-86`), I 는 상태(catch-all 500)로 갈려 M-02 의 두 매핑 제거를 각각 잡음.
  - S3Q-03 — 일부 충족. S3T-01·S3T-02.
- 실행: 작성자 증거(`review/test-evidence.md` «step 3 시험 보강» §7·§9)를 읽음. 시험은 다시 돌리지 않음(샌드박스가 Gradle 캐시 쓰기를 막고 요청도 불필요). `reuse-inventory --target OrderControllerIntegrationTest.kt` 재생성 의심 0건(파일 안 의미 중복은 도구가 못 봄 → S3T-03). 뮤테이션 렌즈: 바뀐 소스 파일 0개라 해당 없음. `skill-session open` 은 샌드박스 EPERM 으로 못 엶(리뷰 내용과 무관).

## 2. 발견 (findings)

| ID | file:line | 렌즈 | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|---|
| S3T-01 | OrderControllerIntegrationTest.kt:279 | 의도 | TT9 | S3Q-03 은 «거부 경로 통합 시험»의 불변 단언을 요구했으나 거부 시험 2개(`:150` 409 `QUOTE_UNAVAILABLE`, `:172` 404 `POSITION_NOT_FOUND`)는 여전히 상태·code 만 봄. 시세·포지션 조회 전에 잠금을 거는 쪽으로 순서가 바뀌고 그 잠금이 커밋되면 두 시험은 초록. 작성자는 ktlint 기준선이 줄 번호로 묶여 있어 뺐다고 기록(기준선 `config/ktlint/baseline.xml` 에 이 파일 200~242행 항목이 있어 그 위에 줄을 넣으면 어긋남 — 확인함) | should-fix | 이 변경 | 아니오 | 높음 | 이 파일의 기준선 위반 23건을 `ktlintFormat` 으로 고치고 기준선 항목을 지운 뒤 두 시험을 `expectOrderError` 로 바꿈(설정 변경이라 메인 세션·사람 판단) | — |
| S3T-02 | OrderControllerIntegrationTest.kt:292 | 정확성 | TT33 | S3Q-03 의 새 불변 단언(`expectOrderError`·`:367-369`·`:385-386`)은 RED 를 한 번도 보지 않음(증거 §8 «초록만»). S3Q-01·02 와 달리 막으려는 잘못된 구현을 잡는지 증명이 없음. 예: `placeOrder` 의 `@Transactional`(`OrderCommandService.kt:56`)을 빼면 409 `INSUFFICIENT_AVAILABLE_DEPOSIT` 에서 `:70` 에 저장된 주문 행이 남아 «주문 행 수»에서 RED 여야 하지만 확인되지 않음 | should-fix | 이 변경 | 아니오 | 중간 | 격리 사본에서 위 돌연변이 1개로 `OrderControllerIntegrationTest` 를 돌려 새 단언만 RED 인지 증거에 한 줄 추가 | — |
| S3T-03 | OrderControllerIntegrationTest.kt:458 | 유지보수성 | label:duplicated-logic-diverges | `expectOrderError`+`assertFootprintUnchanged` 가 기존 `assertOrderRejectedWithoutChange`(`:475-495`)와 같은 «POST → 상태·code → 발자국 비교» 흐름을 새로 만들었고, 이 diff 에서 이미 갈라짐: 새 쪽은 `positionQuantity` 를 `compareTo` 로 보고, 501 쪽은 `positionQuantity` 를 안 보며 `positionLockedQuantity` 를 소수 자릿수까지 같아야 하는 `assertEquals`(`:493`)로 봄. 발자국에 넣은 포지션 수량을 `TOSS_LIVE 매도` 시험(`:519`)은 검사하지 않음. 두 이름도 같은 일을 다르게 부름 | nit | 이 변경 | 아니오 | 높음 | `assertOrderRejectedWithoutChange` 에 `assertFootprintUnchanged(before, after)` 한 줄 추가(단언 삭제 없음 → TT26 게이트 무관) 또는 두 도우미 통합 | — |
| S3T-04 | OrderControllerIntegrationTest.kt:445 | 성능 | label:redundant-work-per-item | `footprint()` 가 `:445` 에서 포지션을 읽어 두고 `:451-452` 에서 같은 행을 다시 조회해 `lockedQuantity` 를 얻음. 발자국 1회에 같은 SELECT 2번, 이 diff 로 호출이 늘어(거부 시험마다 2~4회) 중복도 늘어남 | nit | 이 변경 | 아니오 | 높음 | `positionLockedQuantity = position?.lockedQuantity` | — |
| S3T-05 | OrderControllerIntegrationTest.kt:150 | 표준 | TT21 | 시험 이름 `LOCAL 시장가 매수 주문 — stale 시세면 400`(`:150`)·`LOCAL 미보유 종목 매도 시 400`(`:172`)이 400 이라 하나 `7111818` 이 단언을 409 `QUOTE_UNAVAILABLE`·404 `POSITION_NOT_FOUND` 로 바꿈(`:166-167`·`:188-189`). 빨개지면 이름이 틀린 정책을 가리킴. 이 diff 가 건드리지 않은 줄 | nit | 기존 | 아니오 | 높음 | S3T-01 고칠 때 함께 개명 | — |

## 3. 범위 밖 (안 본 것)

- 커밋 `7111818` 의 제품 코드와 다른 시험 파일 — step 3 합동 리뷰(`review/step3-panel.md`) 몫.
- 시험 재실행 — 작성자 증거로 대신 읽음(37·17·7건 실패 0, 린트 exit 0). 독립 실행은 안 함.
- 새 주석 문구(`:457`·`GlobalExceptionHandlerTest.kt:93` 등 코드를 다시 말하는 주석)는 step 3 리뷰 S3C-02 와 같은 부류라 따로 적지 않음.

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f73746570332d74657374732d636f64652d7265766965772e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2237313131383138383062316539353461313136643438303862353431623335653865656437623066227d7d7d -->
