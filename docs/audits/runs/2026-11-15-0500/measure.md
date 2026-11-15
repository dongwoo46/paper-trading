# 재기(measure) — 무엇을 볼지 고르기 전의 숫자

> 인스턴스를 하나도 띄우지 않고 잰 값이다. 이 표를 보고 렌즈를 고른 뒤 `plan --lens` 로 넘어간다.

대상 부서: data · reliability · 모듈 backend/trading-api

| 항목표 | 부서 | 부서 파일 | 렌즈 대상 | 줄 수 | 신호 | 이 렌즈만 고르면 인스턴스 | 비고 |
|---|---|---|---|---|---|---|---|
| data/caching-strategy | data | 11 | 4 | 229 | raw SQL 7 · await 0 | 1 |  |
| data/query | data | 11 | 4 | 229 | raw SQL 7 · await 0 | 1 |  |
| data/schema-data-model | data | 11 | 4 | 229 | raw SQL 7 · await 0 | 1 |  |
| data/transaction-boundary | data | 11 | 4 | 229 | raw SQL 7 · await 0 | 1 |  |
| reliability/failure-handling | reliability | 1 | 0 | 17 | 예외 처리 0 · await 0 | 0 |  |
| reliability/load-concurrency | reliability | 1 | 0 | 17 | 예외 처리 0 · await 0 | 0 |  |
| reliability/observability | reliability | 1 | 1 | 17 | 예외 처리 0 · await 0 | 1 |  |
| reliability/security-config-hygiene | reliability | 1 | 1 | 17 | 예외 처리 0 · await 0 | 1 |  |
| backend/code-shape | backend | 0 | 0 | 0 | 800줄 초과 파일 0 · 예외 처리 0 | 0 | 이 레포에 그 부서의 대상 파일이 없음 — 스택 불일치 |
| backend/layering | backend | 0 | 0 | 0 | 800줄 초과 파일 0 · 예외 처리 0 | 0 | 이 레포에 그 부서의 대상 파일이 없음 — 스택 불일치 |
| backend/naming-terminology | backend | 0 | 0 | 0 | 800줄 초과 파일 0 · 예외 처리 0 | 0 | 이 레포에 그 부서의 대상 파일이 없음 — 스택 불일치 |
| backend/tdd-purity | backend | 0 | 0 | 0 | 800줄 초과 파일 0 · 예외 처리 0 | 0 | 이 레포에 그 부서의 대상 파일이 없음 — 스택 불일치 |
| contract/api-contract | contract | 0 | 0 | 0 | 시각 호출 0 · any 타입 0 | 0 | 이 레포에 그 부서의 대상 파일이 없음 — 스택 불일치 |
| contract/codebase-hygiene | contract | 0 | 0 | 0 | 시각 호출 0 · any 타입 0 | 0 | 이 레포에 그 부서의 대상 파일이 없음 — 스택 불일치 |
| contract/datetime-timezone | contract | 0 | 0 | 0 | 시각 호출 0 · any 타입 0 | 0 | 이 레포에 그 부서의 대상 파일이 없음 — 스택 불일치 |
| frontend/accessibility | frontend | 0 | 0 | 0 | 800줄 초과 파일 0 · any 타입 0 | 0 | 이 레포에 그 부서의 대상 파일이 없음 — 스택 불일치 |
| frontend/bundle-loading | frontend | 0 | 0 | 0 | 800줄 초과 파일 0 · any 타입 0 | 0 | 이 레포에 그 부서의 대상 파일이 없음 — 스택 불일치 |
| frontend/data-flow | frontend | 0 | 0 | 0 | 800줄 초과 파일 0 · any 타입 0 | 0 | 이 레포에 그 부서의 대상 파일이 없음 — 스택 불일치 |
| frontend/readability-predictability | frontend | 0 | 0 | 0 | 800줄 초과 파일 0 · any 타입 0 | 0 | 이 레포에 그 부서의 대상 파일이 없음 — 스택 불일치 |
| frontend/structure | frontend | 0 | 0 | 0 | 800줄 초과 파일 0 · any 타입 0 | 0 | 이 레포에 그 부서의 대상 파일이 없음 — 스택 불일치 |
| frontend/test-quality | frontend | 0 | 0 | 0 | 800줄 초과 파일 0 · any 타입 0 | 0 | 이 레포에 그 부서의 대상 파일이 없음 — 스택 불일치 |

신호는 결함이 아니라 «볼 것의 양»이다 — 판정은 감사자가 한다.
