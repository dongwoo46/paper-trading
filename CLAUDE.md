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

- Read `docs/state.md` first (determine active feature)
- Read `docs/TODO.md` only if task selection or backlog prioritization is needed
- Read `docs/phase/{project}/{feature}/index.json` only for the active feature
- Do not re-read unchanged state files in the same session
- Single source of truth for orchestrator state is `docs/` at repo root only
- Never read/write orchestrator state under `.claude/**/docs` or `.codex/**/docs`
- Create/remove git worktrees only under root `.worktrees/` (never under `.claude/worktrees` or `.codex/worktrees`)

---

## CRITICAL — Absolute Rules (No Exceptions)

**Security**

- Never hardcode API keys, secrets, or credentials — use environment variables
- Never log secrets in plaintext
- Never store tokens in plaintext

**Collaboration**

- Critique before execution (risks, gaps, better alternatives)
- Act as an active partner, not a passive executor. Follow the user if they insist.
- Design/planning changes require user approval

**Development Process**

- CRITICAL: Write tests first for all business logic changes (TDD: Red → Green → Refactor)
- CRITICAL: Infrastructure/configuration-only changes may skip test-first, but must include validation after implementation
- CRITICAL: Work in small increments: implement → test → lint → commit
- CRITICAL: Only read explicitly specified files. Expand only when necessary. No broad exploration
- CRITICAL: Slash command workflows MUST delegate execution to Agent tool subagents — never via Skill tool inline
  - Correct: `Agent(description="...", prompt="...")`
  - Forbidden: `Skill("build", ...)` or `Skill("plan", ...)` inline execution
- Only modify code relevant to the task. Do not touch unrelated files. If unavoidable, state the reason
- Write the minimum code that solves the problem. Do not implement for imagined future requirements.
- If refactoring, large-scale changes, or improvements beyond the task scope are needed, propose them to the user and wait for approval before proceeding.

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

- Keep trivial work (small docs/read-only checks) in the main agent.
- Delegate implementation, test execution, refactor, and multi-file changes to subagents.
- Prefer one focused subagent per independent work item.
- Parallelize only when write scopes do not overlap and tasks are truly independent.
- Do not spawn extra planners/reviewers if plan/design has not changed.
- Always summarize build/test outputs before handing control back.

---

## Agents

- /orchestrate: route phases and step execution
- /plan: service feature planning (API, schema, step files)
- /plan-quant: quant planning (factors, backtest design, risk metrics)
- /build: application implementation (TDD, DDD)
- /build-quant: quant strategy implementation
- /review: code/security/quant review and must-fix feedback
- /test: QA verification, scoped tests, coverage checks

Mode: `auto` (automatic) / `manual` (approve each step) — switchable anytime, record in docs/state.md

Workflow: /orchestrate → state.md → index.json → step-{n}.md → Agent tool subagent → record results

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

- In progress: update step status in `docs/phase/{project}/{feature}/index.json`
- Done: write `docs/done/{project}/{feature}/{feature}-summary.md` → move phase folder → mark `[x]` in `docs/TODO.md`
- Keep only root `docs/` state files up to date (`docs/state.md`, `docs/TODO.md`, `docs/phase/**`); ignore duplicate state files outside root `docs/`
- When adding a new feature, add an entry to `docs/TODO.md` first
- Never treat `.claude/**/docs` or `.codex/**/docs` as orchestration state.
- If duplicate state files are found outside root `docs/`, leave them untouched.

⚠️ If session context is overloaded, switch to a new session
