package com.papertrading.collector.presentation.subscriptions

import com.papertrading.collector.application.subscriptions.service.FavoritesListResponse
import com.papertrading.collector.application.subscriptions.service.FavoritesWriteResponse
import com.papertrading.collector.application.subscriptions.service.RoutingStatusResponse
import com.papertrading.collector.application.subscriptions.service.RoutingSymbols
import com.papertrading.collector.application.subscriptions.service.RoutingSources
import com.papertrading.collector.application.subscriptions.service.StrategySymbolsListResponse
import com.papertrading.collector.application.subscriptions.service.StrategySymbolsWriteResponse
import com.papertrading.collector.application.subscriptions.service.SubscriptionRoutingService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

class SubscriptionRoutingControllerTest {
    private lateinit var service: SubscriptionRoutingService
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        service = mockk()
        val controller = SubscriptionRoutingController(service)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `GET favorites returns spec shape`() {
        every { service.getFavorites("paper", "ws") } returns FavoritesListResponse(
            mode = "paper",
            channel = "ws",
            items = listOf("005930"),
            returnedCount = 1,
            status = "ok",
        )

        mockMvc.perform(get("/api/subscriptions/favorites?mode=paper&channel=ws").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.mode").value("paper"))
            .andExpect(jsonPath("$.channel").value("ws"))
            .andExpect(jsonPath("$.items[0]").value("005930"))
            .andExpect(jsonPath("$.returnedCount").value(1))
            .andExpect(jsonPath("$.status").value("ok"))
    }

    @Test
    fun `POST favorites supports idempotent already_exists status`() {
        every { service.addFavorite("paper", "ws", "005930") } returns FavoritesWriteResponse(
            status = "already_exists",
            mode = "paper",
            channel = "ws",
            symbol = "005930",
            totalSelected = 3,
        )

        mockMvc.perform(
            post("/api/subscriptions/favorites")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mode":"paper","channel":"ws","symbol":"005930"}""")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("already_exists"))
            .andExpect(jsonPath("$.totalSelected").value(3))
    }

    @Test
    fun `DELETE strategy-symbols supports idempotent not_found status`() {
        every { service.removeStrategySymbol("paper", "035420") } returns StrategySymbolsWriteResponse(
            status = "not_found",
            mode = "paper",
            symbol = "035420",
            totalSelected = 0,
        )

        mockMvc.perform(
            delete("/api/subscriptions/strategy-symbols")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mode":"paper","symbol":"035420"}""")
                .accept(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("not_found"))
            .andExpect(jsonPath("$.totalSelected").value(0))
    }

    @Test
    fun `GET routing-status returns ws rest and sources`() {
        every { service.getRoutingStatus("paper") } returns RoutingStatusResponse(
            generatedAt = Instant.parse("2026-05-05T10:15:30Z").toString(),
            mode = "paper",
            ws = RoutingSymbols(slotUsed = 2, slotMax = 40, symbols = listOf("005930", "000660")),
            rest = RoutingSymbols(slotUsed = 0, slotMax = 0, symbols = listOf("035420")),
            sources = RoutingSources(
                manual = listOf("005930"),
                favorites = listOf("000660"),
                strategyPriority = listOf("035420"),
            ),
            status = "ok",
        )

        mockMvc.perform(get("/api/subscriptions/routing-status?mode=paper").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.ws.symbols[0]").value("005930"))
            .andExpect(jsonPath("$.rest.symbols[0]").value("035420"))
            .andExpect(jsonPath("$.sources.favorites[0]").value("000660"))
    }

    @Test
    fun `GET strategy-symbols returns list shape`() {
        every { service.getStrategySymbols("paper") } returns StrategySymbolsListResponse(
            mode = "paper",
            items = listOf("035420"),
            returnedCount = 1,
            status = "ok",
        )

        mockMvc.perform(get("/api/subscriptions/strategy-symbols?mode=paper").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.returnedCount").value(1))
            .andExpect(jsonPath("$.items[0]").value("035420"))
    }
}


