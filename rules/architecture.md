# Architecture — trading-web FSD 의존 방향 (paper-trading)
<!-- devkit:pointer
path: ^frontend/trading-web/src/.+\.(ts|tsx)$
why: trading-web 의 pages·features·entities·shared 계층 import
-->
<!-- devkit:scope narrows AR2 -->

> 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.

## 옮겨 온 줄 — frontend/trading-web/CLAUDE.md

- FSD 구조: pages → features → entities → shared (역방향 의존 금지)
