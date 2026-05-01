Skill: DDD (Domain-Driven Design)

## Core Concepts

- **Entity**: Identity by ID. Mutable through domain methods only.
- **Value Object**: Identity by value. Immutable.
- **Aggregate**: Consistency boundary. All state changes go through the Root. Internal entities are not accessible from outside.
- **Repository**: One per Aggregate Root. Never for internal entities.
- **Domain Service**: Logic that doesn't belong to any single Entity or VO.
- **Domain Event**: Immutable record of something that happened. Past tense. (e.g. `OrderExecuted`)
- **Bounded Context**: Explicit boundary where a model is valid. Each BC owns its Ubiquitous Language.
- **ACL (Anti-Corruption Layer)**: Translation layer between Bounded Contexts. Prevents external models from leaking in.

## Context Map Patterns

- **ACL (Anti-Corruption Layer)**: Translation layer between BCs. Prevents external models from polluting the internal domain.
- **Shared Kernel**: A small model shared by two BCs. Changes require agreement from both sides.
- **Customer-Supplier**: Upstream (Supplier) accommodates the downstream (Customer).

## Strategic Design

- **Core Domain**: Competitive advantage — maximum investment. (This project: order matching, position management)
- **Supporting Subdomain**: Supports the Core. Build directly, keep simple.
- **Generic Subdomain**: Well-known problem space. Prefer external libraries.

## Implementation Rules

- Domain layer has zero framework dependencies — pure business logic only.
- All state changes go through domain methods (`order.cancel()`, `account.lockDeposit(amount)`).
- Business rule violations throw immediately inside the domain.
- Never create a Repository for an internal Entity — only for Aggregate Roots.
- Cross-Aggregate references use ID only, never object references.
- JPA `@Entity` and DDD Entity are separate — ORM annotations must not appear in the domain layer.

## Do Not Repeat

- **Repository for internal Entity**: `ExecutionRepository`, `RiskPolicyRepository`, etc. are violations. Internal entities are accessed only through the Root's collection.
- **Cross-Aggregate object reference**: `Execution.order: Order` is a violation. Use `Execution.orderId: Long`.

## Project-Specific

- `trading-api` ↔ `collector-api`: separate Bounded Contexts. Apply ACL at the market quote boundary.
- `trading-api` Aggregate Roots: `Order`, `Account`, `Position`, `Strategy`.
- `trading-api` domain package must be pure Kotlin — no Spring, no JPA.
