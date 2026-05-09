## Project Overview

Paper trading (LOCAL/KIS) + algorithmic trading platform.
Market data collection → strategy execution → order/execution/settlement → portfolio analysis.

- trading-api: orders, executions, accounts, positions, settlement — Kotlin/Spring Boot 3/JPA/PostgreSQL/Redis
- collector-api: real-time quote collection, Redis publishing — Kotlin/Spring Boot 3/Redis
- quant-worker: daily OHLCV collection, backtesting, AI quant strategies — Python 3.11/FastAPI/SQLAlchemy/LangChain
- trading-web: operations dashboard UI — React/TypeScript/Vite

Data flow: KIS WebSocket → collector-api → Redis Pub/Sub → trading-api (matching engine)

---

## Session Boot Rules

- Read `docs/state.md` first; read `docs/TODO.md` only when selecting a new phase.
- Use only root `docs/` as orchestration state. Ignore `.claude/**/docs` and `.codex/**/docs`.
- Read only the active phase `docs/phase/{project}/{feature}/index.json`.
- Create/remove git worktrees only under root `.worktrees/`.
- Do not re-read unchanged state files in the same session.

---

## CRITICAL — Absolute Rules (No Exceptions)

**Security**

- Never hardcode API keys, secrets, or credentials — use environment variables
- Never log secrets in plaintext
- Never store tokens in plaintext

**Collaboration**

- Critique before execution; surface risks, gaps, and better alternatives.
- Act as an active partner, not a passive executor.
- Design/planning changes require user approval.

**Development Process**

- Write tests first for business logic changes; infrastructure/config-only changes may skip test-first but still require validation afterward.
- Work in small increments: implement → test → lint → commit.
- Write all commit messages in Korean.
- Read only explicitly specified files; expand only when necessary.
- Slash command workflows must delegate execution to Codex subagents via `spawn_agent`/`wait_agent`.
- User-defined approval gates in project docs/planner instructions override `auto` mode.
- Modify only task-relevant files; do not implement beyond scope without approval.

### Goal-Driven Execution

Define success criteria first, then execute each step with an explicit verification check.
- "Add validation" → write tests for invalid inputs, then make them pass.
- "Fix bug X" → reproduce with a failing test, implement fix, verify test passes.
- "Refactor X" → keep behavior stable and verify tests pass before/after.
- For multi-step tasks, always write steps as `[Step] -> verify: [check]`.

### Completion Criteria (Mandatory)

A task is complete only if:

- Build, tests, and lint/format all pass with no compile/import errors
- No unresolved TODO/FIXME remains in modified files
- Relevant documentation state is updated (`state.md`, `index.json`, `TODO.md` if needed)

---

## CRITICAL — Financial Safety

- Monetary calculations must never use floating-point types
- Use `BigDecimal` for Kotlin/Java monetary calculations
- Use `Decimal` for Python monetary calculations
- All order execution flows must be idempotent
- Order/Execution/Settlement state transitions must be explicit
- Never mutate financial state implicitly
- All financial state changes must be auditable and event-traceable

---

## CRITICAL — Subagent Cost Control

- Keep trivial work in the main agent; delegate implementation, test execution, refactor, and multi-file changes to subagents.
- Prefer one focused subagent per independent work item.
- Parallelize only when write scopes do not overlap and tasks are truly independent.
- Do not spawn extra planners/reviewers if plan/design has not changed.
- Always summarize build/test outputs before handing control back.

---

## Codex Orchestration

- `$orchestrate` uses `.codex/skills/orchestrate/SKILL.md` and role refs under `.codex/references/agent-roles/**`.
- Mode is recorded in `docs/state.md` and can switch between `auto` and `manual`.
- Workflow: `state.md` → active phase `index.json` → current `step-{n}.md` → subagent → doc updates.

---

## Build Verification Commands

```
trading-api:      cd backend/trading-api && ./gradlew compileKotlin
collector-api:    cd backend/collector-api && ./gradlew compileKotlin
quant-worker:     python -m py_compile {file}
trading-web:      cd frontend/trading-web && npm run build
```

---

## Docs Rules

- Keep root `docs/` as the only orchestration state source.
- In progress: update `docs/phase/{project}/{feature}/index.json`.
- Done: write `docs/done/{project}/{feature}/{feature}-summary.md`, move the phase folder, and mark `[x]` in `docs/TODO.md`.
- Add new features to `docs/TODO.md` first.
- Leave duplicate state files outside root `docs/` untouched.

⚠️ If session context is overloaded, switch to a new session
