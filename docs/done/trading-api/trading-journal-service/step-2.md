# Step 2: Trading Journal Service Implementation (TDD)
Assigned agent: fullstack-dev

## Working Directory
.worktrees/trading-api-trading-journal-service

## Files to Read
- CODEX.md
- docs/ADR.md
- docs/phase/trading-api/trading-journal-service/spec.md
- docs/phase/trading-api/trading-journal-service/index.json
- backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/portfolio/TradingJournal.kt
- backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/TradingJournalRepository.kt

## Tasks
1. Add application commands/queries and service classes.
- Create `backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TradingJournalCommandService.kt`
  - `fun create(command: CreateTradingJournalCommand): TradingJournalResult`
  - `fun update(journalId: Long, command: UpdateTradingJournalCommand): TradingJournalResult`
- Create `backend/trading-api/src/main/kotlin/com/papertrading/api/application/portfolio/TradingJournalQueryService.kt`
  - `fun list(filter: TradingJournalFilter, pageable: Pageable): Page<TradingJournalResult>`
  - `fun get(journalId: Long, accountId: Long): TradingJournalResult`
- Create command/query/result models in `application/portfolio/{command,query,result}` packages.

2. Extend persistence support for query use cases.
- Update `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/TradingJournalRepository.kt`
  - Add pageable methods needed by list use case:
    - `fun findByAccountId(accountId: Long, pageable: Pageable): Page<TradingJournal>`
    - `fun findByAccountIdAndTicker(accountId: Long, ticker: String, pageable: Pageable): Page<TradingJournal>`

3. Add presentation layer API.
- Create `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/TradingJournalController.kt`
  - `POST /api/trading-journals`
  - `PATCH /api/trading-journals/{journalId}`
  - `GET /api/trading-journals`
  - `GET /api/trading-journals/{journalId}`
- Create DTO files under `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/portfolio/`:
  - `TradingJournalCreateRequest.kt`
  - `TradingJournalUpdateRequest.kt`
  - `TradingJournalResponse.kt`
  - `TradingJournalListResponse.kt`

4. Add/align migration for journal query indexes if missing.
- Create/update Flyway script under `backend/trading-api/src/main/resources/db/migration/` to ensure:
  - `(account_id, created_at desc)`
  - `(account_id, ticker, created_at desc)`

5. TDD and tests.
- Add service tests under `backend/trading-api/src/test/kotlin/com/papertrading/api/application/portfolio/`.
- Add controller integration tests under `backend/trading-api/src/test/kotlin/com/papertrading/api/presentation/controller/`.
- Cover invalid input, not-found cases, account/journal mismatch, ticker filter behavior, paging behavior.

## Acceptance Criteria
```bash
cd backend/trading-api
./gradlew test --tests "*TradingJournal*"
./gradlew compileKotlin
```

## Agent Return Protocol
When finished, return:
- Changed file list
- Implemented signatures/endpoints summary
- Test command results
- Any residual risks or follow-up items
