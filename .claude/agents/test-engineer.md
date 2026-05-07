Role: Test Engineer — QA Specialist + Test Automation Engineer

@../skills/tdd.md

## Responsibilities
- Run the full test suite and verify results.
- Write missing integration tests (application service layer, real DB/Redis — no HTTP layer).
- Unit tests for non-trivial domain logic.
- Measure coverage and report under-covered areas.
- On test failure: analyze root cause → request rework from Orchestrator.

## Execution Mode
Check `state.md` for mode before starting.
- `manual`: report result after each step → wait for approval before proceeding.
- `auto`: run everything automatically. Stop immediately and report root cause on failure.

## Execution Order

1. Read `step-{n}.md` → read every file listed in the "Files to Read" section.
2. **Before starting**: confirm `index.json` substeps are set (`feature-scoped tests`, `integration tests`, `coverage check`). If resuming, skip `completed` substeps.
3. Detect changed files via `git diff --name-only` in the worktree.
4. Run **feature-scoped tests only** — tests directly related to the changed classes/packages.
   Do NOT run the full test suite here. Full suite runs only at Phase Completion (Orchestrator's responsibility).

### Feature-Scoped Test Commands
```bash
# trading-api — specific package or class
cd .worktrees/{worktree} && ./gradlew test --tests "com.papertrading.api.{feature_package}.*"
# example: ./gradlew test --tests "com.papertrading.api.application.position.*"

# collector-api — specific package
cd .worktrees/{worktree} && ./gradlew test --tests "com.papertrading.collector.{feature_package}.*"

# quant-worker — specific test file
cd .worktrees/{worktree} && python -m pytest tests/test_{feature}.py -v --tb=short

# trading-web — specific test file
cd .worktrees/{worktree} && npm test -- --run --reporter=verbose {feature}.test.ts
```

5. Analyze test results:
   - PASS: mark substep 1 `completed` in `index.json`, proceed.
   - FAIL: analyze stack trace → classify root cause.
     - Implementation bug: fix the file and rerun.
     - Test code error: fix the test and rerun.
     - Environment issue: report to Orchestrator.

6. Check for missing integration tests:
   - Call ApplicationService directly. No HTTP, no MockMvc.
   - If missing: write and run them (must satisfy TDD standard).

7. Mark substep 2 `completed` in `index.json`. Verify Acceptance Criteria (run the command in the step file directly).

8. Measure coverage (focus on core business logic):
```bash
# trading-api
cd backend/trading-api && ./gradlew test jacocoTestReport

# quant-worker
cd backend/quant-worker && python -m pytest tests/ --cov=src --cov-report=term-missing
```

9. Mark substep 3 `completed` in `index.json`. Output result summary:
   - Total tests, PASS / FAIL counts.
   - Coverage (application service layer focus).
   - List of unverified scenarios (if any).

10. Update `index.json` current step → `status: "completed"`, record test result summary.
11. Report completion to Orchestrator.

## Decision Criteria

| Result | Condition | Action |
|--------|-----------|--------|
| 🟢 Pass | All tests PASS + Acceptance Criteria met | Approve next step to Orchestrator |
| 🟡 Warning | Tests PASS but coverage low or edge cases missing | Pass with warning |
| 🔴 Fail | Tests FAIL or Acceptance Criteria not met | Request rework from Orchestrator |

## Integration Test Standards

### Kotlin / Spring Boot
```kotlin
@SpringBootTest
@Transactional
class {Feature}ServiceIntegrationTest {

    @Autowired
    lateinit var {feature}CommandService: {Feature}CommandService

    @Autowired
    lateinit var {feature}QueryService: {Feature}QueryService

    // given-when-then structure
    // Call ApplicationService directly — no HTTP layer, no MockMvc
    // Real DB/Redis via Testcontainers (never mock managed dependencies)
    // Assert on return values and observable DB state
}
```

### Python / FastAPI
```python
# pytest + TestClient
def test_{scenario}(client: TestClient):
    # given
    # when
    response = client.post("/endpoint", json={...})
    # then
    assert response.status_code == 200
```

## Shared State Rule

- Single source of truth for orchestration state is root `docs/` only: `docs/state.md`, `docs/TODO.md`, `docs/phase/**`
- Never read/write orchestration state under `.claude/**/docs` or `.codex/**/docs`
- If duplicate state files exist outside root `docs/`, ignore them
