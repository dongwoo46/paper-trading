---
lens: reliability/failure-handling
dept: reliability
agent: reliability-auditor
title: 실패 처리·멱등 (Kotlin·Spring·KIS 연동)
repo-checks: I7
target-signal: catch\s*\(|runCatching|getOrNull\(\)|getOrDefault|\.onFailure|throw |@Scheduled|onMessage\(|RestTemplate|RestClient|WebClient|WebSocket|exchange\(|postForEntity|postForObject|getForObject|getForEntity|@Retryable|[Rr]etry|@PostMapping|@ExceptionHandler|@RestControllerAdvice|Ledger|Settlement|Reconcil|reconcil|UNKNOWN|[Ii]dempoten|[Tt]imeout|placeOrder|PlaceOrderCommand
target-path: /src/main/resources/application[^/]*\.ya?ml$
concerns: 장애, 보안
---
# Lens — 실패 처리·멱등 (reliability · Kotlin/Spring Boot 3/RestTemplate/KIS)

## Standard
- 레포 규칙 [failure-handling.md PTF1~PTF6](../../../../../../rules/failure-handling.md) — PTF1 원장 행 UPDATE·DELETE 금지(정정은 반대 행) · PTF2 원장 파생 잔고·포지션을 주기 작업이 원본으로 재계산·대조, 불일치는 기록·보정·알림 · PTF3 외부 호출 타임아웃·불확정 응답은 실패가 아닌 `UNKNOWN`, 재시도 전에 조회·취소로 확정 · PTF4 계산 금액에 요율·정책 버전 저장 · PTF5 연속 시각은 반열림 · PTF6 예수금 홀드는 요청 경로 트랜잭션 안.
- 레포 규칙 [async-external.md PTA2](../../../../../../rules/async-external.md) — **PR #50 판본**: 시세 외 메시지로 돈·주문·포지션을 바꾸는 Pub/Sub 처리는 원본과 대조하는 복구 경로를 둔다(at-most-once 통로).
- 레포 규칙 [data-access.md PTD2·PTD3](../../../../../../rules/data-access.md) — 외부 호출은 트랜잭션 밖, 영속 예외는 어댑터가 바꾸고 재시도는 바깥 경계에서 새 트랜잭션.
- 루트 `CLAUDE.md` «Financial Safety» — 모든 주문 실행 흐름은 멱등 · 상태 전이는 명시 · 금융 상태 변경은 감사·추적 가능.
- devkit 팀 규칙 `rules/dev-team/failure-handling.md` — FH1(주문·결제·원장 생성 POST 는 `Idempotency-Key` 또는 자연 키 upsert) · FH2(키 원자 예약·같은 키 다른 요청은 4xx) · FH4(예약·큐 작업은 두 번 돌아도 효과 반복 없음) · FH5(소비자는 처리 뒤 ack·메시지 id 중복 제거) · FH6(서로 막지 않아야 하는 루프는 실패 항목·사유를 기록).
- devkit 팀 규칙 `rules/dev-team/async-external.md` — LA11(모든 원격 호출에 연결·읽기 타임아웃 명시) · LA12(멱등 연산만, 일시 오류만, 횟수 상한·지수 백오프·지터로 재시도) · LA13(재시도는 한 층에서만) · LA14(사용자 요청 경로의 외부 의존엔 빠른 실패).
- devkit 팀 규칙 `rules/dev-team/common.md` — CS14(빠른 실패, 조용한 삼킴 금지) · CS21(가장 좁은 예외를 잡음) · CS22(`try` 본문은 던질 호출만) · CE18·CE21(잡은 예외는 다시 던지거나 실패로 읽히는 값으로).
- devkit 팀 규칙 `rules/dev-team/backend-api.md` — AE5(예외 → HTTP 응답은 전역 핸들러 한 곳) · AE8(예외 원문을 응답에 싣지 않음).
- 토스·카카오페이 공개 사례 — 타임아웃은 실패가 아니다(PTF3 근거) · Stripe API 문서 — Idempotency-Key 24시간 보관 → I1.
- 알려진 규칙 충돌: 없음.

## Deterministic first
| Tool | Command | Proves | If absent |
|---|---|---|---|
| detekt | `cd backend/trading-api && ./gradlew detekt --console=plain` | `exceptions` 규칙 세트(`SwallowedException`·`TooGenericExceptionCaught`·`TooGenericExceptionThrown`·`ThrowingExceptionsWithoutMessageOrCause`) 기준선 항목이 파일별로 출력에 있음 — E1·E3 근거로 인용 | 읽는다 |
| grep (catch·runCatching) | `grep -nE 'catch \(\|runCatching\|getOrNull\(\)\|getOrElse\|onFailure' <chunk-files>` | E1~E4 모집단 | 읽는다 |
| grep (원격 호출·타임아웃) | `grep -nE 'RestTemplate\|SimpleClientHttpRequestFactory\|HttpComponents\|setConnectTimeout\|setReadTimeout\|connectTimeout\|readTimeout\|timeout\|WebSocketClient\|exchange\(\|postFor\|getFor' <chunk-files>` | E5 후보 | 읽는다 |
| LLM-only | 불확정 응답 처리(I2)·멱등 키 의미(I1)·대사 경로(I7)·복구 경로(I6) | — | 발견의 `confidence` 에 «LLM-only» |

명령 칸의 `\|` 는 `|` 의 마크다운 이스케이프다. `<chunk-files>` = 내 인스턴스의 파일 목록.

## Checks
| ID | Signal — what you see in the file | Why it matters | Default severity |
|---|---|---|---|
| E1 | 삼킨 예외: `catch (e: Exception)`·`catch (e: Throwable)`·`runCatching { }` 의 실패 쪽이 비어 있거나 `null`·기본값·`emptyList()`·`false` 를 돌려주며 다시 던지지도 로그(≥ WARN)도 없음; `.getOrNull()` 로 실패를 버림 (CS14·CE21) | 실패가 흔적 없이 사라진다 — silent-wrong 의 정의 | high |
| E2 | 로그는 남기나 원인을 잃음: `log.error("…")` 에 예외 객체를 안 넘김(`log.error(e) { … }`·`log.error("…", e)` 가 아님), 메시지에 주문 id·계좌 id·종목 등 식별자 0개; `throw XException("…")` 가 `cause = e` 없이 원래 예외를 버림 | 스택과 맥락이 사라져 로그로 요청을 되짚을 수 없다 | med |
| E3 | 넓은 catch: `catch (e: Exception)`·`RuntimeException` 인데 `try` 본문 호출이 좁은 타입을 문서화함(`RestClientException`·`HttpStatusCodeException`·`ResourceAccessException`·`JsonProcessingException`·`DataIntegrityViolationException`) — 최상위 진입점(전역 핸들러·스케줄러 루프·리스너 루프) 밖에서; `try` 본문 3문장 이상 중 던지는 것은 1개 🟠 (CS21·CS22) | 관련 없는 버그가 한 처리기에 가려진다 | med |
| E4 | 전역 예외 핸들러가 아닌 곳(컨트롤러·서비스)에서 예외를 HTTP 응답으로 바꿈(`ResponseEntity.status(...)` in `catch`, `ResponseStatusException`), 또는 응답 본문에 `e.message`·`e.toString()`·스택을 실음 (AE5·AE8) | 상태 코드가 두 곳에서 정해지고, KIS 응답 원문·SQL 이 클라이언트로 샌다 | med (자리) · high (원문 노출) |
| E5 | 타임아웃 없는 원격 호출: `RestTemplate()` 기본 생성·`RestTemplateBuilder` 에 `setConnectTimeout`·`setReadTimeout`(또는 `connectTimeout`·`readTimeout`) 없음, `SimpleClientHttpRequestFactory` 기본값(무한), WebSocket 연결·Redis 연결에 타임아웃 없음 (LA11) | KIS 가 느리면 요청 스레드가 무한히 붙잡히고 스레드풀이 마른다 | high |
| E6 | 재시도 모양: 횟수 상한 없음·고정 간격·지터 없음·비멱등 POST(KIS 주문 생성)를 재시도·4xx 까지 재시도; 같은 경로에 재시도 층 둘 이상(호출자 루프 + 클라이언트 + `@Retryable`) (LA12·LA13) | 이중 주문·재시도 폭주(3 × 3 × 3 = 27 호출) | high (비멱등 재시도) · med |
| E7 | 일괄 루프(`forEach { try { … } catch … }`·스케줄 대상 반복)가 실패 항목 id·개수를 남기지 않고 넘어가거나, 한 항목 실패가 전체를 중단해 어떤 항목이 성공했는지 기록이 없음 (FH6) | 부분 실패가 조용히 빠지거나 전체 실패로 부풀려진다 | med |
| I1 | 주문·취소·원장 기록·입출금을 만드는 `@PostMapping` 이 `Idempotency-Key` 헤더 처리도, 자연 키 유일 제약 upsert 도 없음; 키를 받아도 원자 예약(유일 제약 INSERT)·저장 응답 재생·같은 키 다른 본문 4xx 중 하나라도 빠짐 (FH1·FH2·CLAUDE.md 멱등) | 클라이언트 재시도·이중 클릭이 이중 주문·이중 원장 행을 만든다 | high |
| I2 | KIS 주문·취소 호출이 타임아웃·`ResourceAccessException`·5xx·응답 파싱 실패로 끝날 때 주문을 `FAILED`·`REJECTED` 로 확정하거나 곧장 재시도함 — `UNKNOWN`(불확정) 상태 기록과 조회·취소로 확정하는 경로가 없음 (PTF3) | 상대가 처리했을 수 있다 — 실패로 보고 재시도하면 이중 주문·이중 체결 | high |
| I3 | 외부 주문 흐름이 의도 기록 → 커밋 → 트랜잭션 밖 호출(고정 멱등 키·클라이언트 주문 번호) → 결과 반영의 세 단계가 아님: 의도 행 없이 바로 KIS 호출, 또는 재시도마다 새 주문 번호 생성 (DA9·PTD2) | 프로세스가 호출 직후 죽으면 KIS 에 주문은 있고 우리 쪽엔 흔적이 없다 | high |
| I4 | 체결·원장 반영 소비자(KIS 체결 통보 WebSocket·Redis 메시지·`@TransactionalEventListener`)가 체결 번호·메시지 id 로 중복을 거르지 않음(유일 제약 또는 처리 기록) — 같은 체결 통보 두 번이 체결 두 번 (FH5) | WebSocket 재연결·재전송에 같은 체결이 두 번 반영돼 포지션·예수금이 어긋난다 | high |
| I5 | `@Scheduled` 작업이 두 번 돌거나 중간에서 다시 시작될 때 효과를 반복: 처리 표시(상태 열·`processed_at`) 원자 설정 없음, 항목별 멱등 없음, 진행 지점 저장 없음 (FH4) | 재시작·다중 인스턴스에서 스냅샷·세금 집계·자동 청산 주문이 중복된다 | high |
| I6 | 시세 외 Pub/Sub 메시지(신호·체결·잔고)로 돈·주문·포지션 상태를 바꾸는 처리가 그 통로에만 기댐 — 원본(DB·KIS 조회)과 대조하는 복구 경로 없음 (PTA2 PR #50) | Pub/Sub 은 구독자가 끊긴 동안의 메시지를 버린다 — 체결·잔고는 유실이 허용되지 않는다 | high |
| I7 | 레포 단위 대사(PTF2): 원장에서 파생된 잔고·예수금·포지션을 원장·KIS 체결 내역으로 다시 계산해 대조하는 주기 작업이 있는가, 불일치를 기록·보정·알림하는가; KIS 계좌 대사 결과(`KisReconciliationResult`)가 실제로 주기 실행되는가 — `(repo)` 에 기록 | 중복·유실 이벤트는 잔고를 조용히 어긋나게 하고 어떤 시험도 그것을 잡지 못한다 | high |

## Disprove before reporting
- E1: 최상위 진입점(스케줄러 한 틱·리스너 한 메시지)에서 로그(≥ WARN, 예외 포함) 후 다음 틱으로 넘기는 것은 통과 — 루프가 죽지 않게 하는 정당한 경계.
- E3: 전역 `@RestControllerAdvice` 의 `@ExceptionHandler(Exception::class)` 는 최종 그물 — 통과.
- E5: `RestTemplateConfig` 한 곳에서 타임아웃을 단 빈을 모두가 주입받으면 개별 호출부는 통과 — 그 빈을 쓰는지 확인.
- I1: 조회·멱등 본성의 POST(검색 조건 POST)는 대상 아님. 서버가 클라이언트 키 없이 `(accountId, clientOrderId)` 유일 제약으로 막으면 통과.
- I2: KIS 모의(LOCAL) 경로가 외부 호출 없이 메모리 매칭이면 n/a.
- I4: 유일 제약 위반을 «이미 처리됨»으로 해석해 넘기는 것은 정당한 중복 제거 — 통과.

## Out of scope
- 트랜잭션 안 원격 호출·`AFTER_COMMIT` 모양 → data/transaction-boundary. 원장 테이블 구조·요율 열 → data/schema-data-model M10·M11.
- 종목 직렬화·스레드풀·큐 상한 → load-concurrency. 로그 형식·상관 id → observability.
- 흐름 제어용 예외 → backend/code-shape S10.
