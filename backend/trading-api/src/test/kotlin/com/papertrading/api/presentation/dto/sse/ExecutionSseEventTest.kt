package com.papertrading.api.presentation.dto.sse

import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.event.ExecutionFilledEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class ExecutionSseEventTest {

    private val sampleEvent = ExecutionFilledEvent(
        executionId = 42L,
        orderId = 7L,
        ticker = "005930",
        side = OrderSide.BUY,
        quantity = BigDecimal("10"),
        price = BigDecimal("75000.0000"),
        fee = BigDecimal("150.0000"),
        currency = "KRW",
        executedAt = Instant.parse("2026-04-30T08:30:00Z"),
    )

    @Test
    fun `from converts executionId and orderId correctly`() {
        val sseEvent = ExecutionSseEvent.from(sampleEvent)
        assertEquals(42L, sseEvent.executionId)
        assertEquals(7L, sseEvent.orderId)
    }

    @Test
    fun `from converts ticker and tickerName is null`() {
        val sseEvent = ExecutionSseEvent.from(sampleEvent)
        assertEquals("005930", sseEvent.ticker)
        assertNull(sseEvent.tickerName)
    }

    @Test
    fun `from converts side to name string`() {
        val sseEvent = ExecutionSseEvent.from(sampleEvent)
        assertEquals("BUY", sseEvent.side)
    }

    @Test
    fun `from converts BigDecimal fields to plain strings`() {
        val sseEvent = ExecutionSseEvent.from(sampleEvent)
        assertEquals("10", sseEvent.quantity)
        assertEquals("75000.0000", sseEvent.price)
        assertEquals("150.0000", sseEvent.fee)
    }

    @Test
    fun `from converts currency correctly`() {
        val sseEvent = ExecutionSseEvent.from(sampleEvent)
        assertEquals("KRW", sseEvent.currency)
    }

    @Test
    fun `from converts executedAt to ISO instant string`() {
        val sseEvent = ExecutionSseEvent.from(sampleEvent)
        assertEquals("2026-04-30T08:30:00Z", sseEvent.executedAt)
    }

    @Test
    fun `from converts SELL side correctly`() {
        val sellEvent = sampleEvent.copy(side = OrderSide.SELL)
        val sseEvent = ExecutionSseEvent.from(sellEvent)
        assertEquals("SELL", sseEvent.side)
    }

    /** Spec: from_BigDecimal_fields_are_plain_strings — quantity "10.00" must serialize as "10.00", no scientific notation */
    @Test
    fun `from BigDecimal fields are plain strings — no scientific notation`() {
        val event = sampleEvent.copy(
            quantity = BigDecimal("10.00"),
            price = BigDecimal("75000.00"),
            fee = BigDecimal("150.00"),
        )
        val sseEvent = ExecutionSseEvent.from(event)
        assertEquals("10.00", sseEvent.quantity)
        assertEquals("75000.00", sseEvent.price)
        assertEquals("150.00", sseEvent.fee)
        // Verify no scientific notation (e.g., not "1E+5")
        assert(!sseEvent.price.contains('E') && !sseEvent.price.contains('e')) {
            "price must not contain scientific notation: ${sseEvent.price}"
        }
    }

    /** Spec: from_executedAt_is_ISO_instant_string */
    @Test
    fun `from executedAt is ISO instant string`() {
        val instant = Instant.parse("2026-04-30T08:30:00Z")
        val event = sampleEvent.copy(executedAt = instant)
        val sseEvent = ExecutionSseEvent.from(event)
        assertEquals("2026-04-30T08:30:00Z", sseEvent.executedAt)
    }

    /** Spec: from_side_is_string_name_of_enum */
    @Test
    fun `from side is string name of enum — OrderSide BUY`() {
        val event = sampleEvent.copy(side = OrderSide.BUY)
        val sseEvent = ExecutionSseEvent.from(event)
        assertEquals("BUY", sseEvent.side)
    }
}
