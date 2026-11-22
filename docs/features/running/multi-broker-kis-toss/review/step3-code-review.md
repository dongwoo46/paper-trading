# 코드 리뷰 결과 — multi-broker-kis-toss 3단계 «건드린 파일 에러 응답 정렬» (1회차)

- 범위: `변경분` — 미커밋 변경(기준 HEAD a43538a) `backend/` 본 코드 7개 파일 + 시험 7개 파일 + ktlint 기준선
- 읽기 전용. 코드는 고치지 않았습니다.

## 0. 메타데이터

| 항목 | 값 |
|---|---|
| 리뷰 대상 | worktree `.worktrees/multi-broker-kis-toss` 미커밋 diff(`git diff HEAD -- backend/`) + 새 파일 `InternalErrorException.kt`·`NotImplementedException.kt`·`GlobalExceptionHandlerTest.kt` |
| 기준 | `rule-pointer --list <바뀐 본 코드 7개>` 결과 그대로: `rules/dev-team/common.md`(AE18·AE21·CS*·C12·DD*) · `rules/dev-team/failure-handling.md`(FH1~FH6) · 레포 `rules/backend-api.md`(PTB1~PTB9, partial) · 레포 `rules/data-access.md`(PTD1~PTD5, partial) · 레포 `rules/failure-handling.md`(PTF1~PTF6, partial) · `always:dev-team` |
| 프로필 | consumer(`--list`의 `profile:consumer` 행) |
| 리뷰어 | code-reviewer(구현자와 독립) |
| 날짜 | 2026-11-15 |

## 1. 요약

| 항목 | 값 |
|---|---|
| 리뷰한 렌즈 | 정확성 · 표준 · 유지보수성 · 보안 · 성능 · 회귀 시험. 근본성은 버그 수정 diff가 아니라 해당 없음, 의도는 미커밋이라 커밋 의도 블록이 없어 해당 없음 |
| 수정 지킴 확인 | 확인 불가(미커밋 변경이라 `--head` 커밋이 없고, gradle 캐시 쓰기가 탐침 샌드박스 밖이라 실행 불가). 대신 QA 수동 돌연변이(step3-qa §2-2: M2·M5·M6·M7 잡음, M1·M4 못 잡음)를 읽음 |
| 뮤테이션 | `mutation-run.js --base HEAD` exit 4, 목록 없음 → 대상 없음으로 끝남(미커밋 Kotlin 변경을 대상으로 못 잡았을 수 있음) |
| 작성자 시험 증거 | 8개 클래스 154건 실패 0(`build/test-results/test/*.xml`, 2026-11-14T20:51Z) 읽음. 전체 스위트 재실행 안 함 |
| 발견 | 4개 (막음 0 · 후속 후보 4) |
| 머지 판정 | 가능 — 막는 지적 0건 · 후속 후보 4건 (`review-verdict.js <초안>` 첫 줄, 기록 없이 실행. 저장 뒤 오케스트레이터가 다시 계산) |
| 한 줄 | 금융 상태 순서·롤백 경계·호출부 모두 그대로이고 범주 선택도 맞음. 막는 지적 없음. 체결 처리기의 주석 하나가 새 함수에 잘못 붙어 롤백 의존 여부를 반대로 말함(S3C-01) |

참고: `skill-session.js open code-review`는 탐침 샌드박스에서 `~/.devkit` 쓰기가 막혀(EPERM) 열지 못했습니다.

## 1-1. consumer 프로필 검토 절

### 1. 규칙 준수

- 대조한 조항: AE18 · AE21 · C12 · CS13 · CS14 · CS21 · CS24 · CS25 · FH1 · FH6 · PTB2 · PTB4 · PTB5 · PTB7 · PTB9 · PTD1 · PTD3 · PTF6 · always:dev-team(주석 표지·시험 먼저). 레포 `backend-api.md` 머리말(partial 본문)이 가리키는 devkit AE5·AE8도 열어 대조.
- 걸린 것: C12(S3C-01, S3C-02), AE21(S3C-03, 기존).
- 지킨 것: AE5(범주→HTTP 매핑은 `GlobalExceptionHandler` 한 곳) · AE8(K 처리기 39줄이 원문 대신 고정 문구, 원문은 36줄 error 로그와 스택) · PTB4(새 비교 전부 `<`·`<=` = compareTo) · PTB7(새 `InternalErrorException`·`NotImplementedException`은 import 없음) · PTB2(`isActive` private set 유지) · FH1(주문 생성의 idempotencyKey 경로 변화 없음) · PTF6(예수금 홀드가 요청 트랜잭션 안 그대로).
- 범주표(devkit backend-api AE1·AE2)는 이번 `--list`에 없어 지적 근거로 쓰지 않고 참고 대조만 함: 28개 경로 모두 맞음. 다른 계좌 주문 취소의 E-2(404)가 F 상태 검사(409)보다 먼저라 AE2 순서(2단계 → 7단계)와 맞음. `QUOTE_STALE` 409는 기존 `QUOTE_UNAVAILABLE`(ConflictException, 409)과 같은 축. K 경로는 호출부를 읽어 보니 내부 계산값만 닿음(`lockDeposit`은 OrderCommandService:149의 `lockedAmount > 0` 가드 뒤에서만 불림). 단 K-2가 정상 사용으로 닿는 경로는 S3C-04.
- ktlint 기준선: (파일, 규칙)별 개수 762 = 762. 바뀐 본 코드 5개 파일의 항목 줄 번호 이동이 diff의 줄 증감과 정확히 맞아(예: ExecutionProcessor 250 → 254, Account 363 → 398) 새 위반을 옛 항목으로 덮은 곳 없음.

### 2. 사이드 이펙트

- 바뀐 함수 호출부: `Account`의 잠금·해제·확정·매도 대금 메서드는 OrderCommandService(151·286)·ExecutionProcessor(150·165·254)만 부름. `Account.activate()`와 `recordDepositWithLedger`는 본 코드 호출부 없음.
- 옛 `IllegalStateException`·`IllegalArgumentException`을 따로 잡는 호출부 없음. `ApiDomainException`을 잡는 두 곳(SnapshotJobService:88, TaxSummaryCommandService:101)은 이번 예외가 닿지 않는 경로라, `NotImplementedException`이 `ApiDomainException` 밖으로 나가도 영향 없음. PositionExitTriggerOrchestrator:170(`Exception`)·LocalMatchingEngine:28·LocalMatchingScheduler:32·QuoteEventListener:34(`runCatching`)는 전체를 잡아 동작 같음.
- 다른 서비스: backend 아래 Python 서비스에 trading-api 주문·계좌 API 호출 없음(grep). 프런트 호출부는 작성자 점검(step-3 §5-3)을 읽음. OpenAPI 계약 파일 없음.

### 3. 버그·장애 위험

- 금융 상태 순서: 모든 throw가 옛 check/require와 같은 자리. Account 각 메서드는 필드 변경 전, `placeOrder` 비활성 검사는 잠금 전, 매도 수량 검사는 `lockQuantity` 전, 취소 검사는 `order.cancel()` 전, `ensureSellApplied`는 옛 `check(updatedRows == 1)`와 같은 줄(ExecutionProcessor:157). 변경 전후 순서 차이 없음.
- 롤백 경계: 새 예외 전부 `RuntimeException` 상속(`ApiDomainException`·`InternalErrorException`·`NotImplementedException`). Spring 기본 롤백 규칙 그대로 적용되고 `rollbackFor`·`noRollbackFor`·`TransactionTemplate` 없음(grep). 매수 예수금 부족처럼 주문 저장 뒤에 던지는 경로도 옛 IAE와 같이 롤백됨.
- ExecutionProcessor «호출은 processFill에 두고 검사만 분리»: 동작은 옛 코드와 같음. 다만 옛 주석이 새 함수에 붙어 롤백 의존 여부를 반대로 말함(S3C-01).
- 500 응답: K 처리기는 원문을 숨기고 error 로그와 스택을 남김. catch-all 500(83-87)은 로그 없음(S3C-03, 기존). 체결 경로의 K 예외는 HTTP 밖에서 warn 한 줄로만 남음(S3C-04).
- 명세 리뷰가 넘긴 AE21(83-87) 출처 판정: `기존`. 같은 코드가 b7d984b(2026-04-09)부터 있었고 이 diff는 그 줄을 안 건드림. 이 변경으로 새로 그 처리기로 가는 예외도 없음(새 I·K는 25·34줄 전용 처리기, A·E·F 하위는 `handleApiDomain`). 오히려 옛 400·무로그 경로 일부가 K 처리기로 옮겨 로그 없는 경로는 줄었음. 이번 회차를 막지 않는 후속 후보.

## 2. 발견 (findings)

| ID | file:line | 렌즈 | 근거 조항 | 무엇이 틀렸나 | 등급 | 출처 | 회귀 | confidence | 구조 고침 | 재현 |
|---|---|---|---|---|---|---|---|---|---|---|
| S3C-01 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:234 | 유지보수성 | C12 | 옛 주석 «체결 저장·포지션·원장 변경 이전 진입부 거부 (롤백 비의존)»은 `rejectIfBrokerFillNotSupported`의 표지였는데, 새 `ensureSellApplied`가 주석과 그 함수 사이에 끼어 이제 `ensureSellApplied` 위에 붙음. `ensureSellApplied`는 `order.applyExecution`(123)·`executionRepository.save`(125)·조건부 UPDATE(156) 뒤에 돌아 전적으로 롤백에 기대는 검사라 주석이 반대를 말하고, 진입부 거부 함수는 표지를 잃음. 이 주석을 믿고 호출부에서 예외를 잡아 계속 가면 체결 행과 주문 상태 변경이 남는 경로가 생김 | should-fix | 이 변경 | 아니오 | 높음 | 234줄 주석을 `rejectIfBrokerFillNotSupported` 바로 위로 되돌리고 `ensureSellApplied`에는 롤백 의존 표지 | — |
| S3C-02 | backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/account/Account.kt:273 | 유지보수성 | C12 | Account:273·385 «위반 메시지(예외)를 고른 뒤 한 곳에서 던짐»은 코드를 말로 다시 씀. 왜 나눴는지(detekt 던지기 개수 규칙)는 빠짐. GlobalExceptionHandler.kt:24 «…여기서만 한다»는 문장 종결 | nit | 이 변경 | 아니오 | 높음 | — | — |
| S3C-03 | backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/exception/GlobalExceptionHandler.kt:83 | 정확성 | AE21 | catch-all `handleException`이 기록 없이 500을 돌려줌. 격리 지점의 catch-all은 기록하고 가둬야 함. 읽은 근거로 닿는 예: `cancelOrder`의 `findByIdWithOptimisticLock`(OrderCommandService:262)이 체결과 겹칠 때의 낙관적 락 충돌, 본문 JSON 파싱 실패 같은 Spring 자체 예외가 흔적 없이 500. 출처는 §1-1 3 판정대로 기존 | should-fix | 기존 | 아니오 | 중간 | `log.error(ex)` 한 줄 추가 | — |
| S3C-04 | backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt:148 | 정확성 | label:doc-claim-contradicts-code | step-3 §5-3은 K-2 `LOCKED_DEPOSIT_INSUFFICIENT`를 «정상 사용에서는 닿지 않음»으로 적었지만 닿음. LOCAL 시장가 매수는 주문 때 시세로 잠그고(OrderCommandService:117) 다음 틱 시세로 체결한 cost를 `confirmBuy`에 넘김(148-150). 시세가 오르고 다른 잠금이 없으면 `confirmBuy`가 K-2를 던짐. 다른 주문 잠금이 있으면 그 잠금을 조용히 끌어 써, 뒤에 그 주문을 취소할 때 `unlockDeposit`(OrderCommandService:286)이 HTTP 500 K-2를 내고 취소가 계속 실패함. 체결 경로 예외는 LocalMatchingEngine:28-29가 warn 한 줄(스택 없음)로만 남기고 틱마다 다시 시도해, K 범주의 «error 로그·스택»도 이 주 경로에서 안 지켜짐. 잠금 부족 자체는 이 diff 전부터 있던 결함 | should-fix | 기존 | 아니오 | 높음 | — | — |

## 3. 넘길 곳

- S3C-01·S3C-02: Maker(이 단계 안에서 고칠 수 있는 주석 위치·문구).
- S3C-03·S3C-04: 후속 후보로 원장에 기록. S3C-04의 잠금 부족(시장가 매수 체결가 상승)은 금융 동작 변경이라 사람 결정 뒤 별도 단계.
