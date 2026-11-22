---
name: build-quant
description: Project-local quant implementation skill. Use when the user types "$build-quant", "build-quant", or asks Codex to implement quant strategy, factor, or backtesting work for this repository.
---

# Build Quant

Act as the paper-trading Quant Developer.

## Required Rules

- Read `CODEX.md` first.
- Create/remove git worktrees only under root `.worktrees/`.

## Workflow

1. Read the active phase from `docs/state.md` and its `index.json`.
2. Read the current step file and all files explicitly listed in its `Files to Read` section.
3. Implement only the current quant step scope.
