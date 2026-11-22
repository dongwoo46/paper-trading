# Step 1 — PR-A1 execution amount contract and final lock release

> **One implementation step.** It maps to one step row in state.md. Copy it to `steps/step-1.md`, `steps/step-2.md` … for real use.
> A problem met during the work goes into §8; when it needs a new step or a change, raise it in the state.md change history.
>
> **A feature usually splits into 5–7 phases (steps)**, more or fewer by size. Derive steps from the planner / development-plan §5 design — never imagine them from nothing. At kickoff, [`step-scaffold`](../../../../../tools/step-scaffold.js) writes the 5–7 phase roadmap into state.md §2 and creates **only step-1.md**; step-2.md is created when step-1 ends, and so on in order (the roadmap up front / the files one at a time).
> **Fill it in two blocks:**
> - **Plan block = required before starting:** `0 based on · 1 goal and scope · 2 approach (how, in what order) · 3 dependencies · 4 completion criteria`. Derive it from the planner / development-plan design, not imagination. **Never leave it empty — a step is the base for "what, how far, how".**
> - **Record block = while developing, in order:** leave `5 work log · 6 verification · 7 result · 8 problems found` empty and fill them step-1 → step-2 in sequence. Writing them ahead causes drift. **When a problem or change comes up, edit this step.md directly** (the reason goes in state.md §3 change history).
>
> Relation to state = **index (state, thin) vs detail (step, thick).** They do not overlap.
> Tools read the labels below literally — `step-scaffold` fills the plan block by them and `doc-quality` reads the Status row (one of the words listed there).

---

## 0. Metadata

| Field | Value |
|---|---|
| Step | 1 |
| Title | PR-A1 execution amount contract and final lock release |
| Feature | trading-api-money-state |
| Based on | development-plan.md sections 1-1 (#8 #127 #108 #551 #552), 2 (D-01 D-02 fill side), 5 item 1 |
| Status | in progress |
| Owner | Maker (Claude) |

---

**━━ Plan block (required before starting · from the planner / development-plan design) ━━**

## 1. Goal and scope  (plan)

> What this step (phase) **builds (what)** + **how far it goes (this step's boundary — name what is handed to the next step)**.

- Builds: Order.applyExecution takes the fill price and computes totalAmount and average itself; the final fill releases only Order.remainingLockedAmount().
- Scope (where this step stops): ExecutionProcessor, Order, OrderTest, ExecutionProcessorTest only. No cancel-side release (PR-A2), no fee change (PR-A5), no new repository-calling methods, no baseline-listed function signature change.

---

## 2. Approach (how, in what order)  (plan)

> **By what method and in what order** this step (= phase) is solved — briefly (one line to a few). Based on the feature-spec / development-plan design.
> A rough level is enough (the detailed work log is §5). **Never leave it empty — the step's base is here.**

- Commit 1: ktlint-clean the touched files only, behavior unchanged, detekt local items of those files.
- Commit 2: red tests with ledger numbers (4@69000 then 6@70000 gives avg 69600 and total 696000; KIS_PAPER two buy fills end with lockedDeposit 0 and available 304000; same price twice no exception; single price improvement regression). Run and confirm red.
- Commit 3: root fix D-01 and D-02 plus root-cause chain, tests green.
- Commit 4: regenerate lint baselines (stem only), verify not grown against main.

---

## 3. Dependencies  (plan)

- Previous step:
- Prerequisites:

---

## 4. Completion criteria  (plan)

- [x] Red tests fail for the ledger reason before the fix and pass after
- [x] gradlew test ktlintCheck detekt verifyLintBaselinesNotGrown all green (baseline ref e510634, not main — see section 8)
- [x] ArchitectureRulesTest green; archunit_store diff is one deleted line (human-approved, see section 8), not zero
- [x] root-cause-check passes on the staged chains (root-cause.md, root-cause-a1-review.md)

---

**━━ Record block (filled in order while developing · never ahead) ━━**

## 5. Work log  (record)

> Fill it by spelling out the §2 approach as the actual work (concretely, what was done).

- 2980dfa lint-clean of Order, ExecutionProcessor, OrderTest, ExecutionProcessorTest (ktlint run on those four paths only; detekt local items fixed).
- 1dd52a5 red tests with the ledger numbers (3 red, regression green); 474e066 fix D-01 and D-02 plus root-cause.md; 8ee77a3 baseline regeneration.
- Joint review round 1 (review/panel.md): 44a830c red edge tests (11 red), 50f382d fix (no in-memory rounding, overfill rejected, two warn logs, KDoc), cf00dac baseline position update.
- Docs: plan section 8-1 and 6 updated, TODO items for P-05 and AE19, review files and explainer html.
- Re-review round 2 (review/rereview-1.md): 5af13b8 plan note for the column widening; c8136c8 red tests (8 red: real PostgreSQL round trip, settlement exactness, rejection message, socket boundary, log masking); 13f1187 money columns to numeric(28,12) plus V13 plus settlement rounding removed; db40348 notice rejection logged at the socket boundary plus KIS account-number masking; 1d79e26 baseline update.

  Money columns widened (entity @Column and V13 SQL, numeric(20,4) to numeric(28,12), integer digits stay 16): accounts.deposit, available_deposit, locked_deposit (Account.kt:49,53,57); account_ledger.amount, balance_after (AccountLedger.kt:50,54); orders.total_amount, locked_amount (Order.kt:96,105); settlements.realized_pnl, net_pnl, krw_net_pnl (Settlement.kt:52,64,76); positions.total_buy_amount (Position.kt:76).

  Not changed (unsure or out of scope): prices and averages (orders.limit_price, avg_filled_price, executions.executed_price, krw_executed_price, positions.avg_buy_price, current_price), fees and taxes (orders.fee, executions.fee, settlements.fee, tax, fee_policies.min_fee: rule is PR-A5), valuation columns (positions.evaluation_amount, unrealized_pnl), report copies (daily_balances, portfolio_snapshots, tax_summaries), risk_policies, strategy columns.

---

## 6. Verification  (record)

| What | How it was checked | Result |
|---|---|---|
| Red before fix (ledger numbers) | 1dd52a5 run: OrderTest average, ExecutionProcessorTest two-fill cases | 3 red, "잠금 예수금이 부족합니다." as in the ledger |
| Red before review fix | 44a830c run of OrderTest and ExecutionProcessorTest | 11 red, then green after 50f382d |
| Full gate | `./gradlew test ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=e510634 --rerun-tasks` | BUILD SUCCESSFUL at cf00dac (7m40s); baselines grown 0 vs e510634 for ktlint and detekt |
| Frozen store | `git diff` of archunit_store | one line deleted, none added |
| Round 2 red | c8136c8 run of the touched test classes | 8 red: BUY_EXECUTE ledger 50.0001 vs deposit decrease 50.0000, FILLED lock 0.0002 left, settlement 30.0002 vs 30.00015, message without ids, exception escaping the socket handler, account number in 3 logs; green after 13f1187 and db40348 |
| Round 2 gate | `./gradlew test ktlintCheck detekt verifyLintBaselinesNotGrown -PlintBaselineRef=e510634 --rerun-tasks` | BUILD SUCCESSFUL at 1d79e26 (9m15s); baselines grown 0 vs e510634 |

---

## 7. Result  (record)

- Done: PR-A1 code, tests, root-cause chains and review round 1 fixes. The done mark is the human's.
- Changed files / PR: Order.kt, ExecutionProcessor.kt, OrderTest.kt, ExecutionProcessorTest.kt, config baselines, one archunit_store line. PR not opened yet.

---

## 8. Problems found  (record)

> Found during the work — it can cause a new step, a change to an existing one, or a flag. Edit this step.md directly then (the reason goes in state.md §3).

| Problem | How it was handled |
|---|---|
| Deviation 1: ArchitectureRulesTest failed with StoreUpdateFailedException after deleting releaseLockedExcess (a fixed frozen violation cannot leave the read-only store) | human approved deleting that one stored line (2026-11-15); no update option used; plan section 6 expectation changed to "only the fixed line deleted" |
| Deviation 2: a fourth AccountLedgerRepository.save call in processFill would be a new frozen violation | execution ledger save merged into one call site (count 3 kept); review judged it legitimate |
| Deviation 3: baseline ref e510634 instead of main | local main (0f57687) predates the baseline files, so the comparison is skipped ("신규 도입"); e510634 is the branch base and has them |
| Review blockers P-01~P-03: A1 fix added 4-digit in-memory rounding | human decision: no in-memory rounding (plan section 8-1); fixed in 50f382d with red tests first |
| P-05: storage column 4-digit remainder can exceed the lock | TODO item for a column scale change (schema decision); not fixed here |
| P-14: service tests mock repositories | left to the C track (ports), plan section 9 |
| R2-N2, R2-N3 and P-05: stored 4-digit columns cut the unrounded amounts | human decision: widen money columns to numeric(28,12) in A1 (plan section 8-1), V13 added; the repo has no Flyway and uses ddl-auto create, so the entity definition is the real schema and V13 is a manual script (TODO item for schema management) |
| R2-N1: rejection exception closed the KIS session | caught at the socket boundary (outside the transaction, rule PTB9), logged with order id, masked execution id, quantity and remaining quantity; the notice stays rolled back |
| R2-N4: account number in logs | one masking function (common/LogMasking.kt) used by the three log sites |
| Found while testing: LOCAL sell proceeds not credited to the deposit in a real DB test (applySellExecutionAtomically clears the persistence context) | not fixed, recorded as plan O5 and a TODO item (out of ledger, rule: record only) |
| Found: no Flyway dependency, ddl-auto create in application.yaml | recorded in the plan and a TODO item for a human check |
