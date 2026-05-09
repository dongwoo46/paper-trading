# PR Summary: trading-api settlement-cleanup

## Scope Completion Check
- Receivable settlement removal: implemented (`ReceivableSettlement` entity/repo/query/controller and related settlement services removed).
- KIS branch simplification: implemented (KIS sell path no longer creates internal receivable settlement or mutates internal cash; execution sync only).
- LOCAL 0.2% sell tax fix: implemented (`grossProceeds * 0.0020`, scale 4 HALF_UP in `ExecutionProcessor` settlement record path).
- TaxSummary LOCAL-only scope: implemented (LOCAL mode guard in command/query services and LOCAL filter in settlement tax aggregation query).

## Code-Review Gate (Regression/Migration)
- Account balance mutation path:
  - LOCAL sell keeps internal proceeds mutation and settlement recording.
  - KIS_PAPER/KIS_LIVE sell path removes internal receivable settlement side effects.
- Mode separation:
  - TaxSummary APIs now reject non-LOCAL accounts with `INVALID_ACCOUNT_MODE_FOR_TAX_SUMMARY` (400).
- Migration safety:
  - Flyway V9 added: `DROP TABLE IF EXISTS receivable_settlements CASCADE;`
  - Startup impact note: `CASCADE` may remove dependent constraints/views; verify downstream objects in environments with custom extensions before rollout.

## Verification Commands and Results
```bash
cd .worktrees/trading-api-settlement-cleanup/backend/trading-api
./gradlew compileKotlin
./gradlew test
```

- `./gradlew compileKotlin`: PASS (BUILD SUCCESSFUL, up-to-date).
- `./gradlew test`: FAIL (362 tests, 15 failed).
  - Representative failures include:
    - `AccountCommandServiceTest`
    - `OrderCommandServiceTest`
    - `Tax/Controller integration tests`
    - `ExitTriggerPersistenceConstraintTest`
  - Infra symptom in logs: repeated PostgreSQL connection refused/timeouts to localhost ephemeral ports during scheduler/integration execution.

## Risk Notes
- Full-suite acceptance is currently not green; phase cannot be marked done.
- Migration uses table drop with cascade; safe for intended cleanup but requires environment dependency check.

## Rollback Notes
- Revert migration `V9__drop_receivable_settlements.sql` and restore removed receivable settlement files/services if rollback to prior behavior is required.
- If only acceptance instability is blocking, isolate test infrastructure (DB/scheduler) and rerun before any functional rollback.
