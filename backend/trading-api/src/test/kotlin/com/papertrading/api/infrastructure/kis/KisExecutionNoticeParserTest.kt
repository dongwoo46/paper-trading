package com.papertrading.api.infrastructure.kis

import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.TradingMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class KisExecutionNoticeParserTest {

    private val parser = KisExecutionNoticeParser()

    @Test
    fun `paper account execution notice parsed with account-scoped deterministic external execution id`() {
        val notice = parser.parse(
            rawMessage = "0|H0STCNI9|001|ODNO=10000001^CCLD_NO=00003^PDNO=005930^SLL_BUY_DVSN_CD=02^CCLD_QTY=3.5^CCLD_UNPR=70000.25^CCLD_DTTM=20260501093015000^CANO=12345678^ACNT_PRDT_CD=01",
            mode = "paper",
        )

        requireNotNull(notice)
        assertEquals(TradingMode.KIS_PAPER, notice.mode)
        assertEquals("H0STCNI9", notice.channelId)
        assertEquals("10000001", notice.externalOrderId)
        assertEquals("KIS_PAPER:12345678-01:10000001:00003", notice.externalExecutionId)
        assertEquals("005930", notice.ticker)
        assertEquals(OrderSide.BUY, notice.side)
        assertEquals(0, BigDecimal("3.5").compareTo(notice.executedQty))
        assertEquals(0, BigDecimal("70000.25").compareTo(notice.executedPrice))
        assertEquals(Instant.parse("2026-05-01T00:30:15Z"), notice.executedAt)
        assertEquals("12345678", notice.accountNumber)
        assertEquals("01", notice.accountProductCode)
        assertEquals("12345678-01", notice.accountScope)
    }

    @Test
    fun `live account execution notice accepts only live channel`() {
        val notice = parser.parse(
            rawMessage = """{"tr_id":"H0STCNI0","ODNO":"20000001","CCLD_NO":"009","PDNO":"000660","SLL_BUY_DVSN_CD":"01","CCLD_QTY":"2","CCLD_UNPR":"101500","CCLD_DTTM":"20260501100130000"}""",
            mode = "live",
        )

        requireNotNull(notice)
        assertEquals(TradingMode.KIS_LIVE, notice.mode)
        assertEquals("H0STCNI0", notice.channelId)
        assertEquals(OrderSide.SELL, notice.side)
        assertEquals("KIS_LIVE:20000001:009", notice.externalExecutionId)
        assertNull(notice.accountScope)
    }

    @Test
    fun `market wide or malformed messages are ignored`() {
        assertNull(parser.parse("0|H0STCNT0|001|MKSC_SHRN_ISCD=005930^STCK_PRPR=70000", "paper"))
        assertNull(parser.parse("0|H0STCNI9|001|ODNO=10000001^CCLD_QTY=bad", "paper"))
        assertNull(parser.parse("not-a-kis-message", "paper"))
    }
}
