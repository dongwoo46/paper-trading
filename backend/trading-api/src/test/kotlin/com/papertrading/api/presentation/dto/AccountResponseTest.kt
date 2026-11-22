package com.papertrading.api.presentation.dto

import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.enums.AccountSource
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.presentation.dto.account.AccountResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal

class AccountResponseTest {

    private val expected = mapOf(
        TradingMode.LOCAL to AccountSource.LOCAL,
        TradingMode.KIS_PAPER to AccountSource.KIS,
        TradingMode.KIS_LIVE to AccountSource.KIS,
        TradingMode.TOSS_LIVE to AccountSource.TOSS,
        TradingMode.UPBIT_LIVE to AccountSource.UPBIT,
    )

    @Test
    fun `계좌_응답_source_는_계좌_모드에서_산출된다`() {
        assertEquals(TradingMode.entries.toSet(), expected.keys)
        TradingMode.entries.forEach { mode ->
            val response = AccountResponse.from(newAccount(mode))
            assertEquals(mode, response.tradingMode, "$mode")
            assertEquals(expected.getValue(mode), response.source, "$mode")
        }
    }

    private fun newAccount(mode: TradingMode): Account {
        val type = if (mode == TradingMode.UPBIT_LIVE) AccountType.CRYPTO else AccountType.STOCK
        val account = Account.create(
            accountName = "응답시험-$mode",
            accountType = type,
            tradingMode = mode,
            initialDeposit = BigDecimal("1000"),
            externalAccountId = null,
        )
        ReflectionTestUtils.setField(account, "id", 1L)
        return account
    }
}
