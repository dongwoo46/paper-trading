package com.papertrading.api.application.order

import com.papertrading.api.application.order.command.CancelOrderCommand
import com.papertrading.api.application.order.command.PlaceOrderCommand
import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.common.exception.OrderNotFoundException
import com.papertrading.api.common.exception.PositionNotFoundException
import com.papertrading.api.common.exception.QuoteUnavailableException
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.enums.TriggerType
import com.papertrading.api.domain.port.CollectorSubscriptionPort
import com.papertrading.api.domain.port.MarketQuotePort
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

data class AutoExitTriggerAuditInput(
    val triggerId: Long,
    val triggerVersion: Long,
)

@Service
class OrderCommandService(
    private val accountRepository: AccountRepository,
    private val orderRepository: OrderRepository,
    private val positionRepository: PositionRepository,
    private val accountLedgerRepository: AccountLedgerRepository,
    private val marketQuotePort: MarketQuotePort,
    private val collectorSubscriptionPort: CollectorSubscriptionPort,
    private val localMatchingEngine: LocalMatchingEngine,
    private val kisPaperOrderExecutor: KisPaperOrderExecutor,
    private val kisLiveOrderExecutor: KisLiveOrderExecutor,
) {
    private val log = KotlinLogging.logger {}

    @Transactional
    fun placeOrder(accountId: Long, command: PlaceOrderCommand): Order {
        orderRepository.findByAccountIdAndIdempotencyKey(accountId, command.idempotencyKey)
            ?.let { return it }

        validatePlaceOrderCommand(command)
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }
            .also { check(it.isActive) { "비활성화된 계좌입니다." } }
        val tradingMode = requireNotNull(account.tradingMode) { "account.tradingMode is null" }

        lockSellQuantityIfNeeded(accountId, command)
        val lockedAmount = calculateLockedAmount(command, tradingMode)
        val order = saveOrder(account, command, lockedAmount)
        val orderId = requireNotNull(order.id) { "saved order.id is null" }

        recordBuyLockIfNeeded(command, account, orderId, lockedAmount)
        collectorSubscriptionPort.subscribe(tradingMode.name, command.ticker)

        if (cancelIfUnfillableIocFok(accountId, orderId, command, order, tradingMode)) return order

        submitExternalOrderIfNeeded(tradingMode, order)
        log.info { "order placed: orderId=$orderId, mode=$tradingMode, ticker=${command.ticker}" }
        return order
    }

    private fun validatePlaceOrderCommand(command: PlaceOrderCommand) {
        if (command.orderType == OrderType.LIMIT) {
            requireNotNull(command.limitPrice) { "지정가 주문은 limitPrice가 필요합니다." }
        }
        if (command.orderCondition == OrderCondition.GTD) {
            requireNotNull(command.expireAt) { "GTD 주문은 expireAt이 필요합니다." }
        }
    }

    private fun lockSellQuantityIfNeeded(accountId: Long, command: PlaceOrderCommand) {
        if (command.orderSide != OrderSide.SELL) return

        val position = positionRepository.findByAccountIdAndTickerWithLock(accountId, command.ticker)
            .orElseThrow { PositionNotFoundException(ticker = command.ticker) }
        check(position.orderableQuantity >= command.quantity) {
            "주문 가능 수량 부족. available=${position.orderableQuantity}, requested=${command.quantity}"
        }
        position.lockQuantity(command.quantity)
    }

    private fun calculateLockedAmount(command: PlaceOrderCommand, tradingMode: TradingMode): BigDecimal {
        if (command.orderSide != OrderSide.BUY) return BigDecimal.ZERO

        val unitPrice = when (command.orderType) {
            OrderType.LIMIT -> requireNotNull(command.limitPrice) { "limitPrice is null" }
            OrderType.MARKET -> {
                val quote = marketQuotePort.getQuote(tradingMode, command.ticker)
                    ?: throw QuoteUnavailableException(command.ticker)
                check(Duration.between(quote.updatedAt, Instant.now()).seconds <= 60) {
                    "시세가 오래되었습니다. (stale) ticker=${command.ticker}"
                }
                quote.price
            }
        }
        return unitPrice.multiply(command.quantity)
    }

    private fun saveOrder(
        account: com.papertrading.api.domain.entity.account.Account,
        command: PlaceOrderCommand,
        lockedAmount: BigDecimal,
    ): Order = orderRepository.save(
        Order.create(
            account = account,
            ticker = command.ticker,
            marketType = command.marketType,
            orderType = command.orderType,
            orderSide = command.orderSide,
            orderCondition = command.orderCondition,
            quantity = command.quantity,
            limitPrice = command.limitPrice,
            lockedAmount = lockedAmount,
            idempotencyKey = command.idempotencyKey,
            orderGroupId = command.orderGroupId,
            expireAt = command.expireAt,
            strategyId = command.strategyId,
            signalId = command.signalId,
        )
    )

    private fun recordBuyLockIfNeeded(
        command: PlaceOrderCommand,
        account: com.papertrading.api.domain.entity.account.Account,
        orderId: Long,
        lockedAmount: BigDecimal,
    ) {
        if (command.orderSide == OrderSide.BUY && lockedAmount > BigDecimal.ZERO) {
            accountLedgerRepository.save(
                account.recordBuyLock(lockedAmount, orderId, "buy-lock-$orderId")
            )
        }
    }

    private fun cancelIfUnfillableIocFok(
        accountId: Long,
        orderId: Long,
        command: PlaceOrderCommand,
        order: Order,
        tradingMode: TradingMode,
    ): Boolean {
        if (command.orderCondition !in setOf(OrderCondition.IOC, OrderCondition.FOK)) return false

        val quote = marketQuotePort.getQuote(tradingMode, command.ticker)
        if (quote == null) {
            cancelOrder(accountId, orderId, CancelOrderCommand("시세 없음 — IOC/FOK 즉시 취소"))
            return true
        }

        val fillPrice = localMatchingEngine.determineFillPrice(order, quote)
        if (fillPrice == null) {
            cancelOrder(accountId, orderId, CancelOrderCommand("체결 조건 미충족 — IOC/FOK 즉시 취소"))
            return true
        }
        return false
    }

    private fun submitExternalOrderIfNeeded(tradingMode: TradingMode, order: Order) {
        when (tradingMode) {
            TradingMode.KIS_PAPER -> kisPaperOrderExecutor.submit(order)
            TradingMode.KIS_LIVE -> kisLiveOrderExecutor.submit(order)
            else -> {}
        }
    }

    @Transactional
    fun createGroupedAutoExitSellOrder(
        accountId: Long,
        ticker: String,
        marketType: com.papertrading.api.domain.enums.MarketType,
        quantity: BigDecimal,
        orderGroupId: String,
        triggerAuditInputs: List<AutoExitTriggerAuditInput>,
    ): Order {
        require(triggerAuditInputs.isNotEmpty()) { "triggerAuditInputs must not be empty" }
        val triggerAuditKey = triggerAuditInputs
            .sortedWith(compareBy<AutoExitTriggerAuditInput> { it.triggerId }.thenBy { it.triggerVersion })
            .joinToString(",") { "${it.triggerId}@${it.triggerVersion}" }
        val idempotencyKey = "$orderGroupId:$triggerAuditKey"

        return placeOrder(
            accountId,
            PlaceOrderCommand(
                ticker = ticker,
                marketType = marketType,
                orderType = OrderType.MARKET,
                orderSide = OrderSide.SELL,
                orderCondition = OrderCondition.DAY,
                quantity = quantity,
                limitPrice = null,
                expireAt = null,
                idempotencyKey = idempotencyKey,
                orderGroupId = orderGroupId,
            ),
        )
    }

    @Transactional
    fun createAutoExitSellOrder(
        position: Position,
        triggerEntityVersion: Long,
        triggerType: TriggerType,
        orderGroupId: String? = null,
    ): Order {
        val positionId = requireNotNull(position.id) { "position.id is null" }
        val accountId = requireNotNull(position.account.id) { "position.account.id is null" }
        val key = "auto-exit:$positionId:$triggerEntityVersion:${triggerType.name}"
        return placeOrder(
            accountId,
            PlaceOrderCommand(
                ticker = position.ticker,
                marketType = position.marketType,
                orderType = OrderType.MARKET,
                orderSide = OrderSide.SELL,
                orderCondition = OrderCondition.DAY,
                quantity = position.orderableQuantity,
                limitPrice = null,
                expireAt = null,
                idempotencyKey = key,
                orderGroupId = orderGroupId,
            )
        )
    }

    @Transactional
    fun cancelOrder(accountId: Long, orderId: Long, command: CancelOrderCommand): Order {
        val order = orderRepository.findByIdWithOptimisticLock(orderId)
            .orElseThrow { OrderNotFoundException(orderId) }

        check(order.account?.id == accountId) { "해당 계좌의 주문이 아닙니다." }
        check(order.orderStatus in setOf(OrderStatus.PENDING, OrderStatus.PARTIAL)) {
            "취소 불가 상태입니다. status=${order.orderStatus}"
        }

        order.cancel()

        // 매수 잔여 잠금 해제
        if (order.orderSide == OrderSide.BUY && order.lockedAmount > BigDecimal.ZERO) {
            val accountId2 = requireNotNull(order.account?.id) { "order.account.id is null" }
            val account = accountRepository.findByIdWithLock(accountId2)
                .orElseThrow { AccountNotFoundException(accountId2) }

            val releasedQty = order.quantity.subtract(order.filledQuantity)
            val releaseAmount = order.lockedAmount
                .divide(order.quantity, 4, java.math.RoundingMode.HALF_UP)
                .multiply(releasedQty)

            if (releaseAmount > BigDecimal.ZERO) {
                accountLedgerRepository.save(
                    account.recordBuyUnlock(releaseAmount, orderId, "cancel-unlock-$orderId")
                )
            }
        }

        // 매도 잔여 수량 잠금 해제
        if (order.orderSide == OrderSide.SELL) {
            val accountId2 = requireNotNull(order.account?.id) { "order.account.id is null" }
            val ticker = requireNotNull(order.ticker) { "order.ticker is null" }
            val position = positionRepository.findByAccountIdAndTickerWithLock(accountId2, ticker)
                .orElse(null)
            val remainQty = order.quantity.subtract(order.filledQuantity)
            position?.unlockQuantity(remainQty)
        }

        log.info { "order cancelled: orderId=$orderId, reason=${command.reason}" }
        return order
    }
}
