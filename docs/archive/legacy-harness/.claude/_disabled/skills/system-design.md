Skill: System Design

## Design Principles

- **Simplicity first**: prefer a simple solution over a complex one. Add complexity only when necessary.
- **Incremental scaling**: no over-engineering. Design for the current scale.
- **Consistency over availability**: financial domains prioritize consistency.

## Project Architecture

```
KIS WebSocket → collector-api → Redis Pub/Sub → trading-api
                             → Redis Hash (quote cache)
                             → PostgreSQL (history)

collector-api → quant-worker (HTTP trigger)
             → PostgreSQL (OHLCV)

trading-api → PostgreSQL (orders / accounts / positions)
           → Redis (quote subscription)
```

## Inter-Service Communication

- **Synchronous**: HTTP REST (commands and queries).
- **Asynchronous**: Redis Pub/Sub (market quote events).

## Data Design

- Each service owns its own schema only (no cross-service table joins).
- Indexes: design based on query access patterns. No excessive indexing.
- Migrations: Flyway. Only backwards-compatible changes. Drop columns in two phases.
- Money and quantity: `NUMERIC(precision, scale)`. No `float`.

## External Dependencies

- **Circuit Breaker**: fast-fail on consecutive external API failures.
- **Polling vs WebSocket**: WebSocket for real-time quotes. HTTP for periodic batch jobs.

## Performance

- No N+1 queries (use fetch join / `@EntityGraph` / separate queries + in-memory assembly).
- Cache strategy: Redis only for data that is read frequently and changes rapidly (e.g. quotes).
- Pagination: cursor-based for large result sets.

## Failure Handling

- Health check endpoint: `/actuator/health`.
- Graceful shutdown: complete in-flight requests before stopping.
