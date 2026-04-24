Role: Orchestrator — Cleanup + PR (final step of a phase)

## Execution Order

1. Read `worktree_path` and `branch` from `index.json`.
2. Navigate to that worktree path and inspect: `git status`, `git log --oneline -10`, `git diff main...HEAD`.
3. Stage any unstaged changes explicitly by file (never `git add -A`).
4. If multiple WIP commits exist, ask the user whether to squash (wait for approval in manual mode).
5. Write commit message and run `git commit`:
   - Format: `feat({service}): {feature summary}`
   - Example: `feat(trading-api): implement position application service layer`
6. Write `{feature}-summary.md` (based on `spec.md` + each step's result).
7. Move `docs/phase/{project}/{feature}/` → `docs/done/{project}/{feature}/`.
8. Remove the phase from the active phase list in `state.md`.
9. Mark the corresponding item in `docs/TODO.md` as `[x]` with `done: YYYY-MM-DD`.
10. Draft the PR and wait for user confirmation.
11. After confirmation, run `gh pr create`.
12. After PR is created, remove the worktree:
    ```bash
    git worktree remove .worktrees/{project}-{feature}
    ```

## Commit Message Format

```
feat(trading-api): implement position application service layer

- PositionQueryService: list active positions with real-time price injection
- PositionCommandService: update evaluated P&L on quote events
- PositionController: replaced OrderQueryService dependency
- QuoteEventListener: extended to trigger position price update
```

Types: `feat` / `fix` / `refactor` / `docs` / `test` / `chore`

## {feature}-summary.md Format

```markdown
# {Feature Name}

## Core Feature
One line: what this feature does.

## Considerations
- What was prioritized.
- What constraints apply.

## Trade-offs
- Option A vs Option B → chose A because ...

## Implementation Approach
Per-layer implementation summary.

## Workflow
Request → processing → response flow.

## Key APIs
METHOD /path — description

## DB
Table name and key columns.

## Completed / PR
YYYY-MM-DD / #N
```

## PR Format
Title: `feat({service}): {feature summary}`
Body: Summary (change bullet points) / Test plan (checklist)