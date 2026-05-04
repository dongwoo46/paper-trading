# subscription-routing-ui Spec

## 1) Goal
Build a write-centric operations UI for KIS subscription routing in `trading-web` so operators can:
- manually subscribe/unsubscribe symbols,
- manage favorites,
- register/unregister strategy-priority symbols,
- verify routing state and policy application.

This phase is not a read-only monitor dashboard; monitor-only concerns belong to `subscription-monitor-ui`.

## 2) Scope
In scope:
- Frontend page/section for routing operations under realtime KIS management.
- API client integration for routing operations.
- Operator UX for optimistic/pending/success/error state.
- Clear separation of "routing control" and "monitoring".

Out of scope:
- Backend policy engine redesign.
- Trading strategy auto-order logic.
- Historical analytics/reporting.

## 3) Current API Feasibility (based on exposed endpoints)
Available now:
- `GET /api/kis/symbols/catalog`
- `GET /api/kis/symbols/subscriptions?mode=&channel=`
- `POST /api/kis/symbols/subscriptions`
- `DELETE /api/kis/symbols/subscriptions`

Internal-only (not for front direct use):
- `POST /api/internal/subscriptions/{ticker}?mode=`
- `DELETE /api/internal/subscriptions/{ticker}?mode=`

Feasible now (UI can implement):
- Manual add/remove subscription by `(mode, channel, symbol)`.
- Catalog search/filter and current selected list visualization.

Not feasible with stable semantics yet:
- Favorites CRUD (operator-owned favorites list with retrieval/filter semantics).
- Strategy symbol priority registration/removal (explicit strategy-intent API contract).
- Unified routing state endpoint that distinguishes source/policy path
  (manual/favorite/strategy, ws/rest allocation rationale, promotion/demotion reason).

## 4) Required Backend Preconditions (must be finalized before full Step 2 implementation)
Required external API contracts:
1. Manual routing API (if existing `/api/kis/symbols/subscriptions` is canonical, confirm response/error contract).
2. Favorites API:
   - `GET /api/subscriptions/favorites?mode=&channel=`
   - `POST /api/subscriptions/favorites`
   - `DELETE /api/subscriptions/favorites`
3. Strategy-priority API:
   - `GET /api/subscriptions/strategy-symbols?mode=`
   - `POST /api/subscriptions/strategy-symbols`
   - `DELETE /api/subscriptions/strategy-symbols`
4. Routing status API (write-ops verification target):
   - `GET /api/subscriptions/routing-status?mode=`
   - must expose enough fields to validate applied routing source/policy.

Contract items to freeze:
- request/response schema,
- error codes and retryability,
- idempotency behavior for duplicate add/remove,
- mode/channel enum normalization and validation rules.

## 5) UI Functional Requirements
Must provide:
- Mode/channel selector (`paper|live`, `ws|rest`) and symbol input.
- Manual subscribe/unsubscribe actions with disabled/loading states.
- Favorites panel (list/add/remove) once API is confirmed.
- Strategy-priority panel (list/add/remove) once API is confirmed.
- Routing status panel for post-action verification (source and allocation outcome).

UX constraints:
- Show last action result with endpoint-level error reason.
- Prevent duplicate in-flight writes for same symbol/action.
- Keep API sections visibly separated: manual routing vs favorites vs strategy-priority vs status.

## 6) Risks and Decisions
Risks:
- If frontend implements provisional contracts, rework cost will be high.
- Naming overlap with monitor phase can blur ownership and UX.

Decisions:
- Step 2 uses blocked-safe gating: full implementation only after backend contract freeze.
- If only manual API is frozen, deliver manual-routing-only slice behind explicit scope note.

## 7) Definition of Done (phase-level)
- Routing UI write flows implemented for all frozen APIs.
- Tests cover success/failure/idempotent duplicate behavior at UI integration boundary.
- Build/lint/test pass in `frontend/trading-web`.
- Step docs and phase state updated by orchestrator flow.
