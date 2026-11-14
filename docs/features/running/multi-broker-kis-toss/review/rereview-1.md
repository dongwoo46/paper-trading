통과 뒤 회차: 사람 결정 2026-11-15 state.md §4(수정 9건 재리뷰를 다음 세션 첫 일로 정함)

# 재리뷰 1 — multi-broker-kis-toss 2단계 (2회차)

- 범위: 변경분(사람 선택 기록 없음 → 기본값). 수정이 원 변경과 같은 커밋 `52db67b`에 섞여 있어 `git diff e2904c8 52db67b -- backend/` 전체를 읽고, 판정은 1회차 9건과 그 수정이 깨뜨린 것만 했습니다.
- 앞 회차: `review/panel.md`(P-01~P-11, 막는 지적 0건, 병합 가능). 막는 행이 없어 `지난 ID` 표는 쓰지 않았습니다. P-06(기존)·P-07(범위 밖)은 넘긴 항목이라 판정 대상이 아닙니다.
- 프로필: consumer. 기준 규칙 목록(`rule-pointer --list`, 52db67b에서 바뀐 backend 파일 13개): `rules/dev-team/common.md`, `rules/dev-team/failure-handling.md`, `rules/dev-team/tests.md`, `always:dev-team`. 레포 자체 `rules/` 행은 없습니다.
- 읽기 범위: `fix-round-brief check`의 재리뷰 읽기 범위 블록을 받지 못해 바뀐 파일 13개를 범위로 삼았습니다. 범위 밖으로 연 것과 이유: `steps/step-2.md`·`state.md`(P-03·P-04·P-11 수정이 있는 곳), 1회차 `review/code-review.md`·`review/qa.md`(1회차 줄 번호·근거 대조), `fillLocal`·`fillExternal`·`Order.create` 호출 검색(grep만, 진입부 거부가 새 예외 경로를 여는지 확인), devkit `rules/dev-team/common.md` C12 원문.

## 9건 판정

| ID | 판정 | 근거 file:line |
|---|---|---|
| P-01 | 고침 | `ExecutionProcessor.kt:106`·`:236-241` 거부 위치: 체결 중복 확인(:96)·주문 상태 확인(:99)·계좌 잠금(:102) 뒤, 수수료 조회(:110)·`applyExecution`(:122)·Execution 저장(:124)·포지션(:142·:152)·원장(:148·:176)·정산(:181) 앞. 매수·매도 공통. 매도 분기 :171-173은 전수 `when`용 방어 분기(도달 불가). 시험: `ExecutionProcessorTest.kt:212-219`(저장·포지션 조회·저장·원자 차감·원장·정산 0회), :221-237(SELL), :239-254(BUY), 표 :256-307 |
| P-02 | 고침 | `AccountTest.kt:140-146` 표 키가 `TradingMode`, :150 `TradingMode.entries`와 비교, :153-172 모드별 `@ParameterizedTest`(계좌 타입 전부 허용·거부 단언). 기록 정정 `step-2.md:111` |
| P-03 | 고침 | `step-2.md:143` AE20 점검 결과(`fetchPlaceOrder` → `fetchJson`이 상태·code 분기 없이 원문 표시, 깨지는 코드 없음, 다른 호출부 없음), 커밋 52db67b `App-Impact:` 줄. 프런트 파일은 열지 않고 기록만 대조 |
| P-04 | 고침 | `step-2.md:55-56` 선행 조건 RISK-04 «운영 ddl-auto는 기본값 create — 사용자 확인 2026-11-15», :142 |
| P-05 | 고침 | 인용 6곳 명사형: `OrderCommandService.kt:173`·`:181`·`:184`, `ExecutionProcessor.kt:171`·`:235`, `BrokerApiExceptions.kt:3-6`. 같은 꼴이 시험 주석 4곳에 남음 → R2-01(후속) |
| P-08 | 고침 | `OrderCommandServiceTest.kt:338-354` 자동 청산 시험에 `lockedQuantity` 단언 없음(파일 전체 0건). 수량 잠금 부재는 `verifyNoOrderSideEffects`(:266-274)의 포지션 잠금 조회 0회(:269)로 증명 |
| P-09 | 고침 | `OrderCommandServiceTest.kt:384-389` 라우팅별 전수 `when`(`expectedCalls`), :432-433 거부 모드 목록을 표에서 파생. 체결 쪽도 `ExecutionProcessorTest.kt:306-307` 표에서 파생 |
| P-10 | 고침 | `OrderCommandServiceTest.kt:281`~`:396`, `ExecutionProcessorTest.kt:221`·`:239`·`:265` 모두 `@ParameterizedTest(name = "{0}")`, 두 파일에 `for` 없음. 인용 밖 `RedisMarketQuoteAdapterTest.kt:35`는 아직 `for`지만 메시지에 모드가 있어(:38) 판정에 넣지 않음 |
| P-11 | 고침 | `state.md:57` 변경 이력 «예외 범주 J(503)·K(500)는 처음 쓰는 단계로 미룸 · 2단계는 I(501)만 필요». 들어간 커밋은 52db67b가 아니라 직전 `ff2394f` |

## 1. 규칙 준수

- common C12: 인용 6곳은 고쳐졌습니다(P-05). 시험 주석 4곳이 종결어미로 남아 R2-01로 적었습니다.
- failure-handling FH1: `placeOrder` 멱등 키 조회(`OrderCommandService.kt:52`)가 거부(:60)보다 앞입니다. 1회차 기록(`code-review.md` §1·§3의 L52·L60·L62-L68)과 현재 줄 번호가 같아, 수정이 이 순서를 바꾸지 않았습니다.
- failure-handling FH5: 체결 거부(`ExecutionProcessor.kt:106`)가 외부 체결 ID 중복 확인(:96) 뒤라 같은 통지가 두 번 와도 두 번째는 조용히 끝납니다. FH3·FH6은 웹훅·건별 루프 변경이 없어 해당 없음입니다.
- common AE18·AE21: 수정이 새 `catch`를 만들지 않았습니다.
- tests(시험 먼저): P-01 구현 전 Red 6건 기록이 있습니다(`step-2.md:89`·`:112`). 작성자 기록이라 재현은 못 했습니다(아래 시험 절).
- always:dev-team 시간(KST): 수정분에 시간 처리가 없어 해당 없음입니다.

## 2. 부작용

- 진입부 거부로 매수 체결도 막게 됐습니다(1회차엔 매도만). `processFill` 호출부는 `fillLocal`(`LocalMatchingEngine.kt:37`)과 `fillExternal`(`KisExecutionNoticeService.kt:46`) 두 곳뿐이고, 1회차 QA 근거(`qa.md:13`)대로 매칭 엔진은 LOCAL 주문만, KIS 통지는 KIS 모드만 넘깁니다. 주문 생성(`Order.create`)도 `OrderCommandService.kt:119` 한 곳이고 그 앞 :60에서 TOSS_LIVE·UPBIT_LIVE가 거부됩니다. 새로 도달 가능해진 예외 경로는 없습니다.
- API·데이터 계약: 수정분은 응답 형태를 바꾸지 않았습니다(201→501 변경은 1회차 P-03에서 판정).
- 다른 서비스·앱: 수정분은 trading-api 안에서 끝나 해당 없음입니다.

## 3. 버그·장애 위험

- 트랜잭션·금융 상태: 체결 거부(:106) 앞에서 바뀌는 상태는 없습니다(:96 조회, :98 낙관적 잠금 조회, :102 계좌 비관적 잠금 — 예외 시 `fillLocal`·`fillExternal`의 `@Transactional` 롤백으로 잠금 해제). 상태 변경(:122 이후)은 전부 거부 뒤라, 트랜잭션 밖에서 `processFill`을 직접 불러도 부분 반영이 남지 않습니다(1회차 Q1 우려 해소).
- 방어 분기(:171-173)는 진입부 거부 때문에 도달 불가이고 전수 `when` 유지용이라 정확성 영향이 없습니다.
- 아무것도 단언하지 않는 시험: 수정으로 바뀐 시험마다 되돌린 형태에서 실패하는 단언이 있습니다. BUY 거부(`ExecutionProcessorTest.kt:239-254`)는 :106이 없으면 예외가 나지 않습니다. SELL 거부(:221-237)는 :172 방어 분기가 여전히 던지지만 `verifyNoFillSideEffects`의 저장 0회(:213)에서 실패합니다. 주문 매도 거부(`OrderCommandServiceTest.kt:309-320`)는 거부가 잠금 뒤로 가면 스텁 없는 mockk 호출로 다른 예외가 나 `assertThrows`가 실패합니다. 통합 시험의 포지션 잠금 수량 비교(`OrderControllerIntegrationTest.kt:317`)는 매수 경우 null과 null 비교지만, 같은 도우미의 주문 수·원장 수·예수금·구독 단언이 그 경우를 받쳐 지적하지 않았습니다.
- 동시성·부하·외부 호출: 수정분 해당 없음입니다.

## Findings

| ID | file:line | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 |
|---|---|---|---|---|---|---|
| R2-01 | backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt:256 | C12 | 시험 주석 4곳이 종결어미 문장(«…적어야 통과한다»): ExecutionProcessorTest.kt:256, OrderCommandServiceTest.kt:377, AccountTest.kt:139, RedisMarketQuoteAdapterTest.kt:22. P-05는 인용한 본 코드 6곳만 고쳐 같은 꼴이 시험 코드에 남음. 예: «새 TradingMode 추가 시 표 갱신 필수 → 누락 시 키 집합 시험 실패». 1회차 사본이 없어 수정에서 생겼는지 가릴 수 없음 → 출처 이 변경 | nit | 이 변경 | 아니오 |

## 시험·렌즈

- 작성자 증거 대조: `build/test-results/test/*.xml` 58개 파일, 합계 438개, 실패 0·에러 0, 58개 전부 `skipped="0"`(통합 시험 `OrderControllerIntegrationTest` 10개 포함). 결과 시각 2026-11-15 03:52~03:59 KST, 바뀐 backend 파일 최종 수정 03:51:30, backend 미커밋 변경 없음 → 결과가 52db67b 트리와 맞습니다. 지적 증명용 시험이 필요 없어 재실행하지 않았습니다.
- 회귀 시험 렌즈(`guards-fix`): 확인 불가. 수정이 원 변경과 한 커밋이라 «수정만 되돌린» 비교점이 없고, 탐침 샌드박스가 Gradle 캐시 쓰기를 막습니다(1회차와 같음). P-01 Red 6건은 작성자 기록(`step-2.md:112`)으로만 둡니다.
- 뮤테이션 렌즈: `mutation-run --base e2904c8` exit 4, 목록 없음(«뮤테이션 대상 없음») → 범위 안 대상 없음.

## 요약

- 9건 모두 고침. 수정이 깨뜨린 것 없음. 후속 1건(R2-01, nit, 출처 이 변경).
- 병합 판정: `review-verdict … --previous review/panel.md` → «머지 판정: 가능 — 막는 지적 0건 · 지난 지적 미해결 0건 · 후속 후보 1건 (재리뷰 · 2회차)» (exit 0). 기록(`--record`)은 하지 않았습니다(오케스트레이터 몫).

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f70616e656c2e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2265323930346338623436333832623966633630353633353937343766353535333865633366353033227d2c2232223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2236333837373738643331636634366565396632303436323132613438623436323532363865323961227d7d7d -->

<!-- devkit:reviewed-head 6387778d31cf46ee9f2046212a48b4625268e29a -->
