# Step 6 — FSD Violation Rework (fullstack-dev)

## Agent
fullstack-dev

## Working Directory
`.worktrees/front-account-dashboard`

## Context: Why This Rework

Code review (Step 4) returned CHANGES REQUIRED with 1 MUST FIX:

> **FSD Violation**: `shared/api/accountApi.ts` imports `AccountResponse`, `PositionResponse` from `entities/account/model/types`.  
> FSD rule: the `shared` layer must have **zero domain dependencies**. API functions that consume domain types belong in the `entities` layer, not `shared`.

Fix: move `accountApi.ts` into `entities/account/api/` and update every import that references the old path.

## Files to Read First

1. `CLAUDE.md` (main repo root)
2. `frontend/trading-web/src/shared/api/accountApi.ts` — file to move
3. `frontend/trading-web/src/shared/api/index.ts` — confirm `fetchJson` export
4. `frontend/trading-web/src/entities/account/model/types.ts` — confirm type names
5. `frontend/trading-web/src/pages/account/ui/AccountDashboardPage.tsx` — update import here

## Tasks

### Task 1 — Move accountApi.ts to entities layer

1. Create directory `frontend/trading-web/src/entities/account/api/` (if not exists).
2. Create `frontend/trading-web/src/entities/account/api/accountApi.ts` with the same logic as the current `shared/api/accountApi.ts`, adjusting import paths:
   - `AccountResponse`, `PositionResponse` → import from `../model/types` (relative, one level up)
   - `fetchJson` → import from `../../../shared/api` (or keep as `@/shared/api` if tsconfig path alias is configured — check tsconfig.json)
3. Delete `frontend/trading-web/src/shared/api/accountApi.ts`.

### Task 2 — Update all consuming imports

Search for any file that imports from `shared/api/accountApi` (or `@/shared/api/accountApi`).

Expected files to update:
- `frontend/trading-web/src/pages/account/ui/AccountDashboardPage.tsx`
  - Change: `import { fetchAccounts, fetchPositions } from '@/shared/api/accountApi'`
  - To:     `import { fetchAccounts, fetchPositions } from '@/entities/account/api/accountApi'`
  (adjust to the actual alias / relative path used in the project)

Also check test files under `frontend/trading-web/src/` for the same import.

### Task 3 — Build & Test

```bash
cd .worktrees/front-account-dashboard/frontend/trading-web
npm run build
npm test -- --run
```

Both must exit with 0 errors / all tests passing.

## Acceptance Criteria

- `shared/api/accountApi.ts` **deleted** (no longer exists)
- `entities/account/api/accountApi.ts` **exists** and contains `fetchAccounts`, `fetchPositions`
- `entities/account/api/accountApi.ts` imports types only from `../model/types` — no cross-domain imports
- `fetchJson` imported from `shared` layer (correct direction: entities → shared is allowed)
- All consuming files updated (no stale `shared/api/accountApi` import remains)
- `npm run build` — 0 TypeScript errors
- `npm test -- --run` — all tests pass

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to worktree root>
- Test result: <passed N/N | failed N — list failing cases> (if applicable)
- Blockers: <none | description>
---
