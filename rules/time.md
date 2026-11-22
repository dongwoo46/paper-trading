# Time — 퀀트 Python 의 날짜 타입 (paper-trading)
<!-- devkit:pointer
path: ^backend/quant-[a-z]+/.+\.py$
signal: \bdatetime\b|timedelta|\.date\(\)
why: 퀀트 수집·분석 Python 코드의 date·datetime 다루기
-->
<!-- devkit:scope repo-only -->

> 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.

## 옮겨 온 줄 — docs/before/collector-worker/rules/incident-prevention-rules.md

- datetime.date 객체에 .date() 중복 호출 금지. datetime.timedelta 사용, 타입 명시
