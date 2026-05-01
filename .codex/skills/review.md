Skill: Code Review

## Review Order

1. `git diff` — understand the scope of changes.
2. `spec.md` — understand the design intent.
3. Work through the checklist in order.
4. Output results: 🔴 Must fix / 🟡 Recommended improvement / 🟢 Confirmed OK

## Architecture

- Layer dependency direction respected (`presentation → application → domain ← infrastructure`).
- No framework dependencies in the domain layer.
- No mixing of DTOs and Entities.
- `@Transactional` exists only in the application layer.

## Code Quality

- SRP / OCP / DIP respected.
- No duplicated logic.
- No magic numbers (extracted to named constants).
- Domain-term-based naming.
- No unnecessary comments (code should be self-explanatory).
- No commented-out code.
- Transaction boundaries verified.

## Reliability

- `BigDecimal` used for all money and quantity fields.
- No N+1 queries.
- Timeout specified on all external API calls.
- No missing exception handling.
- Null handling consistent.

## Security

- No secrets or credentials logged in plaintext.
- No tokens stored in plaintext.
- SQL Injection and XSS defenses in place.
- No missing input validation.

## Test Quality

- Tests assert on observable behavior only (no implementation detail assertions).
- No domain objects mocked.
- AAA (Arrange / Act / Assert) pattern applied.
- No `verify()` on stubs.
- Boundary values and exception cases covered.
- Test names clearly describe the behavior under test.

## Quant Logic (if applicable)

- Formula matches code implementation.
- No look-ahead bias.
- Transaction costs reflected.
- Missing value and outlier handling present.
- Reproducibility ensured (random seed set).