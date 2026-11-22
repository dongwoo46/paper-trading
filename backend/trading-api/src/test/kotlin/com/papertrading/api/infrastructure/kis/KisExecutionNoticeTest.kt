package com.papertrading.api.infrastructure.kis

import com.papertrading.api.application.order.port.BrokerExecutionNotice
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.TradingMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class KisExecutionNoticeTest {

    private fun kisNotice(accountNumber: String? = "12345678", accountProductCode: String? = "01") = KisExecutionNotice(
        mode = TradingMode.KIS_LIVE,
        channelId = "H0STCNI0",
        externalOrderId = "10000001",
        externalExecutionId = "KIS_LIVE:12345678-01:10000001:00003",
        ticker = "005930",
        side = OrderSide.SELL,
        executedQty = BigDecimal("3"),
        executedPrice = BigDecimal("70000.5"),
        executedAt = Instant.parse("2026-05-01T00:30:15Z"),
        accountNumber = accountNumber,
        accountProductCode = accountProductCode,
    )

    @Test
    fun `KIS 체결 통지는 모드와 계좌 범위를 담은 브로커 체결 통지로 바뀐다`() {
        val result = kisNotice().toBrokerExecutionNotice()

        assertEquals(
            BrokerExecutionNotice(
                mode = TradingMode.KIS_LIVE,
                externalOrderId = "10000001",
                externalExecutionId = "KIS_LIVE:12345678-01:10000001:00003",
                executedQty = BigDecimal("3"),
                executedPrice = BigDecimal("70000.5"),
                executedAt = Instant.parse("2026-05-01T00:30:15Z"),
                accountScope = "12345678-01",
            ),
            result,
        )
    }

    @Test
    fun `계좌 상품코드가 없으면 계좌 범위 없이 바뀐다`() {
        val result = kisNotice(accountProductCode = null).toBrokerExecutionNotice()

        assertNull(result.accountScope)
    }
}
