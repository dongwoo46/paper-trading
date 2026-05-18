---
name: orchestrate
description: Project-local paper-trading orchestrator. Use when the user types "$orchestrate", "orchestrate", "orchestrate 실행", or asks Codex to select, resume, or advance a phase using this repository's root docs/state.md and docs/TODO.md.
---

# Orchestrate

Act as the paper-trading project Orchestrator.

## Required Rules

- Use only this repository's root `docs/` as orchestration state: `docs/state.md`, `docs/TODO.md`, `docs/phase/**`.
- Never read/write orchestration state under `.claude/**/docs` or `.codex/**/docs`.
- Create/remove git worktrees only under root `.worktrees/`.
- Read `CODEX.md` first when running in Codex.
- Use `.codex/references/agent-roles/orchestrator.md` as the detailed routing reference.

## Workflow

1. Read `docs/state.md`.
2. If status is `idle`, read `docs/TODO.md`, propose incomplete `[ ]` work items, and wait for user approval before creating a phase.
3. If status is `in_progress` or `paused`, read the active phase `docs/phase/{project}/{feature}/index.json` and resume from `current_step`.
4. Read only the current step file and explicitly listed supporting files.
5. Route work to the proper agent role:
   - `service-planner` / `quant-planner` for specs and step files.
   - `fullstack-dev` / `quant-dev` for implementation.
   - `test-engineer` for verification.
   - `code-reviewer` for review.
   - `cleanup` for summary, final docs, and PR prep.
6. After every completed/failed/blocked step, update root `docs/state.md` and the phase `index.json` immediately.
7. Report concise progress and next action.
8. Absolute approval gate: even in `auto` mode, after Step 1(planner) completion, never start Step 2 automatically. Ask user approval first, set `state.md` to `needs_input`, and keep Step 2 pending until approval.

## Planner Decision Policy

- The orchestrator and planner agents must not present only a single chosen design path.
- Planner Pass A must give the user real choices: feature/concept explanation, at least 3 options when feasible, pros, cons, implementation difficulty, validation method, and one explicit recommendation per decision.
- If fewer than 3 realistic options exist, explain why.
- The orchestrator must preserve the feature/concept explanation and alternatives when relaying planner output and ask the user to choose.
- User-facing summaries must explain what each feature/decision means before listing options. Do not omit this explanation to shorten the response.
- `spec.md` and downstream step files may be written only from user-confirmed decisions.

## Codex Delegation

- Use Codex subagents via `spawn_agent`/`wait_agent` when delegation is needed and available.
- Keep trivial routing, state reads, and small docs edits in the main agent.
- Use parallel subagents only for independent work with non-overlapping write scopes.
- Within a phase, reuse the same subagent per role; use `send_input` for rework, and spawn only if unavailable.
