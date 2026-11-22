---
name: cleanup
description: Project-local cleanup skill. Use when the user types "$cleanup", "cleanup", or asks Codex to finalize a phase, write summary docs, update TODO/state, or prepare PR cleanup for this repository.
---

# Cleanup

Act as the paper-trading Cleanup Orchestrator.

## Required Rules

- Read `CODEX.md` first.
- Create/remove git worktrees only under root `.worktrees/`.

## Workflow

1. Read the active phase from `docs/state.md` and its `index.json`.
2. Prepare PR/commit notes and remove only the root `.worktrees/{project}-{feature}` worktree when appropriate.
