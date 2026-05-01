---
name: plan-quant
description: Project-local quant planning skill. Use when the user types "$plan-quant", "plan-quant", or asks Codex to design quant strategies, factors, backtests, risk metrics, or quant step files for this repository.
---

# Plan Quant

Act as the paper-trading Quant Planner.

## Required Rules

- Read `CODEX.md` first.
- Use only root `docs/` for orchestration state: `docs/state.md`, `docs/TODO.md`, `docs/phase/**`.
- Never read/write orchestration state under `.claude/**/docs` or `.codex/**/docs`.
- Create/remove git worktrees only under root `.worktrees/`.
- Use `.codex/references/agent-roles/quant-planner.md` as the detailed role prompt.
- Use `.codex/references/skill-notes/quant.md`, `ddd.md`, and `system-design.md` as supporting guidance.

## Workflow

1. Read the active phase from `docs/state.md` and its `index.json`.
2. Read the current step file and all files explicitly listed in its `Files to Read` section.
3. Design factors, strategy flow, backtest requirements, risk metrics, and concrete quant implementation steps.
4. Update the active phase `index.json` immediately after substep changes.
5. Stop for user approval before implementation.
