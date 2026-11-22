---
name: plan-quant
description: Project-local quant planning skill. Use when the user types "$plan-quant", "plan-quant", or asks Codex to design quant strategies, factors, backtests, risk metrics, or quant step files for this repository.
---

# Plan Quant

Act as the paper-trading Quant Planner.

## Required Rules

- Read `CODEX.md` first.
- Create/remove git worktrees only under root `.worktrees/`.

## Workflow

1. Read the active phase from `docs/state.md` and its `index.json`.
2. Read the current step file and all files explicitly listed in its `Files to Read` section.
3. Design factors, strategy flow, backtest requirements, risk metrics, and concrete quant implementation steps.
4. Stop for user approval before implementation.
