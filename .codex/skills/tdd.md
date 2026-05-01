Skill: TDD — Classist (Chicago) + Khorikov Principles

> Hide the implementation from the test. Hide the test from the implementation.
> Only behavior connects them.

## Red-Green-Refactor Cycle (no skipping steps)

1. Red: Write a failing test first → confirm it fails (do NOT proceed to Green without seeing red)
2. Green: Write the minimal implementation to make the test pass
3. Refactor: Remove duplication, improve readability → rerun to confirm still green

Writing production code before tests = TDD violation = forbidden.
Changing production code for test convenience = forbidden.

---

## Core Philosophy

- Test behavior, not implementation. Pure refactors must not break tests.
- Mock only at the system boundary. Everything inside is real.
- Classist TDD: wire internal collaborators for real; mock only Unmanaged external dependencies.
- Fewer meaningful tests beat many leaky ones locked to the current implementation.

---

## Mock Boundary (most critical rule)

### Mock these — Unmanaged external dependencies only

| Target | Examples | Reason |
|--------|----------|--------|
| External HTTP APIs | KIS API, FRED, email servers | You don't control them |
| System clock | `Clock`, `Instant.now()` | Non-deterministic |
| Randomness | `UUID.randomUUID()`, `Random` | Non-deterministic |

Prefer an HTTP-level fake (e.g. WireMock) over an interface/trait mock for HTTP APIs.

### Never mock these — Managed dependencies and internal collaborators

| Target | Correct approach |
|--------|-----------------|
| Database (PostgreSQL) | Testcontainers — real connection |
| Redis | Testcontainers — real connection |
| Domain objects, Entities, Value Objects | Use real instances |
| Internal services (same codebase) | Use real instances |
| Pure functions and utilities | Call directly |

Mocking `PositionRepository` to test `PositionQueryService` = forbidden.
Use `@SpringBootTest` + Testcontainers PostgreSQL instead.

---

## Stub vs Mock

- **Stub**: supplies data the test needs. Not a verification target → `verify()` on a stub is forbidden.
- **Mock**: verifies an outbound side effect (e.g. KIS order API was called with correct params).
- `verify()` on a stub = testing implementation details = destroys refactoring resistance.

---

## Test Layers

| Layer | Scope | Tooling | Speed |
|-------|-------|---------|-------|
| Unit | Domain logic, pure functions, Entities | JUnit5/MockK, no Spring context | Milliseconds |
| Integration | Application services + real DB/Redis | @SpringBootTest + Testcontainers | Seconds |
| E2E | Critical user journeys (order → fill → position) | @SpringBootTest, HTTP client | Slow |
| Regression | One per past incident | Added when bugs occur | — |

- Unit: only where logic is non-trivial. No unit tests for getters, DI wiring, or framework glue.
- Integration: moderate count, focused on critical modules per domain.
- E2E: one per critical journey. More = unsustainable maintenance.
- Gate expensive live tests behind an env flag: `LIVE_TEST=true`.

---

## Naming

Name tests after observable behavior. Never method names or internal call sequences.

```
// Bad — implementation-flavored
test_findUnique_called_once()
listPositions_returns_list()
should_work()

// Good — behavior
order_rejected_when_quantity_exceeds_available_balance()
position_avg_price_recalculated_after_buy_fill()
empty_list_returned_when_account_has_no_open_positions()
```

Template: `<subject>_<expected behavior>_when_<condition>`

---

## Assertion Rules

- Assert on **return values** and **observable state**.
- Do not use `toHaveBeenCalledWith` / `verify()` as the primary assertion.
- Compare whole objects: `assertThat(result).isEqualTo(expected)` (avoid field-by-field decomposition).
- Never snapshot non-deterministic output (timestamps, LLM text, ordering-free sets).

---

## Domain Entity Extraction

Extract a domain entity when **any** of these are true:

- Business logic on the same data is spread across 2+ services.
- A service does arithmetic or state transitions directly on a plain DB row.
- Logic is secretly pure but cannot be tested without spinning up a DB.

```kotlin
// Before — logic buried in service, tied to ORM
position.avgBuyPrice = (position.totalBuyAmount + qty * price) / (position.quantity + qty)
positionRepository.save(position)

// After — logic in entity, service only persists
position.applyBuy(qty, price)
positionRepository.save(position)
// position.applyBuy() is a pure in-memory Unit test. Milliseconds, no mocks, no drift.
```

---

## Priority Order

Happy Path → Core business logic → Boundary values / exceptions

---

## When NOT to Write a Test

- Plain CRUD with no logic → one E2E covers it.
- DI wiring, routing, module setup → the framework tests this.
- Config constants → type system or schema validator covers this.
- Code you are about to delete.
- If you cannot state the behavior the test protects in one sentence → do not write it.

---

## Flaky Test Rules

1. Never commit a flaky test. If one lands, quarantine within 24h.
2. Quarantine = `@Disabled` with a linked issue, owner, and deadline. No owner = delete.
3. Never fix flakiness with `sleep()`, retry loops, or higher timeouts → fix the root cause.
4. Common roots: shared global state, real clock, test ordering, unseeded randomness, network.

---

## Migration from a Mockist Codebase

Do not rewrite everything at once. Apply incrementally:

1. **New tests** from today fully follow these rules.
2. **Touched files**: when editing a test, convert its mocks to the boundary only.
3. **Worst offenders first**: identify top 3–5 files with the most `toHaveBeenCalledWith` — rewrite one domain at a time.
4. Introduce Testcontainers in **one** high-risk domain first → validate the pattern → expand.

---

## PR Red Flags — Reject or Rework

- More `mock.*` calls than real assertions.
- `toHaveBeenCalledWith` / `verify()` as the only assertion.
- Direct tests of `private` methods or internal module paths.
- Snapshots of timestamps, LLM output, or ordering-free collections.
- `@Disabled` without a linked issue and owner.
- Test names change every time the function under test is renamed (implementation leak).
- Internal persistence layer (e.g. `PositionRepository`) replaced with a mock.

---

## Absolute Prohibitions

- Writing production code before the test.
- Proceeding to Green without confirming Red.
- Directly testing `private` methods.
- Mocking domain objects, Entities, or internal collaborators.
- Calling `verify()` on a Stub.
- Testing multiple behaviors in a single test.
- Changing production code for test convenience.
- Replacing DB / Redis with mocks — use Testcontainers.