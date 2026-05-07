package com.papertrading.api.application.order

import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.event.ExecutionFilledEvent
import com.papertrading.api.application.notification.SlackNotificationEventPublisher
import com.papertrading.api.domain.entity.account.AccountLedger
import com.papertrading.api.domain.entity.order.Execution
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.entity.settlement.ReceivableSettlement
import com.papertrading.api.domain.entity.settlement.Settlement
import com.papertrading.api.domain.entity.settlement.SettlementExecution
import com.papertrading.api.domain.port.CollectorSubscriptionPort
import com.papertrading.api.domain.entity.settlement.BusinessDayCalculator
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.ExecutionRepository
import com.papertrading.api.infrastructure.persistence.FeePolicyRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
import com.papertrading.api.infrastructure.persistence.ReceivableSettlementRepository
import com.papertrading.api.infrastructure.persistence.PositionRepository
import com.papertrading.api.infrastructure.persistence.SettlementExecutionRepository
import com.papertrading.api.infrastructure.persistence.SettlementRepository
import mu.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
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
    private val ReceivableSettlementRepository: ReceivableSettlementRepository,
    private val settlementRepository: SettlementRepository,
    private val settlementExecutionRepository: SettlementExecutionRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val slackNotificationEventPublisher: SlackNotificationEventPublisher,
) {
    private val log = KotlinLogging.logger {}

    @Transactional
    fun fill(orderId: Long, fillPrice: BigDecimal, fillQty: BigDecimal) {
        fill(orderId, fillPrice, fillQty, "LOCAL-${UUID.randomUUID()}", Instant.now())
    }

    @Transactional
    fun fill(
        orderId: Long,
        fillPrice: BigDecimal,
        fillQty: BigDecimal,
        externalExecutionId: String,
        executedAt: Instant,
    ) {
        if (executionRepository.findByExternalExecutionId(externalExecutionId).isPresent) return

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
            Execution.create(
                order = order,
                account = account,
                ticker = ticker,
                executedQuantity = fillQty,
                executedPrice = fillPrice,
                fee = fee,
                currency = account.baseCurrency,
                krwExecutedPrice = fillPrice,
                externalExecutionId = externalExecutionId,
                executedAt = executedAt,
            )
        )
        val executionId = requireNotNull(execution.id) { "saved execution.id is null" }

        if (order.orderSide == OrderSide.BUY) {
            val position = positionRepository.findByAccountIdAndTickerWithLock(accountId, ticker)
                .orElseGet { Position.create(account = account, ticker = ticker, marketType = marketType) }
            position.applyBuy(fillQty, fillPrice)
            positionRepository.save(position)

            val cost = fillPrice.multiply(fillQty)
            accountLedgerRepository.save(
                account.recordBuyExecution(cost, orderId, executionId, "buy-exec-$executionId")
            )
        } else {
            val position = positionRepository.findByAccountIdAndTickerWithLock(accountId, ticker)
                .orElseThrow { IllegalStateException("포지션을 찾을 수 없습니다. ticker=$ticker") }
            val positionId = requireNotNull(position.id) { "position.id is null: accountId=$accountId, ticker=$ticker" }
            val updatedRows = positionRepository.applySellExecutionAtomically(positionId, fillQty)
            check(updatedRows == 1) {
                "원자적 매도 반영 실패: positionId=$positionId, qty=$fillQty"
            }

            val grossProceeds = fillPrice.multiply(fillQty).setScale(4, RoundingMode.HALF_UP)
            val netProceeds = grossProceeds.subtract(fee).setScale(4, RoundingMode.HALF_UP)

            when (tradingMode) {
                TradingMode.LOCAL -> {
                    // LOCAL 모드: 즉시 예수금 입금 (모의투자 UX 유지)
                    account.receiveSellProceeds(netProceeds)
                }
                TradingMode.KIS_PAPER, TradingMode.KIS_LIVE -> {
                    // KIS 모드: T+2 결제 — 즉시 입금 없이 ReceivableSettlement 생성
                    val settlementDate = BusinessDayCalculator.addBusinessDays(
                        LocalDate.now(ZoneId.of("Asia/Seoul")), 2
                    )
                    ReceivableSettlementRepository.save(
                        ReceivableSettlement.create(
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

            accountLedgerRepository.save(
                account.recordSellExecution(netProceeds, orderId, executionId, "sell-exec-$executionId")
            )

            if (order.orderStatus == OrderStatus.FILLED && order.orderSide == OrderSide.SELL) {
                val settlement = settlementRepository.save(
                    Settlement.createFromExecution(
                        order = order,
                        account = account,
                        execution = execution,
                        tax = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                        settledAt = Instant.now(),
                    )
                )
                settlementExecutionRepository.save(
                    SettlementExecution.create(
                        settlement = settlement,
                        execution = execution,
                    )
                )
            }
        }

        if (fee > BigDecimal.ZERO) {
            accountLedgerRepository.save(
                account.recordFee(fee, orderId, executionId, "fee-$executionId")
            )
        }

        if (order.orderStatus == OrderStatus.FILLED) {
            releaseLockedExcess(orderId, order.lockedAmount, fillPrice, fillQty, account)
            tryUnsubscribe(accountId, ticker, collectorMode(tradingMode.name))
        }

        log.info { "filled: orderId=$orderId, qty=$fillQty, price=$fillPrice, status=${order.orderStatus}" }

        eventPublisher.publishEvent(
            ExecutionFilledEvent(
                executionId = executionId,
                orderId = orderId,
                ticker = ticker,
                side = requireNotNull(order.orderSide) { "order.orderSide is null: orderId=$orderId" },
                quantity = fillQty,
                price = fillPrice,
                fee = fee,
                currency = requireNotNull(account.baseCurrency) { "account.baseCurrency is null: accountId=$accountId" },
                executedAt = executedAt,
            )
        )
        val side = requireNotNull(order.orderSide) { "order.orderSide is null: orderId=$orderId" }
        slackNotificationEventPublisher.publishExecutionFilled(
            accountId = accountId,
            orderId = orderId,
            executionId = executionId,
            message = "Execution filled: ticker=$ticker, side=$side, qty=$fillQty, price=$fillPrice",
            occurredAt = executedAt,
        )
    }

    /** LIMIT 매수에서 실제 체결가가 잠금 금액보다 낮을 때 차액을 해제 */
    private fun releaseLockedExcess(orderId: Long, lockedAmount: BigDecimal, fillPrice: BigDecimal, fillQty: BigDecimal, account: com.papertrading.api.domain.entity.account.Account) {
        val actualCost = fillPrice.multiply(fillQty)
        val excess = lockedAmount.subtract(actualCost)
        if (excess <= BigDecimal.ZERO) return

        accountLedgerRepository.save(
            account.recordBuyUnlock(excess, orderId, "unlock-excess-$orderId")
        )
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

