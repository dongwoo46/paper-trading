# Step 1: Feature Spec and API Spec

**Agent**: Service Planner
**Branch**: feature/front-realtime-execution
**Working Directory**: C:\Users\dw\Desktop\paper-trading\.worktrees\front-realtime-execution

---

## Files to Read (in order)

1. `C:\Users\dw\Desktop\paper-trading\CLAUDE.md`
2. `C:\Users\dw\Desktop\paper-trading\docs\ADR.md`
3. `C:\Users\dw\Desktop\paper-trading\docs\PRD.md`
4. `C:\Users\dw\Desktop\paper-trading\docs\TODO.md`
5. `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\CLAUDE.md`
6. `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\package.json`
7. `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\src\app\App.tsx`

Also explore the frontend structure:
- `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\src\pages\`
- `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\src\features\`
- `C:\Users\dw\Desktop\paper-trading\frontend\trading-web\src\shared\`

---

## Context

**Feature**: 실시간 체결 알림 (Realtime Execution Notification)

From TODO.md:
- WebSocket 또는 SSE로 체결 이벤트 수신
- 체결 토스트 알림

**Backend**: trading-api handles executions. Check if trading-api has an SSE or WebSocket endpoint for execution events. If not, the spec should note that as a dependency.

**Frontend stack**: React/TypeScript/Vite — see package.json for available libraries.

---

## Tasks

1. Explore the existing frontend codebase to understand:
   - Current page/feature structure (FSD or similar)
   - How existing data fetching is done (REST, polling, existing WS)
   - Notification/toast pattern in use (or none yet)
   - Whether trading-api already exposes execution events via SSE/WebSocket

2. Define the feature:
   - Which transport to use (SSE vs WebSocket vs polling) and why
   - What event shape the backend must send
   - What the toast notification looks like (content, duration, position)
   - Where in the component tree notifications are rendered

3. Design component structure:
   - New files to create
   - Changes to existing files (routing, layout)

4. Write `spec.md`.

5. Generate `step-2.md` through `step-5.md` with concrete implementation directives.
   - Each step file must include "## Agent Return Protocol" section.
   - All doc paths must use main repo root (not worktree).
   - Working directory in each step must be the worktree path.

---

## Acceptance Criteria

- `spec.md` created with: transport decision, event shape, component design, file plan
- `step-2.md` through `step-5.md` created with concrete directives
- Each step file includes "## Agent Return Protocol" section
- User approval received before step files are finalized

---

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
