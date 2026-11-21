# Security·config — 토큰·내부 API·비밀값 (paper-trading)
<!-- devkit:pointer
path: ^(backend/(trading-api|collector-api)/src/main/.+\.kt|frontend/trading-web/src/.+\.(ts|tsx))$
why: 토큰·비밀값 저장과 서비스 간 내부 API 를 다루는 코드
-->
<!-- devkit:scope narrows SC21, SC26 -->

> 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.

## 옮겨 온 줄 — 옛 하네스(.claude/_disabled·.codex)

- `/api/internal/**` 서비스 간 API 는 외부에 노출하지 않고 외부 API 와 다른 인증을 건다.

## 옮겨 온 줄 — 루트 CLAUDE.md

- Never store tokens in plaintext

## 옮겨 온 줄 — README.md 개발 원칙

- 비밀값은 환경 변수로만 다룹니다.
