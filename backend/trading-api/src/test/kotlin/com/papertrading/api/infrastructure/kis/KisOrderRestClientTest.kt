package com.papertrading.api.infrastructure.kis

import com.papertrading.api.support.withId
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.order.Order
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal

class KisOrderRestClientTest {

    private val properties = KisOrderProperties().apply {
        paperRestBaseUrl = "https://kis.example"
        paperAppKey = "app-key"
        paperAppSecret = "app-secret"
    }
    private val tokenManager = mockk<KisTokenManager>()
    private val restTemplate = mockk<RestTemplate>()
    private val client = KisOrderRestClient(properties, tokenManager, restTemplate)

    @Test
    fun `place order maps non zero KIS response to typed exception without credentials`() {
        every { tokenManager.getToken("paper") } returns "secret-token"
        every {
            restTemplate.exchange(
                any<String>(),
                HttpMethod.POST,
                any(),
                any<Class<*>>(),
            )
        } returns ResponseEntity.ok(mapOf("rt_cd" to "1", "msg1" to "invalid account"))

        val error = assertThrows(KisApiException::class.java) {
            client.placeOrder(sampleOrder(), "paper")
        }

        assertEquals(KisErrorCode.KIS_INVALID_ACCOUNT, error.code)
        requireNotNull(error.message)
        assert(!error.message!!.contains("secret-token"))
        assert(!error.message!!.contains("app-secret"))
    }

    private fun sampleOrder(): Order {
        val account = Account.create(
            accountName = "kis",
            accountType = AccountType.STOCK,
            tradingMode = TradingMode.KIS_PAPER,
            initialDeposit = BigDecimal("1000000"),
            externalAccountId = "12345678-01",
        ).withId(1L)
        return Order.create(
            account = account,
            ticker = "005930",
            marketType = MarketType.KOSPI,
            orderType = OrderType.LIMIT,
            orderSide = OrderSide.BUY,
            orderCondition = OrderCondition.DAY,
            quantity = BigDecimal("1"),
            limitPrice = BigDecimal("70000"),
            lockedAmount = BigDecimal("70000"),
            idempotencyKey = "key",
        ).withId(10L)
    }
}
