# 버그 제보 — session-scope allow-once 가 mkdir 폴더와 그 안 파일을 따로 쳐 같은 허락을 세 번 반복

> devkit 1.2.0 · 작성 2026-11-22 · 상태: 제안함 BiocomKR/devkit#2884
<!-- 상태 꼴: 열림 · 제안함 <제안 참조> · 고쳐짐 <해시> · 닫힘 주의 <조항 기호>(또는 주의 없음). 근거는 백틱 밖에 쓴다. 바꾸는 때는 bug-report 스킬 «상태 다루기» 절 -->

## 1. 한 줄 요약
별도 worktree 에 폴더를 만들고 파일을 쓰는 작업에서 allow-once 가 폴더 경로와 파일 경로를 다른 대상으로 쳐, 기록·답 순서가 엇갈리며 세 번 되물었다.

## 2. 환경 (provenance)
- 레포 / 컨텍스트: paper-trading 규칙 이사, 사전 등록 커밋용 별도 worktree(rule-migration-prereg)
- devkit 버전: 1.2.0
- gitSha: 5924bbe 직전
- 모드 / tier: 미상
- OS / 런타임: macOS

## 3. 재현 절차 (결정적으로)
1. 세션이 한 체크아웃에 묶인 상태에서 다른 worktree 에 `mkdir -p <폴더>` 뒤 그 폴더에 파일을 쓴다.
2. session-scope-guard 가 allow-once 를 묻는다.
3. 허락 뒤에도 파일 쓰기에서 다시 묻는다. 3회 반복.

## 4. 기대 동작 vs 실제 동작
- 기대: 한 번 허락한 폴더 아래 파일은 같은 허락으로 통과.
- 실제: 폴더와 파일을 따로 판정. rebind 로 풀렸다.

## 5. 영향 (impact)
- 무엇이 깨지나: 같은 승인을 반복 요구(마찰). 안전 문제는 없음.
- 심각도: low

## 6. 근본 원인 (추정 — 딱지)
`추정` allow-once 기록 열쇠가 정확한 경로 문자열이라 상위 폴더 허락이 하위 경로에 안 닿는다(path-normalization-mismatch 성격). 코드는 읽지 않았다.

## 7. 관련 파일 · 링크
- hooks/session-scope-guard.js
- findings docs/findings/shards/2026-11-20-afe4db3a6712.jsonl(id 9bb704e1feb1)

## 8. (선택) 수정 제안
mkdir 로 허락한 폴더는 그 아래 경로까지 같은 allow-once 로 덮는다.
