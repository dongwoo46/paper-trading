package com.papertrading.api.application.order

import com.papertrading.api.application.order.command.CancelOrderCommand
import com.papertrading.api.application.order.command.PlaceOrderCommand
import com.papertrading.api.domain.enums.OrderCondition
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.OrderType
import com.papertrading.api.domain.enums.TransactionType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.AccountLedger
import com.papertrading.api.domain.model.Order
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
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { NoSuchElementException("계좌를 찾을 수 없습니다. accountId=$accountId") }
        check(account.isActive) { "비활성화된 계좌입니다." }

        val tradingMode = requireNotNull(account.tradingMode) { "account.tradingMode is null" }

        // 멱등성: 동일 idempotencyKey는 저장된 주문 반환
        orderRepository.findByAccountIdAndIdempotencyKey(accountId, command.idempotencyKey)
            ?.let { return it }

        // 지정가 필드 검증
        if (command.orderType == OrderType.LIMIT) {
            requireNotNull(command.limitPrice) { "지정가 주문은 limitPrice가 필요합니다." }
        }
        if (command.orderCondition == OrderCondition.GTD) {
            requireNotNull(command.expireAt) { "GTD 주문은 expireAt이 필요합니다." }
        }

        // 공매도 guard: 미보유 또는 수량 부족 시 거부
        if (command.orderSide == OrderSide.SELL) {
            val position = positionRepository.findByAccountIdAndTickerWithLock(accountId, command.ticker)
                .orElseThrow { IllegalStateException("보유하지 않은 종목입니다. ticker=${command.ticker}") }
            check(position.orderableQuantity >= command.quantity) {
                "주문 가능 수량 부족. available=${position.orderableQuantity}, requested=${command.quantity}"
            }
            position.lockQuantity(command.quantity)
        }

        // 매수: 예수금 잠금 (시장가는 현재 시세 기준, 지정가는 limitPrice 기준)
        val lockedAmount = if (command.orderSide == OrderSide.BUY) {
            val unitPrice = when (command.orderType) {
                OrderType.LIMIT -> requireNotNull(command.limitPrice) { "limitPrice is null" }
                OrderType.MARKET -> {
                    val quote = marketQuotePort.getQuote(command.ticker)
                        ?: throw IllegalStateException("시세 정보가 없습니다. (stale > 60s) ticker=${command.ticker}")
                    check(Duration.between(quote.updatedAt, Instant.now()).seconds <= 60) {
                        "시세가 오래되었습니다. (stale) ticker=${command.ticker}"
                    }
                    quote.price
                }
            }
            val amount = unitPrice.multiply(command.quantity)
            account.lockDeposit(amount)
            amount
        } else {
            BigDecimal.ZERO
        }

        val order = orderRepository.save(
            Order(
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
                expireAt = command.expireAt,
                strategyId = command.strategyId,
                signalId = command.signalId,
            )
        )
        val orderId = requireNotNull(order.id) { "saved order.id is null" }

        if (command.orderSide == OrderSide.BUY && lockedAmount > BigDecimal.ZERO) {
            accountLedgerRepository.save(AccountLedger(
                account = account,
                transactionType = TransactionType.BUY_LOCK,
                amount = lockedAmount,
                balanceAfter = account.availableDeposit,
                refOrderId = orderId,
                idempotencyKey = "buy-lock-$orderId",
            ))
        }

        val mode = collectorMode(tradingMode.name)
        collectorSubscriptionPort.subscribe(mode, command.ticker)

        // IOC/FOK: 즉시 체결 불가 시 취소
        if (command.orderCondition in setOf(OrderCondition.IOC, OrderCondition.FOK)) {
            val quote = marketQuotePort.getQuote(command.ticker)
            if (quote == null) {
                cancelOrder(accountId, orderId, CancelOrderCommand("시세 없음 — IOC/FOK 즉시 취소"))
                return order
            }
            val fillPrice = localMatchingEngine.determineFillPrice(order, quote)
            if (fillPrice == null) {
                cancelOrder(accountId, orderId, CancelOrderCommand("체결 조건 미충족 — IOC/FOK 즉시 취소"))
                return order
            }
        }

        when (tradingMode) {
            TradingMode.KIS_PAPER -> kisPaperOrderExecutor.submit(order)
            TradingMode.KIS_LIVE -> kisLiveOrderExecutor.submit(order)
            else -> {} // LOCAL: QuoteEventListener가 다음 틱에 매칭
        }

        log.info { "order placed: orderId=$orderId, mode=$tradingMode, ticker=${command.ticker}" }
        return order
    }

    @Transactional
    fun cancelOrder(accountId: Long, orderId: Long, command: CancelOrderCommand): Order {
        val order = orderRepository.findByIdWithOptimisticLock(orderId)
            .orElseThrow { NoSuchElementException("주문을 찾을 수 없습니다. orderId=$orderId") }

        check(order.account?.id == accountId) { "해당 계좌의 주문이 아닙니다." }
        check(order.orderStatus in setOf(OrderStatus.PENDING, OrderStatus.PARTIAL)) {
            "취소 불가 상태입니다. status=${order.orderStatus}"
        }

        order.updateStatus(OrderStatus.CANCELLED)

        // 매수 잔여 잠금 해제
        if (order.orderSide == OrderSide.BUY && order.lockedAmount > BigDecimal.ZERO) {
            val accountId2 = requireNotNull(order.account?.id) { "order.account.id is null" }
            val account = accountRepository.findByIdWithLock(accountId2)
                .orElseThrow { NoSuchElementException("계좌를 찾을 수 없습니다.") }

            val releasedQty = order.quantity.subtract(order.filledQuantity)
            val releaseAmount = order.lockedAmount
                .divide(order.quantity, 4, java.math.RoundingMode.HALF_UP)
                .multiply(releasedQty)

            if (releaseAmount > BigDecimal.ZERO) {
                account.unlockDeposit(releaseAmount)
                accountLedgerRepository.save(AccountLedger(
                    account = account,
                    transactionType = TransactionType.BUY_UNLOCK,
                    amount = releaseAmount,
                    balanceAfter = account.availableDeposit,
                    refOrderId = orderId,
                    idempotencyKey = "cancel-unlock-$orderId",
                ))
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