package com.papertrading.collector.application.kis.pipeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class KisOrderbookEventParserTest {

    private val parser = KisOrderbookEventParser()

    /**
     * H0STASP0 메시지 픽스처 생성
     * 포맷: {flag}|H0STASP0|{count}|{^구분 필드}
     *
     * [0]  MKSC_SHRN_ISCD  — ticker
     * [1]  BSOP_HOUR
     * [2]  HOUR_CLS_CODE
     * [3..7]   ASKP1..ASKP5   — 매도호가 (best ask first)
     * [8..12]  (reserved)
     * [13..17] BIDP1..BIDP5   — 매수호가 (best bid first)
     * [18..22] (reserved)
     * [23..27] ASKP_RSQN1..ASKP_RSQN5 — 매도잔량
     * [28..32] (reserved)
     * [33..37] BIDP_RSQN1..BIDP_RSQN5 — 매수잔량
     */
    private fun buildH0STASP0(
        flag: String = "0",
        trId: String = "H0STASP0",
        ticker: String = "005930",
        askPrices: List<String> = listOf("75100", "75200", "75300", "75400", "75500"),
        bidPrices: List<String> = listOf("74900", "74800", "74700", "74600", "74500"),
        askQtys: List<String> = listOf("100", "200", "300", "400", "500"),
        bidQtys: List<String> = listOf("150", "250", "350", "450", "550"),
    ): String {
        val fields = Array(50) { "0" }
        fields[0] = ticker
        fields[1] = "092315"
        fields[2] = "0"
        // ASKP1..ASKP5: indices 3..7
        askPrices.forEachIndexed { i, v -> fields[3 + i] = v }
        // (indices 8..12 reserved)
        // BIDP1..BIDP5: indices 13..17
        bidPrices.forEachIndexed { i, v -> fields[13 + i] = v }
        // (indices 18..22 reserved)
        // ASKP_RSQN1..5: indices 23..27
        askQtys.forEachIndexed { i, v -> fields[23 + i] = v }
        // (indices 28..32 reserved)
        // BIDP_RSQN1..5: indices 33..37
        bidQtys.forEachIndexed { i, v -> fields[33 + i] = v }
        return "$flag|$trId|001|${fields.joinToString("^")}"
    }

    @Test
    fun `H0STASP0 정상 파싱 — bestAsk, bestBid, top-5 qty 검증`() {
        val raw = buildH0STASP0(
            ticker = "005930",
            askPrices = listOf("75100", "75200", "75300", "75400", "75500"),
            bidPrices = listOf("74900", "74800", "74700", "74600", "74500"),
            askQtys  = listOf("100", "200", "300", "400", "500"),
            bidQtys  = listOf("150", "250", "350", "450", "550"),
        )

        val event = parser.parse(raw)

        assertEquals("005930", event?.ticker)
        // best ask = askPrices[0]
        assertEquals(BigDecimal("75100"), event?.askPrices?.get(0))
        // best bid = bidPrices[0]
        assertEquals(BigDecimal("74900"), event?.bidPrices?.get(0))
        // top-5 lists
        assertEquals(5, event?.askPrices?.size)
        assertEquals(5, event?.bidPrices?.size)
        assertEquals(5, event?.askQtys?.size)
        assertEquals(5, event?.bidQtys?.size)
        // spot-check last element
        assertEquals(BigDecimal("75500"), event?.askPrices?.get(4))
        assertEquals(BigDecimal("74500"), event?.bidPrices?.get(4))
        assertEquals(BigDecimal("100"), event?.askQtys?.get(0))
        assertEquals(BigDecimal("550"), event?.bidQtys?.get(4))
    }

    @Test
    fun `잘못된 TR_ID — null 반환`() {
        val raw = buildH0STASP0(trId = "H0STCNT0")
        assertNull(parser.parse(raw))
    }

    @Test
    fun `flag 문자 잘못됨 — null 반환`() {
        val raw = buildH0STASP0(flag = "9")
        assertNull(parser.parse(raw))
    }

    @Test
    fun `필드 수 부족 — null 반환`() {
        // Only 10 fields — below the minimum of 38
        val raw = "0|H0STASP0|001|005930^0^0^75100^75200^75300^74900^74800^74700^74600"
        assertNull(parser.parse(raw))
    }

    @Test
    fun `숫자 파싱 오류 — null 반환`() {
        val raw = buildH0STASP0(askPrices = listOf("N/A", "75200", "75300", "75400", "75500"))
        assertNull(parser.parse(raw))
    }
}
