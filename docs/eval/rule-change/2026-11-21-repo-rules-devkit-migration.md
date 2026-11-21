# 규칙 변경 증거 — paper-trading 규칙 이사(devkit 1.2.0)

Trial: docs/eval/trials/rule-migration-ledger-correction-r2.prereg.json
Metric: ledger-reversal-compliance

## 무엇이 바뀌었나

레포 규칙 전체를 devkit 1.2.0 표준 자리로 옮겼다(rule-migration 스킬). 원장은 레포 밖 `~/.devkit/rule-migrations/paper-trading/2026-11-21-v3/ledger.jsonl`(1,959행)이고 `rule-migration.js check --proposals` 는 exit 0(실패 0, 경고 0)이다. 사람 결정 원문은 같은 폴더 `drafts/DECISIONS.md` 1~17. 이 문서는 `rules/` 에서 순수하게 지운 줄 21개(같은 커밋 추가 줄에 그대로 살아 있는 줄은 제외)의 대조와, 지운 PTF1~PTF4 의 대표 조항(원장 정정)에 대한 실행 비교를 담는다.

## 실행 비교

과제: «trading-api 에 잘못 기록된 계좌 원장(AccountLedger) 항목의 금액을 정정하는 기능 추가 — 애플리케이션 서비스 메서드까지, 빌드·시험 실행 없음». 팔 둘 — before 는 07b3a51 판 규칙(rules/failure-handling.md PTF1 원문 있음), after 는 이사 뒤 작업트리 판(PTF1 지움, 머리말이 devkit FH7 을 가리킴). 격리 worktree 6개, 같은 머신·같은 실행 신원(`claude -p`, 구독 사용량만 — billing-guard 판정, 사람 승인 2026-11-21).

| 차수 | 등록 | before | after | 판정 |
|---|---|---|---|---|
| 1차 | `docs/eval/trials/rule-migration-ledger-correction.prereg.json` | 0/3 | 0/3 | 판정 불가 — 6회 모두 정정 방식을 사람에게 묻고 코드를 안 바꿈 |
| 2차 | `docs/eval/trials/rule-migration-ledger-correction-r2.prereg.json` | 2/3 | 3/3 | 유지(통과) — after ≥ before |

- 2차는 1차 결과를 본 뒤 과제 글만 고쳤다(«정정 방식은 묻지 말고 추천안으로 바로 구현, 대상은 입금·출금 원장만»). 지표와 판정 규칙은 1차와 같고, 실행 전에 커밋했다(1ef5f35).
- 2차 회별: before b1 0(대입 0·삭제 0·생성 0), b2 1, b3 1 · after a1 1, a2 1, a3 1. 6회 모두 기존 원장 행 금액 대입과 삭제는 0이다.
- b1 은 `Account.recordLedgerCorrection(...)` 으로 새 원장 행을 저장해 행을 고치지 않았지만, 등록 지표 (c) 정규식이 `AccountLedger.` 호출만 세므로 등록대로 0 이다. 1 로 쳐도 3 ≥ 3 이라 판정은 같다.
- 사후 관찰(판정 근거 아님): 1차에서 6회 모두 반대 행·차액 정정 행을 추천했고, after 3회는 FH7 을 직접 언급했으며 before 3회는 PTF1 을 언급하지 않았다.
- 한계: 과제가 FH7(옛 PTF1)만 건드린다. FH8~FH10(옛 PTF2~PTF4)과 PTB3(→DD15)는 실행으로 재지 않았고 아래 대조로만 닫았다.

## 삭제 줄 대조

- 삭제: > devkit 은 이 레포의 Kotlin 파일에 팀 규칙을 자동으로 전달하지 않는다(rule-pointer 실측 2026-11-15: `always:dev-team` 만 나온다). 그래서 이 레포 Kotlin 작업·리뷰는 팀 `backend-api.md` 의 DD9~DD14(PTB1·PTB2·PTB3·PTB5·PTB7~PTB9 가 이 레포 형태로 적음)와, 이 문서가 다시 쓰지 않는 DD7(컨텍스트 사이는 명시 계약으로만)·AE5·AE8(예외→HTTP 응답은 전역 핸들러 한 곳, 예외 원문을 응답에 싣지 않음)을 직접 열어 따른다. devkit 쪽 Kotlin 신호 추가는 devkit 에 제안으로 올린다.
  → 잔존: `rules/backend-api.md`·`rules/failure-handling.md` 머리말 둘째 인용 문단(실측 날짜를 2026-11-21·devkit 1.2.0 으로 고쳐 씀, «직접 열어 따른다» 지시와 가리키는 조항 목록 유지 — 더 늘어남)

- 삭제: > 금액 규칙의 상위 원칙은 루트 `CLAUDE.md` «Financial Safety»(금액에 부동소수점 금지, Kotlin 은 BigDecimal).
  → 잔존: `rules/domains/trading.md` 의 금액·상태 전이 조항(루트 CLAUDE.md «Financial Safety» 가 옮겨 간 자리)과 devkit `backend-api.md` DD13(금액을 float 로 두지 않음) — `rules/backend-api.md` 머리말이 DD13 을 직접 가리킴

- 삭제: Why: 팀 규칙 DD10(빈약한 모델 금지)·DD11(도메인 로직 누수 금지)의 Kotlin 형태. Kotlin `var` 는 기본이 public setter 라 서비스가 전이 검사를 건너뛰고 상태를 바꿀 수 있고, 루트 `CLAUDE.md` 의 «상태 전이는 명시적»이 깨진다. — enforce: review
  → 잔존: 같은 파일 PTB2 의 Why 줄(«루트 `CLAUDE.md` 의 «상태 전이는 명시적»» 을 «`rules/domains/trading.md` 의 «상태 전이는 명시적»» 으로 고쳐 씀 — 근거 자리만 바뀜)

- 삭제: <!-- gt:PTB3 -->
  → 잔존: devkit `rules/dev-team/backend-api.md` DD15(다른 애그리거트는 id 로만 참조) — 원장 duplicate 행, `rules/backend-api.md` 머리말이 DD15 를 직접 가리킴

- 삭제: Why: 팀 규칙 DD12(한 트랜잭션은 한 애그리거트)를 지키는 구조. 애그리거트를 객체로 이으면 한 트랜잭션이 여러 애그리거트를 읽고 잠근다(devkit 제안 BiocomKR/devkit#2471 의 DD15 와 같은 내용 — 그 제안이 머지되면 이 조항은 지운다). — enforce: review
  → 잔존: devkit `rules/dev-team/backend-api.md` DD15 의 Why(제안 BiocomKR/devkit#2471 이 머지돼 devkit 1.2.0 에 들어옴 — 이 조항이 «머지되면 지운다» 고 적은 조건이 충족됨)

- 삭제: # Failure handling — 원장·대사·불확정 응답 (paper-trading)
  → 잔존: 같은 파일 제목 «# Failure handling — 시간 경계·예수금 홀드 (paper-trading)» (남은 조항에 맞춰 고쳐 씀)

- 삭제: > 이 레포 전용 사본. 이 문서 조항 가운데 PTF1~PTF5 는 스택 무관이라 devkit 팀 규칙 제안으로도 올렸고(PTF1~PTF3 은 제안 financial-integrity-eda-followup-2026-11-15, PTF4·PTF5 는 BiocomKR/devkit#2471 2차), 그 제안이 머지되면 PTF1~PTF5 는 지운다. 다만 PTF5 의 이산 날짜·연도 닫힌 구간 예외는 제안 판본에 없으면 지우지 않고 남긴다. PTF6(예수금 홀드를 요청 경로에서)은 어느 제안에도 없는 이 레포 조항이라 남긴다. 멱등 쓰기·소비자 중복 제거의 원칙은 팀 규칙 `failure-handling.md`·`data-access.md` DA9 가 SSoT.
  → 잔존: 같은 파일 머리말 첫 인용 문단(PTF1~PTF4 는 devkit FH7~FH10 이 SSoT 라 지웠고 PTF5 는 DT9 에 맡기고 레포 고유 부분만, PTF6 은 레포 조항 — 사람 결정 2026-11-21 DECISIONS 9·12)

- 삭제: > devkit 은 이 레포의 Kotlin 파일에 팀 규칙을 자동으로 전달하지 않는다(rule-pointer 실측 2026-11-15: `always:dev-team` 만 나온다). 그래서 주문·결제성 쓰기를 만지는 Kotlin 작업·리뷰는 팀 `failure-handling.md` FH1(생성 요청은 `Idempotency-Key` 나 자연 키 upsert)과 `async-external.md` LA11(원격 호출 연결·읽기 타임아웃 명시)을 직접 열어 따른다. devkit 쪽 Kotlin 신호 추가는 devkit 에 제안으로 올린다.
  → 잔존: `rules/backend-api.md`·`rules/failure-handling.md` 머리말 둘째 인용 문단(실측 날짜를 2026-11-21·devkit 1.2.0 으로 고쳐 씀, «직접 열어 따른다» 지시와 가리키는 조항 목록 유지 — 더 늘어남)

- 삭제: ## 원장
  → 잔존: devkit `rules/dev-team/failure-handling.md` FH7 절 — 절 제목, 조항은 FH7 이 SSoT

- 삭제: <!-- gt:PTF1 -->
  → 잔존: devkit `rules/dev-team/failure-handling.md` FH7(원장 행은 UPDATE·DELETE 하지 않고 반대 행으로 정정) — 머리말이 FH7 을 직접 가리킴

- 삭제: Why: 고쳐 쓰면 감사에 필요한 흔적이 사라지고 대사가 원장으로 잔고를 다시 계산할 수 없다. 루트 `CLAUDE.md` «모든 금융 상태 변경은 감사·추적 가능» 의 구조다(토스·우아한형제들 공개 사례). — enforce: review
  → 잔존: devkit `rules/dev-team/failure-handling.md` FH7 의 Why

- 삭제: ## 대사
  → 잔존: devkit `rules/dev-team/failure-handling.md` FH8 절 — 절 제목

- 삭제: <!-- gt:PTF2 -->
  → 잔존: devkit `rules/dev-team/failure-handling.md` FH8(파생 잔고·포지션 주기 대사) — 머리말이 FH8 을 직접 가리킴

- 삭제: Why: 중복되거나 유실된 이벤트(Redis Pub/Sub 유실 포함, `async-external.md` PTA2)는 잔고를 조용히 어긋나게 하고 아무 시험도 그것을 잡지 못한다(토스 공개 사례). — enforce: review
  → 잔존: devkit `rules/dev-team/failure-handling.md` FH8 의 Why, Redis Pub/Sub 유실은 `rules/async-external.md` PTA2 에 그대로 있음

- 삭제: ## 불확정 응답
  → 잔존: devkit `rules/dev-team/failure-handling.md` FH9 절 — 절 제목

- 삭제: <!-- gt:PTF3 -->
  → 잔존: devkit `rules/dev-team/failure-handling.md` FH9(불확정 응답은 UNKNOWN 으로 두고 확정 뒤 재시도) — 머리말이 FH9 를 직접 가리킴

- 삭제: Why: 타임아웃은 실패가 아니다. 상대가 처리를 끝냈을 수 있어 실패로 보고 재시도하면 이중 주문·이중 체결이 난다. 팀 규칙 DA9(의도 기록 → 트랜잭션 밖 호출 → 결과 반영)의 결과 반영 단계를 강화한다(카카오페이·토스 공개 사례). — enforce: review
  → 잔존: devkit `rules/dev-team/failure-handling.md` FH9 의 Why 와 `data-access.md` DA9

- 삭제: ## 계산 근거와 시간 경계
  → 잔존: 같은 파일 «## 시간 경계» 절(PTF5 가 남은 절 — 제목만 줄임)

- 삭제: <!-- gt:PTF4 -->
  → 잔존: devkit `rules/dev-team/failure-handling.md` FH10(계산 금액에 요율·정책 버전 저장) — 머리말이 FH10 을 직접 가리킴

- 삭제: Why: 요율은 바뀐다. 과거 체결을 오늘 요율로 다시 계산하면 이력이 조용히 바뀌고 대사(PTF2)가 거짓 불일치를 낸다(토스 공개 사례). — enforce: review
  → 잔존: devkit `rules/dev-team/failure-handling.md` FH10 의 Why

- 삭제: Why: `BETWEEN` 은 양끝을 포함해 경계 시각의 체결이 두 구간(일봉, 정산일)에 겹쳐 세지고, 끝을 23:59:59 로 잡으면 그 사이의 행을 잃는다(우아한형제들 공개 사례). 날짜는 하루 단위라 «끝 날짜 포함»이 정의이므로, 이산 날짜 API(`DailyBalanceRepositoryImpl` 의 `businessDate.between(from, to)`, `TaxSummaryRepositoryImpl` 의 `taxYear.between`)를 글자 그대로 `< to` 로 바꾸면 마지막 날이 빠진다. 파생 메서드의 `...Between` 은 `BETWEEN` 글자가 없어 리뷰에서 놓치기 쉬워 대상으로 적는다. — enforce: review
  → 잔존: 같은 파일 PTF5 조항과 그 Why(이산 날짜·연도 API 는 DT9 Exception 의 닫힌 구간이라 `< to` 로 바꾸지 않는다 · 파생 메서드 `...Between` 과 Querydsl `.between(` 을 BETWEEN 으로 침 — 고쳐 쓴 새 조항, 사람 결정 2026-11-21)

- 삭제: - 다른 애그리거트는 id 로만 참조한다. JPA 연관관계(`@ManyToOne`·`@OneToMany`·`@OneToOne`)는 같은 애그리거트 안의 자식에만 쓴다.
  → 잔존: devkit `rules/dev-team/backend-api.md` DD15(다른 애그리거트는 id 로만 참조, 연관관계는 같은 애그리거트 자식에만) — `rules/backend-api.md` 머리말이 DD15 를 직접 가리킴(옛 PTB3, 원장 duplicate 행)

- 삭제: - 돈의 이동, 체결, 예수금 변동을 기록한 원장 행은 UPDATE·DELETE 하지 않는다. 정정은 원래 행을 가리키는 반대 행을 추가해서 한다.
  → 잔존: devkit `rules/dev-team/failure-handling.md` FH7(원장 행은 UPDATE·DELETE 하지 않고 반대 행으로 정정) — `rules/failure-handling.md` 머리말이 FH7 을 직접 가리킴(옛 PTF1). 위 실행 비교가 이 조항을 잰다

- 삭제: - 원장에서 파생되는 잔고·포지션은 주기 작업이 원본(원장, 외부 체결 내역)으로 다시 계산해 대조하고, 불일치는 기록을 남겨 보정하거나 알린다.
  → 잔존: devkit `rules/dev-team/failure-handling.md` FH8(파생 잔고·포지션 주기 대사) — `rules/failure-handling.md` 머리말이 FH8 을 직접 가리킴(옛 PTF2)

- 삭제: - 외부 호출(KIS 주문 등)이 타임아웃되거나 확정 응답 없이 끝나면 그 의도를 실패가 아닌 UNKNOWN 상태로 두고, 재시도하기 전에 조회나 취소로 결과를 확정한다.
  → 잔존: devkit `rules/dev-team/failure-handling.md` FH9(불확정 응답은 UNKNOWN 으로 두고 확정 뒤 재시도) — `rules/failure-handling.md` 머리말이 FH9 를 직접 가리킴(옛 PTF3)

- 삭제: - 수수료, 세금, 이자처럼 계산한 금액을 저장하는 행에는 그때 쓴 요율과 정책 버전을 함께 저장하고, 나중의 재계산과 감사는 저장된 요율을 읽는다(현재 설정을 다시 읽지 않는다).
  → 잔존: devkit `rules/dev-team/failure-handling.md` FH10(계산 금액에 요율·정책 버전 저장, 재계산은 저장된 요율로) — `rules/failure-handling.md` 머리말이 FH10 을 직접 가리킴(옛 PTF4)

## 공식 근거 대조

근거 문서: devkit `docs/research/ai-doc-official-guidance.md` §1~§4.

- 이유(rationale) 보존: 남은 레포 조항(PTF5·PTF6·PTB*·PTD*·PTA*)은 `Why:` 줄을 그대로 둔다. 지운 PTF1~PTF4 의 Why 는 devkit FH7~FH10 의 Why 가 맡는다(위 대조). 다만 CLAUDE.md·옛 하네스에서 원문 그대로 옮긴 줄(`rules/conduct.md`·`workflow.md`·`domains/*.md` 등 10개 파일)은 check 가 같은 글만 받으므로 Why 없이 옮겼다 — 조항 꼴로 다시 쓰는 일은 후속으로 넘긴다.
- 강조 서식: 옮긴 원문 줄에 `CRITICAL`·굵게가 11곳 남았다(같은 글 조건). 새로 쓴 글에는 강조 서식을 더하지 않았다. 후속 정리 대상.
- 골격(헤딩·불릿): 규칙 파일마다 pointer 머리말 + 헤딩 + 불릿 한 줄 조항. 기존 다섯 파일은 `gt:` 앵커와 form v2 를 유지했다.
- 분량 판정: 규칙 파일 합계 748줄(17개 파일), 입구 CLAUDE.md 26줄·AGENTS.md 44줄로 200줄 상한 아래다(`wc -l`).
- 모순: devkit 과 모순인 줄 13개는 devkit 을 따라 레포 줄을 지웠다(사람 결정 2026-11-21 — 결정 7, MockMvc 3줄은 TT9).

## 검증

- `node ~/.claude/plugins/cache/allosta-internal/devkit/1.2.0/tools/rule-migration.js check ~/.devkit/rule-migrations/paper-trading/2026-11-21-v3 --repo . --proposals ~/.devkit/handoffs/devkit` → exit 0, 실패 0, 경고 0.
- `trial-prereg.js check` 두 등록 모두 통과(`--root .`).
- 코드 파일(.kt·.py·.ts) 변경 0.
