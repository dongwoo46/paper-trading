package com.papertrading.api.application.portfolio

import com.papertrading.api.common.exception.SnapshotComputeFailedException
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.DailyBalanceRepository
import com.papertrading.api.infrastructure.persistence.PortfolioSnapshotRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import com.papertrading.api.infrastructure.persistence.SnapshotJobRunRepository
import org.junit.jupiter.api.Assertions.assertThrows
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
import java.time.LocalDate

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class SnapshotJobServiceAtomicRollbackIntegrationTest {

    @Autowired
    lateinit var snapshotJobService: SnapshotJobService

    @Autowired
    lateinit var accountRepository: AccountRepository

    @Autowired
    lateinit var positionRepository: PositionRepository

    @Autowired
    lateinit var dailyBalanceRepository: DailyBalanceRepository

    @Autowired
    lateinit var portfolioSnapshotRepository: PortfolioSnapshotRepository

    @Autowired
    lateinit var snapshotJobRunRepository: SnapshotJobRunRepository

    @MockitoBean
    lateinit var portfolioSnapshotCommandService: PortfolioSnapshotCommandService

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @ServiceConnection(name = "redis")
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)
    }

    @Test
    fun `when snapshot generation fails then daily balance and snapshots are both rolled back`() {
        portfolioSnapshotRepository.deleteAll()
        dailyBalanceRepository.deleteAll()
        snapshotJobRunRepository.deleteAll()
        positionRepository.deleteAll()
        accountRepository.deleteAll()

        val account = accountRepository.save(
            Account.create(
                accountName = "rollback-account",
                accountType = AccountType.STOCK,
                tradingMode = TradingMode.LOCAL,
                initialDeposit = BigDecimal("1000000"),
            )
        )
        val businessDate = LocalDate.of(2026, 5, 2)

        val position = Position(
            account = account,
            ticker = "005930",
            marketType = MarketType.KOSPI,
            quantity = BigDecimal("10"),
            lockedQuantity = BigDecimal.ZERO,
            orderableQuantity = BigDecimal("10"),
            avgBuyPrice = BigDecimal("70000"),
            totalBuyAmount = BigDecimal("700000"),
            currentPrice = BigDecimal("71000"),
        )
        positionRepository.save(position)

        org.mockito.Mockito.`when`(portfolioSnapshotCommandService.recalculate(account.id!!, businessDate))
            .thenThrow(RuntimeException("forced-failure"))

        assertThrows(SnapshotComputeFailedException::class.java) {
            snapshotJobService.generateDailySnapshots(account.id!!, businessDate)
        }

        val dailyBalanceAfter = dailyBalanceRepository.findByAccountIdAndBusinessDate(account.id!!, businessDate)
        val snapshotsAfter = portfolioSnapshotRepository.findByAccountIdAndBusinessDateOrderByTickerAsc(account.id!!, businessDate)
        val runningAfter = snapshotJobRunRepository.existsByAccountIdAndBusinessDateAndStatus(
            account.id!!,
            businessDate,
            com.papertrading.api.domain.entity.portfolio.SnapshotJobRunStatus.RUNNING,
        )

        org.assertj.core.api.Assertions.assertThat(dailyBalanceAfter).isEmpty
        org.assertj.core.api.Assertions.assertThat(snapshotsAfter).isEmpty()
        org.assertj.core.api.Assertions.assertThat(runningAfter).isFalse()
        org.assertj.core.api.Assertions.assertThat(snapshotJobRunRepository.findAll()).isEmpty()
    }
}
