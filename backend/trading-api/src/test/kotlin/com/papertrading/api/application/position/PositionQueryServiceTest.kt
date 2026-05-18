package com.papertrading.api.application.position

import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.common.exception.PositionNotFoundException
import com.papertrading.api.domain.port.LivePositionCachePort
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles
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

    @Autowired
    lateinit var positionCommandService: PositionCommandService

    @Autowired
    lateinit var livePositionCachePort: LivePositionCachePort

    @Autowired
    lateinit var redisTemplate: StringRedisTemplate

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
        redisTemplate.connectionFactory?.connection?.serverCommands()?.flushDb()
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
    }

    private fun savePosition(ticker: String, qty: BigDecimal, avgPrice: BigDecimal): Position {
        val pos = Position.createWithHolding(
            account = account,
            ticker = ticker,
            marketType = MarketType.KOSPI,
            quantity = qty,
            avgBuyPrice = avgPrice,
        )
        return positionRepository.save(pos)
    }

    private fun saveQuote(mode: String, ticker: String, price: String) {
        val key = if (mode == "local") {
            "quote:local:${ticker.uppercase()}"
        } else {
            "quote:kis:$mode:${ticker.uppercase()}"
        }
        redisTemplate.opsForHash<String, String>().putAll(
            key,
            mapOf(
                "price" to price,
                "askp1" to price,
                "bidp1" to price,
                "updatedAt" to Instant.now().toEpochMilli().toString(),
            ),
        )
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
    fun `listPositionsWithCurrentPrice_returns_cached_live_position_when_cache_exists`() {
        savePosition("005930", BigDecimal("10"), BigDecimal("70000"))
        positionCommandService.updateCurrentPriceByTicker("005930", BigDecimal("75000"), PriceSource.REDIS_LIVE, TradingMode.LOCAL)

        val results = positionQueryService.listPositionsWithCurrentPrice(account.id!!)

        assertThat(results).hasSize(1)
        assertThat(results[0].currentPrice).isEqualByComparingTo("75000")
        assertThat(results[0].priceSource).isEqualTo(PriceSource.REDIS_LIVE)
        assertThat(results[0].evaluationAmount).isEqualByComparingTo("750000")
        assertThat(results[0].unrealizedPnl).isEqualByComparingTo("50000")
    }

    @Test
    fun `listPositionsWithCurrentPrice_uses_db_position_when_live_cache_missing`() {
        savePosition("005930", BigDecimal("10"), BigDecimal("70000"))

        val results = positionQueryService.listPositionsWithCurrentPrice(account.id!!)

        assertThat(results).hasSize(1)
        assertThat(results[0].currentPrice).isNull()
        assertThat(livePositionCachePort.find(account.id!!, "005930")).isNotNull()
    }

    @Test
    fun `listPositionsWithCurrentPrice_evaluates_db_position_with_latest_quote_when_live_cache_missing`() {
        savePosition("005930", BigDecimal("10"), BigDecimal("70000"))
        saveQuote("local", "005930", "75000")

        val results = positionQueryService.listPositionsWithCurrentPrice(account.id!!)

        assertThat(results).hasSize(1)
        assertThat(results[0].currentPrice).isEqualByComparingTo("75000")
        assertThat(results[0].evaluationAmount).isEqualByComparingTo("750000")
        assertThat(results[0].unrealizedPnl).isEqualByComparingTo("50000")
        assertThat(livePositionCachePort.find(account.id!!, "005930")!!.currentPrice).isEqualByComparingTo("75000")
    }

    @Test
    fun `listPositionsWithCurrentPrice_returns_account_live_cache_when_cache_exists`() {
        savePosition("005930", BigDecimal("10"), BigDecimal("70000"))
        savePosition("035720", BigDecimal("5"), BigDecimal("50000"))
        positionCommandService.updateCurrentPriceByTicker("005930", BigDecimal("75000"), PriceSource.REDIS_LIVE, TradingMode.LOCAL)

        val results = positionQueryService.listPositionsWithCurrentPrice(account.id!!)

        assertThat(results.map { it.ticker }).containsExactly("005930")
        assertThat(results.single { it.ticker == "005930" }.currentPrice).isEqualByComparingTo("75000")
    }

    @Test
    fun `getPositionWithCurrentPrice_returns_cached_live_position_when_cache_exists`() {
        savePosition("005930", BigDecimal("5"), BigDecimal("60000"))
        positionCommandService.updateCurrentPriceByTicker("005930", BigDecimal("65000"), PriceSource.REDIS_LIVE, TradingMode.LOCAL)

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
        }.isInstanceOf(PositionNotFoundException::class.java)
            .hasMessageContaining("NOTEXIST")
    }
}
