# Trading Journal Service

## Core Feature
Provide create/update/list/detail APIs for account-level trading journals so the completed frontend trading-journal UI can persist and query data.

## Considerations
- Prioritized fast API integration with existing `TradingJournal` entity and `TradingJournalRepository`.
- Keep DDD boundaries small: journal is a portfolio-writing record tied to one account.
- Must support account and ticker filtering required by UI flows.
- Existing `trading_journals` table/entity exists; feature scope is application/presentation wiring and validation.

## Trade-offs
- Option A: Introduce separate aggregate root/repository abstraction for portfolio context now.
- Option B: Reuse current JPA entity + repository pattern used by adjacent portfolio features.
- Chosen: B, because this phase goal is API enablement for an already-built frontend and consistent short-term delivery.

## Implementation Approach
- Domain layer: keep existing `TradingJournal` entity, add only minimal invariant helpers if required.
- Application layer: add `TradingJournalCommandService` and `TradingJournalQueryService` with explicit command/query DTOs.
- Presentation layer: add `TradingJournalController` + request/response DTOs with validation and error mapping.
- Persistence layer: extend `TradingJournalRepository` query methods for pagination/detail/ticker filtering.

## Workflow
1. Client sends create/update/list/detail request.
2. Controller validates input and maps to command/query objects.
3. Service loads account/journal, enforces ownership and basic domain invariants.
4. Service saves/retrieves via repository.
5. Controller returns API DTOs for UI rendering.

## API
POST /api/trading-journals — create trading journal
Request: `{ accountId: Long, journalType: String, title: String, content: String, orderId?: Long, ticker?: String, sentiment?: String }`
Response: `{ id: Long, accountId: Long, journalType: String, title: String, content: String, orderId?: Long, ticker?: String, sentiment?: String, createdAt: String, updatedAt: String }`
Errors: `400` invalid title/content/type/sentiment, `404` account not found

PATCH /api/trading-journals/{journalId} — update title/content/sentiment
Request: `{ title: String, content: String, sentiment?: String }`
Response: `{ id: Long, accountId: Long, journalType: String, title: String, content: String, orderId?: Long, ticker?: String, sentiment?: String, createdAt: String, updatedAt: String }`
Errors: `400` invalid payload, `404` journal not found

GET /api/trading-journals?accountId={id}&ticker={ticker?}&page={n}&size={n} — list journals
Request: query params
Response: `{ items: TradingJournalItem[], page: Int, size: Int, totalElements: Long, totalPages: Int }`
Errors: `400` missing/invalid accountId or paging params, `404` account not found

GET /api/trading-journals/{journalId}?accountId={id} — get journal detail
Request: path + query param
Response: `{ id: Long, accountId: Long, journalType: String, title: String, content: String, orderId?: Long, ticker?: String, sentiment?: String, createdAt: String, updatedAt: String }`
Errors: `400` invalid accountId, `404` journal not found for account

## DB
`trading_journals`
- Key columns: `id (PK)`, `account_id (FK -> accounts.id)`, `journal_type`, `order_id`, `ticker`, `title`, `content`, `sentiment`, `created_at`, `updated_at`
- Existing repository queries indicate account/ticker + created_at sorting usage.
- Indexes to ensure:
  - `idx_trading_journals_account_created_at (account_id, created_at desc)`
  - `idx_trading_journals_account_ticker_created_at (account_id, ticker, created_at desc)`

## Ambiguities and Decisions
- Sentiment enum source of truth: implement fixed allowed values `BULLISH|BEARISH|NEUTRAL` at API validation layer.
- Journal type taxonomy is not yet formalized: allow bounded string with validation (`<=30`, non-blank) and document future enum migration path.
- Authorization model is not part of current backend scope: account ownership check is limited to account existence and journal-account consistency.
