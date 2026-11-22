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
| 7 | 시세 REST와 최종 확인 | in progress | steps/step-7.md | 코드는 PR #55로 머지(`263f047`, 전체 1073건 실패 0). 남은 것은 §5 8번 수동 실호출 대조(사람 준비 뒤, 2단계와 병행) |
| 8 | 2단계 기준선과 특성화 시험 | done | steps/step-8.md | 기준선 1073건 실패 0, 특성화 10건 통과(`338a5ad`), 리뷰 2회차 막는 지적 0(`review/step8-rereview-1.md`) |
| 9 | KIS 에러 정렬 | done | steps/step-9.md | 잔고 경로만(주문 경로·KIS_ORDER_REJECTED 는 step 11). 전체 1095건 실패 0, 리뷰 막는 지적 0(`review/step9-code-review.md`, 후속 9건 원장) |
| 10 | 브로커 선택과 KIS 잔고 이전 | done | steps/step-10.md | 전체 1116건 실패 0, 특성화 10건 무수정 통과, 리뷰 막는 지적 0(`review/step10-code-review.md`, 메인 인라인 리뷰, 후속 3건 원장) |
| 11 | 주문 공통 포트 | done | steps/step-11.md | 2단계 4번, D26·D28, S9-01·S9-02 함께. 전체 1176건 실패 0, 특성화 10건 무수정 통과, 리뷰 3회차 머지 가능(`review/step11-code-review.md`·`step11-rereview-1.md`·`step11-rereview-2.md`, 3회차 메인 인라인), 후속은 원장·TODO P1 |
| 12 | 계좌 응답 출처와 ADR 갱신 | done | steps/step-12.md | 2단계 5번, D29, ADR-005(D25·D26). 전체 1180건 실패 0, 특성화 10건 무수정 통과, 리뷰 막는 지적 0(`review/step12-code-review.md`, 메인 인라인) |
| 13 | 체결 입력 공통화 | in progress | steps/step-13.md | 2단계 6번, D27. PR #53 머지(486116c, 2026-11-22) 뒤 착수, 브랜치 feature/multi-broker-kis-toss-step13, 별도 PR |
| 14 | 2단계 최종 확인 | done | steps/step-14.md | 2단계 7번, step 13 보다 먼저(§3 2026-11-22). 전체 1186건 실패 0, 독립 QA 막는 지적 0, Q-01~03 고침 `a49e74a`, PR #57 |

- **Status:** todo / in progress / done / blocked — a cell is read by its leading word; anything else (`WIP`, `not done`) is treated as unknown, never as done.

---

## 3. Change history

> Steps added, changed or removed, and why. The first plan rarely survives as is — record the adaptation.

| Date | Change | Reason |
|---|---|---|
| (example) 6/7 | step-2.5 added | step-2 found a missing field in the API response |
| 2026-11-22 | step 14(2단계 최종 확인)를 step 13 보다 먼저 함. 2단계 PR 은 step 8~12 만 싣고, step 13(D27)은 PR #53 머지 뒤 별도 PR | PR #53 이 OPEN·main 과 충돌(2026-11-22 확인), D27 은 열려 있으면 착수 금지. 사용자 결정 «step 14 먼저» |
| 2026-11-21 | step 11 범위에 step 9 리뷰 후속 S9-01(연결 거부가 KIS_TIMEOUT)·S9-02(rt_cd 1 한도가 인증으로 가려짐) 추가, KIS 실패 분류를 잔고·주문 어댑터 공용으로. 한도 판정은 msg_cd EGW00201 또는 «초당 거래건수» 문구(«초과» 단독 금지), ORNO 없는 성공 응답은 KIS_UNAVAILABLE(가정) | 주문 어댑터가 같은 분류를 써 따로 고치면 판정이 갈라짐. 사용자 결정 «함께 고침». 계획서 문구 변경 없음 |
| 2026-11-21 | 주문 경로: mapKisErrorCode 가 «초과»·rate·limit 문구로 붙인 RATE_LIMITED 는 «초당 거래건수» 문구가 없으면 400 KIS_ORDER_REJECTED. step 11 이 KisPaperPollingScheduler 도 삭제(계획서 §10.3 삭제 행, 실행기 삭제로 컴파일 불가) | 고치지 않기로 한 mapKisErrorCode 와 «초과 단독 한도 금지»가 부딪힘. 사용자 결정 «그대로 둠» |
| 2026-11-16 | 2단계 step 8~14 추가(계획서 §5 2단계 1~7번, §10). 범위는 trading-api만, 토스 실주문·trading-web·KIS 토큰 이전 제외. DEC-05~08 모두 A | 사용자 결정 «백엔드만, 화면은 나중», «시작, 대조는 병행», DEC-05~08 추천안 선택. 설계 재승인 필요 |
| 2026-11-15 | 예외 범주 J(503)·K(500)는 처음 쓰는 단계로 미룸 | 2단계는 I(501)만 필요(미사용 코드 금지) |
| 2026-11-15 | 2단계 재리뷰(`review/rereview-1.md`)와 QA 증거(`review/test-evidence.md`) 완료, 보류 해제 | 9건 모두 고침, 막는 지적 0. 후속 P-06·P-07·R2-01은 이유와 함께 deferred 처분 |
| 2026-11-15 | step 5 범위에 R1-01(TTL 단조 식)·R1-02(요청 전체 상한 `token-request-deadline-millis` 20초)·TossTokenManager 빈 등록 추가, 설계 D10·D11·§4 수정 | 재리뷰 후속 FLAG-16 사람 결정, FLAG-14. 설계 재승인 받음 |
| 2026-11-15 | step 6 범위에 step 5 리뷰 후속 C-01(리미터 대기 상한)·C-02(base URL https 검사)·C-03(목록 null·실수 절삭 차단)·C-04(요청 상한 하한 경계)·C-05(429 대기 리미터 공유) 추가 | 호출처가 붙는 step 6부터 드러나는 위험, 사람 결정 «후속은 step 6에». C-06~C-08은 findings 원장 |
| 2026-11-15 | step 7 범위에 step 6 리뷰 후속 S6-01(빈 문자열 금액 null 통과)·S6-02(예약된 호출의 429 대기 미공유)·S6-04(대기 상한 0의 연속 호출 실패)·S6-05(accountSeq 마스킹 한 곳으로) 추가 | step 7이 같은 TossJson·TossRateLimiter를 다시 씀, 사람 결정 «나눠서 처리». S6-03(실호출 대조 필요)·S6-06(기존 처리기)·S6-07(시험 구조)은 findings 원장 |
| 2026-11-16 | 3단계 중 토스 실주문은 범위에서 뺌, 사람이 다시 지시할 때까지 착수하지 않음(웹소켓 등 나머지 3단계 범위는 그대로) | 사용자 결정 «실주문은 제외할거야 바로 하지않는다 개발이 내가 나중에 실주문 다시 구현하라고 시킬거야». 계획서 §5 3단계 문구는 재개 지시 때 함께 고침(지금 고치면 설계 재승인이 다시 필요) |
| 2026-11-16 | step 7 범위에 리뷰 후속 S7-01(429 멈춤 끝 몰림)·S7-02(문자열 "null" 금액)·S7-03(줄 대기 상한 하한을 그룹 간격 최댓값 이상으로, 계획서 D16·§4 수정)·S7-04(컨트롤러·클라이언트 시험의 목 대체) 추가 | step 7 코드 리뷰 1회차(`review/step7-code-review.md`, 막는 지적 0). 사용자 결정 «후속 4건도 고치자». 설계 재승인 `ed07dc1`(2026-11-16) |
| 2026-11-15 | step 7 문서 생성. 시세 응답 시각은 토스 오프셋 원문 그대로, S6-04는 리미터 줄 대기 상한을 새 키 `rate-limit-max-queue-wait-millis`로 분리(계획서 D16·§4 수정, 설계 재승인 필요), 공용 클라이언트 쿼리 `+` 인코딩 결함을 step 7 범위에 추가 | 미국 일봉은 현지 자정 고정이라 KST로 바꾸면 거래일 뜻이 흐려짐. 같은 키를 두 뜻으로 쓰면 0(429 무대기)에서 연속 호출이 실패. candles `before`의 `+`가 공백으로 읽힘(명세 «%2B 인코딩»). 사람 결정 2026-11-15 |
| 2026-11-15 | step 6 문서 생성. 수익률은 퍼센트로 통일(종목은 토스 비율 ×100, 통화별 합계는 §7 KIS 방식 계산), `/api/toss/accounts`의 accountNo는 끝 4자리만, FLAG-10 금액 문자열은 step-6 §1에 반영 | 공식 명세의 rate가 소수 비율이고 합계 rate는 원화 환산 전체 기준이라 통화별 칸에 못 씀. accountNo는 금융 개인정보. 사람 결정 2026-11-15 |
| 2026-11-15 | main(PR #48·#49) 합류, 린트 정리 커밋 `a43538a`. 계획서 D15와 §3 수정: 새 범주 예외는 common 에서 Spring 무의존, HTTP 매핑은 GlobalExceptionHandler. step 3 범위에 NotImplementedException 이동 추가 | 새 레포 규칙(common 은 Spring 금지)과 ArchUnit 동결이 D15와 충돌. 사람 결정 «새 예외만 핸들러 매핑». 설계 재승인 필요 |

---

## 4. Blockers

| Issue | Step | Decision or person needed |
|---|---|---|
| 리뷰·QA 보류: step 11 변경분은 코드 리뷰 3회차(머지 가능)만 거침, 독립 QA 는 step 14 최종 확인에서 2단계 전체로 함 | 11 | 사용자 결정 2026-11-21 «지금은 안 함» |
| 리뷰·QA 보류: 린트 정리 커밋 `a43538a`(서식·파일 이름만, 동작 변경 없음, 바뀐 시험 107건 통과)는 같은 파일을 고치는 step 3 합동 리뷰에 포함 | 3 | 사용자 결정 2026-11-15 |