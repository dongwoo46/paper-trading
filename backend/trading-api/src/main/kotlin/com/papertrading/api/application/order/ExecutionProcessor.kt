package com.papertrading.api.application.order

import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.TransactionType
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.model.AccountLedger
import com.papertrading.api.domain.model.Execution
import com.papertrading.api.domain.model.PendingSettlement
import com.papertrading.api.domain.model.Position
import com.papertrading.api.domain.model.Settlement
import com.papertrading.api.domain.model.SettlementExecution
import com.papertrading.api.domain.port.CollectorSubscriptionPort
import com.papertrading.api.domain.util.BusinessDayCalculator
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.ExecutionRepository
import com.papertrading.api.infrastructure.persistence.FeePolicyRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
import com.papertrading.api.infrastructure.persistence.PendingSettlementRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import com.papertrading.api.infrastructure.persistence.SettlementExecutionRepository
import com.papertrading.api.infrastructure.persistence.SettlementRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * 체결 처리 단일 트랜잭션
 * Order → Execution → Position → Account → AccountLedger 순서로 처리.
 * 데드락 방지: Account 락 먼저, Position 락 나중.
 */
@Service
class ExecutionProcessor(
    private val orderRepository: OrderRepository,
    private val accountRepository: AccountRepository,
    private val positionRepository: PositionRepository,
    private val executionRepository: ExecutionRepository,
    private val accountLedgerRepository: AccountLedgerRepository,
    private val feePolicyRepository: FeePolicyRepository,
    private val collectorSubscriptionPort: CollectorSubscriptionPort,
    private val pendingSettlementRepository: PendingSettlementRepository,
    private val settlementRepository: SettlementRepository,
    private val settlementExecutionRepository: SettlementExecutionRepository,
) {
    private val log = KotlinLogging.logger {}

    @Transactional
    fun fill(orderId: Long, fillPrice: BigDecimal, fillQty: BigDecimal) {
        val order = orderRepository.findByIdWithOptimisticLock(orderId).orElse(null) ?: return
        if (order.orderStatus !in setOf(OrderStatus.PENDING, OrderStatus.PARTIAL)) return

        val accountId = requireNotNull(order.account?.id) { "order.account.id is null: orderId=$orderId" }
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { NoSuchElementException("계좌를 찾을 수 없습니다. accountId=$accountId") }

        val tradingMode = requireNotNull(account.tradingMode) { "account.tradingMode is null: accountId=$accountId" }
        val marketType = requireNotNull(order.marketType) { "order.marketType is null: orderId=$orderId" }
        val ticker = requireNotNull(order.ticker) { "order.ticker is null: orderId=$orderId" }

        val feePolicy = feePolicyRepository.findActivePolicy(tradingMode, marketType, Instant.now())
        val feeRate = feePolicy.map { it.feeRate }.orElse(BigDecimal.ZERO)
        val minFee = feePolicy.map { it.minFee }.orElse(BigDecimal.ZERO)
        val rawFee = fillPrice.multiply(fillQty).multiply(feeRate).setScale(4, RoundingMode.HALF_UP)
        val fee = rawFee.max(minFee)

        val prevQty = order.filledQuantity
        val prevAvg = order.avgFilledPrice ?: BigDecimal.ZERO
        val newAvgPrice = if (prevQty == BigDecimal.ZERO) fillPrice
        else prevAvg.multiply(prevQty).add(fillPrice.multiply(fillQty))
            .divide(prevQty.add(fillQty), 4, RoundingMode.HALF_UP)

        order.applyExecution(fillQty, newAvgPrice, fee)

        val execution = executionRepository.save(
            Execution(
                order = order,
                account = account,
                ticker = ticker,
                executedQuantity = fillQty,
                executedPrice = fillPrice,
                fee = fee,
                currency = account.baseCurrency,
                krwExecutedPrice = fillPrice,
                externalExecutionId = "LOCAL-${UUID.randomUUID()}",
                executedAt = Instant.now(),
            )
        )
        val executionId = requireNotNull(execution.id) { "saved execution.id is null" }

        if (order.orderSide == OrderSide.BUY) {
            val position = positionRepository.findByAccountIdAndTickerWithLock(accountId, ticker)
                .orElseGet { Position(account = account, ticker = ticker, marketType = marketType) }
            position.applyBuy(fillQty, fillPrice)
            positionRepository.save(position)

            val cost = fillPrice.multiply(fillQty)
            account.confirmBuy(cost)

            accountLedgerRepository.save(AccountLedger(
                account = account,
                transactionType = TransactionType.BUY_EXECUTE,
                amount = cost,
                balanceAfter = account.availableDeposit,
                refOrderId = orderId,
                refExecutionId = executionId,
                idempotencyKey = "buy-exec-$executionId",
            ))
        } else {
            val position = positionRepository.findByAccountIdAndTickerWithLock(accountId, ticker)
                .orElseThrow { IllegalStateException("포지션을 찾을 수 없습니다. ticker=$ticker") }
            position.applySell(fillQty)
            positionRepository.save(position)

            val grossProceeds = fillPrice.multiply(fillQty).setScale(4, RoundingMode.HALF_UP)
            val netProceeds = grossProceeds.subtract(fee).setScale(4, RoundingMode.HALF_UP)

            when (tradingMode) {
                TradingMode.LOCAL -> {
                    // LOCAL 모드: 즉시 예수금 입금 (모의투자 UX 유지)
                    account.receiveSellProceeds(netProceeds)
                }
                TradingMode.KIS_PAPER, TradingMode.KIS_LIVE -> {
                    // KIS 모드: T+2 결제 — 즉시 입금 없이 PendingSettlement 생성
                    val settlementDate = BusinessDayCalculator.addBusinessDays(
                        LocalDate.now(ZoneId.of("Asia/Seoul")), 2
                    )
                    pendingSettlementRepository.save(
                        PendingSettlement(
                            account = account,
                            orderId = orderId,
                            settlementDate = settlementDate,
                            amount = netProceeds,
                        )
                    )
                }
                else -> {
                    // 다른 모드는 즉시 입금 처리 (향후 확장 대비)
                    account.receiveSellProceeds(netProceeds)
                }
            }

            accountLedgerRepository.save(AccountLedger(
                account = account,
                transactionType = TransactionType.SELL_EXECUTE,
                amount = netProceeds,
                balanceAfter = account.availableDeposit,
                refOrderId = orderId,
                refExecutionId = executionId,
                idempotencyKey = "sell-exec-$executionId",
            ))

            if (order.orderStatus == OrderStatus.FILLED && order.orderSide == OrderSide.SELL) {
                val realizedPnl = grossProceeds.subtract(fee).setScale(4, RoundingMode.HALF_UP)
                val settlement = settlementRepository.save(
                    Settlement(
                        order = order,
                        account = account,
                        ticker = ticker,
                        realizedPnl = realizedPnl,
                        fee = fee,
                        tax = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                        netPnl = realizedPnl,
                        currency = account.baseCurrency,
                        krwNetPnl = realizedPnl,
                        settledAt = Instant.now(),
                    )
                )
                settlementExecutionRepository.save(
                    SettlementExecution(
                        settlement = settlement,
                        execution = execution,
                    )
                )
            }
        }

        if (fee > BigDecimal.ZERO) {
            accountLedgerRepository.save(AccountLedger(
                account = account,
                transactionType = TransactionType.FEE,
                amount = fee,
                balanceAfter = account.availableDeposit,
                refOrderId = orderId,
                refExecutionId = executionId,
                idempotencyKey = "fee-$executionId",
            ))
        }

        if (order.orderStatus == OrderStatus.FILLED) {
            releaseLockedExcess(orderId, order.lockedAmount, fillPrice, fillQty, account)
            tryUnsubscribe(accountId, ticker, collectorMode(tradingMode.name))
        }

        log.info { "filled: orderId=$orderId, qty=$fillQty, price=$fillPrice, status=${order.orderStatus}" }
    }

    /** LIMIT 매수에서 실제 체결가가 잠금 금액보다 낮을 때 차액을 해제 */
    private fun releaseLockedExcess(orderId: Long, lockedAmount: BigDecimal, fillPrice: BigDecimal, fillQty: BigDecimal, account: com.papertrading.api.domain.model.Account) {
        val actualCost = fillPrice.multiply(fillQty)
        val excess = lockedAmount.subtract(actualCost)
        if (excess <= BigDecimal.ZERO) return

        account.unlockDeposit(excess)
        accountLedgerRepository.save(AccountLedger(
            account = account,
            transactionType = TransactionType.BUY_UNLOCK,
            amount = excess,
            balanceAfter = account.availableDeposit,
            refOrderId = orderId,
            idempotencyKey = "unlock-excess-$orderId",
        ))
    }

    /** 해당 ticker의 활성 주문이 없으면 collector-api 구독 해제 */
    private fun tryUnsubscribe(accountId: Long, ticker: String, mode: String) {
        val activeOrders = orderRepository.findByAccountIdAndOrderStatusIn(
            accountId, listOf(OrderStatus.PENDING, OrderStatus.PARTIAL)
        )
        if (activeOrders.none { it.ticker == ticker }) {
            collectorSubscriptionPort.unsubscribe(mode, ticker)
        }
    }
}

/** TradingMode.name → collector-api mode 문자열 변환 */
internal fun collectorMode(tradingModeName: String): String =
    if (tradingModeName == "KIS_PAPER") "paper" else "live"
