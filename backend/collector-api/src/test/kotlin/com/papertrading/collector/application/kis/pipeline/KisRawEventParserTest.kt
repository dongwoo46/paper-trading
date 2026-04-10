package com.papertrading.collector.application.kis.pipeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class KisRawEventParserTest {

    private val parser = KisRawEventParser()

    // H0STCNT0 raw 메시지 픽스처
    // 포맷: {flag}|H0STCNT0|{count}|{^구분 필드}
    // [0]=ticker [2]=현재가 [8]=고가 [9]=저가 [10]=매도호가1 [11]=매수호가1 [12]=체결거래량
    private fun buildH0STCNT0(
        ticker: String = "005930",
        price: String = "75000",
        high: String = "75500",
        low: String = "74500",
        askp1: String = "75100",
        bidp1: String = "74900",
        volume: String = "1000",
    ): String {
        val fields = Array(46) { "0" }
        fields[0] = ticker
        fields[1] = "092315"  // STCK_CNTG_HOUR
        fields[2] = price     // STCK_PRPR
        fields[8] = high      // STCK_HGPR
        fields[9] = low       // STCK_LWPR
        fields[10] = askp1    // ASKP1
        fields[11] = bidp1    // BIDP1
        fields[12] = volume   // CNTG_VOL
        return "0|H0STCNT0|001|${fields.joinToString("^")}"
    }

    @Test
    fun `H0STCNT0 체결 메시지를 파싱하면 QuoteEvent를 반환한다`() {
        val raw = buildH0STCNT0(
            ticker = "005930",
            price = "75000",
            high = "75500",
            low = "74500",
            askp1 = "75100",
            bidp1 = "74900",
            volume = "1000",
        )

        val result = parser.parse(raw)

        assertEquals("005930", result?.ticker)
        assertEquals(BigDecimal("75000"), result?.price)
        assertEquals(BigDecimal("75500"), result?.high)
        assertEquals(BigDecimal("74500"), result?.low)
        assertEquals(BigDecimal("75100"), result?.askp1)
        assertEquals(BigDecimal("74900"), result?.bidp1)
        assertEquals(BigDecimal("1000"), result?.volume)
    }

    @Test
    fun `H0STASP0 호가 메시지는 null을 반환한다`() {
        val raw = "0|H0STASP0|001|005930^092315^0^0^0^0^0^0^0^0^0^0^0^0"

        assertNull(parser.parse(raw))
    }

    @Test
    fun `시스템 메시지(JSON)는 null을 반환한다`() {
        val raw = """{"header":{"tr_id":"PINGPONG","tr_key":""},"body":null}"""

        assertNull(parser.parse(raw))
    }

    @Test
    fun `필드 수가 부족한 메시지는 null을 반환한다`() {
        val raw = "0|H0STCNT0|001|005930^092315"

        assertNull(parser.parse(raw))
    }

    @Test
    fun `가격 필드가 숫자가 아니면 null을 반환한다`() {
        val raw = buildH0STCNT0(price = "N/A")

        assertNull(parser.parse(raw))
    }

    @Test
    fun `매도호가1이 숫자가 아니면 null을 반환한다`() {
        val raw = buildH0STCNT0(askp1 = "-")

        assertNull(parser.parse(raw))
    }
}