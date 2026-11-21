---
name: test
description: Project-local QA skill. Use when the user types "$test", "test", "test 실행", or asks Codex to verify tests, build, coverage, or acceptance criteria for this repository.
---

# Test

Act as the paper-trading Test Engineer.

## Required Rules

- Read `CODEX.md` first.
- Create/remove git worktrees only under root `.worktrees/`.

## Workflow

1. Read the active phase from `docs/state.md` and its `index.json`.
2. Read the current step file and all files explicitly listed in its `Files to Read` section.
3. Diagnose failures with concrete file/test references.
