# 코드 리뷰 결과 — multi-broker-kis-toss step 11 (재리뷰 2 = 3회차)

범위: `변경분` — 고침 라운드 3(R1-02·R1-01 주석·R1-03 TODO)이 건드린 곳. 재리뷰 모드: 지난 지적 판정 + 이번 수정이 깬 것만.

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | `KisFailureClassifier.kt`(orderHttpFailure·orderServerErrorFailure·isAuthMessage·orderFailureByCode), `KisOrderAdapterTest.kt` HTTP 오류 표 291~298행, `docs/TODO.md:124`, `development-plan.md` D28 |
| 기준 | step11-rereview-1.md 와 같음 |
| 프로필 | consumer |
| 리뷰어 | 메인 세션 인라인 리뷰(사람 결정 2026-11-21 «메인이 직접», 리뷰어 상한 5회 초과, step 10 선례) |
| 날짜 | 2026-11-21 |

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 회귀 시험 |
| 수정 지킴 확인 | 읽기 판단: 295~297행(500 + EGW00002·주문 거부 문구 → 결과 미확인)은 2회차 코드에서 `KisOrderRejectedException` 이라 빨개질 단언. 작성자 보고 구현 전 `35 tests completed, 3 failed` |
| 작성자 시험 증거 | `./gradlew test --rerun --offline` exit 0, 새 xml 87개, 1176건 실패·오류·건너뜀 0, 특성화 10건 통과·폴더 diff 0, 린트 exit 0 |
| 발견 | 0개 |
| 한 줄 | R1-02 해소. 주문 5xx 는 한도·인증만 본문으로 확정하고 그 밖(본문 없음 포함)은 결과 미확인. 4xx·200 본문 분류와 잔고 경로는 그대로 |

## 1-1. consumer 프로필

- 1. 규칙 준수 — PTF3: 5xx 의 불확정 결과가 확정 거부(400)로 나가는 길이 사라짐. 새 msg_cd 값을 지어내지 않고 기존 문구 판정(«인증»·«token»)만 공용화.
- 2. 사이드 이펙트 — `orderHttpFailure` 의 4xx 분기(본문 없음 → KIS_UNAVAILABLE, 본문 있음 → orderResponseFailure)는 2회차와 같음. 잔고 `balanceFailure` 는 `isAuthMessage` 로 옮긴 같은 문구 판정이라 동작 같음.
- 3. 버그·장애 위험 — 5xx 에 주문 거부 문구가 와도 결과 미확인이라 사용자가 KIS 내역을 한 번 더 확인함(안전 쪽, step-11 §8 기록). 인증 msg_cd 목록은 실호출 대조 몫(§8).

## 2. 발견 (findings)

| ID | file:line | 렌즈 | 근거 조항 | 무엇이 틀렸나 | 등급 | 영향 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 지적 없음 | | | | | | | | | | | |

## 2-1. 재리뷰 — 지난 지적 판정

| 지난 ID | 판정 | 근거 |
|---|---|---|
| R1-02 | 고침 | `KisFailureClassifier.kt` `orderServerErrorFailure`: body null → 결과 미확인, isRateLimited → KIS_RATE_LIMITED, isAuthMessage → KIS_AUTH_FAILED, 그 밖 → KisOrderResultUnknownException. 시험 `KisOrderAdapterTest.kt:291`·`:295-298` |
| R1-01 | 고침(주석) | IllegalArgumentException 줄 주석이 «ORNO 없음 외에 토큰 발급 실패·빈 base URL 도 섞인 보수 분류»로 사실과 일치. 동작 변경 없음, step-11 §8 후속 기록 |
| R1-03 | 수용 | `docs/TODO.md:124` P1 `kis-order-cancel-timeout` 항목에 자동 청산 재접수 위험 기록, 코드는 안 고침(사람 결정) |

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f7374657031312d636f64652d7265766965772e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2237373330366339623538343932656164613263313832346434656537323566623237303239346437227d2c2232223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2237373330366339623538343932656164613263313832346434656537323566623237303239346437227d2c2233223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2237373330366339623538343932656164613263313832346434656537323566623237303239346437227d7d7d -->
