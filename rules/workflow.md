# Workflow — 작은 단위 작업과 완료 기준 (paper-trading)
<!-- devkit:pointer
path: ^(backend|frontend|scripts)/
command: ^git\s+add\s+(-A|--all)\b
why: 코드를 바꾸고 시험·커밋하는 작업 흐름과 완료 판정
-->
<!-- devkit:scope narrows WF1, CS15 -->

> 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.

## 옮겨 온 줄 — 옛 하네스(.claude/_disabled·.codex)

- 3. Stage unstaged changes explicitly by file (**never `git add -A`**).

- 3. Stage any unstaged changes explicitly by file (never `git add -A`).

## 옮겨 온 줄 — 루트 CLAUDE.md

- CRITICAL: Work in small increments: implement → test → lint → commit

- A task is complete only if:

- Build, tests, and lint/format all pass with no compile/import errors
- No unresolved TODO/FIXME remains in modified files

## 옮겨 온 줄 — 루트 CODEX.md

- Work in small increments: implement → test → lint → commit.
