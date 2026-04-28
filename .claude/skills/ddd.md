Skill: DDD (Domain-Driven Design)

## Core Concepts

- **Bounded Context**: A clear boundary within which a model is valid. Each BC has its own Ubiquitous Language.
- **Entity**: Identity determined by ID, not attributes.
- **Value Object**: Identity determined by value. Immutable. (e.g. Money, Quantity, DateRange)
- **Aggregate**: Consistency boundary. Access internal objects only through the Root. Reference other Aggregates by ID only.
- **Domain Service**: Domain operations that don't naturally belong to any Entity or VO.
- **Domain Event**: A fact that occurred in the domain. Past-tense noun. (e.g. OrderPlaced, TradeExecuted)
- **Repository**: Defined per Aggregate Root only.
- **Factory**: Encapsulates creation of complex Aggregates or Entities.
- **Ubiquitous Language**: Use the same domain terms consistently across code, docs, and conversations.

## Context Map Patterns

- **Anti-Corruption Layer (ACL)**: Prevents external BC models from polluting the internal domain. Acts as a translation layer.
- **Shared Kernel**: A small model shared by two BCs. Changes require agreement from both sides.
- **Customer-Supplier**: The upstream (Supplier) accommodates the needs of the downstream (Customer).

## Strategic Design

- **Core Domain**: The competitive advantage. Maximum investment. (This project: order matching, position management)
- **Supporting Subdomain**: Supports the Core. Build it directly but keep it simple.
- **Generic Subdomain**: A well-known problem space. Replace with external libraries.

## Implementation Rules

- Domain layer: no framework dependencies — pure business logic only.
- State changes go through domain methods: `order.cancel()`, `account.lockDeposit(amount)`.
- Business rule violations throw immediately in the domain: `require(amount > 0) { "..." }`.
- Never modify internal objects of an Aggregate from outside its boundary.
- Keep JPA `@Entity` and DDD Entity separate — ORM concerns must not pollute the domain.

## Project-Specific Application

- `trading-api` ↔ `collector-api`: separate Bounded Contexts. Apply ACL when consuming market quotes.
- `trading-api`: `domain` package = pure domain model.
- `collector-api`: `domain` package = pure domain model.
- `quant-worker`: dataclasses act as Value Objects.