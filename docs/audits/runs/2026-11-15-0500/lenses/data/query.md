---
lens: data/query
dept: data
agent: data-auditor
title: 조회 (Spring Data JPA·Querydsl·Hibernate)
repo-checks: Q13
target-signal: JpaRepository|CrudRepository|JPAQueryFactory|QuerydslRepositorySupport|@Query\(|@EntityGraph|fetchJoin\(|\.fetch\(|selectFrom\(|\.findAll|\.findBy|\.countBy|\.existsBy|Repository\b|@OneToMany|@ManyToOne|@OneToOne|@ElementCollection|FetchType|@BatchSize|Pageable|PageRequest|\.offset\(|\.limit\(|nativeQuery|createQuery\(|createNativeQuery\(
concerns: 정합성
---
# Lens — 조회 (data · Spring Data JPA/Querydsl 5/Hibernate 6/PostgreSQL)

## Standard
- 레포 규칙 [data-access.md PTD5](../../../../../../rules/data-access.md) — 컬렉션 fetch join 과 페이징을 같이 쓰지 않는다(`@BatchSize`·id 먼저 조회로 나눔, Querydsl `.fetchJoin()` + `.offset()` 도 같다). `@Modifying` 벌크 쿼리는 `flushAutomatically = true, clearAutomatically = true` 를 함께.
- 레포 규칙 [failure-handling.md PTF5](../../../../../../rules/failure-handling.md) — 연속 시각(`Instant`·`LocalDateTime`) 구간은 반열림 `[start, end)`: `>= start AND < end`, `BETWEEN`·Querydsl `.between(`·파생 메서드 `...Between` 금지. 이산 날짜·연도(`LocalDate`·`Int` 연도)는 닫힌 구간이 맞고 대상 아님.
- 레포 규칙 [backend-api.md PTB3](../../../../../../rules/backend-api.md) — 다른 애그리거트는 id 로만 → 애그리거트 사이 연관관계 지연 로딩 N+1 의 뿌리.
- devkit 팀 규칙 `rules/dev-team/data-access.md` — DA19(입력 크기가 정하는 조회엔 상한과 그 이유) · DA20(루프 안 행마다 조회 금지 — 미리 일괄 읽기, 일괄 쓰기) · DA21(인덱스를 쓸 수 있는 조건 — 컬럼 쪽 함수·캐스트 금지, 앞 와일드카드 금지, 타입 일치) · DA22(깊어지는 목록은 키셋, OFFSET 이면 페이지 상한) · DA23(집계·정렬·필터는 DB 에서, 목록은 필요한 열만) · DA24(운영에 느린 쿼리 로그·통계).
- Hibernate 6 사용자 가이드 — `HHH90003004`(컬렉션 fetch join + 페이징 = 메모리 페이징), `hibernate.default_batch_fetch_size`.
- Use The Index, Luke (Markus Winand) — 함수·캐스트가 붙은 컬럼은 B-tree 를 못 쓴다, OFFSET 은 버린 행을 다 읽는다 — https://use-the-index-luke.com/
- 알려진 규칙 충돌: 없음.

## Deterministic first
| Tool | Command | Proves | If absent |
|---|---|---|---|
| grep (fetch join·페이징) | `grep -nE 'fetchJoin\(\)\|JOIN FETCH\|join fetch\|@EntityGraph\|\.offset\(\|\.limit\(\|Pageable\|@BatchSize\|default_batch_fetch_size' <chunk-files>` | Q1·E3 후보 | 읽는다 |
| grep (구간·함수 조건) | `grep -nE '\.between\(\|Between\(\|BETWEEN\|lower\(\|upper\(\|function\(\|date\(\|cast\(\|like\(\|contains\(' <chunk-files>` | Q2·Q3 후보 | 읽는다 |
| grep (무제한 목록) | `grep -nE 'findAll\(\)\|\.fetch\(\)\|findAllBy[A-Za-z]*\(' <chunk-files>` | Q5·E4 후보 — 상한을 읽어 확인 | 읽는다 |
| LLM-only | 루프 안 지연 로딩(E1)·같은 요청 중복 조회(E2)·메모리 집계(E5) | — | 발견의 `confidence` 에 «LLM-only» |

명령 칸의 `\|` 는 `|` 의 마크다운 이스케이프다. `<chunk-files>` = 내 인스턴스의 파일 목록. 인덱스 정의는 엔티티 `@Table(indexes = …)` 와 `src/main/resources/db/migration/*.sql` 을 열어 대조한다(이 렌즈의 대상 파일이 아니어도 읽어도 된다).

## Checks
| ID | Signal — what you see in the file | Why it matters | Default severity |
|---|---|---|---|
| Q1 | 컬렉션(`@OneToMany`·`@ElementCollection`) fetch join(`JOIN FETCH`·`.fetchJoin()`·`@EntityGraph(attributePaths=[컬렉션])`)과 `Pageable`·`.offset()`·`.limit()` 을 한 쿼리에 (PTD5) | Hibernate 가 전체를 메모리로 읽어 자른다(`HHH90003004` 경고뿐) — 행이 늘면 OOM | high |
| Q2 | 연속 시각 구간을 `BETWEEN`·`.between(`·`findBy…Between`·`<= end`·`23:59:59` 끝으로 조회 (PTF5) — `LocalDate`·연도 정수 구간은 n/a | 경계 시각 체결이 두 구간(일봉·정산일)에 겹쳐 세지거나 사이 행을 잃는다 | high |
| Q3 | 컬럼 쪽 함수·캐스트·형 불일치: Querydsl `.stringValue()`·`Expressions.dateTemplate(...)`·`lower(col)`·JPQL `function('date', col)`·`CAST(col AS …)`·`col.like("%x%")`/`.contains(x)`(앞 와일드카드), `String` 컬럼에 숫자 비교 — 대응 표현식 인덱스·`pg_trgm` 없음 (DA21) | B-tree 를 못 써 행마다 전체 스캔 | med |
| Q4 | `@Modifying` 벌크 `@Query`(UPDATE·DELETE)에 `flushAutomatically = true` 와 `clearAutomatically = true` 둘 다 없음 (PTD5) | clear 만 하면 같은 트랜잭션의 다른 엔티티(Account 잔고) 변경이 오류 없이 버려지고, 없으면 낡은 엔티티를 읽는다 | high |
| Q5 | 목록 엔드포인트·조회 유스케이스가 `findAll()`·`.fetch()`·`findAllBy…()` 를 상한 없이(`Pageable`·`.limit()`·기간 조건 없음) 돌려줌, 또는 `size`·`limit` 요청 인자에 최댓값 검증(`@Max`)이 없음 (DA19) | 한 클라이언트가 테이블을 통째로 당긴다 — 원장·체결은 계속 자란다 | high |
| Q6 | 깊어지는 목록(원장·체결·주문 이력)을 `.offset(page * size)`·`PageRequest.of(page, …)` 로 페이지 상한 없이 (DA22) | OFFSET 은 버린 행을 다 읽어 뒤 페이지일수록 느려진다 — 키셋(`WHERE (createdAt, id) < (:c, :i)`)이 답 | med |
| Q7 | 같은 대상·같은 필터 조회가 서로 다른 모듈 2곳 이상에서 따로 작성됨(파생 메서드·Querydsl 조건 중복) | 필터가 바뀔 때(소프트 삭제·계좌 범위) 한 사본이 빠진다 | low |
| Q8 | 읽기 전용 조회 경로가 `@Transactional(readOnly = true)` 없이 엔티티를 대량 로딩해 더티 체킹 스냅샷을 만든다, 또는 목록 응답에 열 몇 개만 쓰는데 엔티티 전체를 로딩(`select(entity)`)하고 DTO 프로젝션(`Projections.constructor`)이 없음 (DA23) | 영속성 컨텍스트가 행마다 스냅샷을 잡아 메모리·flush 비용이 늘고 넓은 행이 오간다 | low |
| E1 | 루프(`for`·`forEach`·`map`) 안에서 지연 연관(`@ManyToOne(fetch = LAZY)`·`@OneToMany`) 속성 접근 또는 응답 매핑 중 지연 로딩 — fetch join·`@EntityGraph`·`@BatchSize`·`default_batch_fetch_size` 없음 | N+1: 목록 하나에 쿼리 N 개 | high |
| E2 | 루프 안 리포지토리 호출: `ids.map { repo.findById(it) }`·`forEach { repo.save(it) }`·`for (o in orders) accountRepository.findBy…` — 데이터 크기가 정하는 반복 (DA20) | 행 하나에 왕복 하나 — 매칭·스냅샷 일괄이 종목 수에 비례해 느려진다 | high |
| E3 | `@ManyToOne`·`@OneToOne` 이 `fetch = FetchType.LAZY` 없이 선언(JPA 기본 EAGER), 또는 컬렉션에 `FetchType.EAGER` | 부모를 읽을 때마다 연관이 딸려 오고, 목록에서 N+1 이 숨는다 | med |
| E4 | `findAll()`·`.fetch()` 뒤 Kotlin 에서 `filter`·`sortedBy`·`sumOf`·`groupBy`·`maxBy`·`count()` 로 집계·필터 — 그 조건이 컬럼으로 표현 가능 (DA23) | 테이블을 앱으로 옮겨 집계한다 — 원장·체결 합계는 DB `SUM` 이 답 | med |
| E5 | 같은 요청·같은 유스케이스 안에서 같은 행·같은 조회를 2번 이상(서비스가 서로 다른 메서드에서 같은 `findBy…` 를 다시 부름) | 두 번째 읽기는 순수 비용이고 첫 번째와 다른 값을 볼 수 있다 | low |
| Q13 | 레포 단위: `application.yaml` 에 Hibernate `default_batch_fetch_size`·느린 쿼리 로그(`hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS` 등)·통계 설정이 있는가, PostgreSQL `pg_stat_statements` 를 운영에서 켜는 근거가 있는가 — `(repo)` 에 기록 (DA24) | 운영에서 어떤 쿼리가 느린지 모르면 이 렌즈의 발견에 우선순위를 줄 수 없다 | low |

## Disprove before reporting
- Q1: `@ManyToOne`(단건) fetch join + 페이징은 문제없음 — 컬렉션만.
- Q2: `LocalDate`·연도 `Int` 의 `between`(예 `DailyBalanceRepositoryImpl` 의 `businessDate.between`, `TaxSummaryRepositoryImpl` 의 `taxYear.between`)은 PTF5 가 명시한 대상 밖 — 통과.
- Q5·E4: 행 수가 도메인으로 묶인 표(수수료 정책·계좌 몇 개)는 통과 — 그 근거(«계좌당 정책 1행»)를 `notes` 에.
- E1: 이미 `@BatchSize`·`default_batch_fetch_size` 가 걸린 연관은 N+1 이 1+⌈N/size⌉ 로 줄어 통과.
- E2: 루프 횟수가 상수(≤ 3)면 통과.

## Out of scope
- 인덱스가 아예 없음 → data/schema-data-model. 트랜잭션 범위·락 보호 → data/transaction-boundary.
- 목록 응답 모양·DTO 필드 → contract(이 회차 범위 밖). 루프 안 외부 HTTP 호출 → reliability/load-concurrency.
