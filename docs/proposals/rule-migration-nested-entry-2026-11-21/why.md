# 하위 폴더 입구 파일 정리 — 규칙 이사(devkit 1.2.0)

## Executive Summary

규칙 이사 커밋이 하위 폴더 입구 파일 8개(리뷰 뒤 고침에서 2개 더해 10개, 아래 절)(`backend/AGENTS.md`, `backend/CLAUDE.md`, `backend/collector-api/AGENTS.md`, `backend/quant-worker/AGENTS.md`, `backend/trading-api/AGENTS.md`, `backend/trading-api/CLAUDE.md`, `frontend/trading-web/AGENTS.md`, `frontend/trading-web/CLAUDE.md`)를 바꾼다. 커밋 게이트(dirty-harness)는 하위 폴더 입구를 하네스로 보고 이 번들을 요구한다. 승인은 이 번들을 담은 PR 의 사람 머지다.

## 문제

- 무엇: 하위 폴더 `CODEX.md` 6개가 Codex 표준 입구(`AGENTS.md`)가 아니었고, 하위 `CLAUDE.md` 에 옛 오케스트레이터 절차와 낡은 빌드 명령(`compileJava`, Kotlin 293·Java 0)이 남아 있었다.
- 어디서: 규칙 이사 원장(레포 밖 `~/.devkit/rule-migrations/paper-trading/2026-11-21-v3/ledger.jsonl`)이 그 줄들의 목적지를 정했다.
- 왜 지금: 사람 지시 2026-11-21 «내가 원하는건 규칙 마이그레이션이야 devkit 1.2.0에 맞게», 사람 결정 DECISIONS 5·8·16(옛 오케스트레이터 은퇴, compileJava 제거, CODEX.md → AGENTS.md).
- 근본 원인: 입구 파일이 표준(AGENTS.md + CLAUDE.md 가져오기) 이전 관례로 만들어졌다.

## 설계 결정

- 채택: 하위 폴더마다 `agents-entry.js sync` 가 짓는 `AGENTS.md` 를 두고, 옛 `CODEX.md` 사실 줄은 원장대로 AGENTS.md 레포 블록이나 `rules/` 로 옮기고 CODEX.md 는 지운다. 지운 `CODEX.md` 를 가져오던 `@../CODEX.md` 줄도 지운다.
- 기각: CODEX.md 를 그대로 두고 deferred(표준 밖 자리가 남음), CODEX.md 문서째 삭제만(사실 줄이 사라짐).

## 변경 명세와 수용 기준

- 하위 입구 파일에 지시 줄이 남지 않는다 — `rule-migration.js check` exit 0(`directive-kept-outside-rules` 0).
- 입구 파일마다 200줄 이하.
- `agents-entry.js check` 가 «최신».

## Risk

- Codex 가 하위 폴더에서 옛 CODEX.md 대신 AGENTS.md 를 읽는다 — Codex 표준 동작이라 위험 낮음.

## Next Step

- PR 리뷰 뒤 사람 머지. 적용은 같은 PR 의 이사 커밋이 이미 담고 있다.

## 리뷰 뒤 고침 (2026-11-21)

- 합동 리뷰가 하위 `CLAUDE.md` 5개가 옆 `AGENTS.md` 본문을 복사했고(표준은 가져오기) 빈 제목이 남았다고 막았다(P10·P11). 사람 지시 «이 PR 에서 모두 고침».
- 고침: 하위 `CLAUDE.md` 는 `@../CLAUDE.md`·`@AGENTS.md` 두 줄만 두고 본문은 `AGENTS.md` 하나에. `backend/AGENTS.md`·`frontend/trading-web/AGENTS.md` 는 빈 제목 대신 사실 줄. 대상 파일에 `backend/collector-api/CLAUDE.md`·`backend/quant-worker/CLAUDE.md` 추가.
