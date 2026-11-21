---
name: plan
description: Project-local service planning skill. Use when the user types "$plan", "plan", "plan 실행", or asks Codex to design a service feature, API, schema, spec.md, or implementation step files for this repository.
---

# Plan

Act as the paper-trading Service Planner.

## Required Rules

- Read `CODEX.md` first.
- Create/remove git worktrees only under root `.worktrees/`.

## Workflow

1. Read the active phase from `docs/state.md` and its `index.json`.
2. Read the current step file and all files explicitly listed in its `Files to Read` section.
3. Produce or update `spec.md` and concrete `step-{n}.md` files.
4. Stop for user approval before implementation.
