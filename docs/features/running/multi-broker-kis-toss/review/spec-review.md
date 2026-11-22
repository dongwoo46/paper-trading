# 정합성 리뷰 결과 — multi-broker-kis-toss 2단계 (모드 추가와 주문 안전장치)

구현을 명세·계획에 대고 본 읽기 전용 리뷰입니다(spec-reviewer). 코드 품질·스타일은 보지 않았습니다(code-review 몫). 근거는 실제로 연 파일에만 둡니다.

---

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | `.worktrees/multi-broker-kis-toss` 미커밋 변경(기준 `e2904c8`) — `backend/trading-api` main 수정 7·신설 1(`BrokerApiExceptions.kt`), test 수정 4·신설 1(`RedisMarketQuoteAdapterTest.kt`) |
| 기준 문서 | `steps/step-2.md` §1·§3·§4, `development-plan.md` §2 D4·D5·D6·D15와 모드 영향 표, §3, §5-2, §6 2단계 행, §8 DEC-01·RISK-04, `feature-spec.md` §6 RULE-01~04, §8 EDGE-08 |
| 규칙 묶음 | `rule-pointer --list`: `dev-team/common.md`, `dev-team/failure-handling.md`, 상시 규칙. broken·repo 행 0. 계약 조항은 묶음에 없어, 계획이 인용한 `dev-team/backend-api.md` AE1·AE6·AE20을 직접 열어 대조 |
| 리뷰어 | spec-reviewer (Claude Opus 5.5, 구현자와 독립) |
| 날짜 | 2026-11-15 |

---

## 1. 요약

| 항목 | 값 |
|---|---|
| 커버리지 신호 (매치율) | 2단계 명세 항목 15개 중 13개 충족 (참고용, 합격 게이트 아님) |
| 정합성 영향 간격 | 4개 (막음 0 · 후속 후보 4) |
| 머지 판정 | 가능 — 막는 지적 0건 · 후속 후보 4건 |
| 한 줄 | 선행 거부·전수 `when`·체결 분기·시세 키·계좌 검증·누락 표는 명세대로입니다. 남은 것은 DEC-01이 부른 trading-web 점검(AE20)과 RISK-04 사람 확인, 기록 2건입니다. |

---

## 2. 발견 (findings)

| ID | file:line | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | 확신도 | 근거 조항 |
|---|---|---|---|---|---|---|---|
| R-01 | `OrderCommandService.kt:60`, `:177` · `frontend/trading-web/src/entities/order/model/types.ts:6` · `frontend/trading-web/src/entities/order/api/orderApi.ts:9` | DEC-01은 «주문 API 응답이 바뀌므로 AE20 대상(trading-web 호출부 점검)»으로 정했고, 이번 diff가 UPBIT_LIVE 주문 응답을 201에서 501 `BROKER_ORDER_NOT_SUPPORTED`로 바꿉니다. step-2.md에는 trading-web 점검 결과도, 3단계로 넘긴다는 기록도 없습니다(AE20·trading-web 언급 0건). trading-web은 주문 모드 타입에 `UPBIT_LIVE`를 갖고 주문 POST를 부릅니다. 커밋 전에 호출부 점검 결과를 step 문서에 남기고 커밋 메시지에 `App-Impact` 줄을 달아야 합니다 | should-fix | 이 변경 | 아니오 | 높음 | AE20 |
| R-02 | `steps/step-2.md:55`, `:126` · `development-plan.md:246` | RISK-04 완화책은 «§5의 2번 착수 때 운영자에게 실제 ddl-auto 값 확인, 덮어쓴 환경이면 멈추고 ASK»이고 step-2 §3의 선행 조건입니다. 확인 없이(§8 «확인 못 함») 구현이 끝났습니다. 기존 DB를 유지하는 환경이면 enum 컬럼 CHECK 제약이 `TOSS_LIVE` 저장을 막을 수 있어, 단계 완료 선언 전에 사람 확인이 필요합니다 | should-fix | 이 변경 | 아니오 | 중간 | label:precondition-check-missing |
| R-03 | `AccountTest.kt:146`, `:149` · `steps/step-2.md:98` | step-2 §6 «완료 기준 1» 행은 계좌 표까지 4개 표 모두 «`TradingMode.entries`와 표 키 집합이 같은지 단언»한다고 적었지만, 계좌 표는 키 집합을 `AccountType.entries`와 비교하고 `TradingMode.entries`는 순회만 합니다. 새 모드를 표에 안 넣으면 «거부 기대»로 통과합니다. 계좌 검증이 모르는 모드를 거부하는 쪽이라 RULE-03 위반은 아니고, 증거 문구만 사실과 다릅니다 | nit | 이 변경 | 아니오 | 높음 | label:doc-claim-contradicts-code |
| R-04 | `state.md:54-56` · `steps/step-2.md:122` | 범주 J·K를 미룬 계획 이탈이 step §8에만 있고 state.md §3 변경 이력·§4 막힘에는 없습니다(step 템플릿: 변경은 state.md 변경 이력에). J·K를 처음 쓰는 단계의 계획 블록에서 빠질 수 있습니다 | nit | 이 변경 | 아니오 | 중간 | label:stale-doc-after-change |

---

## 2-2. 알려진 계획 이탈 판정 — 범주 J·K 미추가

판정: 명세 정합 결함이 아닙니다.

1. feature-spec RULE-01~04·EDGE-08과 step-2 §4 완료 기준은 J·K를 요구하지 않습니다. step-2 §1 범위도 «브로커 예외»만 적었고, 이번 단계에 필요한 범주 I(501)는 들어왔습니다(`CommonApiExceptions.kt:23-28`).
2. 이번 diff에 J(503)·K(500)를 던지는 곳이 없습니다. D15의 J·K 첫 사용처는 토큰 기반(D11 `TOSS_TOKEN_UNAVAILABLE` J, D17 `TOSS_INTEGRATION_DISABLED` K-1, 로드맵 4단계)이고, 3단계(DEC-02 정렬)에서 기존 불변식 검사를 K-3로 옮기면 그쪽이 먼저입니다. 시점만 바뀌고 최종 산출물은 같습니다.
3. 남는 일은 기록입니다: 첫 사용 단계의 계획 블록에 J·K 추가를 적고 이탈을 state.md §3에 남기는 것(R-04). 계획 변경 승인 여부는 사람 몫이며 step §8에 «사람 확인 필요»로 이미 올라가 있습니다.

---

## 2-3. 대조 결과 — 지켜진 항목

| 명세 항목 | 코드 근거 | 시험 근거 |
|---|---|---|
| D1·모드 영향 표: `TOSS_LIVE`를 끝에 추가 | `TradingMode.kt:8` | 누락 표 4종 |
| 모드 영향 표: `AccountSource.TOSS` | `AccountSource.kt:6` | 없음(6단계에서 사용) |
| RULE-02·모드 영향 표: STOCK+TOSS_LIVE 허용, CRYPTO 거부 유지 | `Account.kt:363-367` | `AccountTest` 허용·거부 2종과 조합 표 |
| D4 선행 거부 위치: 계좌 로드 직후, 매도 수량 잠금(L62)·주문 저장(L64)·매수 잠금(L67)·구독(L68)·시세 조회(L103, L159)보다 앞 | `OrderCommandService.kt:59-60`, `:174-179` | `OrderCommandServiceTest` 거부 6종: 주문 저장·원장·포지션 잠금 조회·구독·시세·실행기 0회 |
| D4 `submitExternalOrderIfNeeded`의 `else` 제거, 모든 모드 명시(TOSS_LIVE는 방어 예외) | `OrderCommandService.kt:182-189` | 주문 모드 누락 표 |
| RULE-03·RULE-04 누락 시 실패, 누락 잡는 시험 | 세 `when` 모두 `else` 없음. Kotlin 2.1.20(`build.gradle:2`)은 enum 대상 비전수 `when` 문을 컴파일 오류로 처리 | 주문·체결·시세 키 표가 키 집합을 `TradingMode.entries`와 단언 |
| D5 범주 I 501, code `BROKER_ORDER_NOT_SUPPORTED`, KDoc 첫 줄 기호 I-1 | `BrokerApiExceptions.kt:3-11`, `CommonApiExceptions.kt:23-28`, `GlobalExceptionHandler.kt:13-16`(예외의 status 그대로 응답) | 통합 5종 status 501·code 단언 |
| D6·DEC-01 체결 분기 `else` 제거: LOCAL 입금, KIS 동기화만, TOSS_LIVE·UPBIT_LIVE 예외·입금 없음 | `ExecutionProcessor.kt:162-173`. 예외는 입금·매도 원장(L175)·정산(L180) 전. `processFill`은 트랜잭션 선언된 `fillLocal`(L56)·`fillExternal`(L71)에서만 불려 앞선 체결 저장·포지션 갱신도 롤백 | `ExecutionProcessorTest` 2종과 체결 누락 표 |
| D6 시세 키 `quote:toss:live:{ticker}` | `RedisMarketQuoteAdapter.kt:25` | `RedisMarketQuoteAdapterTest` 2종 |
| DEC-01 UPBIT_LIVE 주문 501 | `OrderCommandService.kt:177` | 단위 6종(두 모드), 통합 UPBIT_LIVE 지정가 |
| EDGE-08 주문·자금 잠금 미생성 | D4 행과 같음 | 통합 4종(TOSS_LIVE 지정가·시장가·IOC·매도): 주문·원장 수, 예수금·잠금, 포지션 잠금 수량 불변, 구독 mock 미호출 |
| D4·모드 영향 표: 자동 청산 두 경로가 같은 거부를 탐 | `OrderCommandService.kt:206`, `:233`이 `placeOrder` 호출 | 자동 청산 거부 2종 |
| D4 근거: 주문 생성 지점 하나 | main 전체에서 `Order.create`·`recordBuyLock`·`lockQuantity` 호출은 `OrderCommandService.kt:94`, `:119`, `:145`뿐 | 없음 |
| step-2 완료 기준 3: 기준선 대비 새 실패 0 | `build/test-results/test` XML 58개 스위트, 실패·에러 0이 아닌 스위트 0건, 대상 5개 스위트 합 84개(재실행 없이 XML만 확인) | 없음 |

검토했으나 결함 아닌 것:

- 비활성 계좌·지정가 가격 누락이 거부보다 먼저 400: D4가 «L58 다음»을 지정했으므로 계획대로입니다.
- 멱등 키 재요청(L52-53)이 거부보다 먼저 기존 주문 반환: 새 주문·잠금을 만들지 않아 EDGE-08과 무관합니다.
- 체결의 매수 경로(`ExecutionProcessor.kt:140-149`)에 모드 분기 없음: D6은 매도 `else`만 대상이고 매수 체결은 원래 모드 분기가 없습니다. 명세에 없는 요구라 지적하지 않습니다.
- DEC-02(기존 `check`·`require` 범주 정렬)와 `Account.validateTradingMode`의 400 유지: 로드맵 3단계 항목입니다.

---

## 3. 범위 밖 (안 본 것)

- 코드 품질·이름·주석·메시지 문구(code-review 몫)
- 3~7단계 항목(토큰·전송 계층·잔고·시세 REST)
- trading-web 호출부의 실제 에러 분기 내용(R-01은 호출부 존재까지만 확인)
- 전체 테스트 재실행(작성자 XML 증거만 확인)
- `steps/step-2.md:24` Status 행이 템플릿 문구 그대로(`todo / in progress / done / blocked`)인 문서 형식 문제
