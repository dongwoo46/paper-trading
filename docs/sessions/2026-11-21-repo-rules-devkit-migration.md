# 세션 인계 — paper-trading 규칙 이사(devkit 1.2.0 기준) 이어하기

> devkit v1.2.0 · 작성 2026-11-21 · 상태: open
> 🧠 대화 컨텍스트가 살아있는 동안 *메인 세션이 직접* 채운다. **자동 주입되는 내용(CLAUDE.md·rules·훅 주입·메모리)과 파일로 이미 남은 내용은 넣지 않는다** — 다음 세션도 자동으로 받거나 읽을 수 있다. *대화에만 있는* 정보가 우선. 모르면 추측으로 메우지 말고 "미상" + ASK([ask-and-fallback]).
> 📎 **이 일을 구속하는 설계·제안·결정 문서는 주입되지 않는다 — 주입되지 않는 근거 문서는 0절에 가리킨다.** 파일에 있다고 읽히는 게 아니다. 가리키지 않으면 그 조건이 할 일 목록에서 조용히 빠진다.
> 📌 **상태는 3단계** — `open`(대기: 아무도 안 집음) → `in-use(시각 · 세션id)`(어느 세션이 이어받아 진행 중 — 다른 세션은 이 문서를 집지 않는다) → `done(날짜)`(남은 일 전부 끝 → `docs/archive/sessions/` 로 이관). 표시는 손으로 고치지 말고 `session-handoff.js` 의 `take`·`release`·`done` 으로 바꾼다(마커 문법이 곧 잠금이다).

## 0. 근거 문서
- docs/proposals/rule-migration-nested-entry-2026-11-21/why.md — 구속: 하위 입구 8개 정리는 사람 승인(applied, 2026-11-21 팝업)이고 최종 승인은 PR 머지 — 처분: 넘김(§4)
- docs/eval/rule-change/2026-11-21-repo-rules-devkit-migration.md — 구속: 규칙 변경 증거(대조 소실 0, 2차 실행 비교 유지), PR 에서 사람이 검토 — 처분: 넘김(§4)
- docs/eval/trials/rule-migration-ledger-correction-r2.prereg.json — 구속: 없음, 판정 끝(after 3/3 ≥ before 2/3) — 처분: 끝(1ef5f35)
- docs/adr/rule-migration-2026-11-21.md — 구속: 없음, 이사 기록(지운 줄 표·리뷰 뒤 고침) — 처분: 끝(c1bc888·10b4177)
- docs/eval/rule-change/2026-11-22-repo-rules-why-lines-batch1.md — 구속: 묶음 1 규칙 변경 증거(소실 0, 2차 실행 비교 유지), PR #60 에서 사람이 검토 — 처분: 넘김(§4)
- docs/proposals/precommit-build-gate-compile-fail-fronte-551d24/ — 구속: 없음 — 처분: 구속 없음(하네스 자동 감지가 다른 세션의 frontend 빌드 실패 반복으로 만든 제안 초안, 이 일과 무관)
- docs/eval/rule-change/2026-11-21-review-fix-dedupe.md — 구속: 리뷰 뒤 순삭제 28줄 증거, PR #56 에서 사람이 검토 — 처분: 넘김(§2 남음 PR 머지)
- docs/eval/rule-change/2026-11-22-repo-rules-why-lines-batch2.md — 구속: 없음, 묶음 2 규칙 변경 증거(소실 0, 실행 비교 3/3 대 3/3 유지) — 처분: 끝(PR #62 b363640)
- docs/eval/trials/rule-why-lines-batch2-decimal-fee.prereg.json — 구속: 없음, 판정 끝(after 3/3 ≥ before 3/3) — 처분: 끝(f809612)
<!-- 사람 결정 원문은 레포 밖 /Users/dongwoo/.devkit/rule-migrations/paper-trading/drafts/DECISIONS.md(1~17), 절차 기준은 devkit rule-migration 스킬(migrate.md 9~12절) -->

## 1. 목표 (무엇을 만들고 있었나)
- GL1: devkit 팀 규칙 FH7~FH10·DT9 와 겹치는 rules/failure-handling.md 의 PTF1~PTF5 정리 — 사람 «이 레포 rules/failure-handling.md 의 사본 PTF1~PTF5 를 정리해줘» (2026-11-21) (대체: GL2)
- GL2: 이 레포 규칙 전체를 devkit 1.2.0 표준으로 이사 — 사람 «내가 원하는건 규칙 마이그레이션이야 devkit 1.2.0에 맞게» (2026-11-21)

처음 요청은 PTF1~PTF5 정리였고, 사람이 범위를 레포 전체 규칙 이사(rule-migration 스킬 절차)로 넓혔다. 두 번째 세션(2026-11-21 이어받음)에서 사람 질문, 원장, 새 자리 쓰기, 증거, 커밋까지 끝냈고 남은 것은 이사 끝내기·PR·정리다.

## 2. 진행 상태 (기억 아닌 파일·git 대조 후 기록)
- **완료:**
  - 1~4단계: 보관본 /Users/dongwoo/.devkit/rule-migrations/paper-trading/2026-11-21-v3, 정식 원장 같은 폴더 ledger.jsonl(1,959행, 사람 미답 0). 사람 결정 DECISIONS.md 1~17, 줄별 답 drafts/answers-2026-11-21.jsonl.
  - 5~8단계: 규칙 17개 파일·ADR·archive 이동·삭제·AGENTS.md·scope 선언·devkit 제안 번들 5개(레포 밖 ~/.devkit/handoffs/devkit/paper-trading-*, 미제출). check(--proposals) exit 0·실패 0·경고 0(drafts/check6.json 이후). refs: graphify.yaml 만 고침, 시험 입력 4줄 --ignore, 나머지 22건 그대로(사람 답). removals 1,214줄 drafts/removals.md, 전후 표 drafts/after.md.
  - 규칙 변경 증거: 1차 실행 비교 판정 불가(6회 모두 질문하며 멈춤), 2차 before 2/3 · after 3/3 → 유지. 대조 소실 0, rule-change-gate 통과.
  - 커밋(chore/repo-rules-devkit-dedupe, 미 push): 5924bbe·1ef5f35(사전 등록 2개), c4e51fa(이사 본 커밋 136파일), 93a1f8f(test 문서 archive 지시 줄 정리), 97a53a6(graphify 경로).
  - 세 번째 세션(2026-11-21~22): 미룬 299행 해제(fc842d9, 사람 «미루지말고»), finish exit 0, 12단계 재측정(규칙 밖 지시 0·미룬 줄 0·규칙 18개 모두 닿음), 이사 기록 docs/adr/rule-migration-2026-11-21.md(c1bc888), 합동 리뷰 막는 9건 고침(10b4177·914af05·d42552e, 4회차 재리뷰로 머지 판정 «가능»), push·PR #56(https://github.com/dongwoo46/paper-trading/pull/56, base main), 임시 worktree 13개 삭제(사람 확인).
  - 네 번째 세션(2026-11-22): PR #56 머지(08be37e, 사람 «지금 머지», 미추적 인계문서 하나라 DEVKIT_ALLOW_DIRTY_HEAD=1) · chore/rule-migration-prereg 삭제 · 제안 번들 5개 제출(BiocomKR/devkit #2873~#2877, 사람 «5개 모두 제출») · 결함 기록 5건 docs/bug-reports/ + 신호 shard 2026-11-22-9e927734c5d9 커밋 5604698, PR #58(https://github.com/dongwoo46/paper-trading/pull/58).
  - 다섯 번째 세션(2026-11-22 오후): 결함 5건 고침 제안 번들(레포 밖 ~/.devkit/handoffs/devkit/{git-hook-wrapper-upgrade,nested-entry-files-entry-gate,test-gate-doc-under-test-dir,allow-once-new-dir-children,presence-same-host-after-clear}-2026-11-22) 작성·제출 → BiocomKR/devkit #2881~#2885(사람 «5개 모두 제출»). 원인 자리는 플러그인 1.2.0 코드와 devkit development 최신(gh api)에서 확인, 이력 게이트 5건 실행. 보고서 상태 «제안함» 커밋 15cef7c, PR #59(https://github.com/dongwoo46/paper-trading/pull/59, base main).
  - 여섯 번째 세션(2026-11-22 밤): PR #59 머지(a076842, 사람 «지금 머지», 미추적 인계문서 하나라 DEVKIT_ALLOW_DIRTY_HEAD=1 사람 확인). Why 줄 다시 쓰기 범위를 다시 잼: Why 없는 파일은 10개가 아니라 12개, 강조는 11곳이 아니라 7곳. 사람 결정: 4묶음으로 PR 하나씩, 이유 없는 줄은 근거 찾고 못 찾으면 묻기. 묶음 1(architecture·conduct·docs-ops·security-privacy-config·time·workflow)을 form v2 조항 14개로 다시 씀, 증거 docs/eval/rule-change/2026-11-22-repo-rules-why-lines-batch1.md(소실 0, 실행 비교 2차 3/3 대 3/3 유지). 합동 리뷰 3회차: 1회차 막는 지적 12건(핵심: 지운 README «비밀값은 환경 변수로만»을 SC1 이 못 덮음 — SC1 지목 경로가 .kt·.yml 밖) → PTS3 으로 되살림(사람 결정), 2회차 토큰 조항 PTS2 가 서버로 좁아짐 → 원문 범위 되살림(사람 결정), 3회차 SC21 쿠키 자리 구절 더함. 후속 3건 findings(PTC3 재리뷰 문구, PTW3↔PTC2 TODO 충돌, compose 비밀번호 지목 밖). PR #60 머지(da13051, 사람 «지금 머지», CI 초록, head 9254b81 일치, DEVKIT_ALLOW_DIRTY_HEAD=1 — 미추적은 인계문서와 남이 만든 자동 제안 초안 폴더).
  - 일곱 번째 세션(2026-11-22~23): 묶음 2(domains/broker·quant)를 form v2 조항 PTK1~PTK5·PTQ1~PTQ4 로 다시 씀. quant 이유 없는 줄은 팝업 4건으로 사람이 Why 고름(리뷰 대조, 누적 오차, 성과 부풀림, Flyway 미사용 사실). 사전 등록 f809612, 실행 비교 before 3/3 대 after 3/3 유지(과제 cost_profiles.py fill_cost, 천장), 증거 docs/eval/rule-change/2026-11-22-repo-rules-why-lines-batch2.md(소실 0). 합동 리뷰 3회차: 1회차 막는 2건(PTK2 가 429 뒤 «다시 부른다» 더함 → 원문대로 되돌리고 재시도는 devkit LA12 지목, PTK4 가 토스로 좁힘 → «증권사(토스·KIS 등)»), 2회차 막는 1건(LA12 괄호 요약이 4xx 빠뜨림 → 요약 뺌), 3회차 «가능». 후속 7건 findings(PTQ1 시험 방식 좁힘, PTQ2 LEAN 경계 예외, quant Why 경로, PTQ3 주어, PTQ4 수단, 토스 조사 문서 빈 절, broker pointer 가 Kis*.kt 못 닿음). PR #62 머지(b363640, CI 초록, head 362c443 일치, 다른 세션 경유 사용자 지시 «이 세션에서 머지 맡은 건 다 머지해» + 팝업 «CI 초록 뒤 머지»).
  - 여덟 번째 세션(2026-11-23): 묶음 3(design-system PTV1~PTV15, ui-common PTU1~PTU7) 다시 씀 + 사람 결정 둘(quant PTQ2 LEAN Exception, broker pointer Kis 넓힘·토스 전용 조항 표시). 팝업 답: Why 10줄 제안안 채택, RULE-03 그래프 좌표 Exception 더함, 4상태 유지·메타 2줄 삭제. 실행 비교 1차 판정 불가(clone 안 worktree 게이트가 6회 모두 편집 막음, 652f0c4) → 2차 DEVKIT_WORKTREE_EXEMPT=1 등록(38a7d45) before 3/3·after 3/3 유지. 증거 docs/eval/rule-change/2026-11-23-repo-rules-why-lines-batch3.md(rule-change-gate verified, 순삭제 18줄). 본 커밋 b811bab, id 고침 2c45915, PR #64(https://github.com/dongwoo46/paper-trading/pull/64, base main, 리뷰 필수, rule-merge-scope 범위 밖이라 머지는 사람 지시).
- **남음:**
  - PR #64 합동 리뷰(정합성은 띄움, 코드는 맥 부하로 대기) → 막는 지적 고침 → 사람 머지 지시.
  - 이 worktree 는 브랜치 chore/repo-rules-why-lines-batch3 위(이 인계문서 미추적, 하네스 자동 감지가 만든 docs/proposals/precommit-build-gate-compile-fail-fronte-551d24/ 도 미추적 — 이 일과 무관, 손대지 않음).
  - 묶음 4(domains/trading·market-data, 64줄, PRD 본문을 조항과 갈라야 함). 묶음 1과 같은 꼴: 원문 대조 → devkit 중복 판정 → 근거 없는 줄은 묶어서 묻기 → 다시 쓰기 → rule-change-verify(사전 등록 → 실행 비교 → 대조표) → PR.
  - 강조 남은 4곳: data-access.md:54, design-system.md:13, tests.md:28 등(묶음 밖 파일은 손대는 묶음에서 함께).
  - devkit #2881~#2885 는 사람 리뷰·구현 몫 — 고쳐지면 보고서 상태를 «고쳐짐 <해시>» 로.
  - 후속: dead-path 484, docs/done·docs/phase 기록 수확(record-rule-candidates).

## 3. 핵심 결정과 이유
- 1~12: 첫 세션(끝난 기록은 줄 이사에서 뺌, 옛 오케스트레이터 은퇴, 옛 하네스 archive, harness_analysis·HELP 삭제, devkit 모순은 devkit 따름, compileJava 낡음, 제안 10건 번들+사본, 고쳐 쓴 18건 승인).
- 13~17(이 세션): 디자인 시스템 조사 문서째 archive(Blend·Tremor 필요 없음), MockMvc 금지 3줄은 TT9 모순으로 제거, 옛 제목 3줄 제거(판단 위임), 선택지 형식 6줄은 A3 중복, 통째 비교는 사람 결정 제거(위반 464), CODEX.md 6개 → AGENTS.md, 옮기기·지우기 승인.
- 그 뒤 팝업 답: .codex/hooks 스크립트 10개 삭제, 고쳐 쓴 7줄은 원문 human remove + 새 지시, 번호 매긴 40줄 승인, 찌꺼기 정리 승인(빈 제목·CODEX 사본·깨진 가져오기), .gitignore HELP.md 줄은 Spring 생성물이라 둠, 실행 비교 2차 재등록, git 훅 래퍼 1.2.0 으로(사람이 ! 로 직접), 하위 입구 번들 applied 승인, DEVKIT_ALLOW_UNMERGED_PROPOSAL=1 예외로 같은 PR.
- 기각: 사전 등록 없이 판정, --no-verify 커밋, 번들 PR 선머지(이사 게이트가 중간 상태를 막음).

## 4. 다음 첫 행동
**2026-11-23 상태**
이어받기 한 줄: Why 줄 묶음 3 → 묶음 3 리뷰
- 다음 첫 행동: origin/main 에서 새 브랜치(worktree .worktrees/repo-rules-devkit-dedupe)를 따고 묶음 3(rules/design-system.md·ui-common.md, 30줄, 강조 1곳)을 묶음 1·2와 같은 꼴로 다시 쓴다 — 원문 대조, devkit 중복 판정(지울 때 그 devkit 조항 pointer 가 이 레포 파일에 닿는지 확인), 근거 없는 줄은 묶어서 묻기, rule-change-verify(사전 등록 → 실행 비교 → 대조표), PR, 합동 리뷰, CI 초록·head 일치면 머지 — 섬김: GL2
- 같은 PR 에 함께 넣을 사람 결정 둘(2026-11-23 팝업): 1. `rules/domains/quant.md` PTQ2 에 Exception 줄 «LEAN 입출력 경계의 수치 변환(금액은 경계 안쪽에서 Decimal)»을 더한다(근거 `docs/done/quant-worker/backtest-engine/spec.md:63`). 2. `rules/domains/broker.md` pointer path 를 `(Toss|Broker|Kis)\w*\.kt` 로 넓히고, 토스 전용 조항(PTK1·PTK2·PTK3·PTK5)은 토스 코드에만 해당한다고 밝힌다. 둘 다 findings 후속(P6·N2)의 처분이라 기록 행에 fixed 로 닫는다 — 섬김: GL2
- 함정(묶음 2 교훈): 고쳐 쓸 때 원문에 없는 동작(재호출 등)이나 범위 좁힘(특정 증권사로)을 더하지 않는다 — 리뷰 막는 지적 3건이 모두 이것. devkit 조항을 괄호로 요약하지 말고 기호만 지목한다. 출처 커밋은 `git log -S` 로 줄마다 확인(aa6097e·ad2c033 혼동). 증거 문서의 커밋 id 는 rebase 뒤 값으로. review-verdict 는 재리뷰에 «지난 ID | 판정 | 근거» 표, 지적이 없으면 표 안에 «지적 없음» 행이 있어야 읽는다.

**2026-11-22 깊은 밤 상태(대체됨)**
- 이전 다음 첫 행동: 묶음 2(rules/domains/broker.md·quant.md) 다시 쓰기와 리뷰(일곱 번째 세션이 끝냄 — PR #62)

**2026-11-22 밤 상태(대체됨)**
- 이전 다음 첫 행동: PR #60(규칙 Why 줄 묶음 1)에 review-panel 변경분 합동 리뷰(코드, 정합성 — 고쳐 쓴 조항이 원문보다 약해졌는지)를 돌리고(사람 결정 2026-11-22 «리뷰 뒤 머지 판단», 맥 부하 36 아래에서), blocker 는 같은 PR 에서 고친 뒤 사람에게 머지를 묻고, 지시가 있으면 git-conventions 로 머지(pr-head-sync 대조, 미추적 인계문서는 DEVKIT_ALLOW_DIRTY_HEAD=1 사람 확인). 그다음 묶음 2(rules/domains/broker.md·quant.md)를 묶음 1과 같은 꼴로 시작(worktree .worktrees/repo-rules-devkit-dedupe, origin/main 에서 새 브랜치) — 섬김: GL2
- 함정: 맥에 `timeout` 이 없어 실행 비교 스크립트는 `perl -e "alarm 900; exec @ARGV"` 를 쓴다. 과제 대상 파일은 미리 열어 과제가 성립하는지(예: 주석 없는 함수) 확인한다 — 1차가 이것으로 판정 불가였다. 실행 비교 산출은 scratchpad rcv3(휘발, 결과는 증거 문서에 옮김).

**2026-11-22 저녁 상태(대체됨)**
- 이전 다음 첫 행동: 사람에게 PR #59(보고서 상태 5줄) 머지를 묻고, 지시가 있으면 git-conventions 로 머지(pr-head-sync 대조, 미추적 인계문서는 DEVKIT_ALLOW_DIRTY_HEAD=1 사람 확인). 그다음 §2 후속 첫 항목(Why 줄 없는 규칙 10개·CRITICAL 11곳 조항 꼴 다시 쓰기)을 rule-migration 또는 rule-change-verify 로 착수할지 묻는다(worktree .worktrees/repo-rules-devkit-dedupe) — 섬김: GL2

**2026-11-22 오후 상태(대체됨)**
- 결함 5건 고침 제안 제출(다섯 번째 세션이 끝냄 — devkit #2881~#2885, PR #59)

**2026-11-22 오전 상태(대체됨)**
- PR #56 머지 확인 → 번들 제출 → 결함 보고(네 번째 세션이 끝냄)

**2026-11-21 상태(대체됨)**
- 이사 finish → push·PR → 임시 폴더 정리(세 번째 세션이 끝냄)

## 5. 주의·함정
- 이사 진행 표시가 이 worktree 에 켜져 있음 — 커밋마다 이사 check 가 작업트리를 읽는다. 부분 커밋(파일 하나)은 새 규칙 파일이 없는 트리로 판정돼 unknown-clause 로 막힌다 → 사전 등록은 별도 worktree(rule-migration-prereg) 커밋 + 빨리 감기로 넣었다.
- 하네스 결함 4건(bug-report 대상): 1. .git/hooks 래퍼가 devkit 0.95.0 에 박혀 1.2.0 조항(AP10·WI10)을 모름 — 사람이 sed 로 고침. 2. propose-harness-change 는 소비 레포 번들을 레포 밖에 쓰라 하고 dirty-harness 는 같은 커밋 docs/proposals 를 요구, 섞임 게이트는 같은 커밋을 막음, applied 는 trunk 선머지 요구 — 하위 입구 변경 이사가 세 게이트에 연달아 걸림. 3. test-gate isTest 가 `/test/` 폴더의 .md 를 시험으로 봄 — 원문 그대로 이름 바꿈(R100) 커밋 + 다음 커밋 정리로 넘김. 4. session-scope allow-once 가 mkdir 폴더와 파일을 따로 쳐 3회 반복 — rebind 로 풀림(findings 에 기록, docs/findings/shards/2026-11-20-afe4db3a6712.jsonl).
- trial-prereg.js 는 기본 root 가 devkit 플러그인이라 `--root .` 필요.
- 실행 비교 세션은 고영향 설계면 묻고 멈춘다 — 과제 글에 «묻지 말고 추천안으로» 가 필요했다.
- 메인 체크아웃·다른 worktree 의 미커밋은 남의 것 — 건드리지 않음.

## 6. 관련 파일·경로
- worktree: .worktrees/repo-rules-devkit-dedupe (브랜치 chore/repo-rules-why-lines-batch2, PR #62 로 머지됨(b363640) · chore/repo-rules-why-lines-batch1 은 #60(da13051) · chore/bug-reports-proposed-2026-11-22 는 #59 로 머지됨 · 옛 브랜치 chore/repo-rules-devkit-dedupe 는 #56, chore/harness-bug-reports-2026-11-22 는 #58 로 머지됨)
- 레포 밖: /Users/dongwoo/.devkit/rule-migrations/paper-trading/{2026-11-21-v3/, drafts/DECISIONS.md, drafts/answers-2026-11-21.jsonl, drafts/apply-decisions.js, drafts/postprocess.js, drafts/build-step5.js, drafts/removals.md, drafts/after.md, drafts/check6.json}, ~/.devkit/handoffs/devkit/paper-trading-*
- 실행 비교 산출: /private/tmp/claude-501/-Users-dongwoo-Desktop-paper-trading/254aed86-27e2-4eef-83fb-10176019b44b/scratchpad/rcv, rcv2(휘발 — 결과는 증거 문서에 옮겨 둠)

## 7. 검증 상태
- `rule-migration.js check ... --proposals ~/.devkit/handoffs/devkit` → exit 0, 실패 0, 경고 0(2026-11-21, 커밋 전).
- `rule-change-gate.js --evidence docs/eval/rule-change/2026-11-21-repo-rules-devkit-migration.md` → 통과(verified, 순삭제 13줄).
- `trial-prereg.js verify <증거> --root .` → 통과.
- 커밋 게이트: c4e51fa 는 DEVKIT_ALLOW_UNMERGED_PROPOSAL=1(사람 예외)로 통과, 그 밖 게이트 통과. frontend `npm run build` 통과(이 worktree 에 npm ci 함).
- 코드 파일(.kt·.py·.ts) 변경 0. 백엔드 빌드·시험은 미실행(문서·설정만 바뀜).
- 2026-11-22: `finish` exit 0(--ignore docs/findings/shards/2026-11-15-ad4c4c4fc12a.jsonl:4, 사람 결정). 합동 리뷰 사슬 4회차 `review-verdict` → «가능»(막음 0, 미해결 0). 리뷰 뒤 고침은 finish 뒤라 레포 밖 원장 기준 check 가 69건 어긋남(원장은 이사 시점 기록으로 둠, 이사 기록 «리뷰 뒤 고침» 절에 밝힘). 규칙 변경 증거 docs/eval/rule-change/2026-11-21-review-fix-dedupe.md(rule-change-gate verified). QA: source-guards.test.ts 8/8, agents-entry check 최신.
