---
lens: data/schema-data-model
dept: data
agent: data-auditor
title: 스키마·데이터 모델 (JPA 매핑·SQL 마이그레이션)
repo-checks: M12
target-signal: @Entity|@Embeddable|@MappedSuperclass|@Table\(|@Column\(|@Version|@Enumerated|@JoinColumn|@Converter|@Convert\(
target-path: /db/migration/[^/]+\.sql$|/src/main/resources/application[^/]*\.ya?ml$
concerns: 정합성
---
# Lens — 스키마·데이터 모델 (data · JPA/Hibernate 6/PostgreSQL)

## Standard
- 루트 `CLAUDE.md` «Financial Safety» — 금액은 부동소수 금지, Kotlin 은 `BigDecimal` · 모든 금융 상태 변경은 감사·추적 가능.
- 레포 규칙 [backend-api.md PTB1](../../../../../../rules/backend-api.md)(PR #50 판본) — JPA 엔티티는 infrastructure 영속 어댑터에만. 이 렌즈는 엔티티가 어디 있든 매핑 자체를 본다(자리는 layering D1).
- 레포 규칙 PTB4 — `BigDecimal` scale 을 명시하고 값 객체는 scale 을 정규화 → 컬럼 `precision`·`scale` 과 맞물린다.
- 레포 규칙 [data-access.md PTD3](../../../../../../rules/data-access.md) — 잔고·예수금·포지션·주문 상태는 `@Version`·비관 락·조건부 UPDATE 로 보호.
- 레포 규칙 [failure-handling.md PTF1·PTF3·PTF4](../../../../../../rules/failure-handling.md) — 원장 행은 UPDATE·DELETE 금지(정정은 반대 행) · 불확정 응답은 `UNKNOWN` 상태로 저장 · 계산한 금액 행엔 그때의 요율·정책 버전을 함께 저장.
- devkit 팀 규칙 `rules/dev-team/schema-migrations.md` — SM1(FK 는 인덱스 선두 열) · SM2(필터·정렬 열 인덱스) · SM3(늘 채워지는 열 NOT NULL) · SM4(자연 키 UNIQUE·도메인 범위 CHECK) · SM5(enum 은 안정된 명시 값으로 저장) · SM6(한 개념 한 이름 한 타입) · SM9(모델 변경과 마이그레이션은 같은 변경, 드리프트 없음) · SM11(파괴적 연산엔 근거) · SM12(구조 변경과 데이터 백필 분리) · SM18(앞으로 고치는 마이그레이션).
- devkit 팀 규칙 `rules/dev-team/data-access.md` DA11 — «없으면 삽입»은 DB 유일 제약으로 막는다.
- LINE Engineering — enum 의 암묵 이름·순번을 저장하지 않는다 — https://techblog.lycorp.co.jp/en/20240527b
- 우아한형제들 기술 블로그 — 한 개념 = 모든 테이블에서 한 이름 한 타입 — https://techblog.woowahan.com/2656/
- 알려진 규칙 충돌: `rules/dev-team/time.md`(KST naive 컬럼)는 devkit 팀 기본값이고 이 레포 규칙에 시간대 조항이 없다 — M6 은 «한 레포 안 혼용»만 발견으로 보고, `timestamptz` 자체를 쓴 것은 `ruleConflicts` 행으로 사람이 고르게 한다.

## Deterministic first
| Tool | Command | Proves | If absent |
|---|---|---|---|
| grep (금액 열·버전·enum) | `grep -nE 'Double\|Float\|precision\|scale\|@Version\|@Enumerated\|EnumType\|numeric\|double precision\|real\b' <chunk-files>` | M1·M2·M5·M7 후보 | 읽는다 |
| grep (마이그레이션 DDL) | `grep -nE 'CREATE (UNIQUE )?INDEX\|FOREIGN KEY\|REFERENCES\|UNIQUE\|CHECK\|NOT NULL\|DROP\|ALTER COLUMN\|UPDATE \|DELETE FROM' <chunk-files>` | M3·M4·M9·M10 후보 | 읽는다 |
| LLM-only | 상태 전이 정의(M8)·원장 불변(M10)·요율 스냅샷(M11) | — | 발견의 `confidence` 에 «LLM-only» |

명령 칸의 `\|` 는 `|` 의 마크다운 이스케이프다. `<chunk-files>` = 내 인스턴스의 파일 목록. DB 에 붙지 않는다(실 DB 없음) — 매핑·마이그레이션으로 판정하고 `confidence` 는 med 상한.

## Checks
| ID | Signal — what you see in the file | Why it matters | Default severity |
|---|---|---|---|
| M1 | 금액·가격·수수료·세금·잔고·요율 필드가 `Double`·`Float`, 또는 SQL `double precision`·`real`·`float`; `BigDecimal` 필드에 `@Column(precision = …, scale = …)` 가 없거나 마이그레이션이 `numeric` 을 precision·scale 없이 선언 (CLAUDE.md Financial Safety·PTB4) | 이진 부동소수 반올림이 합계에 쌓이고, scale 없는 열은 DB 가 받은 scale 을 그대로 저장해 `compareTo` 밖 비교가 갈린다 | high |
| M2 | 동시 갱신되는 잔고·예수금·포지션·주문 상태 엔티티에 `@Version` 이 없고, 그 테이블을 갱신하는 쿼리에 조건부 UPDATE·비관 락 흔적도 없음 (PTD3) | 동시 요청이 갱신을 덮어써 체결 금액이 사라진다(갱신 쪽 판정은 transaction-boundary X8 과 함께) | high |
| M3 | 자연 키·멱등 키(계좌 번호·KIS 주문 번호·체결 번호·`(accountId, idempotencyKey)`·`(accountId, businessDate)` 스냅샷)에 `@Table(uniqueConstraints)`·`UNIQUE` 인덱스가 없음; 유일성을 조회 후 삽입으로만 지킴 (SM4·DA11) | 조회-후-삽입은 경쟁 조건 — 중복 체결·중복 스냅샷의 자리 | high |
| M4 | FK 열(`account_id`·`order_id`·`position_id`)이 어떤 인덱스의 선두 열도 아님(엔티티 `@Index`·마이그레이션 `CREATE INDEX` 모두 확인); 필터·정렬 열(`status`·`symbol`·`created_at`)에 인덱스 없음 (SM1·SM2) | 자식 조회와 부모 갱신이 자식 테이블 전체 스캔이 된다 | med |
| M5 | `@Enumerated(EnumType.ORDINAL)` 또는 `@Enumerated` 없는 enum 필드(JPA 기본 ORDINAL); 마이그레이션이 enum 열을 정수로 (SM5) | 상수 순서를 바꾸면 저장된 행의 의미가 조용히 바뀐다 | high |
| M6 | 한 개념이 테이블마다 다른 이름·타입: `account_id` 가 `bigint`/`varchar`, `created_at` 이 `timestamp`/`timestamptz`/`date`, `symbol`/`stock_code` (SM6) | 조인마다 캐스트, 코드에 번역 표, 시간대 9시간 어긋남 | med |
| M7 | 늘 채워지는 열이 nullable: Kotlin 비널 타입(`val price: BigDecimal`)인데 `@Column(nullable = false)`·`NOT NULL` 없음, 또는 반대로 nullable Kotlin 타입인데 DB 는 NOT NULL (SM3) | NULL 이 셋째 상태가 되어 소비자마다 분기하거나, 매핑이 런타임에 터진다 | med |
| M8 | 상태 열(`status`·`state`)이 2곳 이상에서 쓰이는데 전이 정의(허용 전이 표·`transitionTo()`·DB `CHECK`) 없음; 불확정 응답을 담을 `UNKNOWN` 성격 상태가 KIS 주문 상태 enum·열에 없음 (PTB2·PTF3) | 불법 전이를 시험만 잡고, 타임아웃이 «실패»로 저장돼 재시도가 이중 주문을 낸다 | med (전이) · high (UNKNOWN 없음) |
| M9 | 마이그레이션이 데이터를 지우거나 고침: `DROP TABLE`·`DROP COLUMN`·`ALTER COLUMN … TYPE`·`DELETE`·`UPDATE` 가 근거 주석 없이; 구조 변경과 백필이 한 파일 (SM11·SM12) | 배포가 데이터를 파괴하고, 백필이 DDL 락 안에서 돈다 | high (파괴) · med (혼합) |
| M10 | 원장 성격 테이블(`account_ledger`·`executions`·`settlement_*`)에 정정용 반대 행 구조(원 행 참조 `reversal_of`·`correction_of`)가 없고 UPDATE 가능한 금액·상태 열을 둠, 또는 `@Modifying` UPDATE·`delete` 가 원장 엔티티에 걸림 (PTF1) | 고쳐 쓰면 감사 흔적이 사라지고 원장으로 잔고를 다시 계산할 수 없다 | high |
| M11 | 계산된 금액(수수료·세금·이자)을 저장하는 엔티티·테이블에 그때 쓴 요율·정책 버전 열(`fee_rate`·`tax_rate`·`policy_version`)이 없고 재계산이 현재 `FeePolicy` 를 다시 읽음 (PTF4) | 요율이 바뀌면 과거 이력이 조용히 바뀌고 대사가 거짓 불일치를 낸다 | high |
| M12 | 레포 단위: 스키마 소유자가 하나인가 — `spring.jpa.hibernate.ddl-auto` 값(`create`·`update` 면 운영 위험)과 마이그레이션 도구(Flyway·Liquibase) 의존성·설정 유무, `db/migration` 버전 순서의 빈 번호(`V1` 부재 등), 엔티티 매핑과 마이그레이션 DDL 의 드리프트 — `(repo)` 에 기록 (SM9·SM19) | 둘이 스키마를 쓰면 마이그레이션은 안 돌고 `ddl-auto: create` 가 기동마다 테이블을 지운다 — 데이터 소실 | high |

## Disprove before reporting
- M1: 화면용 비율·통계 지표의 `Double` 은 금액이 아니면 통과. `BigDecimal` 이 `@Embedded` 값 객체 안에서 precision·scale 을 가지면 통과.
- M2: 생성 후 바뀌지 않는(append-only) 엔티티는 `@Version` 불필요 — 통과.
- M4: 선두 열이 아니면 인덱스가 아니다 — `(account_id, created_at)` 은 `account_id` 를 덮지만 `created_at` 단독은 안 덮는다.
- M5: `EnumType.STRING` 은 통과(이름 변경 위험은 naming N8).
- M9: `V9__drop_receivable_settlements.sql` 처럼 의도적 삭제는 근거 주석·대응 코드 삭제가 같은 변경에 있으면 low.
- 단순 CRUD·설정 성격 테이블(알림 설정·전략 로그)은 팀 규칙 «No over-engineering» 으로 M8 을 n/a — M1·M3·M5 는 그대로 본다.

## Out of scope
- 인덱스는 있는데 조건이 못 쓰는 경우 → data/query Q3. N+1 → data/query.
- 엔티티가 domain 패키지에 있는 것 자체 → backend/layering D1. 컬럼 이름 철자 → backend/naming-terminology.
- 갱신 쿼리의 락·버전 사용 → data/transaction-boundary X8.
