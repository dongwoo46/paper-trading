# 버그 제보 — 소비 레포 하위 입구 변경 이사가 게이트 넷에 연달아 막힘

> devkit 1.2.0 · 작성 2026-11-22 · 상태: 열림
<!-- 상태 꼴: 열림 · 제안함 <제안 참조> · 고쳐짐 <해시> · 닫힘 주의 <조항 기호>(또는 주의 없음). 근거는 백틱 밖에 쓴다. 바꾸는 때는 bug-report 스킬 «상태 다루기» 절 -->

## 1. 한 줄 요약
하위 입구 파일(CODEX.md 등 8개) 정리를 이사에 넣자, 네 게이트의 요구가 서로 맞물려 정상 경로로는 커밋할 수 없었다.

## 2. 환경 (provenance)
- 레포 / 컨텍스트: paper-trading 규칙 이사(chore/repo-rules-devkit-dedupe, 본 커밋 c4e51fa)
- devkit 버전: 1.2.0
- gitSha: c4e51fa
- 모드 / tier: 미상
- OS / 런타임: macOS

## 3. 재현 절차 (결정적으로)
1. 소비 레포에서 하네스 입구 파일을 바꾸는 이사를 진행한다.
2. propose-harness-change 절차대로 제안 번들을 레포 밖(~/.devkit/handoffs)에 쓴다.
3. 커밋하면 dirty-harness 가 같은 커밋에 docs/proposals 를 요구한다.
4. docs/proposals 를 같은 커밋에 넣으면 섞임 게이트가 막는다.
5. 번들 상태를 applied 로 하면 check-proposal-applied 가 trunk 선머지를 요구한다.

## 4. 기대 동작 vs 실제 동작
- 기대: 승인된 하위 입구 변경을 한 경로로 커밋할 수 있음.
- 실제: 단계마다 다른 게이트가 반대 조건을 요구. 사람 예외 DEVKIT_ALLOW_UNMERGED_PROPOSAL=1 로만 통과(같은 PR).

## 5. 영향 (impact)
- 무엇이 깨지나: 승인된 작업이 예외 변수 없이는 막힘. 예외 사용이 습관이 되면 게이트 의미가 줄어든다.
- 심각도: med

## 6. 근본 원인 (추정 — 딱지)
`추정` 번들 자리(레포 밖 vs 레포 안 docs/proposals)와 적용 시점(선머지 vs 같은 PR)에 대한 판정이 스킬·도구마다 따로 정해져 서로 어긋난다. 근거: 같은 변경이 각 단계에서 다른 요구에 막힌 순서.

## 7. 관련 파일 · 링크
- tools/dirty-harness.js · tools/check-proposal-applied.js · skills/propose-harness-change/SKILL.md
- docs/proposals/rule-migration-nested-entry-2026-11-21/why.md
- 인계문서 §5 2번

## 8. (선택) 수정 제안
소비 레포 하네스 입구 변경에 대한 «번들 자리·적용 시점» 판정을 한 곳에 두고 세 게이트가 그것을 읽게.
