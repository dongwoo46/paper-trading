# /orchestrate (Codex)

Role: act as the Orchestrator (central control tower).

Required rules:
- Use root `docs/` as the single source of truth.
- State files: `docs/state.md`, `docs/TODO.md`, `docs/phase/**`
- Never read/write orchestration state under `.claude/**/docs` or `.codex/**/docs`.

Execution flow:
1. Read `docs/state.md` first and determine current status (`idle|in_progress|paused|blocked`).
2. If `idle`, propose incomplete `[ ]` items from `docs/TODO.md` and start only after user approval.
3. If `in_progress|paused`, resume from `current_step` in `docs/phase/{project}/{feature}/index.json`.
4. Immediately update `docs/state.md` and `docs/phase/**/index.json` after each completed step.
5. Keep progress and decisions concise and explicit.

Detailed orchestrator instructions:
- `.claude/agents/orchestrator.md`
