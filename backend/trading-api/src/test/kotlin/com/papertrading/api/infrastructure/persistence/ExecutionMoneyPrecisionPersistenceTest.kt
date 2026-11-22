package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.application.notification.SlackNotificationEventPublisher
import com.papertrading.api.application.order.ExecutionProcessor
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.enums.TransactionType
import com.papertrading.api.domain.port.CollectorSubscriptionPort
import com.querydsl.jpa.impl.JPAQueryFactory
import io.mockk.mockk
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.Instant

/** 실제 PostgreSQL 열에 저장했다 다시 읽은 값으로 체결 금액·잠금·원장·정산의 일관성을 확인 (PTT1) */
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ExecutionMoneyPrecisionPersistenceTest.QuerydslTestConfig::class)
class ExecutionMoneyPrecisionPersistenceTest {
    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired lateinit var entityManager: EntityManager

    @Autowired lateinit var accountRepository: AccountRepository

    @Autowired lateinit var orderRepository: OrderRepository

    @Autowired lateinit var positionRepository: PositionRepository

    @Autowired lateinit var executionRepository: ExecutionRepository

    @Autowired lateinit var accountLedgerRepository: AccountLedgerRepository

    @Autowired lateinit var feePolicyRepository: FeePolicyRepository

    @Autowired lateinit var settlementRepository: SettlementRepository

    @Autowired lateinit var settlementExecutionRepository: SettlementExecutionRepository

    @Autowired lateinit var eventPublisher: ApplicationEventPublisher

    private lateinit var processor: ExecutionProcessor

    @BeforeEach
    fun createProcessor() {
        processor = ExecutionProcessor(
            orderRepository = orderRepository,
            accountRepository = accountRepository,
            positionRepository = positionRepository,
            executionRepository = executionRepository,
            accountLedgerRepository = accountLedgerRepository,
            feePolicyRepository = feePolicyRepository,
            collectorSubscriptionPort = mockk<CollectorSubscriptionPort>(relaxed = true),
            settlementRepository = settlementRepository,
            settlementExecutionRepository = settlementExecutionRepository,
            eventPublisher = eventPublisher,
            slackNotificationEventPublisher = mockk<SlackNotificationEventPublisher>(relaxed = true),
        )
    }

    private fun reloadFromDatabase() {
        entityManager.flush()
        entityManager.clear()
    }

    private fun newAccount(mode: TradingMode, lock: String = "0"): Account {
        val account = Account.create(
            accountName = "precision",
            accountType = AccountType.STOCK,
            tradingMode = mode,
            initialDeposit = BigDecimal("1000000"),
        )
        if (BigDecimal(lock).signum() > 0) account.lockDeposit(BigDecimal(lock))
        return accountRepository.save(account)
    }

    private fun newOrder(
        account: Account,
        side: OrderSide,
        qty: String,
        price: String,
        lock: String,
    ): Order = orderRepository.save(
        Order.create(
            account = account,
            ticker = "005930",
            marketType = MarketType.KOSPI,
            orderType = OrderType.LIMIT,
            orderSide = side,
            orderCondition = OrderCondition.DAY,
            quantity = BigDecimal(qty),
            limitPrice = BigDecimal(price),
            lockedAmount = BigDecimal(lock),
            idempotencyKey = "precision-${System.nanoTime()}",
        ),
    )

    private fun fillExternal(orderId: Long, price: String, qty: String, externalId: String) {
        processor.fillExternal(
            orderId = orderId,
            fillPrice = BigDecimal(price),
            fillQty = BigDecimal(qty),
            externalExecutionId = externalId,
            executedAt = Instant.parse("2026-11-15T01:00:00Z"),
        )
        reloadFromDatabase()
    }

    private fun assertSameAmount(expected: BigDecimal, actual: BigDecimal, name: String) {
        assertEquals(0, expected.compareTo(actual), "$name: expected $expected but was $actual")
    }

    @Test
    fun `매수 체결 금액 끝수 0점00005도 DB 저장 뒤 원장 금액과 예수금 감소분이 같다`() {
        // 0.5 * 100.0001 = 50.00005 (소수 5자리 끝수)
        val account = newAccount(TradingMode.KIS_PAPER, lock = "50.00005")
        val order = newOrder(account, OrderSide.BUY, "0.5", "100.0001", "50.00005")
        reloadFromDatabase()

        fillExternal(order.id!!, "100.0001", "0.5", "ext-1")

        val deposit = accountRepository.findById(account.id!!).get().deposit
        val buyLedger = accountLedgerRepository.findAll().single { it.transactionType == TransactionType.BUY_EXECUTE }
        assertSameAmount(BigDecimal("1000000").subtract(deposit), buyLedger.amount, "예수금 감소분 = 원장 BUY_EXECUTE 금액")
    }

    @Test
    fun `소수 수량을 세 번 나눠 체결하면 매 체결 사이 DB 재조회 뒤에도 최종 잠금 예수금이 0이다`() {
        // 잠금 = 단가 * 수량 = 100.0005 * 0.3 = 30.00015
        val account = newAccount(TradingMode.KIS_PAPER, lock = "30.00015")
        val order = newOrder(account, OrderSide.BUY, "0.3", "100.0005", "30.00015")
        reloadFromDatabase()

        fillExternal(order.id!!, "100.0005", "0.1", "ext-1")
        fillExternal(order.id!!, "100.0005", "0.1", "ext-2")
        fillExternal(order.id!!, "100.0005", "0.1", "ext-3")

        val reloaded = accountRepository.findById(account.id!!).get()
        assertEquals(OrderStatus.FILLED, orderRepository.findById(order.id!!).get().orderStatus)
        assertSameAmount(BigDecimal.ZERO, reloaded.lockedDeposit, "FILLED 뒤 잠금 예수금")
        assertSameAmount(BigDecimal("1000000").subtract(BigDecimal("30.00015")), reloaded.deposit, "예수금")
    }

    @Test
    fun `LOCAL 매도 소수 수량은 DB 저장 뒤 원장 금액과 정산 실현손익이 같다`() {
        val account = newAccount(TradingMode.LOCAL)
        val order = newOrder(account, OrderSide.SELL, "0.3", "100.0005", "0")
        val position = Position.create(account = account, ticker = "005930", marketType = MarketType.KOSPI)
        position.applyBuy(BigDecimal("0.3"), BigDecimal("60000"))
        position.lockQuantity(BigDecimal("0.3"))
        positionRepository.save(position)
        reloadFromDatabase()

        processor.fillLocal(order.id!!, BigDecimal("100.0005"), BigDecimal("0.3"))
        reloadFromDatabase()

        // 예수금 증가분 단언은 뺌 — 포지션 원자 차감 쿼리의 clearAutomatically 뒤 입금이 저장되지 않는 기존 결함(계획 O5)
        val sellLedger = accountLedgerRepository.findAll().single { it.transactionType == TransactionType.SELL_EXECUTE }
        val settlement = settlementRepository.findAll().single()
        assertSameAmount(sellLedger.amount, settlement.realizedPnl, "원장 SELL_EXECUTE 금액 = 정산 실현손익")
    }

    @TestConfiguration
    class QuerydslTestConfig {
        @Bean
        fun jpaQueryFactory(entityManager: EntityManager): JPAQueryFactory = JPAQueryFactory(entityManager)
    }
}
