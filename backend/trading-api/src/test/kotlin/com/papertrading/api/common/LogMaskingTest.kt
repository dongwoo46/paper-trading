package com.papertrading.api.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LogMaskingTest {

    @Test
    fun `체결 id 의 계좌번호 앞자리를 가리고 주문번호와 체결번호는 남긴다`() {
        assertEquals(
            "KIS_LIVE:******00-01:0000123:0002",
            maskKisAccountNumber("KIS_LIVE:50000000-01:0000123:0002"),
        )
    }

    @Test
    fun `계좌 범위만 있어도 가린다`() {
        assertEquals("******78-01", maskKisAccountNumber("12345678-01"))
    }

    @Test
    fun `계좌번호가 없는 체결 id 는 그대로다`() {
        assertEquals("KIS_PAPER:0000123:0002", maskKisAccountNumber("KIS_PAPER:0000123:0002"))
    }
}
