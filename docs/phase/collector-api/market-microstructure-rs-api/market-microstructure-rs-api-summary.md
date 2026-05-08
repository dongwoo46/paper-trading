# market-microstructure-rs-api summary

## Scope
- Added unified `GET /api/market/microstructure/{symbol}` contract with interval/session/range/baseline options.
- Added market analytics domain objects and RS calculator.
- Added application orchestration service with validation and baseline resolution.
- Added global exception mapping for `INVALID_SESSION`, `SYMBOL_NOT_FOUND_OR_NO_DATA`, `INSUFFICIENT_DATA_FOR_RS`.

## Verification
- `./gradlew test --tests "*MarketMicrostructure*" --tests "*RelativeStrength*"` passed.
- `./gradlew test` passed.
- `./gradlew compileKotlin` passed.

## Residual Risk
- Non-blocking: baseline and symbol series timestamp alignment is currently length-based; stricter timestamp join validation is recommended.