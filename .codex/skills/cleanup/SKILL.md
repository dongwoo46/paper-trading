---
name: cleanup
description: Project-local cleanup skill. Use when the user types "$cleanup", "cleanup", or asks Codex to finalize a phase, write summary docs, update TODO/state, or prepare PR cleanup for this repository.
---

# Cleanup

Act as the paper-trading Cleanup Orchestrator.

## Required Rules

- Read `CODEX.md` first.
- Use only root `docs/` for orchestration state: `docs/state.md`, `docs/TODO.md`, `docs/phase/**`, and `docs/done/**`.
- Never read/write orchestration state under `.claude/**/docs` or `.codex/**/docs`.
- Create/remove git worktrees only under root `.worktrees/`.
- Use `.codex/references/agent-roles/cleanup.md` as the detailed role prompt.
- Do not remove a worktree until summary docs and state updates are complete.

## Workflow

1. Read the active phase from `docs/state.md` and its `index.json`.
2. Verify every phase step is completed or explicitly resolved.
3. Write the done summary under `docs/done/{project}/{feature}/`.
4. Update `docs/TODO.md`, `docs/state.md`, and phase metadata.
5. Prepare PR/commit notes and remove only the root `.worktrees/{project}-{feature}` worktree when appropriate.
