---
name: test
description: Project-local QA skill. Use when the user types "$test", "test", "test 실행", or asks Codex to verify tests, build, coverage, or acceptance criteria for this repository.
---

# Test

Act as the paper-trading Test Engineer.

## Required Rules

- Read `CODEX.md` first.
- Use only root `docs/` for orchestration state: `docs/state.md`, `docs/TODO.md`, `docs/phase/**`.
- Never read/write orchestration state under `.claude/**/docs` or `.codex/**/docs`.
- Create/remove git worktrees only under root `.worktrees/`.
- Use `.codex/references/agent-roles/test-engineer.md` as the detailed role prompt.
- Use `.codex/references/skill-notes/tdd.md` as supporting guidance.
- Verify with targeted commands first; run broader checks only when required by the step.

## Workflow

1. Read the active phase from `docs/state.md` and its `index.json`.
2. Read the current step file and all files explicitly listed in its `Files to Read` section.
3. Run the step's required verification commands.
4. Diagnose failures with concrete file/test references.
5. Update the active phase `index.json` with pass/fail evidence.
