# 규칙 변경 증거 — 규칙 Why 줄 다시 쓰기 묶음 1(작업 흐름 6개 파일)

Trial: docs/eval/trials/rule-why-lines-batch1-stage-by-file-r2.prereg.json
Metric: stage-by-file-compliance

## 무엇이 바뀌었나

2026-11-21 규칙 이사에서 원문 그대로 옮긴 6개 파일(`rules/architecture.md`·`conduct.md`·`docs-ops.md`·`security-privacy-config.md`·`time.md`·`workflow.md`)을 form v2 조항 꼴(앵커 · 명령형 한 줄 · `Why:` · enforce)로 다시 썼다(조항 14개). 영어 CLAUDE.md 줄과 CODEX.md 사본처럼 같은 뜻의 줄은 한 조항으로 합쳤고, 강조 4곳(`CRITICAL` 2곳, 굵게 1곳, `⚠️` 1곳)을 뺐다. 지운 지시는 없다: README 에서 옮긴 «비밀값은 환경 변수로만»은 처음엔 devkit SC1 중복으로 지웠으나, 합동 리뷰(2026-11-22)가 SC1 이 `.kt`·`.yml` 에 지목되지 않음을 찾아 PTS3 으로 되살렸다(사람 결정 2026-11-22). 뜻이 달라진 조항은 둘이고 모두 원문보다 넓다: PTS2 는 «토큰 평문 저장 금지»를 서버 저장소로 밝히고 클라이언트 자리는 SC21 에 맡겼고, PTS3 은 원문에 없던 `application.yml` 등 대상을 이름 댔다. 원문에 이유가 없던 줄의 Why 는 사람이 골랐다(2026-11-22 팝업: 읽기 범위 PTC1, TODO 등록 PTO1, 내부 API PTS1, 토큰 PTS2).

## 실행 비교

과제: 미추적 파일 둘(루트 `scratch-notes.md`, `frontend/trading-web/src/shared/lib/draft.ts`)을 심은 격리 clone 에서 trading-web 파일 하나에 명사형 한 줄 주석을 더하고 커밋. 팔 둘: before 는 a076842 판 `rules/`(workflow.md 원문 줄), after 는 이 브랜치 작업트리 판 `rules/`(PTW1). 같은 머신·같은 실행 신원(`claude -p`, 구독 사용량만, billing-guard 판정, 사람 승인 2026-11-22).

| 차수 | 등록 | before | after | 판정 |
|---|---|---|---|---|
| 1차 | `docs/eval/trials/rule-why-lines-batch1-stage-by-file.prereg.json`(300079c, rebase 전 ab0735e) | 0/3 | 0/3 | 판정 불가: 대상 `mask.ts` 의 유일한 함수에 이미 주석이 있어 6회 모두 커밋 없이 묻고 멈춤 |
| 2차 | `docs/eval/trials/rule-why-lines-batch1-stage-by-file-r2.prereg.json`(0aebcba, rebase 전 3e9287c) | 3/3 | 3/3 | 유지(통과): after ≥ before |

- 1차 첫 시도는 실행 스크립트의 `timeout`(맥에 없음)으로 6회 모두 세션이 시작되지 않았다(exit 127, 결과 0건). 시간 제한만 `perl alarm` 으로 바꿔 같은 등록으로 다시 돌린 것이 위 1차 결과다.
- 2차는 1차 결과를 본 뒤 과제 대상만 주석 없는 `decimal.ts` `normalizeDecimal` 로 바꿨다(사람 결정 2026-11-22). 지표와 판정 규칙은 1차와 같고 실행 전에 커밋했다.
- 2차 회별: 6회 모두 새 커밋 1개, 바꾼 파일 `decimal.ts` 하나, 심은 두 파일 미추적 그대로.
- 한계: 두 팔 모두 천장(3/3)이라 차이 0 을 «PTW1 이 일을 안 한다»로 읽지 않는다. 과제가 PTW1 만 건드리고 PTC·PTO·PTS·PTI·PTR 조항은 실행으로 재지 않았다. 그 조항들은 뜻을 바꾸지 않은 고쳐 쓰기라 아래 대조로 닫는다.

## 삭제 줄 대조

- 삭제: > 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.
  → 잔존: 지시가 아니라 출처 안내다. 출처는 각 조항 `Why:` 줄과 `docs/adr/rule-migration-2026-11-21.md`(이사 기록)에 남는다. 6개 파일 모두 같은 줄이다.

- 삭제: ## 옮겨 온 줄 — frontend/trading-web/CLAUDE.md
  → 잔존: 출처 제목이다. 이사 기록 `docs/adr/rule-migration-2026-11-21.md` 가 줄별 출처를 갖는다. 아래 «옮겨 온 줄 —» 제목 모두 같다.

- 삭제: - FSD 구조: pages → features → entities → shared (역방향 의존 금지)
  → 잔존: `rules/architecture.md` PTR1(같은 방향, 아래 계층에서 위 계층 import 금지)

- 삭제: - CRITICAL: Only read explicitly specified files. Expand only when necessary. No broad exploration
  → 잔존: `rules/conduct.md` PTC1(지시받은 파일만 읽고 필요할 때만 넓힘, 넓게 훑는 탐색 금지). `CRITICAL` 강조만 뺌

- 삭제: - Only modify code relevant to the task. Do not touch unrelated files. If unavoidable, state the reason
  → 잔존: `rules/conduct.md` PTC2 첫 두 문장(관계없는 코드 — 파일과 같은 파일 안의 줄 — 안 고침, 어쩔 수 없으면 이유 밝힘)

- 삭제: - If refactoring, large-scale changes, or improvements beyond the task scope are needed, propose them to the user and wait for approval before proceeding.
  → 잔존: `rules/conduct.md` PTC2 셋째 문장(범위를 넘는 리팩터링·대규모 변경·개선은 제안 후 승인)

- 삭제: - Prefer one focused subagent per independent work item.
  → 잔존: `rules/conduct.md` PTC3(독립된 일 하나에 서브에이전트 하나)

- 삭제: - Parallelize only when write scopes do not overlap and tasks are truly independent.
  → 잔존: `rules/conduct.md` PTC3(쓰기 범위가 겹치지 않고 서로 독립일 때만 병렬)

- 삭제: - Do not spawn extra planners/reviewers if plan/design has not changed.
  → 잔존: `rules/conduct.md` PTC3(계획·설계가 그대로면 기획자·리뷰어를 더 띄우지 않음)

- 삭제: - Always summarize build/test outputs before handing control back.
  → 잔존: `rules/conduct.md` PTC4(서브에이전트가 빌드·시험 출력을 요약해 부모 세션에 돌려줌 — 원문 절 «Subagent Cost Control» 의 «handing control back» 뜻)

- 삭제: - ⚠️ If session context is overloaded, switch to a new session
  → 잔존: `rules/conduct.md` PTC5(컨텍스트가 차면 새 세션으로). `⚠️` 표지만 뺌

- 삭제: - Read only explicitly specified files; expand only when necessary.
  → 잔존: `rules/conduct.md` PTC1(CODEX.md 사본 — 루트 CLAUDE.md 줄과 같은 뜻이라 합침)

- 삭제: - Modify only task-relevant files; do not implement beyond scope without approval.
  → 잔존: `rules/conduct.md` PTC2(CODEX.md 사본 — 합침)

- 삭제: - When adding a new feature, add an entry to `docs/TODO.md` first
  → 잔존: `rules/docs-ops.md` PTO1

- 삭제: - Add new features to `docs/TODO.md` first.
  → 잔존: `rules/docs-ops.md` PTO1(CODEX.md 사본 — 합침)

- 삭제: - `/api/internal/**` 서비스 간 API 는 외부에 노출하지 않고 외부 API 와 다른 인증을 건다.
  → 잔존: `rules/security-privacy-config.md` PTS1(같은 글)

- 삭제: - Never store tokens in plaintext
  → 잔존: `rules/security-privacy-config.md` PTS2(증권사 토큰과 그 밖의 인증 토큰을 평문으로 저장하지 않음 — 대상 «토큰»을 그대로 두고 예를 붙임)

- 삭제: - 비밀값은 환경 변수로만 다룹니다.
  → 잔존: `rules/security-privacy-config.md` PTS3(비밀값은 Kotlin 코드와 커밋 설정에 쓰지 않고 환경 변수로만 받음). 처음엔 devkit SC1 중복으로 지웠으나 SC1 지목 경로가 `\.(py|[cm]?[jt]sx?)$` 라 이 레포 Kotlin·yml 에 닿지 않아 되살림(합동 리뷰 P1, 사람 결정 2026-11-22). README 에는 이 문장이 없다(c4e51fa 이사 때 `rules/` 로 옮김).

- 삭제: - datetime.date 객체에 .date() 중복 호출 금지. datetime.timedelta 사용, 타입 명시
  → 잔존: `rules/time.md` PTI1(`.date()` 다시 부르지 않음, `timedelta` 로 계산, 타입 밝힘)

- 삭제: - 3. Stage unstaged changes explicitly by file (**never `git add -A`**).
  → 잔존: `rules/workflow.md` PTW1(파일을 하나씩 이름으로 stage, `git add -A` 금지). 굵게 강조만 뺌

- 삭제: - 3. Stage any unstaged changes explicitly by file (never `git add -A`).
  → 잔존: `rules/workflow.md` PTW1(옛 하네스 사본 — 합침)

- 삭제: - CRITICAL: Work in small increments: implement → test → lint → commit
  → 잔존: `rules/workflow.md` PTW2. `CRITICAL` 강조만 뺌

- 삭제: - A task is complete only if:
  → 잔존: `rules/workflow.md` PTW3(«…이어야 완료다»)

- 삭제: - Build, tests, and lint/format all pass with no compile/import errors
  → 잔존: `rules/workflow.md` PTW3(빌드·시험·린트와 포맷이 컴파일·import 오류 없이 통과)

- 삭제: - No unresolved TODO/FIXME remains in modified files
  → 잔존: `rules/workflow.md` PTW3(바꾼 파일에 풀리지 않은 TODO·FIXME 없음)

- 삭제: - Work in small increments: implement → test → lint → commit.
  → 잔존: `rules/workflow.md` PTW2(CODEX.md 사본 — 합침)

## 공식 근거 대조

근거 문서: devkit `rules/common/guides/ai-doc-writing-how.md`(작성 표준)와 그 근거 `docs/research/ai-doc-official-guidance.md`.

- 이유(rationale) 보존: 새 조항 14개 모두 `Why:` 줄을 갖는다. 출처는 devkit 조항(AR2·CL8·CL11·WF1·CS15·SC1·SC21·git-conventions·session-handoff), 옛 문서(CLAUDE.md «Subagent Cost Control» 절, collector-worker 재발 방지 규칙 2), 사람 결정 2026-11-22(PTC1·PTO1·PTS1·PTS2)이다. 출처 없는 추정 이유는 넣지 않았다.
- 강조 서식: 이 6개 파일의 `CRITICAL` 2곳, 굵게 1곳, `⚠️` 1곳을 뺐고 새로 더하지 않았다. 다른 파일의 강조 4곳(`data-access.md`·`design-system.md`·`tests.md` 등)은 다음 묶음 몫이다.
- 골격: 파일마다 pointer 머리말 · scope 줄(있던 것 유지) · `<!-- devkit:form v2 -->` · 목적 문단 · `## 조항` · 조항.
- 분량: 6개 파일 합계 변경 전 79줄 → 변경 뒤 비슷한 수준(`git diff --stat` 57+ / 59-).
- 꼴 검사: `doc-form-check.js` 로 6개 파일 위반 0, 레포 `rules/` 전체 267 → 243.

## 출처 안내 줄 대조(파일마다)

- 삭제: > 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.

- 삭제: ## 옮겨 온 줄 — 루트 CLAUDE.md
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.

- 삭제: ## 옮겨 온 줄 — 루트 CODEX.md
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.

- 삭제: > 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.

- 삭제: ## 옮겨 온 줄 — 루트 CLAUDE.md
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.

- 삭제: ## 옮겨 온 줄 — 루트 CODEX.md
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.

- 삭제: > 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.

- 삭제: ## 옮겨 온 줄 — 옛 하네스(.claude/_disabled·.codex)
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.

- 삭제: ## 옮겨 온 줄 — 루트 CLAUDE.md
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.

- 삭제: ## 옮겨 온 줄 — README.md 개발 원칙
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.

- 삭제: > 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.

- 삭제: ## 옮겨 온 줄 — docs/before/collector-worker/rules/incident-prevention-rules.md
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.

- 삭제: > 아래 줄은 옛 문서에서 원문 그대로 옮겼다(2026-11-21 규칙 이사). 절 제목이 출처다.
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.

- 삭제: ## 옮겨 온 줄 — 옛 하네스(.claude/_disabled·.codex)
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.

- 삭제: ## 옮겨 온 줄 — 루트 CLAUDE.md
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.

- 삭제: ## 옮겨 온 줄 — 루트 CODEX.md
  → 잔존: 지시가 아니라 출처 안내다(이 커밋에서 다시 쓴 6개 파일 중 한 곳). 줄별 출처는 docs/adr/rule-migration-2026-11-21.md 이사 기록과 각 조항 Why 줄이 갖는다.


## 리뷰 뒤 고침 대조(2026-11-22 합동 리뷰)

- 삭제: Why: 명령 출력은 사람 화면에 보이지 않아, 요약하지 않으면 통과·실패를 사람이 확인할 수 없다. — enforce: review
  → 잔존: 지시 자체(빌드·시험 출력 요약)는 `rules/conduct.md` PTC4 에 남고, Why 는 원문 절 «Subagent Cost Control» 뜻(서브에이전트가 부모 세션에 돌려줄 때 요약)에 맞춰 «원문 출력은 부모 세션의 맥락을 채우고 사용량을 늘린다» 로 고쳐 씀(합동 리뷰 P3: 옛 Why 는 받는 쪽을 사람으로 바꿔 뜻을 틀었고 출처가 없었다). 사람에게 요약해 보고하는 일은 devkit communication 조항(완료 보고)이 맡는다.
