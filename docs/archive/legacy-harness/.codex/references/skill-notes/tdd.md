Skill: TDD — Classist (Chicago) + Khorikov Principles

> Hide the implementation from the test. Hide the test from the implementation.
> Only behavior connects them.

## Red-Green-Refactor Cycle (no skipping steps)

---

## Core Philosophy

- Mock only at the system boundary. Everything inside is real.
- Classist TDD: wire internal collaborators for real; mock only Unmanaged external dependencies.
- Fewer meaningful tests beat many leaky ones locked to the current implementation.

---

## Mock Boundary (most critical rule)

### Mock these — Unmanaged external dependencies only

| Target | Examples | Reason |
|--------|----------|--------|
| System clock | `Clock`, `Instant.now()` | Non-deterministic |
| Randomness | `UUID.randomUUID()`, `Random` | Non-deterministic |

| Target | Correct approach |
|--------|-----------------|
| Database (PostgreSQL) | Testcontainers — real connection |
| Redis | Testcontainers — real connection |
| Domain objects, Entities, Value Objects | Use real instances |
| Internal services (same codebase) | Use real instances |
| Pure functions and utilities | Call directly |

---

## Stub vs Mock

- **Mock**: verifies an outbound side effect (e.g. KIS order API was called with correct params).
- `verify()` on a stub = testing implementation details = destroys refactoring resistance.

---

## Test Layers

| Layer | Scope | Tooling | Speed |
|-------|-------|---------|-------|
| Unit | Domain logic, pure functions, Entities | JUnit5/MockK, no Spring context | Milliseconds |
| Integration | Application services + real DB/Redis | @SpringBootTest + Testcontainers | Seconds |
| Regression | One per past incident | Added when bugs occur | — |

- Unit: only where logic is non-trivial. No unit tests for getters, DI wiring, or framework glue.
- Integration: moderate count, focused on critical modules per domain.
- Gate expensive live tests behind an env flag: `LIVE_TEST=true`.

---

## Naming

Template: `<subject>_<expected behavior>_when_<condition>`

---

## Assertion Rules

- Assert on **return values** and **observable state**.

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

- Plain CRUD with no logic → skip extra tests beyond minimal safety coverage.
- DI wiring, routing, module setup → the framework tests this.
- Config constants → type system or schema validator covers this.
- Code you are about to delete.

---

## Flaky Test Rules

2. Quarantine = `@Disabled` with a linked issue, owner, and deadline. No owner = delete.
3. Common roots: shared global state, real clock, test ordering, unseeded randomness, network.

---

## Migration from a Mockist Codebase

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
- Adding or expanding E2E tests for service/business-logic verification.
