# Root-cause chain — PR #50 의 @Async 한정 ArchUnit 규칙 2건이 본 분석에 등록되지 않아 CI 가 application 전체 @Async 를 통과시킴

status: resolved

## 1. Chain

| Layer | Content |
|---|---|
| Symptom | application 의 일반 메서드·클래스 수준에 `@Async` 를 붙여도 `./gradlew test --tests '*ArchitectureRulesTest*'` 가 초록(재현: OrderQueryService 에 `@Async fun probe() = Unit` 을 넣고 BUILD SUCCESSFUL). c791289 에서는 같은 코드가 `applicationUsesOnlyAllowedSpringTypes` 로 빨강이었음 |
| Direct cause | ArchitectureRulesTest.kt 의 `@ArchTest` 규칙 13개에 `asyncOnlyOnAfterCommitListenerMethodsRule`·`applicationClassesAreNotAsyncRule` 이 없고(ApplicationLayerRulesTest 가 픽스처에만 `check`), `applicationUsesOnlyAllowedSpringTypesRule` 은 `Async` 를 통째로 면제함 |
| Enabling structure | 규칙 본체(ApplicationLayerRules.kt)와 본 분석 등록(ArchitectureRulesTest.kt)이 분리돼 있고, 정의한 규칙이 모두 등록됐는지 보는 장치가 없음. 픽스처 시험이 초록이라 등록 누락이 보이지 않음 |

## 2. Fix

| Item | Value |
|---|---|
| Site | backend/trading-api/src/test/kotlin/com/papertrading/api/architecture/ArchitectureRulesTest.kt#ArchitectureRulesTest |
| Fixed layer | Direct cause |
| Clause | PTB9 |
| Changed files | backend/trading-api/src/test/kotlin/com/papertrading/api/architecture/ArchitectureRulesTest.kt |
| Recurrence |  |
| Why the upper layer stays | 정의한 규칙의 등록 누락을 자동으로 잡는 장치(예: ApplicationLayerRules.kt 의 규칙 함수가 모두 `@ArchTest` 로 쓰였는지 검사)는 이 PR 승인 범위 밖의 새 시험 설계라 후속으로 넘김. 현재 규칙 함수는 3개뿐이고 3개 모두 등록돼 사람이 확인 가능 |

## 3. Siblings (other sites sharing the same cause)

| Item | Value |
|---|---|
| Search | `grep -rn "Rule()" backend/trading-api/src/test/kotlin` 와 `grep -rn "@ArchTest" backend/trading-api/src/test/kotlin` — 규칙 본체 함수는 ApplicationLayerRules.kt 의 3개뿐이고 `@ArchTest` 는 ArchitectureRulesTest.kt 한 파일뿐. 3개 모두 이번에 등록돼 남은 미등록 규칙 없음 |

| Site (file:line) | Fixed / Left | Reason |
|---|---|---|
|  |  |  |
