# Root-cause chain — KisAccessTokenClientTest 캐시 미스 테스트가 CI 에서 실패

status: resolved

## 1. Chain

| Layer | Content |
|---|---|
| Symptom | CI full-test 와 로컬에서 `KisAccessTokenClientTest > Redis 토큰 없으면 캐시 미스 — rateLimiter acquireApproval 호출` 이 KisAccessTokenClientTest.kt:51 `verify` 에서 AssertionError 로 실패 |
| Direct cause | KisAccessTokenClient.kt:38 `tokenDbStore.findValid` 가 stub 없는 엄격한 mockk 에서 MockKException 을 던지고, 테스트의 `runCatching` 이 삼켜 `acquireApproval`(51행)에 도달하지 못함 |
| Enabling structure | 커밋 af881ce 가 Redis 다음 DB 조회 단계를 추가했으나 테스트 준비 코드(mock 설정)가 새 협력 객체를 따라가지 못했고, 예외를 삼키는 `runCatching` 이 원인을 가려 실패 원인이 단언 실패로만 보임 |

## 2. Fix

| Item | Value |
|---|---|
| Site | backend/collector-api/src/test/kotlin/com/papertrading/collector/infra/kis/source/rest/KisAccessTokenClientTest.kt#KisAccessTokenClientTest |
| Fixed layer | Direct cause |
| Clause | label:runtime-parity-gap |
| Changed files | backend/collector-api/src/test/kotlin/com/papertrading/collector/infra/kis/source/rest/KisAccessTokenClientTest.kt |
| Recurrence |  |
| Why the upper layer stays | 단언은 그대로 두고 준비 코드에 `tokenDbStore.findValid("paper") returns null` stub 만 더했다. `runCatching` 이 예외를 삼키는 구조는 이 테스트가 KIS HTTP 호출 실패를 무시하려고 쓴 것이라 범위 밖이며 TODO 로 넘긴다 |

## 3. Siblings (other sites sharing the same cause)

| Item | Value |
|---|---|
| Search | `grep -rn "KisAccessTokenClient(\|KisTokenDbStore" backend/collector-api/src/test` — 이 클래스를 만들거나 DB 저장소를 mock 하는 테스트는 이 파일 하나뿐 |

| Site (file:line) | Fixed / Left | Reason |
|---|---|---|
|  |  |  |
