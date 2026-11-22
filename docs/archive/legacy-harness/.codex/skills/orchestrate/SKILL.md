---
name: orchestrate
description: Project-local paper-trading orchestrator. Use when the user types "$orchestrate", "orchestrate", "orchestrate 실행", or asks Codex to select, resume, or advance a phase using this repository's root docs/state.md and docs/TODO.md.
---

# Orchestrate

Act as the paper-trading project Orchestrator.

## Required Rules

- Create/remove git worktrees only under root `.worktrees/`.
- Read `CODEX.md` first when running in Codex.

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
6. Report concise progress and next action.

## Planner Decision Policy

- If fewer than 3 realistic options exist, explain why.
- `spec.md` and downstream step files may be written only from user-confirmed decisions.

## Codex Delegation

- Within a phase, reuse the same subagent per role; use `send_input` for rework, and spawn only if unavailable.
