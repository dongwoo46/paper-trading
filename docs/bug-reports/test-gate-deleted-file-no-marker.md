# 버그 제보 — 시험 약화 게이트가 파일째 삭제된 시험에 근거 자리를 주지 않음

> devkit v0.95.0 · 작성 2026-11-15 · 상태: open

## 1. 한 줄 요약
`git-precommit` 시험 약화 게이트는 `devkit:test-change` 근거를 약화된 시험 파일 안의 추가 줄에서만 받는다. 그래서 파일째 삭제된 시험은 정식 통과 경로가 없다.

## 2. 환경 (provenance)
- **레포 / 컨텍스트:** paper-trading, worktree `trading-web-shadcn-rebuild`, step-1 교체 골격
- **devkit 버전:** v0.95.0 (`update-check.js` 무음, exit 0)
- **gitSha:** c0e18a6 (커밋 시도 직전 HEAD)
- **모드 / tier:** 미상
- **OS / 런타임:** macOS, Node 26

## 3. 재현 절차 (결정적으로)
1. 시험 파일이 있는 앱 폴더를 `git rm -r` 로 통째 삭제한다(이번: 시험 34건 포함 146개 파일).
2. 같은 커밋에 새 앱과 새 시험을 stage 한다.
3. 커밋 메시지에 `devkit:test-change <근거>` 를 넣고 `git commit` 한다.

## 4. 기대 동작 vs 실제 동작
- **기대:** 사람이 승인한 삭제는 근거를 남기고 통과할 길이 있어야 한다. 시험 대상 코드까지 함께 지운 경우는 약화로 세지 않아도 된다.
- **실제:** `✖ [devkit] 기존 테스트 약화 34건 — 차단(테스트가 기준)`. 커밋 메시지와 `.md` 근거는 인정되지 않는다. 삭제된 파일에는 추가 줄을 넣을 수 없어 통과할 방법이 없다.

## 5. 영향 (impact)
- **무엇이 깨지나 / 누가 영향:** 승인된 전면 재작성·모듈 삭제 커밋은 AI가 만들 수 없고, 사람이 `--no-verify` 로 모든 훅을 건너뛰게 된다. 그러면 빌드·비밀값 검사까지 함께 꺼진다.
- **심각도:** med

## 6. 근본 원인 (추정 — 딱지)
- `코드` `tools/git-precommit.js` 501~520행: `testChangeReason(stagedDiff(), { files: weakened.map((w) => w.file) })` 로 근거를 약화된 파일 범위에서만 찾는다(2026-07-23 자기 우회 방지).
- `코드` `tools/test-gate.js:496` `markerReason`, `:520` `testChangeReason`: 그 파일들의 diff 추가 줄에서만 마커를 찾는다.
- `추정` 파일 삭제는 추가 줄이 0이라, 약화 분류 `weakenedTests` 가 삭제 파일을 포함하는 순간 통과 경로가 없어진다.

## 7. 관련 파일 · 링크
- `tools/git-precommit.js:501-520`, `tools/test-gate.js:496,520`
- `docs/features/running/trading-web-shadcn-rebuild/steps/step-1.md` §8

## 8. (선택) 수정 제안
- 삭제된 시험 파일이 import 하던 시험 대상 소스도 같은 커밋에서 삭제됐으면 약화로 세지 않는다.
- 또는 파일 전체 삭제에 한해, 같은 커밋에 새로 추가된 시험 파일 안의 `devkit:test-change` 줄을 근거로 인정한다.
