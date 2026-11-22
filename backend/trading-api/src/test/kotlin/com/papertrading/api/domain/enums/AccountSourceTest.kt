package com.papertrading.api.domain.enums

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccountSourceTest {

    private val expected = mapOf(
        TradingMode.LOCAL to AccountSource.LOCAL,
        TradingMode.KIS_PAPER to AccountSource.KIS,
        TradingMode.KIS_LIVE to AccountSource.KIS,
        TradingMode.TOSS_LIVE to AccountSource.TOSS,
        TradingMode.UPBIT_LIVE to AccountSource.UPBIT,
    )

    @Test
    fun `모드_표가_TradingMode_전부를_덮는다`() {
        assertEquals(TradingMode.entries.toSet(), expected.keys)
    }

    @Test
    fun `모든_거래_모드가_정해진_출처로_바뀐다`() {
        TradingMode.entries.forEach { mode ->
            assertEquals(expected.getValue(mode), AccountSource.fromTradingMode(mode), "$mode")
        }
    }
}
