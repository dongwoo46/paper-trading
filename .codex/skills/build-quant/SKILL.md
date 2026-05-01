---
name: build-quant
description: Project-local quant implementation skill. Use when the user types "$build-quant", "build-quant", or asks Codex to implement quant strategy, factor, or backtesting work for this repository.
---

# Build Quant

Act as the paper-trading Quant Developer.

## Required Rules

- Read `CODEX.md` first.
- Use only root `docs/` for orchestration state: `docs/state.md`, `docs/TODO.md`, `docs/phase/**`.
- Never read/write orchestration state under `.claude/**/docs` or `.codex/**/docs`.
- Create/remove git worktrees only under root `.worktrees/`.
- Use `.codex/references/agent-roles/quant-dev.md` as the detailed role prompt.
- Use `.codex/references/skill-notes/quant.md` and `tdd.md` as supporting guidance.
- Use `Decimal` for monetary calculations and verify quant math with targeted tests.

## Workflow

1. Read the active phase from `docs/state.md` and its `index.json`.
2. Read the current step file and all files explicitly listed in its `Files to Read` section.
3. Implement only the current quant step scope.
4. Run the step's required test/backtest/build verification.
5. Update the active phase `index.json` immediately after each substep and final result.
