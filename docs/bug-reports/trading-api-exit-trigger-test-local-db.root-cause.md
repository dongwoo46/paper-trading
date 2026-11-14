# Root-cause chain — ExitTriggerPersistenceConstraintTest 가 DB 없는 CI 에서 실패

status: resolved

## 1. Chain

| Layer | Content |
|---|---|
| Symptom | CI full-test 에서 `ExitTriggerPersistenceConstraintTest` 2건이 컨텍스트 생성 단계(DefaultCacheAwareContextLoaderDelegate)에서 ConnectException 으로 실패 |
| Direct cause | ExitTriggerPersistenceConstraintTest.kt 의 `@DataJpaTest` + `Replace.NONE` 이 자체 DB 를 정하지 않아 application.yaml 의 기본값 `jdbc:postgresql://localhost:5432/paper` 에 붙음 |
| Enabling structure | 같은 모듈의 다른 통합 테스트는 `@Testcontainers` + `@ServiceConnection` 으로 DB 를 직접 띄우는데 이 테스트만 개발자 PC 의 로컬 Postgres 를 전제로 작성돼 환경 의존이 숨어 있었음 |

## 2. Fix

| Item | Value |
|---|---|
| Site | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/persistence/ExitTriggerPersistenceConstraintTest.kt#ExitTriggerPersistenceConstraintTest |
| Fixed layer | Direct cause |
| Clause | label:env-or-local-state-assumed |
| Changed files | backend/trading-api/src/test/kotlin/com/papertrading/api/infrastructure/persistence/ExitTriggerPersistenceConstraintTest.kt |
| Recurrence |  |
| Why the upper layer stays | 테스트가 직접 컨테이너를 띄우도록 고쳤다. 컨테이너 설정을 공통 베이스로 모으는 일은 다른 테스트 클래스까지 바꾸는 리팩터라 범위 밖이다 |

## 3. Siblings (other sites sharing the same cause)

| Item | Value |
|---|---|
| Search | `@DataJpaTest`·`@SpringBootTest` 를 쓰는 backend/trading-api/src/test 전체 파일을 훑어 `Testcontainers`·`PostgreSQLContainer` 가 없는 파일을 찾음 — 수정 전 이 파일 외에는 없었음 |

| Site (file:line) | Fixed / Left | Reason |
|---|---|---|
|  |  |  |
