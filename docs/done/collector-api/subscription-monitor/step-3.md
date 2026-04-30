# Step 3: Testing and QA Verification

**Agent**: test-engineer
**Branch**: feature/collector-api-subscription-monitor
**Working Directory**: C:\Users\dw\Desktop\paper-trading\.worktrees\collector-api-subscription-monitor

---

## Files to Read (in order)

1. `C:\Users\dw\Desktop\paper-trading\docs\phase\collector-api\subscription-monitor\spec.md`
2. `C:\Users\dw\Desktop\paper-trading\docs\phase\collector-api\subscription-monitor\step-2.md`
3. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\main\kotlin\com\papertrading\collector\application\subscriptions\service\SubscriptionStatusService.kt`
4. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\main\kotlin\com\papertrading\collector\presentation\subscriptions\SubscriptionStatusController.kt`
5. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\test\kotlin\com\papertrading\collector\application\subscriptions\service\SubscriptionStatusServiceTest.kt`
6. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\test\kotlin\com\papertrading\collector\presentation\subscriptions\SubscriptionStatusControllerTest.kt`
7. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\test\kotlin\com\papertrading\collector\application\kis\service\KisWsHealthServiceTest.kt`

---

## Build Verification Command

```bash
cd backend/collector-api && ./gradlew compileKotlin
cd backend/collector-api && ./gradlew test
```

---

## Substep 3-1: Feature-Scoped Tests

Verify that all unit tests from step-2 are present and passing.

### Checklist

**SubscriptionStatusServiceTest** (5 tests):
- [ ] `report_returns_empty_when_kis_disabled`
- [ ] `report_assembles_mode_status_correctly`
- [ ] `report_sorts_modes_alphabetically`
- [ ] `report_calculates_wsSlotUsed_with_multiple_trIds`
- [ ] `report_sets_totalWsSlotUsed_as_sum_of_all_modes`

**SubscriptionStatusControllerTest** (3 tests):
- [ ] `GET_subscriptions_status_returns_200_with_correct_structure`
- [ ] `GET_subscriptions_status_maps_null_lastConnectedAt_to_null_in_json`
- [ ] `GET_subscriptions_status_returns_200_with_empty_modes_when_disabled`

Run:
```bash
cd backend/collector-api && ./gradlew test --tests "*.SubscriptionStatusServiceTest" --tests "*.SubscriptionStatusControllerTest" 2>&1
```

If any test is missing or failing, write the missing test or fix the implementation.

---

## Substep 3-2: Edge Case Tests

Add the following edge case tests if they are not already present.

### Edge Case A — restSymbols is empty when REST watchlist returns nothing for a mode

Add to `SubscriptionStatusServiceTest`:

```kotlin
@Test
fun `report_returns_empty_restSymbols_when_mode_not_in_rest_watchlist`() {
    every { properties.enabled } returns true
    every { properties.maxRealtimeRegistrations } returns 41
    every { properties.normalizedModes() } returns listOf("paper")
    every { properties.resolvedTrIds() } returns listOf("H0STCNT0")
    every { healthService.health() } returns listOf(
        WsHealthSnapshot("paper", WsConnectionStatus.CONNECTED, null, 0)
    )
    every { wsSubscriptionService.listSymbolsPerMode() } returns mapOf("paper" to listOf("005930"))
    // REST watchlist does not contain "paper" key
    every { restWatchlistService.listSymbolsPerMode() } returns emptyMap()

    val report = service.report()

    assertThat(report.modes[0].restSymbols).isEmpty()
}
```

### Edge Case B — health snapshot missing for a mode (fallback to DISCONNECTED)

Add to `SubscriptionStatusServiceTest`:

```kotlin
@Test
fun `report_falls_back_to_disconnected_when_health_snapshot_missing`() {
    every { properties.enabled } returns true
    every { properties.maxRealtimeRegistrations } returns 41
    every { properties.normalizedModes() } returns listOf("live")
    every { properties.resolvedTrIds() } returns listOf("H0STCNT0")
    // health returns empty (mode not registered in registry)
    every { healthService.health() } returns emptyList()
    every { wsSubscriptionService.listSymbolsPerMode() } returns mapOf("live" to emptyList())
    every { restWatchlistService.listSymbolsPerMode() } returns mapOf("live" to emptyList())

    val report = service.report()

    assertThat(report.modes[0].connectionStatus).isEqualTo(WsConnectionStatus.DISCONNECTED)
    assertThat(report.modes[0].reconnectAttempts).isEqualTo(0)
    assertThat(report.modes[0].lastConnectedAt).isNull()
}
```

### Edge Case C — wsSymbols are sorted alphabetically in response

Add to `SubscriptionStatusServiceTest`:

```kotlin
@Test
fun `report_returns_wsSymbols_sorted_alphabetically`() {
    every { properties.enabled } returns true
    every { properties.maxRealtimeRegistrations } returns 41
    every { properties.normalizedModes() } returns listOf("paper")
    every { properties.resolvedTrIds() } returns listOf("H0STCNT0")
    every { healthService.health() } returns listOf(
        WsHealthSnapshot("paper", WsConnectionStatus.CONNECTED, null, 0)
    )
    // Redis set returns in arbitrary order
    every { wsSubscriptionService.listSymbolsPerMode() } returns mapOf("paper" to listOf("035720", "000660", "005930"))
    every { restWatchlistService.listSymbolsPerMode() } returns mapOf("paper" to emptyList())

    val report = service.report()

    assertThat(report.modes[0].wsSymbols).containsExactly("000660", "005930", "035720")
}
```

Run edge case tests:
```bash
cd backend/collector-api && ./gradlew test --tests "*.SubscriptionStatusServiceTest"
```

---

## Substep 3-3: Full Test Suite + Coverage Check

Run the complete test suite to ensure no regressions:

```bash
cd backend/collector-api && ./gradlew test 2>&1
```

Check that all pre-existing tests still pass:
- `KisWsHealthServiceTest`
- `KisWsConnectionRegistryTest`
- `KisWsHealthControllerTest`
- `KisRawEventParserTest`
- `KisAccessTokenClientTest`
- `QuoteRedisPublisherTest`

### Pass Criteria
- Total tests: all passing (0 failures)
- New feature tests: 5 (service) + 3 (controller) + 3 (edge cases) = 11 tests minimum
- No compilation warnings treated as errors

---

## Completion Criteria

- [ ] `compileKotlin` passes with 0 errors
- [ ] All new tests pass
- [ ] All pre-existing tests still pass (no regression)
- [ ] Edge case tests added: empty restSymbols, missing health snapshot, sorted wsSymbols

---

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to worktree root>
- Test result: <passed N/N | failed N — list failing cases>
- Blockers: <none | description>
---
