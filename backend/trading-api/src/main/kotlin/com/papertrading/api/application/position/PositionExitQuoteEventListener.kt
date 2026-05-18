package com.papertrading.api.application.position

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

@Component
class PositionExitQuoteEventListener(
    private val orchestrator: PositionExitTriggerOrchestrator,
) {
    fun onQuote(ticker: String, price: BigDecimal, quoteAt: Instant) {
        orchestrator.onQuote(ticker, price, quoteAt)
    }
}
