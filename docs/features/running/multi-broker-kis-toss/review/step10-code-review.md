# 코드 리뷰 결과 — multi-broker-kis-toss step 10 (브로커 선택과 KIS 잔고 이전) 1회차

범위: `변경분`(사람 선택 기록 없음 → 기본값)

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | worktree `.worktrees/multi-broker-kis-toss-phase2`, HEAD `7fb0a4e` 위 미커밋 변경 — backend/trading-api main 수정 5·신설 2·삭제 1, test 5, ktlint baseline, archunit_store 1줄 교체 (tech-plan-approval.md·state.md·step-10.md 제외) |
| 기준 | `rule-pointer --list` 프로필 consumer. dev-team `common.md`(AE21·CS*) · `failure-handling.md` · 레포 `rules/backend-api.md`. 스펙: step-10.md, development-plan §10.2 D22~D25, §10.4, 가정 2~4 |
| 프로필 | consumer |
| 리뷰어 | 메인 세션 인라인(구현은 maker 서브에이전트 — 구현자와 다른 컨텍스트). 리뷰어 에이전트는 맥 부하 67~69(기준 36)로 착수 차단, 사람 지시 «걍 너가해»(2026-11-16)로 메인이 직접 리뷰 |
| 날짜 | 2026-11-16 |

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 표준 · 유지보수성 · 보안 · 의도(step-10 §1 을 의도로 봄) |
| 수정 지킴 확인 | 확인 불가(미커밋 대상) |
| 뮤테이션 | 확인 불가(뮤테이션) — 부하로 실행하지 않음 |
| 발견 | 3개 (막음 0 · 후속 후보 3) |
| 머지 판정 | `review-verdict` 로 계산 |
| 한 줄 | D25 선택 검사(어댑터로 보내는 모드 집합 = 표 키 집합, 모드마다 구현 1개), D22 모드 전수 `when`, D23 옛 경로 성공 계약(asOf UTC 변환, 금액 숫자 그대로), D24 broker-balance KIS 지원은 스펙대로입니다. 남는 것은 표 중복 생성과 계좌번호 형식 판정 중복입니다 |
| 기타 | 시험은 작성자 증거(집중 141건·전체 1116건 실패 0, 린트 exit 0)와 메인 재실행(집중 11 xml 144건 실패 0, 특성화 시험 파일 변경 0)을 씀 |

## 1-1. consumer 프로필 검토 절

- 1. 규칙 준수
  - 지킴: 모드 분기 전수 `when`(else 없음 — `BrokerAccountQueryService.routeOf`, `KisAccountBalanceAdapter.fetchBalance`, `KisAccountQueryService.getBalance`) · AE21(KRW 행·금액 없음은 0 대체 대신 `InternalErrorException`) · application 에 `@Qualifier` 없음(ArchUnit 5ffdb0db) · `!!` 제거 · 바뀐 시험 5파일 `devkit:test-change` 근거 줄 · Red 기록(step-10 §5).
  - archunit_store `bb05cc60…` 1줄 교체: 메인이 diff 로 확인. 같은 `KisAccountQueryService` 생성자의 같은 `PositionRepository` 매개변수 위반이고 서명 글자만 바뀜(새 의존 아님).
- 2. 사이드 이펙트 — `BrokerAccountBalancePort` 구현 둘(토스·KIS)이 `List` 주입. `TradingApiApplicationTests` 가 모드 표로 단언하도록 바뀌어 기동 검사(RISK-17)의 실행 증거가 됨. 옛 포트·스냅샷 타입 사용처 0. trading-web 호출부 0건(maker 재검색).
- 3. 버그·장애 위험 — 돈 경로(Map → 문자열 → BigDecimal, 0 채움, 수익률)는 diff 에 없음. 응답에 계좌번호 없음(`BrokerAccountRef.toString` 마스킹 유지, 새 예외 메시지에 계좌번호 없음). 토큰 로그 변경 없음. 외부 호출 분류는 step 9 후속(S9-01·02·04·05) 그대로.
- 의도 렌즈 — step-10 §1 이 말한 변경과 diff 가 맞음. 범위 밖 시험 2파일(`TradingApiApplicationTests`, `TossAccountBalanceAdapterTest`)은 포트 변경에 따른 단언 갱신이라 동작 변경 아님.

## 2. 발견 (findings)

| ID | file:line | 렌즈 | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|---|
| S10-01 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/KisAccountQueryService.kt:32 | 유지보수성 | label:duplicated-logic-diverges | `KisAccountQueryService` 가 `BrokerBalancePortTable` 을 따로 한 번 더 만들고, 기동 검사(D25)는 `BrokerAccountQueryService.init` 에만 있음. 지금은 같은 빈 목록이라 그쪽 검사가 KIS 두 모드를 보장하지만, 누가 `BrokerAccountQueryService` 쪽 라우팅만 바꾸면 옛 경로는 호출 때 `portFor` 의 `checkNotNull` → `IllegalStateException` → 핸들러 묶음 매핑(FLAG-11)으로 400 이 됨 | should-fix | 이 변경 | 아니오 | 중간 | 표를 한 곳(빈 하나 또는 `BrokerAccountQueryService` 가 노출하는 KIS 조회 함수)에서만 만들고 옛 경로도 그것을 씀 | — |
| S10-02 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/KisAccountNumber.kt:8 | 유지보수성 | label:duplicated-logic-diverges | KIS 계좌번호 `XXXXXXXX-XX` 형식 판정이 서비스(`linkedKisAccountNumber`)와 어댑터(`parseExternalAccountId`)에 따로 있음. 한쪽만 바뀌면 서비스는 통과, 어댑터는 409 처럼 판정이 갈림(maker 도 §8 에 기록) | should-fix | 이 변경 | 아니오 | 높음 | 형식 판정을 한 곳에 두고 어댑터는 이미 검사된 값만 나눔 | — |
| S10-03 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/account/KisAccountQueryService.kt:102 | 유지보수성 | label:duplicated-logic-diverges | `Asia/Seoul` 시간대 상수가 KIS 어댑터와 옛 경로 서비스에 따로 있음(토스 어댑터는 `Clock.systemDefaultZone()`). KST 고정 원칙의 근거 자리가 여럿 | nit | 이 변경 | 아니오 | 중간 | KST 상수를 공용 한 곳에 둠 | — |

<!-- devkit:standard-list hex:7b22636861696e223a22646f63732f66656174757265732f72756e6e696e672f6d756c74692d62726f6b65722d6b69732d746f73732f7265766965772f7374657031302d636f64652d7265766965772e6d64222c22726f756e6473223a7b2231223a7b226361757365223a22505220eab8b0eca48020ebb88ceb9e9cecb998eba5bc20eca095ed9598eca78020ebaabbed95a8287472756e6b20ed8c90eca09520ebb688eab08029222c2268656164223a2237666230613465656635333831333163336136613533356437393539373839623362346137323939227d7d7d -->
