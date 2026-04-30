# Step 2: Implementation (TDD)

**Agent**: fullstack-dev
**Branch**: feature/collector-api-subscription-monitor
**Working Directory**: C:\Users\dw\Desktop\paper-trading\.worktrees\collector-api-subscription-monitor

---

## Files to Read (in order)

1. `C:\Users\dw\Desktop\paper-trading\docs\phase\collector-api\subscription-monitor\spec.md`
2. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\main\kotlin\com\papertrading\collector\domain\kis\WsConnectionStatus.kt`
3. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\main\kotlin\com\papertrading\collector\application\kis\dto\WsHealthSnapshot.kt`
4. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\main\kotlin\com\papertrading\collector\application\kis\service\KisWsHealthService.kt`
5. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\main\kotlin\com\papertrading\collector\application\kis\service\KisWsSubscriptionService.kt`
6. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\main\kotlin\com\papertrading\collector\application\kis\service\KisRestWatchlistService.kt`
7. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\main\kotlin\com\papertrading\collector\infra\kis\KisProperties.kt`
8. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\main\kotlin\com\papertrading\collector\infra\kis\source\ws\KisWsConnectionRegistry.kt`
9. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\main\kotlin\com\papertrading\collector\presentation\kis\KisWsHealthController.kt`
10. `C:\Users\dw\Desktop\paper-trading\backend\collector-api\src\test\kotlin\com\papertrading\collector\application\kis\service\KisWsHealthServiceTest.kt`

---

## Build Verification Command

```bash
cd backend/collector-api && ./gradlew compileKotlin
cd backend/collector-api && ./gradlew test
```

---

## TDD Workflow: Red → Green → Refactor

모든 소스 파일 생성 전에 실패하는 테스트를 먼저 작성한다.
빌드 통과 후 테스트를 통과시키는 최소 구현을 추가한다.

---

## Substep 2-1: Application DTOs

**Goal**: `SubscriptionModeStatus` + `SubscriptionStatusReport` value objects 생성

### Files to create

**`backend/collector-api/src/main/kotlin/com/papertrading/collector/application/subscriptions/dto/SubscriptionModeStatus.kt`**

```kotlin
package com.papertrading.collector.application.subscriptions.dto

import com.papertrading.collector.domain.kis.WsConnectionStatus
import java.time.Instant

data class SubscriptionModeStatus(
    val mode: String,
    val connectionStatus: WsConnectionStatus,
    val lastConnectedAt: Instant?,
    val reconnectAttempts: Long,
    val wsSymbols: List<String>,
    val restSymbols: List<String>,
    val wsSlotUsed: Int,
    val wsSlotMax: Int,
)
```

**`backend/collector-api/src/main/kotlin/com/papertrading/collector/application/subscriptions/dto/SubscriptionStatusReport.kt`**

```kotlin
package com.papertrading.collector.application.subscriptions.dto

import java.time.Instant

data class SubscriptionStatusReport(
    val generatedAt: Instant,
    val modes: List<SubscriptionModeStatus>,
    val totalWsSlotUsed: Int,
    val totalWsSlotMax: Int,
)
```

### Build check
```bash
cd backend/collector-api && ./gradlew compileKotlin
```

---

## Substep 2-2: SubscriptionStatusService (TDD)

**Goal**: Application service — WS health + WS symbols + REST symbols 조립

### Step A — Write failing test first

**File**: `backend/collector-api/src/test/kotlin/com/papertrading/collector/application/subscriptions/service/SubscriptionStatusServiceTest.kt`

```kotlin
package com.papertrading.collector.application.subscriptions.service

import com.papertrading.collector.application.kis.dto.WsHealthSnapshot
import com.papertrading.collector.application.kis.service.KisRestWatchlistService
import com.papertrading.collector.application.kis.service.KisWsHealthService
import com.papertrading.collector.application.kis.service.KisWsSubscriptionService
import com.papertrading.collector.domain.kis.WsConnectionStatus
import com.papertrading.collector.infra.kis.KisProperties
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class SubscriptionStatusServiceTest {

    private lateinit var healthService: KisWsHealthService
    private lateinit var wsSubscriptionService: KisWsSubscriptionService
    private lateinit var restWatchlistService: KisRestWatchlistService
    private lateinit var properties: KisProperties
    private lateinit var service: SubscriptionStatusService

    @BeforeEach
    fun setUp() {
        healthService = mockk()
        wsSubscriptionService = mockk()
        restWatchlistService = mockk()
        properties = mockk()
        service = SubscriptionStatusService(healthService, wsSubscriptionService, restWatchlistService, properties)
    }

    @Test
    fun `report_returns_empty_when_kis_disabled`() {
        every { properties.enabled } returns false
        every { properties.maxRealtimeRegistrations } returns 41

        val report = service.report()

        assertThat(report.modes).isEmpty()
        assertThat(report.totalWsSlotUsed).isEqualTo(0)
        assertThat(report.totalWsSlotMax).isEqualTo(41)
    }

    @Test
    fun `report_assembles_mode_status_correctly`() {
        val now = Instant.now()
        every { properties.enabled } returns true
        every { properties.maxRealtimeRegistrations } returns 41
        every { properties.normalizedModes() } returns listOf("paper")
        every { properties.resolvedTrIds() } returns listOf("H0STCNT0")
        every { healthService.health() } returns listOf(
            WsHealthSnapshot(
                mode = "paper",
                status = WsConnectionStatus.CONNECTED,
                lastConnectedAt = now,
                reconnectAttempts = 0,
            )
        )
        every { wsSubscriptionService.listSymbolsPerMode() } returns mapOf("paper" to listOf("005930", "035720"))
        every { restWatchlistService.listSymbolsPerMode() } returns mapOf("paper" to listOf("035420"))

        val report = service.report()

        assertThat(report.modes).hasSize(1)
        val mode = report.modes[0]
        assertThat(mode.mode).isEqualTo("paper")
        assertThat(mode.connectionStatus).isEqualTo(WsConnectionStatus.CONNECTED)
        assertThat(mode.lastConnectedAt).isEqualTo(now)
        assertThat(mode.wsSymbols).containsExactly("005930", "035720")
        assertThat(mode.restSymbols).containsExactly("035420")
        assertThat(mode.wsSlotUsed).isEqualTo(2) // 2 symbols * 1 trId
        assertThat(mode.wsSlotMax).isEqualTo(41)
    }

    @Test
    fun `report_sorts_modes_alphabetically`() {
        every { properties.enabled } returns true
        every { properties.maxRealtimeRegistrations } returns 41
        every { properties.normalizedModes() } returns listOf("paper", "live")
        every { properties.resolvedTrIds() } returns listOf("H0STCNT0")
        every { healthService.health() } returns listOf(
            WsHealthSnapshot("paper", WsConnectionStatus.DISCONNECTED, null, 0),
            WsHealthSnapshot("live", WsConnectionStatus.CONNECTED, null, 0),
        )
        every { wsSubscriptionService.listSymbolsPerMode() } returns mapOf("paper" to emptyList(), "live" to emptyList())
        every { restWatchlistService.listSymbolsPerMode() } returns mapOf("paper" to emptyList(), "live" to emptyList())

        val report = service.report()

        assertThat(report.modes.map { it.mode }).containsExactly("live", "paper")
    }

    @Test
    fun `report_calculates_wsSlotUsed_with_multiple_trIds`() {
        every { properties.enabled } returns true
        every { properties.maxRealtimeRegistrations } returns 41
        every { properties.normalizedModes() } returns listOf("paper")
        every { properties.resolvedTrIds() } returns listOf("H0STCNT0", "H0STASP0")
        every { healthService.health() } returns listOf(
            WsHealthSnapshot("paper", WsConnectionStatus.CONNECTED, null, 0)
        )
        every { wsSubscriptionService.listSymbolsPerMode() } returns mapOf("paper" to listOf("005930", "035720", "000660"))
        every { restWatchlistService.listSymbolsPerMode() } returns mapOf("paper" to emptyList())

        val report = service.report()

        // 3 symbols * 2 trIds = 6
        assertThat(report.modes[0].wsSlotUsed).isEqualTo(6)
    }

    @Test
    fun `report_sets_totalWsSlotUsed_as_sum_of_all_modes`() {
        every { properties.enabled } returns true
        every { properties.maxRealtimeRegistrations } returns 41
        every { properties.normalizedModes() } returns listOf("paper", "live")
        every { properties.resolvedTrIds() } returns listOf("H0STCNT0")
        every { healthService.health() } returns listOf(
            WsHealthSnapshot("paper", WsConnectionStatus.CONNECTED, null, 0),
            WsHealthSnapshot("live", WsConnectionStatus.CONNECTED, null, 0),
        )
        every { wsSubscriptionService.listSymbolsPerMode() } returns mapOf(
            "paper" to listOf("005930"),
            "live" to listOf("035720", "000660"),
        )
        every { restWatchlistService.listSymbolsPerMode() } returns mapOf("paper" to emptyList(), "live" to emptyList())

        val report = service.report()

        assertThat(report.totalWsSlotUsed).isEqualTo(3) // 1 + 2
    }
}
```

Verify test fails (compile error is acceptable at this stage):
```bash
cd backend/collector-api && ./gradlew test --tests "*.SubscriptionStatusServiceTest" 2>&1 | tail -20
```

### Step B — Implement SubscriptionStatusService

**File**: `backend/collector-api/src/main/kotlin/com/papertrading/collector/application/subscriptions/service/SubscriptionStatusService.kt`

```kotlin
package com.papertrading.collector.application.subscriptions.service

import com.papertrading.collector.application.kis.dto.WsHealthSnapshot
import com.papertrading.collector.application.kis.service.KisRestWatchlistService
import com.papertrading.collector.application.kis.service.KisWsHealthService
import com.papertrading.collector.application.kis.service.KisWsSubscriptionService
import com.papertrading.collector.application.subscriptions.dto.SubscriptionModeStatus
import com.papertrading.collector.application.subscriptions.dto.SubscriptionStatusReport
import com.papertrading.collector.infra.kis.KisProperties
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SubscriptionStatusService(
    private val healthService: KisWsHealthService,
    private val wsSubscriptionService: KisWsSubscriptionService,
    private val restWatchlistService: KisRestWatchlistService,
    private val properties: KisProperties,
) {
    fun report(): SubscriptionStatusReport {
        val maxSlot = properties.maxRealtimeRegistrations
        if (!properties.enabled) {
            return SubscriptionStatusReport(
                generatedAt = Instant.now(),
                modes = emptyList(),
                totalWsSlotUsed = 0,
                totalWsSlotMax = maxSlot,
            )
        }

        val trIdCount = properties.resolvedTrIds().size
        val healthByMode: Map<String, WsHealthSnapshot> = healthService.health().associateBy { it.mode }
        val wsSymbolsByMode: Map<String, List<String>> = wsSubscriptionService.listSymbolsPerMode()
        val restSymbolsByMode: Map<String, List<String>> = restWatchlistService.listSymbolsPerMode()

        val modes = properties.normalizedModes().sorted().map { mode ->
            val health = healthByMode[mode]
            val wsSymbols = wsSymbolsByMode[mode].orEmpty().sorted()
            val restSymbols = restSymbolsByMode[mode].orEmpty().sorted()
            SubscriptionModeStatus(
                mode = mode,
                connectionStatus = health?.status ?: com.papertrading.collector.domain.kis.WsConnectionStatus.DISCONNECTED,
                lastConnectedAt = health?.lastConnectedAt,
                reconnectAttempts = health?.reconnectAttempts ?: 0,
                wsSymbols = wsSymbols,
                restSymbols = restSymbols,
                wsSlotUsed = wsSymbols.size * trIdCount,
                wsSlotMax = maxSlot,
            )
        }

        return SubscriptionStatusReport(
            generatedAt = Instant.now(),
            modes = modes,
            totalWsSlotUsed = modes.sumOf { it.wsSlotUsed },
            totalWsSlotMax = maxSlot,
        )
    }
}
```

### Step C — Run tests green

```bash
cd backend/collector-api && ./gradlew test --tests "*.SubscriptionStatusServiceTest"
```

---

## Substep 2-3: Presentation Layer (TDD)

**Goal**: `SubscriptionStatusController` + response DTO

### Step A — Write failing controller test first

**File**: `backend/collector-api/src/test/kotlin/com/papertrading/collector/presentation/subscriptions/SubscriptionStatusControllerTest.kt`

```kotlin
package com.papertrading.collector.presentation.subscriptions

import com.papertrading.collector.application.subscriptions.dto.SubscriptionModeStatus
import com.papertrading.collector.application.subscriptions.dto.SubscriptionStatusReport
import com.papertrading.collector.application.subscriptions.service.SubscriptionStatusService
import com.papertrading.collector.domain.kis.WsConnectionStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

class SubscriptionStatusControllerTest {

    private lateinit var statusService: SubscriptionStatusService
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        statusService = mockk()
        val controller = SubscriptionStatusController(statusService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `GET_subscriptions_status_returns_200_with_correct_structure`() {
        val now = Instant.parse("2026-04-30T10:00:00Z")
        val lastConnected = Instant.parse("2026-04-30T09:55:00Z")
        every { statusService.report() } returns SubscriptionStatusReport(
            generatedAt = now,
            modes = listOf(
                SubscriptionModeStatus(
                    mode = "paper",
                    connectionStatus = WsConnectionStatus.CONNECTED,
                    lastConnectedAt = lastConnected,
                    reconnectAttempts = 0,
                    wsSymbols = listOf("005930"),
                    restSymbols = emptyList(),
                    wsSlotUsed = 1,
                    wsSlotMax = 41,
                )
            ),
            totalWsSlotUsed = 1,
            totalWsSlotMax = 41,
        )

        mockMvc.perform(get("/api/subscriptions/status").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.generatedAt").value("2026-04-30T10:00:00Z"))
            .andExpect(jsonPath("$.totalWsSlotUsed").value(1))
            .andExpect(jsonPath("$.totalWsSlotMax").value(41))
            .andExpect(jsonPath("$.modes[0].mode").value("paper"))
            .andExpect(jsonPath("$.modes[0].connectionStatus").value("CONNECTED"))
            .andExpect(jsonPath("$.modes[0].lastConnectedAt").value("2026-04-30T09:55:00Z"))
            .andExpect(jsonPath("$.modes[0].reconnectAttempts").value(0))
            .andExpect(jsonPath("$.modes[0].wsSymbols[0]").value("005930"))
            .andExpect(jsonPath("$.modes[0].restSymbols").isEmpty)
            .andExpect(jsonPath("$.modes[0].wsSlotUsed").value(1))
            .andExpect(jsonPath("$.modes[0].wsSlotMax").value(41))
    }

    @Test
    fun `GET_subscriptions_status_maps_null_lastConnectedAt_to_null_in_json`() {
        every { statusService.report() } returns SubscriptionStatusReport(
            generatedAt = Instant.now(),
            modes = listOf(
                SubscriptionModeStatus(
                    mode = "paper",
                    connectionStatus = WsConnectionStatus.DISCONNECTED,
                    lastConnectedAt = null,
                    reconnectAttempts = 0,
                    wsSymbols = emptyList(),
                    restSymbols = emptyList(),
                    wsSlotUsed = 0,
                    wsSlotMax = 41,
                )
            ),
            totalWsSlotUsed = 0,
            totalWsSlotMax = 41,
        )

        mockMvc.perform(get("/api/subscriptions/status").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.modes[0].lastConnectedAt").doesNotExist())
    }

    @Test
    fun `GET_subscriptions_status_returns_200_with_empty_modes_when_disabled`() {
        every { statusService.report() } returns SubscriptionStatusReport(
            generatedAt = Instant.now(),
            modes = emptyList(),
            totalWsSlotUsed = 0,
            totalWsSlotMax = 41,
        )

        mockMvc.perform(get("/api/subscriptions/status").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.modes").isArray)
            .andExpect(jsonPath("$.modes").isEmpty)
            .andExpect(jsonPath("$.totalWsSlotUsed").value(0))
    }
}
```

Verify test fails (compile error acceptable at this stage):
```bash
cd backend/collector-api && ./gradlew test --tests "*.SubscriptionStatusControllerTest" 2>&1 | tail -20
```

### Step B — Implement response DTO + Controller

**File**: `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/subscriptions/dto/SubscriptionStatusResponse.kt`

```kotlin
package com.papertrading.collector.presentation.subscriptions.dto

import com.fasterxml.jackson.annotation.JsonInclude

data class SubscriptionModeStatusResponse(
    val mode: String,
    val connectionStatus: String,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val lastConnectedAt: String?,
    val reconnectAttempts: Long,
    val wsSymbols: List<String>,
    val restSymbols: List<String>,
    val wsSlotUsed: Int,
    val wsSlotMax: Int,
)

data class SubscriptionStatusResponse(
    val generatedAt: String,
    val totalWsSlotUsed: Int,
    val totalWsSlotMax: Int,
    val modes: List<SubscriptionModeStatusResponse>,
)
```

**File**: `backend/collector-api/src/main/kotlin/com/papertrading/collector/presentation/subscriptions/SubscriptionStatusController.kt`

```kotlin
package com.papertrading.collector.presentation.subscriptions

import com.papertrading.collector.application.subscriptions.dto.SubscriptionModeStatus
import com.papertrading.collector.application.subscriptions.dto.SubscriptionStatusReport
import com.papertrading.collector.application.subscriptions.service.SubscriptionStatusService
import com.papertrading.collector.presentation.subscriptions.dto.SubscriptionModeStatusResponse
import com.papertrading.collector.presentation.subscriptions.dto.SubscriptionStatusResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/subscriptions")
class SubscriptionStatusController(
    private val statusService: SubscriptionStatusService,
) {
    @GetMapping("/status")
    fun status(): SubscriptionStatusResponse {
        return statusService.report().toResponse()
    }

    private fun SubscriptionStatusReport.toResponse() = SubscriptionStatusResponse(
        generatedAt = generatedAt.toString(),
        totalWsSlotUsed = totalWsSlotUsed,
        totalWsSlotMax = totalWsSlotMax,
        modes = modes.map { it.toResponse() },
    )

    private fun SubscriptionModeStatus.toResponse() = SubscriptionModeStatusResponse(
        mode = mode,
        connectionStatus = connectionStatus.name,
        lastConnectedAt = lastConnectedAt?.toString(),
        reconnectAttempts = reconnectAttempts,
        wsSymbols = wsSymbols,
        restSymbols = restSymbols,
        wsSlotUsed = wsSlotUsed,
        wsSlotMax = wsSlotMax,
    )
}
```

### Step C — Run all tests green + build

```bash
cd backend/collector-api && ./gradlew compileKotlin
cd backend/collector-api && ./gradlew test
```

---

## Completion Criteria

- [ ] `compileKotlin` passes with 0 errors
- [ ] All 8 tests pass (5 service + 3 controller)
- [ ] No `!!` usage, no field injection
- [ ] No hardcoded credentials
- [ ] No TODO/FIXME in new files

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
