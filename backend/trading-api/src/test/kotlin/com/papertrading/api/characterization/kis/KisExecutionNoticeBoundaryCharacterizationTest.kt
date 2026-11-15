package com.papertrading.api.characterization.kis

import com.papertrading.api.application.order.KisExecutionNoticeService
import com.papertrading.api.domain.entity.account.Account
import com.papertrading.api.domain.entity.account.AccountLedger
import com.papertrading.api.domain.entity.feepolicy.FeePolicy
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.AccountType
import com.papertrading.api.domain.enums.MarketType
import com.papertrading.api.domain.enums.NotificationEventType
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.enums.TransactionType
import com.papertrading.api.domain.port.CollectorSubscriptionPort
import com.papertrading.api.infrastructure.kis.KisExecutionNotice
import com.papertrading.api.infrastructure.kis.KisExecutionNoticeParser
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.ExecutionRepository
import com.papertrading.api.infrastructure.persistence.FeePolicyRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import com.papertrading.api.infrastructure.persistence.SettlementRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.Instant

/**
 * 특성화 시험(step 8, D21 c): KIS 체결 통지 원문 1건이 만드는 체결·포지션·원장·정산 행과 알림을 단언한다.
 * 입구는 파서(원문 → 통지) 와 KisExecutionNoticeService.handle 이고, 주문·잠금 상태는 접수 때와 같은 도메인 메서드로 DB 에 미리 둔다.
 * 규칙: 이후 step 9~14 에서 이 시험은 고치지 않고 통과시킨다 (고쳐야 하면 멈추고 사람 결정).
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(KisBoundaryEventCaptureConfig::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Suppress("LongParameterList") // 스프링 생성자 주입으로 저장소를 받는 시험 클래스라 매개변수 수를 줄일 수 없음
class KisExecutionNoticeBoundaryCharacterizationTest(
    private val parser: KisExecutionNoticeParser,
    private val noticeService: KisExecutionNoticeService,
    private val accountRepository: AccountRepository,
    private val orderRepository: OrderRepository,
    private val positionRepository: PositionRepository,
    private val executionRepository: ExecutionRepository,
    private val accountLedgerRepository: AccountLedgerRepository,
    private val settlementRepository: SettlementRepository,
    private val feePolicyRepository: FeePolicyRepository,
    private val transactionManager: PlatformTransactionManager,
    private val eventCapture: KisBoundaryEventCapture,
) {
    @MockitoBean
    lateinit var collectorSubscriptionPort: CollectorSubscriptionPort

    private val transactionTemplate = TransactionTemplate(transactionManager)

    companion object {
        @Container
        @ServiceConnection
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @ServiceConnection(name = "redis")
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)
    }

    // 모드·시장마다 유효 정책은 1건이어야 하므로 클래스 DB 에 한 번만 심는다 (수수료율 0.015%, 최소 100원, 거래세 0.18%)
    @BeforeEach
    fun seedFeePolicies() {
        if (feePolicyRepository.count() > 0L) return
        listOf(TradingMode.KIS_PAPER, TradingMode.KIS_LIVE).forEach {
            feePolicyRepository.save(
                FeePolicy.create(
                    tradingMode = it,
                    marketType = MarketType.KOSPI,
                    feeRate = BigDecimal("0.000150"),
                    minFee = BigDecimal("100"),
                    transactionTaxRate = BigDecimal("0.001800"),
                    effectiveFrom = Instant.parse("2020-01-01T00:00:00Z"),
                ),
            )
        }
    }

    @Test
    fun `KIS_PAPER 매수 전량 체결 통지는 체결 포지션 원장 수수료 초과잠금 해제를 만들고 알림을 낸다`() {
        val seeded = seedBuyOrder("notice-paper-buy", TradingMode.KIS_PAPER, "12345678-01", "PAPERORD01")

        noticeService.handle(parse(paperNotice("PAPERORD01", "00003", "02", qty = "10", price = "69900"), "paper"))

        val order = orderRepository.findById(seeded.orderId).orElseThrow()
        assertThat(order.orderStatus).isEqualTo(OrderStatus.FILLED)
        assertThat(order.filledQuantity).isEqualByComparingTo("10")
        assertThat(order.avgFilledPrice).isEqualByComparingTo("69900")
        assertThat(order.totalAmount).isEqualByComparingTo("699000")
        assertThat(order.fee).isEqualByComparingTo("104.85")

        val execution = executionRepository.findByOrderId(seeded.orderId).single()
        assertThat(execution.externalExecutionId).isEqualTo("KIS_PAPER:12345678-01:PAPERORD01:00003")
        assertThat(execution.ticker).isEqualTo("005930")
        assertThat(execution.executedQuantity).isEqualByComparingTo("10")
        assertThat(execution.executedPrice).isEqualByComparingTo("69900")
        assertThat(execution.krwExecutedPrice).isEqualByComparingTo("69900")
        assertThat(execution.fee).isEqualByComparingTo("104.85")
        assertThat(execution.currency).isEqualTo("KRW")
        assertThat(execution.executionMode).isEqualTo(TradingMode.KIS_PAPER)
        assertThat(execution.executedAt).isEqualTo(Instant.parse("2026-05-01T00:30:15Z"))
        val executionId = requireNotNull(execution.id)

        val position = positionRepository.findByAccountIdAndTicker(seeded.accountId, "005930").orElseThrow()
        assertThat(position.quantity).isEqualByComparingTo("10")
        assertThat(position.lockedQuantity).isEqualByComparingTo("0")
        assertThat(position.orderableQuantity).isEqualByComparingTo("10")
        assertThat(position.avgBuyPrice).isEqualByComparingTo("69900")
        assertThat(position.totalBuyAmount).isEqualByComparingTo("699000")

        val account = accountRepository.findById(seeded.accountId).orElseThrow()
        assertThat(account.deposit).isEqualByComparingTo("9301000")
        assertThat(account.availableDeposit).isEqualByComparingTo("9301000")
        assertThat(account.lockedDeposit).isEqualByComparingTo("0")

        val ledger = ledgerRows(seeded.accountId)
        assertThat(ledger.map { it.transactionType }).containsExactly(
            TransactionType.BUY_LOCK,
            TransactionType.BUY_EXECUTE,
            TransactionType.FEE,
            TransactionType.BUY_UNLOCK,
        )
        assertThat(ledger.map { it.amount })
            .usingComparatorForType(BigDecimal::compareTo, BigDecimal::class.java)
            .containsExactly(BigDecimal("700000"), BigDecimal("699000"), BigDecimal("104.85"), BigDecimal("1000"))
        assertThat(ledger.map { it.balanceAfter })
            .usingComparatorForType(BigDecimal::compareTo, BigDecimal::class.java)
            .containsExactly(BigDecimal("9300000"), BigDecimal("9300000"), BigDecimal("9300000"), BigDecimal("9301000"))
        assertThat(ledger.map { it.idempotencyKey }).containsExactly(
            "buy-lock-${seeded.orderId}",
            "buy-exec-$executionId",
            "fee-$executionId",
            "unlock-excess-${seeded.orderId}",
        )
        assertThat(ledger.map { it.refOrderId }).containsOnly(seeded.orderId)
        assertThat(ledger.map { it.refExecutionId }).containsExactly(null, executionId, executionId, null)
        assertThat(settlementRepository.findByOrderId(seeded.orderId)).isEmpty

        verify(collectorSubscriptionPort).unsubscribe("KIS_PAPER", "005930")
        assertFilledEvents(seeded, executionId, ExpectedFill(OrderSide.BUY, "10", "69900", "104.85"))
        assertThat(eventCapture.slackEventsOf(seeded.accountId).single().message)
            .isEqualTo("Execution filled: ticker=005930, side=BUY, qty=10, price=69900")
    }

    @Test
    fun `KIS_PAPER 매수 부분 체결 통지는 최소 수수료를 적용하고 잠금을 남기며 같은 통지 재수신은 무시한다`() {
        val seeded = seedBuyOrder("notice-paper-partial", TradingMode.KIS_PAPER, "12345678-01", "PAPERORD02")
        val notice = parse(paperNotice("PAPERORD02", "00001", "02", qty = "4", price = "69900"), "paper")

        noticeService.handle(notice)
        noticeService.handle(notice)

        val order = orderRepository.findById(seeded.orderId).orElseThrow()
        assertThat(order.orderStatus).isEqualTo(OrderStatus.PARTIAL)
        assertThat(order.filledQuantity).isEqualByComparingTo("4")
        assertThat(order.fee).isEqualByComparingTo("100")

        val execution = executionRepository.findByOrderId(seeded.orderId).single()
        assertThat(execution.externalExecutionId).isEqualTo("KIS_PAPER:12345678-01:PAPERORD02:00001")
        assertThat(execution.fee).isEqualByComparingTo("100")
        val executionId = requireNotNull(execution.id)

        val position = positionRepository.findByAccountIdAndTicker(seeded.accountId, "005930").orElseThrow()
        assertThat(position.quantity).isEqualByComparingTo("4")
        assertThat(position.totalBuyAmount).isEqualByComparingTo("279600")

        val account = accountRepository.findById(seeded.accountId).orElseThrow()
        assertThat(account.deposit).isEqualByComparingTo("9720400")
        assertThat(account.availableDeposit).isEqualByComparingTo("9300000")
        assertThat(account.lockedDeposit).isEqualByComparingTo("420400")

        val ledger = ledgerRows(seeded.accountId)
        assertThat(ledger.map { it.transactionType }).containsExactly(
            TransactionType.BUY_LOCK,
            TransactionType.BUY_EXECUTE,
            TransactionType.FEE,
        )
        assertThat(ledger[1].amount).isEqualByComparingTo("279600")
        assertThat(ledger[2].amount).isEqualByComparingTo("100")
        assertThat(ledger[2].idempotencyKey).isEqualTo("fee-$executionId")

        verify(collectorSubscriptionPort, never()).unsubscribe("KIS_PAPER", "005930")
        assertThat(eventCapture.executionFilledEvents.filter { it.orderId == seeded.orderId }).hasSize(1)
        assertThat(eventCapture.slackEventsOf(seeded.accountId)).hasSize(1)
    }

    @Test
    fun `KIS_LIVE 매도 전량 체결 통지는 체결 포지션 매도 원장 수수료 정산을 만들고 예수금은 건드리지 않는다`() {
        val seeded = seedSellOrder("notice-live-sell", "87654321-01", "LIVEORD01")

        noticeService.handle(parse(liveNotice("LIVEORD01", "00007", qty = "10", price = "70000"), "live"))

        val order = orderRepository.findById(seeded.orderId).orElseThrow()
        assertThat(order.orderStatus).isEqualTo(OrderStatus.FILLED)
        assertThat(order.filledQuantity).isEqualByComparingTo("10")
        assertThat(order.avgFilledPrice).isEqualByComparingTo("70000")
        assertThat(order.fee).isEqualByComparingTo("105")

        val execution = executionRepository.findByOrderId(seeded.orderId).single()
        assertThat(execution.externalExecutionId).isEqualTo("KIS_LIVE:87654321-01:LIVEORD01:00007")
        assertThat(execution.executedPrice).isEqualByComparingTo("70000")
        assertThat(execution.fee).isEqualByComparingTo("105")
        assertThat(execution.executionMode).isEqualTo(TradingMode.KIS_LIVE)
        assertThat(execution.executedAt).isEqualTo(Instant.parse("2026-05-01T01:01:30Z"))
        val executionId = requireNotNull(execution.id)

        val position = positionRepository.findByAccountIdAndTicker(seeded.accountId, "005930").orElseThrow()
        assertThat(position.quantity).isEqualByComparingTo("0")
        assertThat(position.lockedQuantity).isEqualByComparingTo("0")
        assertThat(position.orderableQuantity).isEqualByComparingTo("0")

        val account = accountRepository.findById(seeded.accountId).orElseThrow()
        assertThat(account.deposit).isEqualByComparingTo("10000000")
        assertThat(account.availableDeposit).isEqualByComparingTo("10000000")
        assertThat(account.lockedDeposit).isEqualByComparingTo("0")

        val ledger = ledgerRows(seeded.accountId)
        assertThat(ledger.map { it.transactionType }).containsExactly(TransactionType.SELL_EXECUTE, TransactionType.FEE)
        assertThat(ledger[0].amount).isEqualByComparingTo("699895")
        assertThat(ledger[0].balanceAfter).isEqualByComparingTo("10000000")
        assertThat(ledger[0].idempotencyKey).isEqualTo("sell-exec-$executionId")
        assertThat(ledger[1].amount).isEqualByComparingTo("105")
        assertThat(ledger[1].idempotencyKey).isEqualTo("fee-$executionId")

        val settlement = settlementRepository.findByOrderId(seeded.orderId).orElseThrow()
        assertThat(settlement.ticker).isEqualTo("005930")
        assertThat(settlement.realizedPnl).isEqualByComparingTo("700000")
        assertThat(settlement.fee).isEqualByComparingTo("105")
        assertThat(settlement.tax).isEqualByComparingTo("1260")
        assertThat(settlement.netPnl).isEqualByComparingTo("698635")
        assertThat(settlement.krwNetPnl).isEqualByComparingTo("698635")
        assertThat(settlement.currency).isEqualTo("KRW")

        verify(collectorSubscriptionPort).unsubscribe("KIS_LIVE", "005930")
        assertFilledEvents(seeded, executionId, ExpectedFill(OrderSide.SELL, "10", "70000", "105"))
        assertThat(eventCapture.slackEventsOf(seeded.accountId).single().message)
            .isEqualTo("Execution filled: ticker=005930, side=SELL, qty=10, price=70000")
    }

    private data class Seeded(val accountId: Long, val orderId: Long)

    // 접수 직후 상태: 주문 PENDING + 외부 주문번호 저장 + 매수 잠금 원장 (700,000원 = 70,000원 x 10주)
    private fun seedBuyOrder(name: String, mode: TradingMode, accountNo: String, externalOrderId: String): Seeded {
        val seeded = transactionTemplate.execute {
            val account = accountRepository.save(newAccount(name, mode, accountNo))
            val order = orderRepository.save(newOrder(account, OrderSide.BUY, "70000", name, BigDecimal("700000")))
            order.assignExternalOrderId(externalOrderId)
            val orderId = requireNotNull(order.id)
            accountLedgerRepository.save(account.recordBuyLock(BigDecimal("700000"), orderId, "buy-lock-$orderId"))
            Seeded(requireNotNull(account.id), orderId)
        }
        return requireNotNull(seeded)
    }

    // 접수 직후 상태: 보유 10주 전량 잠금 + 매도 주문 PENDING + 외부 주문번호 저장
    private fun seedSellOrder(name: String, accountNo: String, externalOrderId: String): Seeded {
        val seeded = transactionTemplate.execute {
            val account = accountRepository.save(newAccount(name, TradingMode.KIS_LIVE, accountNo))
            positionRepository.save(
                Position.createWithHolding(
                    account,
                    "005930",
                    MarketType.KOSPI,
                    BigDecimal("10"),
                    BigDecimal("65000"),
                    lockedQuantity = BigDecimal("10"),
                ),
            )
            val order = orderRepository.save(newOrder(account, OrderSide.SELL, "70000", name, BigDecimal.ZERO))
            order.assignExternalOrderId(externalOrderId)
            Seeded(requireNotNull(account.id), requireNotNull(order.id))
        }
        return requireNotNull(seeded)
    }

    private fun newAccount(name: String, mode: TradingMode, accountNo: String): Account {
        val account = Account.create(
            accountName = name,
            accountType = AccountType.STOCK,
            tradingMode = mode,
            initialDeposit = BigDecimal("10000000"),
            externalAccountId = accountNo,
        )
        return account
    }

    private fun newOrder(account: Account, side: OrderSide, price: String, key: String, locked: BigDecimal): Order {
        val order = Order.create(
            account = account,
            ticker = "005930",
            marketType = MarketType.KOSPI,
            orderType = OrderType.LIMIT,
            orderSide = side,
            orderCondition = OrderCondition.DAY,
            quantity = BigDecimal("10"),
            limitPrice = BigDecimal(price),
            lockedAmount = locked,
            idempotencyKey = key,
        )
        return order
    }

    private fun paperNotice(orderNo: String, execNo: String, sideCode: String, qty: String, price: String): String {
        val head = "0|H0STCNI9|001|ODNO=$orderNo^CCLD_NO=$execNo^PDNO=005930^SLL_BUY_DVSN_CD=$sideCode"
        val tail = "^CCLD_QTY=$qty^CCLD_UNPR=$price^CCLD_DTTM=20260501093015000^CANO=12345678^ACNT_PRDT_CD=01"
        return head + tail
    }

    private fun liveNotice(orderNo: String, execNo: String, qty: String, price: String): String {
        val head = "0|H0STCNI0|001|ODNO=$orderNo^CCLD_NO=$execNo^PDNO=005930^SLL_BUY_DVSN_CD=01"
        val tail = "^CCLD_QTY=$qty^CCLD_UNPR=$price^CCLD_DTTM=20260501100130000^CANO=87654321^ACNT_PRDT_CD=01"
        return head + tail
    }

    private fun parse(raw: String, mode: String): KisExecutionNotice {
        val notice = parser.parse(raw, mode)
        return requireNotNull(notice) { "notice not parsed: $raw" }
    }

    private fun ledgerRows(accountId: Long): List<AccountLedger> {
        val rows = accountLedgerRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
        return rows.sortedBy { it.id }
    }

    private data class ExpectedFill(val side: OrderSide, val qty: String, val price: String, val fee: String)

    private fun assertFilledEvents(seeded: Seeded, executionId: Long, expected: ExpectedFill) {
        val domainEvent = eventCapture.executionFilledEvents.single { it.orderId == seeded.orderId }
        assertThat(domainEvent.executionId).isEqualTo(executionId)
        assertThat(domainEvent.side).isEqualTo(expected.side)
        assertThat(domainEvent.quantity).isEqualByComparingTo(expected.qty)
        assertThat(domainEvent.price).isEqualByComparingTo(expected.price)
        assertThat(domainEvent.fee).isEqualByComparingTo(expected.fee)
        assertThat(domainEvent.currency).isEqualTo("KRW")
        val slack = eventCapture.slackEventsOf(seeded.accountId).single()
        assertThat(slack.eventType).isEqualTo(NotificationEventType.EXECUTION_FILLED)
        assertThat(slack.orderId).isEqualTo(seeded.orderId)
        assertThat(slack.executionId).isEqualTo(executionId)
        assertThat(slack.occurredAt).isEqualTo(domainEvent.executedAt)
    }
}
