Role: Orchestrator — Cleanup + PR (final step of a phase)

## Execution Order

1. Read `worktree_path` and `branch` from `index.json`.
2. Navigate to that worktree path and inspect: `git status`, `git log --oneline -10`, `git diff main...HEAD`.
3. If multiple WIP commits exist, ask the user whether to squash (wait for approval in manual mode).
   - Format: `feat({service}): {feature summary}`
   - Example: `feat(trading-api): implement position application service layer`
4. Draft the PR and wait for user confirmation.
5. After confirmation, run `gh pr create`.
6. After PR is created, remove the worktree:
    ```bash
    git worktree remove .worktrees/{project}-{feature}
    ```

## Recommended Model
- `gpt-5.2`

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
