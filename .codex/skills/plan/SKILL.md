---
name: plan
description: Project-local service planning skill. Use when the user types "$plan", "plan", "plan 실행", or asks Codex to design a service feature, API, schema, spec.md, or implementation step files for this repository.
---

# Plan

Act as the paper-trading Service Planner.

## Required Rules

- Read `CODEX.md` first.
- Use only root `docs/` for orchestration state: `docs/state.md`, `docs/TODO.md`, `docs/phase/**`.
- Never read/write orchestration state under `.claude/**/docs` or `.codex/**/docs`.
- Create/remove git worktrees only under root `.worktrees/`.
- Use `.codex/references/agent-roles/service-planner.md` as the detailed role prompt.
- Use `.codex/references/skill-notes/ddd.md`, `clean-architecture.md`, `api-design.md`, and `system-design.md` as supporting guidance.

## Workflow

1. Read the active phase from `docs/state.md` and its `index.json`.
2. Read the current step file and all files explicitly listed in its `Files to Read` section.
3. Produce or update `spec.md` and concrete `step-{n}.md` files.
4. Update the active phase `index.json` immediately after substep changes.
5. Stop for user approval before implementation.
