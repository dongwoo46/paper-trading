# Conduct — 작업 범위·서브에이전트 운용 (paper-trading)
<!-- devkit:pointer
path: ^(backend|frontend|scripts|rules|docs)/
why: 이 레포의 코드·규칙·문서를 고치는 작업의 범위와 서브에이전트 운용
-->

> 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.

## 옮겨 온 줄 — 루트 CLAUDE.md

- CRITICAL: Only read explicitly specified files. Expand only when necessary. No broad exploration
- Only modify code relevant to the task. Do not touch unrelated files. If unavoidable, state the reason
- If refactoring, large-scale changes, or improvements beyond the task scope are needed, propose them to the user and wait for approval before proceeding.

- Prefer one focused subagent per independent work item.
- Parallelize only when write scopes do not overlap and tasks are truly independent.
- Do not spawn extra planners/reviewers if plan/design has not changed.
- Always summarize build/test outputs before handing control back.

- ⚠️ If session context is overloaded, switch to a new session

## 옮겨 온 줄 — 루트 CODEX.md

- Read only explicitly specified files; expand only when necessary.
- Modify only task-relevant files; do not implement beyond scope without approval.
