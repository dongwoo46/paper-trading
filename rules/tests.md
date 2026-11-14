# Tests — 영속·캐시 어댑터 시험 (paper-trading)
<!-- devkit:pointer
path: ^backend/(trading-api|collector-api)/src/test/.+\.kt$
signal: @DataJpaTest|@SpringBootTest|Repository|Redis|H2|h2|Testcontainers|@Container|mockk<.*(Repository|Redis)
why: PostgreSQL·Redis 를 쓰는 코드를 시험하는 Kotlin 테스트
-->
<!-- devkit:form v2 -->

> 이 레포 전용 규칙. 시험 원칙(시험 먼저, 무엇을 가짜로 둘지)은 devkit 팀 규칙 `tests.md` 가 SSoT 이고, 여기서는 영속·캐시 어댑터의 시험 엔진만 정한다.

## 실제 엔진

<!-- gt:PTT1 -->
- PostgreSQL 과 Redis 어댑터의 쿼리, 락, TTL, 제약 조건은 Testcontainers 로 띄운 실제 엔진으로 시험한다. H2, 인메모리 가짜, 리포지토리 목으로 이 동작을 대신 검증하지 않는다.
  Why: H2 는 PostgreSQL 의 락(`FOR UPDATE SKIP LOCKED`), 격리 수준, 부분 인덱스, 시퀀스 동작을 흉내 내지 못해 통과한 시험이 운영에서 깨지고, 가짜 Redis 가 음수 TTL 운영 버그를 가린 공개 사례가 있다(강남언니 기술 블로그). 이 레포는 `org.testcontainers:postgresql` 을 이미 쓴다. — enforce: review
  Exception: 도메인 모델과 유스케이스의 단위 시험은 포트를 가짜로 바꿔 엔진 없이 돌린다.

<!-- devkit:rule-verified docs/eval/rule-change/2026-11-15-repo-rules-spring-ddd.md -->
