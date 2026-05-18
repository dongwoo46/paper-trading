package com.papertrading.api.application.position

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class PositionExitQuoteEventListenerTest {
    @Test
    fun `quote 이벤트를 orchestrator에 전달한다`() {
        val orchestrator = mockk<PositionExitTriggerOrchestrator>(relaxed = true)
        val listener = PositionExitQuoteEventListener(orchestrator)
        val now = Instant.now()

        listener.onQuote("005930", BigDecimal("100"), now)

        verify(exactly = 1) { orchestrator.onQuote("005930", BigDecimal("100"), now) }
    }
}
