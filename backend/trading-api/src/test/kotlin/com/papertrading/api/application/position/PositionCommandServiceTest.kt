package com.papertrading.api.application.position

import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.port.LivePositionCachePort
import com.papertrading.api.domain.port.LivePositionSnapshot
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
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

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class PositionCommandServiceTest {

    @Autowired
    lateinit var positionCommandService: PositionCommandService

    @Autowired
    lateinit var positionRepository: PositionRepository

    @Autowired
    lateinit var accountRepository: AccountRepository

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
                accountName = "커맨드테스트계좌",
                accountType = AccountType.STOCK,
                tradingMode = TradingMode.LOCAL,
                initialDeposit = BigDecimal("1000000"),
            )
        )
    }

    private fun savePosition(ticker: String, qty: BigDecimal, avgPrice: BigDecimal): Position {
        return savePosition(account, ticker, qty, avgPrice)
    }

    private fun savePosition(account: Account, ticker: String, qty: BigDecimal, avgPrice: BigDecimal): Position {
        val pos = Position.createWithHolding(
            account = account,
            ticker = ticker,
            marketType = MarketType.KOSPI,
            quantity = qty,
            avgBuyPrice = avgPrice,
        )
        return positionRepository.save(pos)
    }

    private fun saveAccount(name: String, tradingMode: TradingMode): Account =
        accountRepository.save(
            Account.create(
                accountName = name,
                accountType = AccountType.STOCK,
                tradingMode = tradingMode,
                initialDeposit = BigDecimal("1000000"),
            )
        )

    @Test
    fun `updateCurrentPriceByTicker_updates_live_position_cache_without_mutating_db_price_fields`() {
        savePosition("005930", BigDecimal("10"), BigDecimal("70000"))

        positionCommandService.updateCurrentPriceByTicker("005930", BigDecimal("75000"), PriceSource.REDIS_LIVE, TradingMode.LOCAL)

        val dbPosition = positionRepository.findByAccountIdAndQuantityGreaterThan(account.id!!, BigDecimal.ZERO).single()
        assertThat(dbPosition.currentPrice).isNull()
        assertThat(dbPosition.evaluationAmount).isNull()
        assertThat(dbPosition.unrealizedPnl).isNull()

        val cached = livePositionCachePort.find(account.id!!, "005930")
        assertThat(cached).isNotNull
        assertThat(cached!!.currentPrice).isEqualByComparingTo("75000")
        assertThat(cached.priceSource).isEqualTo(PriceSource.REDIS_LIVE)
        assertThat(cached.evaluationAmount).isEqualByComparingTo("750000")
        assertThat(cached.unrealizedPnl).isEqualByComparingTo("50000")
        assertThat(cached.returnRate).isEqualByComparingTo("0.0714")
        assertThat(cached.priceUpdatedAt).isNotNull()
    }

    @Test
    fun `updateCurrentPriceByTicker_updates_cached_snapshots_without_db_select`() {
        val repository = mock(PositionRepository::class.java)
        val cache = InMemoryLivePositionCachePort()
        cache.save(
            LivePositionSnapshot(
                id = 1L,
                accountId = 1L,
                tradingMode = TradingMode.KIS_PAPER,
                ticker = "005930",
                marketType = MarketType.KOSPI,
                quantity = BigDecimal("10"),
                orderableQuantity = BigDecimal("10"),
                lockedQuantity = BigDecimal.ZERO,
                avgBuyPrice = BigDecimal("70000"),
                totalBuyAmount = BigDecimal("700000"),
                currentPrice = null,
                evaluationAmount = null,
                unrealizedPnl = null,
                returnRate = null,
                priceSource = PriceSource.UNKNOWN,
                priceUpdatedAt = null,
            )
        )
        val service = PositionCommandService(repository, cache)

        service.updateCurrentPriceByTicker("005930", BigDecimal("75000"), PriceSource.REDIS_LIVE, TradingMode.KIS_PAPER)

        verify(repository, never()).findOpenByTickerAndMode("005930", TradingMode.KIS_PAPER, BigDecimal.ZERO)
        val cached = cache.find(1L, "005930")
        assertThat(cached!!.currentPrice).isEqualByComparingTo("75000")
        assertThat(cached.evaluationAmount).isEqualByComparingTo("750000")
        assertThat(cached.unrealizedPnl).isEqualByComparingTo("50000")
    }

    @Test
    fun `updateCurrentPriceByTicker_updates_only_positions_for_quote_mode`() {
        val paperAccount = saveAccount("paper-account", TradingMode.KIS_PAPER)
        val liveAccount = saveAccount("live-account", TradingMode.KIS_LIVE)
        savePosition(paperAccount, "005930", BigDecimal("10"), BigDecimal("70000"))
        savePosition(liveAccount, "005930", BigDecimal("10"), BigDecimal("70000"))

        positionCommandService.updateCurrentPriceByTicker(
            "005930",
            BigDecimal("75000"),
            PriceSource.REDIS_LIVE,
            TradingMode.KIS_PAPER,
        )

        assertThat(livePositionCachePort.find(paperAccount.id!!, "005930")!!.currentPrice)
            .isEqualByComparingTo("75000")
        assertThat(livePositionCachePort.find(liveAccount.id!!, "005930")).isNull()
    }

    @Test
    fun `updateCurrentPriceByTicker_skips_positions_with_zero_quantity`() {
        savePosition("005930", BigDecimal.ZERO, BigDecimal("70000")) // 청산 포지션

        // 예외 없이 정상 종료 (처리할 포지션 없음)
        positionCommandService.updateCurrentPriceByTicker("005930", BigDecimal("75000"), PriceSource.REDIS_LIVE, TradingMode.LOCAL)

        val positions = positionRepository.findAll()
        assertThat(positions[0].currentPrice).isNull()
        assertThat(livePositionCachePort.find(account.id!!, "005930")).isNull()
    }

    @Test
    fun `updateCurrentPriceByTicker_does_nothing_when_no_positions_exist`() {
        // 포지션 없을 때 예외 없이 정상 종료
        positionCommandService.updateCurrentPriceByTicker("NOTEXIST", BigDecimal("10000"), PriceSource.REDIS_LIVE, TradingMode.LOCAL)
        assertThat(positionRepository.findAll()).isEmpty()
    }

    private class InMemoryLivePositionCachePort : LivePositionCachePort {
        private val snapshots = linkedMapOf<String, LivePositionSnapshot>()

        override fun save(snapshot: LivePositionSnapshot): LivePositionSnapshot {
            snapshots[key(snapshot.accountId, snapshot.ticker)] = snapshot
            return snapshot
        }

        override fun find(accountId: Long, ticker: String): LivePositionSnapshot? =
            snapshots[key(accountId, ticker)]

        override fun findByAccountId(accountId: Long): List<LivePositionSnapshot> =
            snapshots.values.filter { it.accountId == accountId && it.quantity > BigDecimal.ZERO }

        override fun findByTicker(ticker: String): List<LivePositionSnapshot> =
            snapshots.values.filter { it.ticker == ticker.uppercase() && it.quantity > BigDecimal.ZERO }

        override fun findByTickerAndMode(ticker: String, tradingMode: TradingMode): List<LivePositionSnapshot> =
            snapshots.values.filter {
                it.ticker == ticker.uppercase() && it.tradingMode == tradingMode && it.quantity > BigDecimal.ZERO
            }

        private fun key(accountId: Long, ticker: String): String = "$accountId:${ticker.uppercase()}"
    }
}
