Skill: Clean Architecture

## Layer Structure

```
presentation → application → domain ← infrastructure
```

- **domain**: Pure business rules. No framework, DB, or external dependencies. The most stable layer.
- **application**: UseCase orchestration. Owns transaction boundaries. Coordinates domain objects.
- **infrastructure**: Adapters for DB / Redis / external APIs. Implements domain interfaces (Ports).
- **presentation**: HTTP translation, input validation, response assembly. Must not contain domain logic.

## Dependency Rule

- Dependencies always flow inward: outer → inner (presentation → domain).
- `domain` depends on nothing.
- `infrastructure` implements domain interfaces (Dependency Inversion).
- `presentation` calls only application UseCases.

## Layer Responsibilities

**Domain**
- Entity, Value Object, Aggregate, Domain Service, Domain Event.
- Protects business invariants (state changes via domain methods only).
- Unit-testable with no external dependencies.

**Application**
- Orchestration in UseCase classes.
- `@Transactional` lives here and nowhere else.
- Converts domain objects → DTOs before returning to presentation.

**Infrastructure**
- Repository implementations (JPA, Redis, etc.).
- External API clients.
- Timeout and Circuit Breaker configuration.

**Presentation**
- Controller: receive HTTP request → call UseCase → return response.
- Input validation (`@Valid`, `BindingResult`).
- Exception handling: centralized via `@ControllerAdvice`.

## Core Rules

- No mixing of DTOs and Entities. Convert at layer boundaries.
- Constructor injection only. Field injection via `@Autowired` is forbidden.
- Domain exceptions are defined in the domain layer.
- No magic numbers — extract to named constants.
- No N+1 queries (use fetch join / `@EntityGraph`).
- Money and quantity use `BigDecimal` only (no `double` or `float`).