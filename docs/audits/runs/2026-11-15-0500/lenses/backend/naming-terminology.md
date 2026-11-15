---
lens: backend/naming-terminology
dept: backend
agent: backend-auditor
title: 이름·용어 (Kotlin·Spring)
repo-checks: N1
target-path: /src/main/kotlin/
concerns: 구조
---
# Lens — 이름·용어 (backend · Kotlin/Spring Boot 3)

## Standard
- devkit 팀 규칙 `rules/dev-team/common.md` — CS2 이름에 소속(도메인 접두 vs 공유) · CS3 보편 언어(기획 = 코드 = 테이블 = 시험), 의미 없는 약어 금지, 긍정형 불리언 · CS19 이름 길이는 범위에 비례 · CS24 상태·코드 값은 enum·상수.
- devkit 팀 규칙 `rules/dev-team/common.md` DD2(전략 설계) — 컨텍스트 하나에 용어 하나.
- 레포 규칙 [backend-api.md PTB7·PTB8](../../../../../../rules/backend-api.md)(PTB7 PR #50 판본) — 패키지 이름이 계층을 정한다(`application.<ctx>.port`·`application.common.port`·`presentation`·`infrastructure`). 잘못된 패키지 이름은 ArchUnit 판정을 엇나가게 한다.
- 레포 규칙 [failure-handling.md PTF3](../../../../../../rules/failure-handling.md) — 불확정 응답은 `UNKNOWN` 상태 — 상태 enum 이름이 의미를 정확히 담아야 한다.
- Kotlin 공식 코딩 컨벤션 §Naming rules — 클래스 UpperCamel · 함수·속성 lowerCamel · `const val`·최상위 불변 상수 SCREAMING_SNAKE · 백킹 속성 `_x` — https://kotlinlang.org/docs/coding-conventions.html#naming-rules
- Apple Swift API Design Guidelines — 부작용 없음 = 명사구, 부작용 = 동사, `is` 형 불리언 → N2·N3 — https://www.swift.org/documentation/api-design-guidelines/
- Google AIP-190 — 같은 개념 같은 용어, 포괄적 이름 금지 → N1·N5 — https://google.aip.dev/190
- Uber Go guide — 기간 값엔 단위 → N9 — https://github.com/uber-go/guide/blob/master/style.md
- 네이버 핵데이 컨벤션 — 한글 로마자 식별자 금지, 약어는 대문자 하나 → N6 — https://naver.github.io/hackday-conventions-java/
- LINE — 수식어가 머리 명사 앞(`SecuritySettingRepository`) → N10 · enum 이름·순번을 저장값으로 쓰지 않음 → N8 — https://techblog.lycorp.co.jp/en/icq15 · https://techblog.lycorp.co.jp/en/20240527b
- 알려진 규칙 충돌: 없음. CS19 는 한 화면 안 지역 이름(`i`·`it`)을 허용 — N5·N6 은 함수 밖으로 나가는 이름(인자·속성·최상위)만 본다.

## Deterministic first
| Tool | Command | Proves | If absent |
|---|---|---|---|
| ktlint | `cd backend/trading-api && ./gradlew ktlintCheck --console=plain` | `standard:class-naming`·`function-naming`·`property-naming`·`package-name` 위반 — 기준선 항목이 파일별로 출력에 있음, 내 파일로 걸러 N7 근거로 인용 | 읽는다 |
| detekt | `cd backend/trading-api && ./gradlew detekt --console=plain` | `naming` 규칙 세트(`FunctionNaming`·`VariableNaming`·`BooleanPropertyNaming` 등) 기준선 항목 — 내 파일로 걸러 읽는다 | 읽는다 |
| grep (포괄적 이름) | `grep -nE '(class\|object\|interface\|fun) (Data\|Info\|Item\|Manager\|Handler\|Util\|Utils\|Helper\|Helpers\|Common\|Misc\|Process\|Processor)[A-Z(<: ]' <chunk-files>` | N5 후보 | 읽는다 |
| grep (기간 단위) | `grep -nE '(timeout\|ttl\|delay\|interval\|expire\|retention\|period)[A-Za-z]*\s*(:\s*(Int\|Long)\|=\s*[0-9]+)' <chunk-files>` | N9 후보 | 읽는다 |

명령 칸의 `\|` 는 `|` 의 마크다운 이스케이프다. `<chunk-files>` = 내 인스턴스의 파일 목록.

## Checks
| ID | Signal — what you see in the file | Why it matters | Default severity |
|---|---|---|---|
| N1 | 레포 단위 용어 조사: 한 개념이 두 철자 이상 — 엔티티 필드·`@Column(name)`·DTO 필드·enum 값·함수 이름·테이블 사이(예 `quantity`/`qty`, `executedAt`/`filledAt`, `cancel`/`revoke`, `exitTrigger`/`trigger`, `snapshot`/`dailyBalance` 를 같은 뜻으로); 결과는 `{개념: [철자, 파일:줄…]}` — `(repo)` 에 기록 (DD2·CS3) | 용어 사이 번역층에 버그가 산다(AIP-190). 주문·체결·정산 용어가 갈리면 대사(PTF2)가 엇나간다 | med |
| N2 | 부작용 있는 함수(저장·발행·외부 호출·상태 변경)가 명사·형용사구 이름(`total()`·`snapshot()`), 또는 아무것도 안 바꾸는 순수 함수가 변경 동사 이름(`updateTotal` 이 새 값만 돌려줌) | 이름이 변경 여부를 속여 읽는 사람이 공짜라고 믿는 호출을 건너뛴다 | low |
| N3 | `Boolean` 속성·인자·지역 변수(함수 밖으로 나가는 것)가 `is`·`has`·`can`·`should`·`allow`·`needs` 접두 없음(`active`·`enabled`·`flag`), 또는 부정형(`isNotFilled`·`disabled`) (CS3) | `if (active)` 가 명사처럼 읽히고 부정형은 이중 부정을 만든다 | low |
| N4 | `get*`·`find*` 가 DB·Redis·HTTP 조회를 하는데 같은 클래스에 같은 접두의 값싼 접근자도 있어 비용이 이름에서 안 갈린다; Spring Data 파생 메서드(`findBy…`)는 제외 🟠 | `get` 은 값싼 접근을 약속해 호출자가 루프에서 부른다 | low |
| N5 | 클래스·object·함수 이름이 `Data`·`Info`·`Item`·`Manager`·`Handler`·`Util(s)`·`Helper(s)`·`Common`·`Misc`·`Process(or)` 만이거나 그것으로 끝나 소속·의도를 말하지 않음; 파일 이름 `*Utils.kt`·`*Helper.kt` 🟠. Spring 관례(`*EventHandler`·`*ExceptionHandler`)는 제외 | 포괄적 이름은 쓰레기통이 된다(카카오·AIP-190) | med (클래스·함수) · low (변수) |
| N6 | 허용 목록 밖 약어(허용: `id db url uri api http json csv html sql dto uuid jwt sse ws kis vo min max avg idx req res ctx msg`)가 함수 밖으로 나가는 이름에 있음(`qty`·`amt`·`cnt`·`acct`·`ord`·`exec`), 또는 한글 로마자 식별자(`jumun`·`chegyeol`·`jango`) | 약어는 읽는 사람마다 다르게 펼친다(네이버) | low (약어) · med (로마자) |
| N7 | Kotlin 이름 규칙 위반: 클래스 UpperCamel 아님 · 함수·속성 lowerCamel 아님 · `const val`·최상위 불변 상수 SCREAMING_SNAKE 아님 · 예외 클래스가 `Exception`·`Error` 로 끝나지 않음 · 패키지에 대문자·밑줄 (ktlint `standard:*-naming` 기준선 항목을 근거로 인용) | 종류마다 한 표기여야 클래스와 함수를 눈으로 가른다 | low |
| N8 | 저장·직렬화되는 enum 이 `@Enumerated(EnumType.ORDINAL)` 이거나 `@Enumerated` 없이 매핑(JPA 기본 ORDINAL); 저장값이 enum 상수 이름 자체(`EnumType.STRING`)인데 상수 이름을 바꾼 흔적·바꿀 계획이 있는 이름(`TEMP_*`·`OLD_*`) (LINE) | 순서를 바꾸면 이력이 다시 쓰이고, 이름을 바꾸면 저장된 행이 고아가 된다 | high (ORDINAL) · med |
| N9 | 기간·타임아웃·TTL·주기 식별자가 단위 없는 `Int`·`Long`·리터럴(`TIMEOUT = 3000`·`ttl: Long`) — `Millis`·`Seconds`·`Minutes` 접미나 `Duration` 타입이 아님. `@Scheduled(fixedDelay = …)` 인자는 n/a (Uber) | 3000 이 무엇인지 모른다 — 단위 착오는 조용한 1000배 버그 | low |
| N10 | 타입 명사(`Repository`·`Service`·`Port`·`Adapter`·`Client`·`Controller`·`Exception`·`Event`·`Command`·`Result`·`Request`·`Response`)가 이름 끝이 아님(`RepositoryOrder`), 또는 역할과 다른 접미 — 포트가 아닌 클래스에 `Port`, 어댑터가 아닌 클래스에 `Adapter`, 유스케이스가 아닌 리포지토리 구현에 `Service` (LINE·PTB8) | 접미가 역할을 속이면 ArchUnit 규칙과 사람의 판단이 함께 엇나간다 | low |
| N11 | 상태·유형 값을 문자열·숫자 리터럴로 반복 비교(`status == "FILLED"` 2곳 이상, `if (type == 3)`, KIS 응답 코드 `"0"`·`"00"` 비교가 상수 없이 흩어짐) (CS24) | 오타가 조용한 분기 누락이 된다 | low |
| N12 | 소속과 이름 불일치(CS2): 한 컨텍스트만 쓰는 심벌이 공유 자리·이름(`application.common`·`common`)에 있음, 또는 공유 심벌에 한 컨텍스트 접두(`OrderQuoteSnapshot` 을 세 컨텍스트가 씀) | 다음 재사용자가 이름에 속는다 | low |

## Disprove before reporting
- N1: 컨텍스트마다 일부러 다른 용어(주문 컨텍스트의 `Execution` vs 정산의 `SettlementExecution`)는 경계 설계 — 한 컨텍스트 안에서만 본다.
- N3·N5·N6: 한 화면 안 지역 이름은 CS19 로 면제 — 함수 밖으로 나가는 이름만.
- N6: KIS API 필드 이름을 그대로 받는 역직렬화 DTO(`@JsonProperty("ORD_QTY")`)의 JSON 쪽 이름은 외부 계약 — 통과, Kotlin 속성 이름만 본다.
- N8: 메모리에서만 쓰는 enum(저장·응답 안 함)은 통과.
- N10: Spring 관례 이름(`*Config`·`*Properties`·`*Scheduler`)은 통과.

## Out of scope
- 컬럼 이름·타입 일관성 → data/schema-data-model. 시험 이름 → tdd-purity T6.
- 계층·패키지 자리 자체 → layering. 질의 이름 함수의 쓰기 → code-shape F2.
