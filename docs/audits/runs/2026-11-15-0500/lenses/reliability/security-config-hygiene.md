---
lens: reliability/security-config-hygiene
dept: reliability
agent: reliability-auditor
title: 보안·설정 위생 (Spring 설정·KIS 자격 증명)
repo-checks: S10, S11
target-signal: @Value\(|@ConfigurationProperties|System\.getenv|[Aa]pp[Kk]ey|[Aa]pp[Ss]ecret|[Ss]ecret|[Tt]oken|[Pp]assword|[Ww]ebhook|Authorization|@CrossOrigin|CorsConfiguration|CorsRegistry|@RequestBody|@PathVariable|@RequestParam|nativeQuery|createNativeQuery|createQuery\(|ProcessBuilder|Runtime\.getRuntime|ObjectInputStream|TrustManager|HostnameVerifier
target-path: /src/main/resources/application[^/]*\.ya?ml$|/build\.gradle$|/Dockerfile$
concerns: 보안
---
# Lens — 보안·설정 위생 (reliability · Spring Boot 3 설정/KIS 자격 증명/Redis 토큰)

## Standard
- 루트 `CLAUDE.md` «Security» — API 키·시크릿·자격 증명 하드코딩 금지(환경 변수) · 시크릿을 평문 로그 금지 · 토큰을 평문 저장 금지.
- 레포 규칙 [backend-api.md PTB7](../../../../../../rules/backend-api.md)(PR #50 판본) — presentation 은 엔티티를 요청·응답에 쓰지 않는다(엔티티 바인딩 = 대량 할당 위험과 같은 자리).
- devkit 팀 규칙 `rules/dev-team/security-privacy-config.md` — SC1(코드·커밋 설정에 시크릿 금지) · SC3(환경 변수는 시작 시 검증하는 설정 모듈 한 곳) · SC4(예시 env 파일) · SC5(환경 이름 분기 대신 설정 값) · SC6(시험·우회 경로는 기본 꺼짐) · SC7(디버그 모드는 설정으로만) · SC8(설정·자격 증명을 로그·응답에 쓰지 않음) · SC9(기본 인증) · SC10(클라이언트 id 로 자원을 읽을 때 호출자 범위를 같은 쿼리에) · SC11(요청 모델엔 클라이언트가 정할 필드만) · SC13(모든 요청 본문·인자 타입 지정) · SC14(바인딩 인자로만 SQL) · SC15(코드 실행 가능한 역직렬화 금지) · SC16(셸 없이 외부 프로그램) · SC17(CORS 명시 출처) · SC20(서명·인증서 검증) · SC22(잠금 파일·고정 버전).
- OWASP ASVS v4 §V14 구성 · §V2 인증 — 의존성 취약점 검사와 기본 인증.
- Spring Boot 문서 «Externalized Configuration» — `${ENV:default}` 자리표시의 기본값은 커밋된 값이다(S1 근거).
- 알려진 규칙 충돌: 없음. 이 서비스는 모의투자·운영 대시보드 내부용이라 «인증 없음»이 의도일 수 있다 — S10 은 발견으로 남기고 근거 문서가 있으면 그 경로를 `notes` 에 적어 사람이 고르게 한다.

## Deterministic first
| Tool | Command | Proves | If absent |
|---|---|---|---|
| detekt | `cd backend/trading-api && ./gradlew detekt --console=plain` | 기준선 항목 중 보안성 규칙(`potential-bugs`·`exceptions`)이 파일별로 출력에 있음 — 참고 | 읽는다 |
| grep (시크릿 후보) | `grep -nE '(key\|secret\|token\|password\|webhook)[A-Za-z-]*\s*[:=]\s*[^$ {]\|\$\{[A-Z_]+:[^}]+\}' <chunk-files>` | S1·S2 후보 — `${ENV:기본값}` 의 기본값도 본다 | 읽는다 |
| grep (원시 SQL) | `grep -nE 'nativeQuery\s*=\s*true\|createNativeQuery\|createQuery\(\|"\s*\+\s*[a-z]\|\$\{?[a-z][A-Za-z]*\}?.*(SELECT\|WHERE\|ORDER BY)' <chunk-files>` | S5 후보 | 읽는다 |
| LLM-only | 소유권 범위 조회(S7)·요청 모델 필드(S6)·토큰 저장(S3) | — | 발견의 `confidence` 에 «LLM-only» |

명령 칸의 `\|` 는 `|` 의 마크다운 이스케이프다. `<chunk-files>` = 내 인스턴스의 파일 목록. 값 원문을 발견의 `evidence` 에 옮기지 않는다 — 키 이름과 줄만 적고 값은 `***` 로 가린다.

## Checks
| ID | Signal — what you see in the file | Why it matters | Default severity |
|---|---|---|---|
| S1 | 코드·`application*.yaml`·`build.gradle`·`Dockerfile` 에 시크릿 리터럴: KIS `appKey`·`appSecret`·접근 토큰, DB 비밀번호, Slack 웹훅 URL, 서명 키 — `${ENV:기본값}` 의 기본값이 실제 같은 값(`${DB_PASSWORD:paper}` 처럼 쓰이는 비밀번호 포함), `Dockerfile` `ENV` 의 시크릿 (SC1·CLAUDE.md) | 커밋된 값은 회수할 수 없다 — 기본값이 운영에 그대로 쓰이면 누구나 아는 비밀번호다 | high (실 자격 증명) · med (기본값 비밀번호) |
| S2 | 운영 위험 기본값이 커밋된 설정에: `ddl-auto: create`·`update`, `show-sql: true`, Actuator `include: "*"`, `server.error.include-stacktrace: always`, `server.error.include-message: always`, 디버그 로그 수준 — 프로필 분리(`application-prod.yaml`·환경 변수) 없이 기본 프로필에 (SC7) | 운영이 기본 프로필로 뜨면 기동마다 스키마를 지우거나 스택·SQL 이 응답·로그로 샌다 | high (`ddl-auto: create`) · med |
| S3 | 토큰·자격 증명 저장: KIS 접근 토큰·WebSocket 승인 키를 Redis·DB·파일에 평문 저장, 만료(TTL) 없이 저장, 또는 로그·예외 메시지·응답에 포함 (CLAUDE.md «Never store tokens in plaintext»·SC8) | 저장소 하나가 털리면 실계좌 주문 권한이 넘어간다 | high |
| S4 | 설정 읽기가 흩어짐: `@Value("\${…}")`·`System.getenv` 가 여러 클래스에 산재하고 시작 시 검증(`@ConfigurationProperties` + `@Validated`·`@NotBlank`)이 없음; 필수 시크릿 기본값이 빈 문자열(`${KIS_PAPER_APP_KEY:}`)이라 누락이 기동 때가 아니라 첫 호출에 드러남 (SC3) | 빠진 키가 운영 첫 주문에서야 터진다 | med |
| S5 | 문자열로 조립한 SQL·JPQL: `@Query(nativeQuery = true)`·`createNativeQuery`·`createQuery` 에 `"…" + x`·`"…$x"` 문자열 보간, 사용자 입력 정렬 열을 그대로 `ORDER BY` 에; Querydsl `Expressions.stringTemplate` 에 입력 연결 (SC14) | 주입 — 바인딩 인자(`:name`·`?1`·Querydsl 표현식)는 통과 | high |
| S6 | 요청 모델이 서버 소유 필드를 받음: `@RequestBody` DTO 에 `status`·`filledQuantity`·`accountBalance`·`price`(시장가)·`accountId`(호출자 결정이 아니어야 할 때)·`id`; 또는 `@RequestBody` 가 엔티티 타입 (SC11·PTB7) | 클라이언트가 체결 수량·상태·잔고를 정해 보낸다 — 대량 할당 | high |
| S7 | 클라이언트가 준 id(`accountId`·`orderId`·`positionId`)로 자원을 읽을 때 호출자 범위를 같은 쿼리에 넣지 않음(`findById(orderId)` 후 소유 검사 없음 또는 나중에 검사해 404/403 이 갈림) (SC10) | 남의 계좌 주문을 취소·조회한다 — 인증이 생겨도 이 모양이면 IDOR | med (인증 없는 지금) |
| S8 | 요청 경계가 타입 없음: `@RequestBody Map<String, Any>`·`JsonNode`·`String` 본문 수동 파싱, enum 이어야 할 값이 `String`, 수량·가격에 `@Positive`·`@DecimalMin`·`@Digits` 검증 없음, 컨트롤러에 `@Valid`·`@Validated` 없음 (SC13) | 음수 수량·소수점 30자리 가격이 도메인까지 들어온다 | med |
| S9 | 외부 연결 보안: `TrustManager` 전부 신뢰·`HostnameVerifier { _, _ -> true }`·`ws://`·`http://` 운영 URL 기본값(KIS 실전 WebSocket 등)에 그 근거 없음; CORS `allowedOrigins("*")` + `allowCredentials(true)` (SC17·SC20) | 중간자 공격으로 토큰·주문이 가로채인다 | high (검증 끔) · med (평문 기본값) |
| S10 | 레포 단위 인증: `spring-boot-starter-security`(또는 동등 인증 필터)가 main 의존성에 있는가, 주문·취소·계좌 변경 라우트가 인증을 요구하는가, 인증 없음이 의도라면 그 근거 문서(네트워크 격리 등)가 있는가 — `(repo)` 에 기록 (SC9) | 주문을 넣는 API 가 열려 있으면 네트워크에 닿는 누구나 주문한다 — 실전(KIS live) 모드면 실계좌 | high |
| S11 | 레포 단위 공급망: 의존성 취약점 검사(OWASP dependency-check·`gradle dependencyCheck`·Dependabot·Renovate)가 없음, Gradle 의존성 잠금(`dependencyLocking`·`gradle.lockfile`)·wrapper 검증(`distributionSha256Sum`) 없음, Docker 기반 이미지 태그 고정 안 됨 — `(repo)` 에 기록 (SC22) | 알려진 취약점이 조용히 함께 배포된다 | med |

## Disprove before reporting
- S1: `${KIS_PAPER_APP_KEY:}`(빈 기본값)는 시크릿 리터럴이 아니다 — S4 쪽. 로컬 개발용 `localhost` URL 기본값은 시크릿이 아니다.
- S2: `application-test.yaml`·`application-local.yaml` 의 값은 대상이 아니다(이 회차 대상 파일도 아님). 기본 `application.yaml` 만 운영 위험으로 본다.
- S5: Spring Data 파생 메서드·`@Query` 의 `:param` 바인딩·Querydsl 표현식은 통과.
- S6: 응답 DTO 는 대상 아님. 서버가 덮어쓰는 필드라도 요청 모델에 있으면 발견 — 단 무시됨이 코드로 보이면 low.
- S9: KIS 모의투자(`ws://ops.koreainvestment.com:31000`)가 KIS 공식 제공 주소라면 근거를 `notes` 에 적고 low.

## Out of scope
- 로그에 비밀이 찍힘 → observability O7(S3 은 저장·응답만). 예외 원문 응답 → failure-handling E4.
- 스키마 소유(`ddl-auto` 와 마이그레이션 도구의 관계) → data/schema-data-model M12(S2 는 설정 위생 쪽만).
