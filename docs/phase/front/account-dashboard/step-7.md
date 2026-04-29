# Step 7 — Code Review 2nd Pass (code-reviewer)

## Agent
code-reviewer

## Working Directory
`.worktrees/front-account-dashboard`

## Context

Step 4 code review returned **CHANGES REQUIRED** (1 MUST FIX — FSD violation).  
Step 6 rework moved `accountApi.ts` from `shared/api/` → `entities/account/api/` and updated all imports.

This is the **targeted 2nd review** — verify the rework resolved the violation and no regressions were introduced.

## Files to Read

1. `frontend/trading-web/src/entities/account/api/accountApi.ts` — new location (must exist)
2. `frontend/trading-web/src/pages/account/ui/AccountDashboardPage.tsx` — import updated
3. `frontend/trading-web/src/entities/account/model/types.ts` — referenced by accountApi
4. `frontend/trading-web/src/shared/api/index.ts` — fetchJson still lives here

## Review Checklist

### Primary: FSD Compliance (the MUST FIX from Step 4)

- [ ] `shared/api/accountApi.ts` **no longer exists**
- [ ] `entities/account/api/accountApi.ts` **exists** with `fetchAccounts` and `fetchPositions`
- [ ] `entities/account/api/accountApi.ts` imports types only from `../model/types` (within entities/account — correct)
- [ ] `entities/account/api/accountApi.ts` imports `fetchJson` from `shared/api` (entities → shared is allowed in FSD)
- [ ] `AccountDashboardPage.tsx` imports from `entities/account/api/accountApi` (not from `shared/api/accountApi`)
- [ ] Grep for `shared/api/accountApi` across the entire `src/` — result must be 0 matches

### Secondary: Regression Check

- [ ] Build passes: run `cd .worktrees/front-account-dashboard/frontend/trading-web && npm run build`
- [ ] All items that passed Step 4 review remain passing (type safety, business logic, UX, API mapping)

## Output Format

Result per checklist item (🟢 PASS / 🟡 WARNING / 🔴 MUST FIX).  
Final verdict:
- **APPROVED** — 0 MUST FIX items → Status: PASS
- **CHANGES REQUIRED** — 1+ MUST FIX items → Status: FAIL

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: none (review only)
- Test result: n/a
- Blockers: <none | description>
---
