# 진행 — backtest-multifactor-kr-investor-flow

> **이 기능 하나의 현황 + 구현 체크리스트.** (옛 전체 PROGRESS 보드를 *기능별*로 쪼갠 것 — 각자 자기 것만.)
> **점유(누가 잡고 있나)는 이 문서에 없다** — 레포 밖 런타임 저장소가 갖는다(`node tools/feature-claim.js` · 한 기능 = 한 세션은 `session-scope` 훅이 그걸 보고 지킨다). 점유는 같은 기계·같은 시간대에서만 의미가 있어 git 에 들어갈 값이 아니다.

## 현황

| 항목 | 값 |
|---|---|
| 상태 | 🟡 진행중 |
| 단계 | — |
| 브랜치 | feature/backtest-multifactor-kr-investor-flow |
| worktree | /Users/dongwoo/Desktop/paper-trading/.worktrees/backtest-multifactor-kr-investor-flow |
| 갱신 | 2026-08-15T18:08:03.105Z |

> `상태`·`단계` = 구현 착수 시 `step-scaffold` 가 만드는 `state.md` 에서 자동 동기화(progress-sync 훅) · `브랜치`·`worktree`·`갱신` = 작업 시작 시 자동 기록(session-scope 훅). 손으로 안 채워도 됨.
>
> ⚠ **`상태` 칸은 고정 어휘만** — `시작전` · `설계만` · `진행중` · `막힘` · `완료`(개발 끝, done 이동 대기) · `중단`(접음). 손으로 산문을 적지 말고 `node tools/progress-board.js set <기능> <상태>` 로 바꾼다(정확일치만 — 「거의 완료」 같은 산문은 거부된다). 릴리즈 게이트([`feature-status`](../../../../tools/feature-status.js))가 이 칸을 *선언*으로 읽어 `완료`·`중단`을 세우기 때문 — 자유 문장이면 「어휘아님」으로 릴리즈가 멈춘다. 맥락·단서는 아래 본문에 얼마든 적는다.
>
> **종결(`완료`·`중단`)은 사람만 내린다** — 자동 스탬프(세션 점유·state.md 동기화)는 진행 상태만 쓰고 종결 선언을 덮지 않는다. step 이 다 끝난 것과 「이 기능 끝」은 다른 판단이다(PR 머지·검증이 남는다).

## 완료

- [ ] (예) 시안 분석 · 디자인 토큰 매핑

## 진행중

- [ ] (예) 권한 로직 검토

## 대기

- [ ] (예) 노드값 vs 구현 대조 검증
