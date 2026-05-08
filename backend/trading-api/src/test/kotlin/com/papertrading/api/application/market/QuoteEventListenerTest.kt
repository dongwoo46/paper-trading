package com.papertrading.api.application.market

import com.papertrading.api.application.position.PositionExitTriggerOrchestrator
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class QuoteEventListenerTest {
    @Test
    fun `quote 이벤트를 orchestrator에 전달한다`() {
        val orchestrator = mockk<PositionExitTriggerOrchestrator>(relaxed = true)
        val listener = QuoteEventListener(orchestrator)
        val now = Instant.now()

        listener.onQuote("005930", BigDecimal("100"), now)

        verify(exactly = 1) { orchestrator.onQuote("005930", BigDecimal("100"), now) }
    }
}
