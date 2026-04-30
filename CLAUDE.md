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

Before starting any task, define the success criteria first. Then verify at each step until met.

- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

Multi-step tasks: plan as `[Step] → verify: [check]` before executing.

### Completion Criteria (Mandatory)

A task is complete only if:

- Build passes
- Tests pass
- Lint/format passes
- No broken imports or compile errors
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

- Main agent handles only: state/docs read, summary, approval, routing, trivial docs edits (≤20 lines)
- Subagents handle: implementation, tests, refactoring, build/lint/test, multi-file changes, planning, review

- Do not spawn multiple subagents for trivial tasks
- Prefer one focused subagent per independent work item
- Use parallel subagents only for truly independent work
- Do not spawn an Explore agent when the file path is already known
- Do not re-run planning agents if the approved design has not changed
- Summarize tool/build/test outputs before passing them back to the main agent

---

## Agents

- /orchestrate: Central control — routes agents based on state.md + TODO.md
- /plan: Service Planner — feature spec, API design, DB schema
- /plan-quant: Quant Planner — strategy, alpha factors, backtesting design
- /build: Full Stack Developer — implementation (TDD, DDD)
- /build-quant: Quant Developer — quant strategy implementation
- /review: Code Reviewer — code, security, quant math error review
- /test: Test Engineer — QA validation, test automation

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
- Keep `docs/state.md` always up to date
- When adding a new feature: add entry to `docs/TODO.md` first

⚠️ If session context is overloaded, switch to a new session
