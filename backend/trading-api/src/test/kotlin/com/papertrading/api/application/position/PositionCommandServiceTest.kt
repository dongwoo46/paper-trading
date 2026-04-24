package com.papertrading.api.application.position

import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.PriceSource
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.Account
import com.papertrading.api.domain.model.Position
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
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
                accountName = "커맨드테스트계좌",
                accountType = AccountType.STOCK,
                tradingMode = TradingMode.LOCAL,
                initialDeposit = BigDecimal("1000000"),
            )
        )
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
    fun `updateCurrentPriceByTicker_updates_evaluation_fields_for_all_positions_with_ticker`() {
        savePosition("005930", BigDecimal("10"), BigDecimal("70000"))
        // ticker가 같은 다른 계좌 포지션 (수량 > 0)은 현재 테스트에서는 단일 계좌로 검증

        positionCommandService.updateCurrentPriceByTicker("005930", BigDecimal("75000"), PriceSource.REDIS_LIVE)

        val updated = positionRepository.findByAccountIdAndQuantityGreaterThan(account.id!!, BigDecimal.ZERO)
        assertThat(updated).hasSize(1)
        assertThat(updated[0].currentPrice).isEqualByComparingTo("75000")
        assertThat(updated[0].priceSource).isEqualTo(PriceSource.REDIS_LIVE)
        assertThat(updated[0].evaluationAmount).isEqualByComparingTo("750000")
        assertThat(updated[0].unrealizedPnl).isEqualByComparingTo("50000")
        assertThat(updated[0].priceUpdatedAt).isNotNull()
    }

    @Test
    fun `updateCurrentPriceByTicker_skips_positions_with_zero_quantity`() {
        savePosition("005930", BigDecimal.ZERO, BigDecimal("70000")) // 청산 포지션

        // 예외 없이 정상 종료 (처리할 포지션 없음)
        positionCommandService.updateCurrentPriceByTicker("005930", BigDecimal("75000"), PriceSource.REDIS_LIVE)

        val positions = positionRepository.findAll()
        // 수량 0 포지션의 currentPrice는 변경되지 않음
        assertThat(positions[0].currentPrice).isNull()
    }

    @Test
    fun `updateCurrentPriceByTicker_does_nothing_when_no_positions_exist`() {
        // 포지션 없을 때 예외 없이 정상 종료
        positionCommandService.updateCurrentPriceByTicker("NOTEXIST", BigDecimal("10000"), PriceSource.REDIS_LIVE)
        assertThat(positionRepository.findAll()).isEmpty()
    }
}