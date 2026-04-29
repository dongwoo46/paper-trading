# Step 1: Feature Spec and Component Design
Assigned agent: Service Planner

## Working Directory
.worktrees/front-account-dashboard

## Context
Main repo root: C:\Users\dw\Desktop\paper-trading

## Files to Read
- CLAUDE.md (main repo root)
- docs/ADR.md (if exists)
- docs/PRD.md (if exists)
- frontend/trading-web/src structure (explore to understand existing patterns)
- backend/trading-api graph or key files to understand available APIs

## Tasks
1. Analyze existing frontend architecture (routing, state management, component patterns).
2. Identify all trading-api endpoints relevant to account-dashboard:
   - GET /accounts (계좌 목록: 예수금, 평가금액, 수익률)
   - GET /positions (포지션: 종목, 수량, 평균단가, 현재가, 평가손익)
   - Any related endpoints
3. Design component tree for account-dashboard page.
4. Define TypeScript interfaces for API response types.
5. Write spec.md.
6. Generate step-2.md ~ step-5.md with concrete implementation directives.
   - Explicitly list worktree path (.worktrees/front-account-dashboard) in each step.
   - Explicitly list all required file paths in each step's "Files to Read" section.

## Acceptance Criteria
- spec.md created with component tree, API mapping, TypeScript interfaces.
- step-2.md ~ step-5.md created.
- Each step file includes the "## Agent Return Protocol" section.
- User approval received.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <list relative to main repo root>
- Blockers: <none | description>
---
