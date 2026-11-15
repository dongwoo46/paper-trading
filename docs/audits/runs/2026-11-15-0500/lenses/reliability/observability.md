---
lens: reliability/observability
dept: reliability
agent: reliability-auditor
title: 관측성 (kotlin-logging·Actuator·추적)
repo-checks: O10, O11
target-signal: KotlinLogging|LoggerFactory|logger\.|log\.(info|warn|error|debug|trace)|catch\s*\(|@RestControllerAdvice|@ExceptionHandler|@Scheduled|onMessage\(|RestTemplate|WebSocket|OncePerRequestFilter|HandlerInterceptor|MDC|println\(|printStackTrace|ApplicationEventPublisher|publishEvent\(
target-path: /src/main/resources/application[^/]*\.ya?ml$|/build\.gradle$|/logback[^/]*\.xml$|/application/[a-z]+/[A-Za-z]+(CommandService|TxService|BatchService|Processor|Engine|Orchestrator|PlacementService|Executor)\.kt$
concerns: 장애
---
# Lens — 관측성 (reliability · kotlin-logging(SLF4J)/Spring Boot Actuator)

## Standard
- 루트 `CLAUDE.md` — «Never log secrets in plaintext» · «All financial state changes must be auditable and event-traceable».
- 레포 규칙 [failure-handling.md PTF2·PTF3](../../../../../../rules/failure-handling.md) — 대사 불일치는 기록을 남겨 보정·알림 · 불확정(`UNKNOWN`) 응답은 확정될 때까지 추적 가능해야 함.
- 레포 규칙 [async-external.md PTA2](../../../../../../rules/async-external.md)(PR #50 판본) — Pub/Sub 유실은 흔적 없이 사라진다 → 놓친 구간을 측정해야 복구 경로가 돈다.
- devkit 팀 규칙 `rules/dev-team/backend-api.md` AE12(로그 식별자) · AE7(범주별 로그 수준 — 4xx 는 오류 경보가 아님) · AE5(전역 핸들러 한 곳).
- devkit 팀 규칙 `rules/dev-team/security-privacy-config.md` — SC8(설정·자격 증명을 로그·응답에 쓰지 않음) · SC23(로그에 원본 개인정보 없음) · SC24(운영 로그·오류 추적은 지역 변수·요청 본문을 담지 않게 시작 시 명시 설정).
- devkit 팀 규칙 `rules/dev-team/data-access.md` DA24(운영에 느린 쿼리 로그·통계, 바인딩 값 없이).
- devkit 팀 규칙 `rules/dev-team/common.md` — 사용자 메시지와 내부 로그 분리 · CS15 추적 가능한 TODO.
- Google SRE Book «Monitoring Distributed Systems» — 네 황금 신호(지연·트래픽·오류·포화) → O5·O6.
- Spring Boot Actuator 문서 — `management.endpoints.web.exposure.include`·health 그룹(liveness·readiness) → O9.
- 알려진 규칙 충돌: 없음.

## Deterministic first
| Tool | Command | Proves | If absent |
|---|---|---|---|
| detekt | `cd backend/trading-api && ./gradlew detekt --console=plain` | `PrintStackTrace`·`SwallowedException` 등 기준선 항목이 파일별로 출력에 있음 — O2·O1 근거 | 읽는다 |
| grep (로그 호출) | `grep -nE 'log(ger)?\.(error\|warn\|info\|debug)\|println\(\|printStackTrace\|MDC\.' <chunk-files>` | O1~O4·O7 모집단 | 읽는다 |
| LLM-only | 실패 경로 로그 유무(O1)·로그 수준 적합성(O4)·감사 이벤트(O8) | — | 발견의 `confidence` 에 «LLM-only» |

명령 칸의 `\|` 는 `|` 의 마크다운 이스케이프다. `<chunk-files>` = 내 인스턴스의 파일 목록.

## Checks
| ID | Signal — what you see in the file | Why it matters | Default severity |
|---|---|---|---|
| O1 | 로그 없는 실패 경로: `catch` 가 5xx 응답·오류 결과·기본값을 돌려주거나 삼키는데 WARN 이상 로그 0줄; 전역 `@ExceptionHandler` 가 5xx 를 만들면서 예외를 로그에 안 남김(4xx 는 면제) | 장애가 검색할 줄 하나 없이 지나간다 | high |
| O2 | `println(`·`print(`·`e.printStackTrace()`·`System.out`·`System.err` 이 애플리케이션 코드에 있음 | 수준·형식·수집을 우회하고, 스택이 로그 저장소에서 갈라진다 | low (스택 출력이면 med) |
| O3 | 로그 메시지를 미리 조립: `log.info("… $x")`·`log.info("…" + x)`·`String.format` — kotlin-logging 람다(`log.info { "… $x" }`) 나 SLF4J 자리표시(`log.info("… {}", x)`)가 아님 | 수준이 꺼져도 문자열을 만들고, 수집기가 템플릿으로 묶지 못한다 | low |
| O4 | 로그 수준 오용: 클라이언트 4xx(검증 실패·없는 주문)에 ERROR, 5xx·삼킨 예외·KIS 불확정 응답에 INFO·DEBUG, 대사 불일치(PTF2)·체결 유실 의심에 INFO (AE7) | 잡음에 경보가 울리고 진짜 장애엔 조용하다 | med |
| O5 | 외부 의존 호출(KIS REST·KIS WebSocket·Slack 웹훅·collector-api·Redis 발행)의 지연·결과가 기록되지 않음 — 시작·끝 로그(소요 ms·상태)·Micrometer `Timer`·`RestTemplate` 인터셉터·`ObservationRegistry` 모두 없음 | 장애 때 «KIS 가 느린가 우리가 느린가»를 가를 수 없다 | med |
| O6 | 비동기 작업·통로의 적체·지연 미측정: Redis 리스너 실행기 큐 깊이, `@Async` 큐, SSE 연결 수, 시세 처리 지연(`now − updatedAt`), 매칭 틱 소요 — 계측(로그·게이지·타이머) 0개 | 적체는 보이지 않다가 장애가 되고, 놓친 시세 구간(PTA2)을 알 수 없다 | med |
| O7 | 로그에 비밀·개인정보: KIS `appKey`·`appSecret`·접근 토큰·승인 키, `Authorization` 헤더, Slack 웹훅 URL, 계좌 번호 원문, 요청·응답 본문 통째(`$request`·`$response`·`toString()` 이 토큰 필드를 품은 data class) (SC8·SC23·CLAUDE.md) | 로그 저장소로 되돌릴 수 없이 노출된다 — 토큰 탈취는 실계좌 주문으로 이어진다 | high |
| O8 | 금융 상태 변경(주문 상태 전이·체결 반영·원장 기록·예수금 홀드·자동 청산 발동)에 남는 흔적이 없음 — 이벤트 발행·감사 행·구조화 로그(주문 id·이전/이후 상태·금액) 중 아무것도 (CLAUDE.md «auditable and event-traceable») | 사후에 «왜 이 잔고인가»를 되짚을 수 없다 | high |
| O9 | 헬스 체크 모양(`application.yaml`·Actuator): liveness 와 readiness 가 한 엔드포인트, readiness 에 DB·Redis·KIS 의존 검사 없음 또는 liveness 에 외부 의존 검사가 들어감, `management.endpoints.web.exposure.include: "*"` | 느린 의존 하나가 오케스트레이터의 재시작 폭주를 부르고, `*` 노출은 `env`·`heapdump` 를 연다 | med |
| O10 | 레포 단위: 요청 상관 id — `X-Request-Id`·`traceparent` 를 읽어 MDC 에 넣는 필터(`OncePerRequestFilter`)·Micrometer Tracing 이 없고, 스케줄러·Redis 리스너·WebSocket 처리 진입에서 상관 id 를 새로 만들지 않으며, 외부 호출에 전달하지 않음 — `(repo)` 에 기록 | 한 주문의 접수 → KIS 호출 → 체결 통보 → 원장 반영을 로그로 이을 수 없다 | med |
| O11 | 레포 단위: 운영 로그 형식·설정 — JSON 구조 로그(`logging.structured.format.console`·logback 인코더) 없음, 운영 로그 수준·느린 쿼리 로그(DA24) 설정 없음, SQL 바인딩 값 로그(`org.hibernate.orm.jdbc.bind: TRACE`)가 운영 기본값으로 켜짐 — `(repo)` 에 기록 (SC24) | 수집기가 필드로 거르지 못하고, 바인딩 로그는 계좌 번호·금액을 남긴다 | low |
| O12 | 같은 로그 문자열이 3곳 이상, 상수 단어 3개 미만 메시지(`"error: {}"`), 메시지가 검사한 조건과 다름 🟠 | 로그 한 줄을 코드 한 자리로 되짚을 수 없다 | low |

## Disprove before reporting
- O1: 다시 던지는 `catch`(상위가 로그)는 통과 — 같은 예외를 두 층에서 로그하면 O12 가 아니라 failure-handling E2 쪽 중복으로 본다.
- O3: 상수 문자열만 있는 로그는 통과.
- O7: 마스킹 함수를 거친 값(`mask(accountNo)`)은 통과. 토큰 길이·만료 시각만 찍으면 통과.
- O8: 원장 행 자체가 감사 흔적이면(PTF1 append-only) 그 경로는 통과 — 원장 밖 상태 전이만 본다.
- O9: Actuator 의존성이 있어도 노출 설정이 기본(`health` 만)이면 `*` 항목은 통과.

## Out of scope
- 예외 삼킴·원인 상실 자체 → failure-handling E1·E2(O1 은 «로그 줄이 없음»만).
- 시크릿 하드코딩·설정 위생 → security-config-hygiene. 대사 작업 존재 → failure-handling I7.
