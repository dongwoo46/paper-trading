package com.papertrading.api.application.position

import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.Account
import com.papertrading.api.domain.model.Position
import com.papertrading.api.domain.port.MarketQuotePort
import com.papertrading.api.domain.port.QuoteSnapshot
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.Instant

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class PositionQueryServiceTest {

    @Autowired
    lateinit var positionQueryService: PositionQueryService

    @Autowired
    lateinit var positionRepository: PositionRepository

    @Autowired
    lateinit var accountRepository: AccountRepository

    @MockitoBean
    lateinit var marketQuotePort: MarketQuotePort

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @ServiceConnection(name = "redis")
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)
    }

    private lateinit var account: Account

    @BeforeEach
    fun setUp() {
        positionRepository.deleteAll()
        accountRepository.deleteAll()
        account = accountRepository.save(
            Account.create(
                accountName = "테스트계좌",
                accountType = AccountType.STOCK,
                tradingMode = TradingMode.LOCAL,
                initialDeposit = BigDecimal("1000000"),
            )
        )
        // MarketQuotePort: 기본 null 반환 (no quote)
        org.mockito.Mockito.`when`(marketQuotePort.getQuote(org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(null)
    }

    private fun savePosition(ticker: String, qty: BigDecimal, avgPrice: BigDecimal): Position {
        val pos = Position(
            ticker = ticker,
            marketType = MarketType.KOSPI,
            quantity = qty,
            lockedQuantity = BigDecimal.ZERO,
            orderableQuantity = qty,
            avgBuyPrice = avgPrice,
            totalBuyAmount = avgPrice.multiply(qty),
        )
        pos.account = account
        return positionRepository.save(pos)
    }

    @Test
    fun `listPositionsWithCurrentPrice_returns_positions_with_quantity_greater_than_zero`() {
        savePosition("005930", BigDecimal("10"), BigDecimal("70000"))
        savePosition("035720", BigDecimal.ZERO, BigDecimal("50000")) // 수량 0 → 제외

        val results = positionQueryService.listPositionsWithCurrentPrice(account.id!!)

        assertThat(results).hasSize(1)
        assertThat(results[0].ticker).isEqualTo("005930")
    }

    @Test
    fun `listPositionsWithCurrentPrice_injects_redis_current_price_when_quote_available`() {
        savePosition("005930", BigDecimal("10"), BigDecimal("70000"))

        val quote = QuoteSnapshot(
            ticker = "005930",
            price = BigDecimal("75000"),
            askp1 = BigDecimal("75100"),
            bidp1 = BigDecimal("74900"),
            updatedAt = Instant.now(),
        )
        org.mockito.Mockito.`when`(marketQuotePort.getQuote("005930")).thenReturn(quote)

        val results = positionQueryService.listPositionsWithCurrentPrice(account.id!!)

        assertThat(results).hasSize(1)
        assertThat(results[0].currentPrice).isEqualByComparingTo("75000")
        assertThat(results[0].priceSource).isEqualTo(PriceSource.REDIS_LIVE)
        // evaluationAmount = 75000 * 10 = 750000
        assertThat(results[0].evaluationAmount).isEqualByComparingTo("750000")
    }

    @Test
    fun `listPositionsWithCurrentPrice_uses_db_price_when_no_redis_quote`() {
        val pos = savePosition("005930", BigDecimal("10"), BigDecimal("70000"))
        // DB에 저장된 마지막 현재가 없음
        org.mockito.Mockito.`when`(marketQuotePort.getQuote("005930")).thenReturn(null)

        val results = positionQueryService.listPositionsWithCurrentPrice(account.id!!)

        assertThat(results).hasSize(1)
        assertThat(results[0].currentPrice).isNull()
    }

    @Test
    fun `getPositionWithCurrentPrice_returns_position_with_injected_price`() {
        savePosition("005930", BigDecimal("5"), BigDecimal("60000"))

        val quote = QuoteSnapshot(
            ticker = "005930",
            price = BigDecimal("65000"),
            askp1 = BigDecimal("65100"),
            bidp1 = BigDecimal("64900"),
            updatedAt = Instant.now(),
        )
        org.mockito.Mockito.`when`(marketQuotePort.getQuote("005930")).thenReturn(quote)

        val result = positionQueryService.getPositionWithCurrentPrice(account.id!!, "005930")

        assertThat(result.ticker).isEqualTo("005930")
        assertThat(result.currentPrice).isEqualByComparingTo("65000")
        assertThat(result.quantity).isEqualByComparingTo("5")
        assertThat(result.avgBuyPrice).isEqualByComparingTo("60000")
    }

    @Test
    fun `getPositionWithCurrentPrice_throws_when_ticker_not_found`() {
        assertThatThrownBy {
            positionQueryService.getPositionWithCurrentPrice(account.id!!, "NOTEXIST")
        }.isInstanceOf(NoSuchElementException::class.java)
            .hasMessageContaining("NOTEXIST")
    }
}