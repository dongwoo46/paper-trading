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
| Feature | trading-api-money-state |
| Based on | development-plan.md |
| Updated | 2026-11-15 |
| Overall status | in progress |

> Step, status and ownership (session, branch, worktree) are collected in the status table of [progress.md](./progress.md) — this file's job is the step roadmap detail.

---

## 1. Current position

> One line on where things are now. Example: "step-3 in progress (step-1 and step-2 done)"

step-1 in progress: PR-A1 implemented and committed; joint review round 1 and re-review round 2 findings fixed (money columns widened to numeric(28,12), notice rejection handled at the socket boundary, KIS execution id masked); only P-14 and the follow-up TODO items remain; waiting for the human to mark step-1 done and open the PR.

---

## 2. Roadmap

> **Derivation rule:** list **all 5–7 phases** of development-plan §5 in this table (the roadmap) up front. Step files are created one at a time from step-1 — [`step-scaffold`](../../../../tools/step-scaffold.js) init writes this table plus step-1, then `--step N` adds the next one.
> Change, add or remove rows as development and discussion go on, and record why in §3 change history. This table is always the current truth.
> Without a table, phase bullets inside this section also count (`` - `P1` … `` · `- **Phase 1** …` · `- Phase 1: …`). If fewer phases are right, add a line `Phase-count reason: <reason>`.

| Step | Title | Status | File | Note |
|---|---|---|---|---|
| 1 | PR-A1 execution amount contract and final lock release | in progress | steps/step-1.md | review rounds 1 and 2 fixed (R2-N1~R2-N4, P-05 solved by the column widening); P-14 and O5 are TODO items; done mark is the human's |
| 2 | PR-A2 cancel-side lock release | todo | steps/step-2.md |  |
| 3 | PR-A3 block user cancel of broker-submitted orders | todo | steps/step-3.md |  |
| 4 | PR-A4 idempotency key reuse and concurrent same key | todo | steps/step-4.md |  |
| 5 | PR-A5 fee policy lookup time and rounding source | todo | steps/step-5.md |  |
| 6 | PR-A6 conditional total_amount correction | todo | steps/step-6.md |  |

- **Status:** todo / in progress / done / blocked — a cell is read by its leading word; anything else (`WIP`, `not done`) is treated as unknown, never as done.

---

## 3. Change history

> Steps added, changed or removed, and why. The first plan rarely survives as is — record the adaptation.

| Date | Change | Reason |
|---|---|---|
| (example) 6/7 | step-2.5 added | step-2 found a missing field in the API response |
| 2026-11-15 | step-1 completion criteria deviate: baseline ref e510634 instead of main, archunit_store 1 line deleted, ledger save merged | local main is stale and has no baseline files; the frozen store cannot shrink without the human-approved deletion (plan §8-1) |
| 2026-11-15 | step-1 scope grew: in-memory rounding removed, overfill rejected, two warn logs | joint review blockers P-01~P-03 and human decision on rounding (plan §8-1) |
| 2026-11-15 | step-1 scope grew again: money columns widened to numeric(28,12) with V13, settlement rounding removed, notice rejection handled at the socket boundary, KIS execution id masked | re-review round 2 (R2-N1~R2-N4) and the human decision "금액 열 확장" (plan section 8-1) |
| 2026-11-22 | origin/main merged into the PR branch; the 2026-11-15 "wait until the rules are wired" hold is replaced: conflict resolution, QA round 3 and merge go ahead now | human decision 2026-11-22 «해소부터 머지까지» (multi-broker step 13 is blocked on this PR). Resolution keeps main's exhaustive `when` with entry rejection for TOSS_LIVE·UPBIT_LIVE (no `else` deposit) and this PR's `Order.applyExecution`/`remainingLockedAmount`; full test 92 xml 1216 tests 0 failures, lint baselines grew 0 vs origin/main |

---

## 4. Blockers

| Issue | Step | Decision or person needed |
|---|---|---|
|  |  |  |
