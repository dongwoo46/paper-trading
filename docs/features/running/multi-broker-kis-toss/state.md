# State — <feature>

> **A living index of how far the implementation has come.** It takes the development plan (fixed) and reflects the actual progress.
> **Keep it current at all times.** When a problem adds or changes a step, record the "why" in the change history.
> **Based on:** development-plan.md / **Step detail:** steps/step-N.md
> **Resume (after a stop, crash or handoff):** `node tools/resume.js <feature>` — compares this file with git and briefs the current position and drift deterministically (read-only).
> Tools read the table headers, status words and labels below literally; the definition is [`state-format`](../../../../tools/state-format.js) (it also reads the old Korean form).

---

## 0. Metadata

| Field | Value |
|---|---|
| Feature | multi-broker-kis-toss |
| Based on | development-plan.md |
| Updated |  |
| Overall status | not started / in progress / done / blocked |

> Step, status and ownership (session, branch, worktree) are collected in the status table of [progress.md](./progress.md) — this file's job is the step roadmap detail.

---

## 1. Current position

> One line on where things are now. Example: "step-3 in progress (step-1 and step-2 done)"

---

## 2. Roadmap

> **Derivation rule:** list **all 5–7 phases** of development-plan §5 in this table (the roadmap) up front. Step files are created one at a time from step-1 — [`step-scaffold`](../../../../tools/step-scaffold.js) init writes this table plus step-1, then `--step N` adds the next one.
> Change, add or remove rows as development and discussion go on, and record why in §3 change history. This table is always the current truth.
> Without a table, phase bullets inside this section also count (`` - `P1` … `` · `- **Phase 1** …` · `- Phase 1: …`). If fewer phases are right, add a line `Phase-count reason: <reason>`.

| Step | Title | Status | File | Note |
|---|---|---|---|---|
| 1 | 기준선 기록 | done | steps/step-1.md | 393개 통과, 실패 0 (`e2904c8`) |
| 2 | 모드 추가와 주문 안전장치 | done | steps/step-2.md | 438개 통과, 합동 리뷰 막는 지적 0 (후속 9 수정, 2 넘김) |
| 3 | 건드린 파일 에러 응답 정렬 | done | steps/step-3.md | 관련 시험 154건 통과, CI 전체 success(`7111818`), 합동 리뷰 막는 지적 0(`review/step3-panel.md`) |
| 4 | 토스 토큰 기반 | done | steps/step-4.md | 시험 71건 통과, CI 전체 success(`da617e5`, 실행 43688563304), 리뷰 막는 지적 0(후속 9건은 FLAG-15로 step 5 전 수정) |
| 5 | 스키마 확정과 토스 전송 계층 | done | steps/step-5.md | 관련 시험 253건 통과, CI 전체 success(`908ac89`, 실행 43700036485), 리뷰 막는 지적 0(후속 C-01~C-05는 step 6 범위) |
| 6 | 잔고 공통 포트와 토스 어댑터 | done | steps/step-6.md | 관련 시험 297건 통과, CI 전체 success(`2692c2d`, 실행 43723583394), 리뷰 막는 지적 0(후속 S6-01·02·04·05는 step 7 범위) |
| 7 | 시세 REST와 최종 확인 | todo | steps/step-7.md |  |

- **Status:** todo / in progress / done / blocked — a cell is read by its leading word; anything else (`WIP`, `not done`) is treated as unknown, never as done.

---

## 3. Change history

> Steps added, changed or removed, and why. The first plan rarely survives as is — record the adaptation.

| Date | Change | Reason |
|---|---|---|
| (example) 6/7 | step-2.5 added | step-2 found a missing field in the API response |
| 2026-11-15 | 예외 범주 J(503)·K(500)는 처음 쓰는 단계로 미룸 | 2단계는 I(501)만 필요(미사용 코드 금지) |
| 2026-11-15 | 2단계 재리뷰(`review/rereview-1.md`)와 QA 증거(`review/test-evidence.md`) 완료, 보류 해제 | 9건 모두 고침, 막는 지적 0. 후속 P-06·P-07·R2-01은 이유와 함께 deferred 처분 |
| 2026-11-15 | step 5 범위에 R1-01(TTL 단조 식)·R1-02(요청 전체 상한 `token-request-deadline-millis` 20초)·TossTokenManager 빈 등록 추가, 설계 D10·D11·§4 수정 | 재리뷰 후속 FLAG-16 사람 결정, FLAG-14. 설계 재승인 받음 |
| 2026-11-15 | step 6 범위에 step 5 리뷰 후속 C-01(리미터 대기 상한)·C-02(base URL https 검사)·C-03(목록 null·실수 절삭 차단)·C-04(요청 상한 하한 경계)·C-05(429 대기 리미터 공유) 추가 | 호출처가 붙는 step 6부터 드러나는 위험, 사람 결정 «후속은 step 6에». C-06~C-08은 findings 원장 |
| 2026-11-15 | step 7 범위에 step 6 리뷰 후속 S6-01(빈 문자열 금액 null 통과)·S6-02(예약된 호출의 429 대기 미공유)·S6-04(대기 상한 0의 연속 호출 실패)·S6-05(accountSeq 마스킹 한 곳으로) 추가 | step 7이 같은 TossJson·TossRateLimiter를 다시 씀, 사람 결정 «나눠서 처리». S6-03(실호출 대조 필요)·S6-06(기존 처리기)·S6-07(시험 구조)은 findings 원장 |
| 2026-11-15 | step 6 문서 생성. 수익률은 퍼센트로 통일(종목은 토스 비율 ×100, 통화별 합계는 §7 KIS 방식 계산), `/api/toss/accounts`의 accountNo는 끝 4자리만, FLAG-10 금액 문자열은 step-6 §1에 반영 | 공식 명세의 rate가 소수 비율이고 합계 rate는 원화 환산 전체 기준이라 통화별 칸에 못 씀. accountNo는 금융 개인정보. 사람 결정 2026-11-15 |
| 2026-11-15 | main(PR #48·#49) 합류, 린트 정리 커밋 `a43538a`. 계획서 D15와 §3 수정: 새 범주 예외는 common 에서 Spring 무의존, HTTP 매핑은 GlobalExceptionHandler. step 3 범위에 NotImplementedException 이동 추가 | 새 레포 규칙(common 은 Spring 금지)과 ArchUnit 동결이 D15와 충돌. 사람 결정 «새 예외만 핸들러 매핑». 설계 재승인 필요 |

---

## 4. Blockers

| Issue | Step | Decision or person needed |
|---|---|---|
| 리뷰·QA 보류: 린트 정리 커밋 `a43538a`(서식·파일 이름만, 동작 변경 없음, 바뀐 시험 107건 통과)는 같은 파일을 고치는 step 3 합동 리뷰에 포함 | 3 | 사용자 결정 2026-11-15 |