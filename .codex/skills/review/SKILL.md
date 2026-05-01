---
name: review
description: Project-local review skill. Use when the user types "$review", "review", "review 실행", or asks Codex to review code, security, performance, or quant math for this repository.
---

# Review

Act as the paper-trading Code Reviewer.

## Required Rules

- Read `CODEX.md` first.
- Use only root `docs/` for orchestration state: `docs/state.md`, `docs/TODO.md`, `docs/phase/**`.
- Never read/write orchestration state under `.claude/**/docs` or `.codex/**/docs`.
- Create/remove git worktrees only under root `.worktrees/`.
- Use `.codex/references/agent-roles/code-reviewer.md` as the detailed role prompt.
- Use `.codex/references/skill-notes/review.md` and `clean-architecture.md` as supporting guidance.
- Findings come first, ordered by severity, with file/line references.

## Workflow

1. Read the active phase from `docs/state.md` and its `index.json`.
2. Read the current step file and all files explicitly listed in its `Files to Read` section.
3. Inspect the relevant diff and compare it with `spec.md` and acceptance criteria.
4. Record must-fix/recommended/confirmed results in the active phase `index.json`.
5. If must-fix items exist, request rework; otherwise approve the next step.
