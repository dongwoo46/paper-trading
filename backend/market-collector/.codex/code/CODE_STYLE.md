# market-collector Code Style Guide

## Language and Baseline
- Kotlin 1.9+, Java 21, Spring Boot 3.x
- UTF-8, LF line endings, 4-space indentation
- Keep code and comments in English for consistency

## Naming
- Package: lowercase (`com.papertrading.collector.source.upbit`)
- Class/Object: `PascalCase`
- Function/Property/Variable: `camelCase`
- Constant: `UPPER_SNAKE_CASE`
- Suffix by responsibility:
- `*Config`, `*Properties`, `*Client`, `*Collector`, `*Repository`, `*Service`

## File and Package Layout
- One primary class per file
- Group by domain/source first, then layer
- Preferred pattern:
- `source/<source>/config`
- `source/<source>/client`
- `source/<source>/collector`
- `source/<source>/normalize`

## Kotlin Rules
- Prefer immutable values (`val`) over mutable (`var`)
- Keep functions short and focused (single responsibility)
- Use data classes for transport/domain payloads
- Avoid nullable types unless truly optional
- Use `sealed interface/class` for controlled event/result types
- Prefer expression style when readability is preserved

## Spring Rules
- Use constructor injection only
- Keep `@ConfigurationProperties` for all external configs
- Avoid field injection and static state
- Keep startup behavior explicit (no hidden side effects)

## Reactive and Concurrency Rules
- WebSocket/stream code must include reconnect + backoff policy
- No blocking calls in reactive pipelines
- Timeouts must be explicit for IO boundaries
- Add structured logs with source, symbol, and event time

## Error Handling and Logging
- Do not swallow exceptions
- Wrap external API failures with source-specific context
- Use `info` for lifecycle, `warn` for recoverable issues, `error` for failures
- Never log secrets or full credentials

## Testing
- Unit tests for normalization and mapping logic
- Integration tests for client adapters when possible
- Follow Given-When-Then naming style
- Every new collector path should have at least one happy-path test

## What to Avoid
- Large god classes
- Mixed domain concerns in one package
- Hardcoded endpoints/keys in code
- Over-abstracting before a second real use case exists
