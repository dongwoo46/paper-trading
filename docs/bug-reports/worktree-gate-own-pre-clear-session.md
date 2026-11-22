# 버그 제보 — /clear 뒤 같은 세션이 앞 세션 id 의 점유 표시를 남의 것으로 봄

> devkit 1.2.0 · 작성 2026-11-22 · 상태: 제안함 BiocomKR/devkit#2885
<!-- 상태 꼴: 열림 · 제안함 <제안 참조> · 고쳐짐 <해시> · 닫힘 주의 <조항 기호>(또는 주의 없음). 근거는 백틱 밖에 쓴다. 바꾸는 때는 bug-report 스킬 «상태 다루기» 절 -->

## 1. 한 줄 요약
인계를 take·confirm 한 세션인데도 worktree-gate 가 /clear 이전 세션 id(254aed86)의 신선한 표시를 co-occupied 로 보고 Edit 을 2회 거부했다.

## 2. 환경 (provenance)
- 레포 / 컨텍스트: paper-trading 규칙 이사 마무리, worktree .worktrees/repo-rules-devkit-dedupe
- devkit 버전: 1.2.0
- gitSha: 미상
- 모드 / tier: 미상
- OS / 런타임: macOS · Claude Code

## 3. 재현 절차 (결정적으로)
1. 세션 A 가 worktree 에서 편집해 점유 표시를 남긴다.
2. 같은 창에서 /clear 해 세션 id 가 B 로 바뀐다.
3. B 가 인계를 take·confirm 한 뒤 같은 worktree 를 Edit 한다.
4. 표시 만료(약 10분) 전이면 거부된다.

## 4. 기대 동작 vs 실제 동작
- 기대: 같은 프로세스·창에서 이어진 세션, 또는 그 worktree 인계를 잡은 세션은 자기 것으로 침.
- 실제: 남의 세션으로 쳐서 만료까지 약 4분 대기.

## 5. 영향 (impact)
- 무엇이 깨지나: /clear 뒤 이어받기마다 편집이 잠시 막힘(마찰).
- 심각도: med

## 6. 근본 원인 (추정 — 딱지)
`추정` 점유 판정이 세션 id 하나만 보고, /clear 로 id 가 바뀌는 경우를 같은 주인으로 잇는 근거(인계 점유·부모 프로세스)를 안 본다(session-ownership-misattributed 성격).

## 7. 관련 파일 · 링크
- hooks/worktree-gate.js
- findings docs/findings/shards/2026-11-21-cfafe71e2014.jsonl(id ae40b10a99e1)

## 8. (선택) 수정 제안
이 worktree 인계를 in-use 로 쥔 세션은 앞 세션 표시를 넘겨받게 하거나, /clear 전후 id 를 잇는 기록을 둔다.
