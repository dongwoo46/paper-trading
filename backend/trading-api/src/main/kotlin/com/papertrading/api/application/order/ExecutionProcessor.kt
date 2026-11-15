package com.papertrading.api.application.order

import com.papertrading.api.application.notification.SlackNotificationEventPublisher
import com.papertrading.api.common.exception.AccountNotFoundException
import com.papertrading.api.common.exception.PositionNotFoundException
import com.papertrading.api.domain.entity.account.AccountLedger
import com.papertrading.api.domain.entity.order.Execution
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.entity.settlement.Settlement
import com.papertrading.api.domain.entity.settlement.SettlementExecution
import com.papertrading.api.domain.enums.OrderSide
import com.papertrading.api.domain.enums.OrderStatus
import com.papertrading.api.domain.enums.TradingMode
import com.papertrading.api.domain.event.ExecutionFilledEvent
import com.papertrading.api.domain.port.CollectorSubscriptionPort
import com.papertrading.api.infrastructure.persistence.AccountLedgerRepository
import com.papertrading.api.infrastructure.persistence.AccountRepository
import com.papertrading.api.infrastructure.persistence.ExecutionRepository
import com.papertrading.api.infrastructure.persistence.FeePolicyRepository
import com.papertrading.api.infrastructure.persistence.OrderRepository
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
    private val settlementRepository: SettlementRepository,
    private val settlementExecutionRepository: SettlementExecutionRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val slackNotificationEventPublisher: SlackNotificationEventPublisher,
) {
    private val log = KotlinLogging.logger {}

    // local 전용, Execution에 체결 출처 등록필요할듯
    @Transactional
    fun fillLocal(
        orderId: Long,
        fillPrice: BigDecimal,
        fillQty: BigDecimal,
    ) {
        processFill(
            orderId = orderId,
            fillPrice = fillPrice,
            fillQty = fillQty,
            externalExecutionId = "LOCAL-${UUID.randomUUID()}",
            executedAt = Instant.now(),
        )
    }

    @Transactional
    fun fillExternal(
        orderId: Long,
        fillPrice: BigDecimal,
        fillQty: BigDecimal,
        externalExecutionId: String,
        executedAt: Instant,
    ) {
        processFill(
            orderId = orderId,
            fillPrice = fillPrice,
            fillQty = fillQty,
            externalExecutionId = externalExecutionId,
            executedAt = executedAt,
        )
    }

    // 메인 체결 함수
    fun processFill(
        orderId: Long,
        fillPrice: BigDecimal,
        fillQty: BigDecimal,
        externalExecutionId: String,
        executedAt: Instant,
    ) {
        if (executionRepository.findByExternalExecutionId(externalExecutionId).isPresent) return

        val order = orderRepository.findByIdWithOptimisticLock(orderId).orElse(null) ?: return
        if (order.orderStatus !in setOf(OrderStatus.PENDING, OrderStatus.PARTIAL)) {
            log.warn {
                "종료된 주문에 새 체결 무시: orderId=$orderId, externalExecutionId=$externalExecutionId, " +
                    "status=${order.orderStatus}"
            }
            return
        }

        val accountId = requireNotNull(order.account?.id) { "order.account.id is null: orderId=$orderId" }
        val account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow { AccountNotFoundException(accountId) }

        val tradingMode = requireNotNull(account.tradingMode) { "account.tradingMode is null: accountId=$accountId" }
        val marketType = requireNotNull(order.marketType) { "order.marketType is null: orderId=$orderId" }
        val ticker = requireNotNull(order.ticker) { "order.ticker is null: orderId=$orderId" }

        val feePolicy = feePolicyRepository.findActivePolicy(tradingMode, marketType, Instant.now())
        val feeRate = feePolicy.map { it.feeRate }.orElse(BigDecimal.ZERO)
        val minFee = feePolicy.map { it.minFee }.orElse(BigDecimal.ZERO)
        val rawFee = fillPrice.multiply(fillQty).multiply(feeRate).setScale(Order.AMOUNT_SCALE, RoundingMode.HALF_UP)
        val fee = rawFee.max(minFee)

        val executedAmount = order.applyExecution(fillQty, fillPrice, fee)

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
                executionMode = tradingMode,
            ),
        )
        val executionId = requireNotNull(execution.id) { "saved execution.id is null" }

        // 체결 원장은 매수·매도 공통으로 한 곳에서 저장(동결된 ArchUnit 호출 수 유지)
        val executionLedger: AccountLedger
        if (order.orderSide == OrderSide.BUY) {
            val position = positionRepository.findByAccountIdAndTickerWithLock(accountId, ticker)
                .orElseGet { Position.create(account = account, ticker = ticker, marketType = marketType) }
            position.applyBuy(fillQty, fillPrice)
            positionRepository.save(position)

            executionLedger = account.recordBuyExecution(executedAmount, orderId, executionId, "buy-exec-$executionId")
        } else {
            val position = positionRepository.findByAccountIdAndTickerWithLock(accountId, ticker)
                .orElseThrow { PositionNotFoundException(ticker = ticker) }
            val positionId = requireNotNull(position.id) { "position.id is null: accountId=$accountId, ticker=$ticker" }
            val updatedRows = positionRepository.applySellExecutionAtomically(positionId, fillQty)
            check(updatedRows == 1) {
                "원자적 매도 반영 실패: positionId=$positionId, qty=$fillQty"
            }

            val grossProceeds = executedAmount
            val netProceeds = grossProceeds.subtract(fee)

            when (tradingMode) {
                TradingMode.LOCAL -> {
                    // LOCAL 모드: 즉시 예수금 입금 (모의투자 UX 유지)
                    account.receiveSellProceeds(netProceeds)
                }

                TradingMode.KIS_PAPER, TradingMode.KIS_LIVE -> {
                    // KIS 모드: 체결 상태 동기화만 수행한다.
                }

                else -> {
                    account.receiveSellProceeds(netProceeds)
                }
            }

            executionLedger = account.recordSellExecution(netProceeds, orderId, executionId, "sell-exec-$executionId")

            if (order.orderStatus == OrderStatus.FILLED && order.orderSide == OrderSide.SELL) {
                val settlement = settlementRepository.save(
                    Settlement.createFromExecution(
                        order = order,
                        account = account,
                        execution = execution,
                        tax = feePolicy.map { it.calculateTax(grossProceeds) }.orElse(BigDecimal.ZERO),
                        settledAt = Instant.now(),
                    ),
                )
                settlementExecutionRepository.save(
                    SettlementExecution.create(
                        settlement = settlement,
                        execution = execution,
                    ),
                )
            }
        }
        accountLedgerRepository.save(executionLedger)

        if (fee > BigDecimal.ZERO) {
            accountLedgerRepository.save(
                account.recordFee(fee, orderId, executionId, "fee-$executionId"),
            )
        }

        if (order.orderStatus == OrderStatus.FILLED) {
            if (order.isLockedAmountExceeded()) {
                log.warn {
                    "체결 금액이 주문 잠금을 넘음(해제액 0 하한): orderId=$orderId, " +
                        "lockedAmount=${order.lockedAmount}, totalAmount=${order.totalAmount}"
                }
            }
            val remainingLock = order.remainingLockedAmount()
            if (remainingLock > BigDecimal.ZERO) {
                accountLedgerRepository.save(
                    account.recordBuyUnlock(remainingLock, orderId, "unlock-excess-$orderId"),
                )
            }
            tryUnsubscribe(accountId, ticker, tradingMode.name)
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
                currency = requireNotNull(account.baseCurrency) {
                    "account.baseCurrency is null: accountId=$accountId"
                },
                executedAt = executedAt,
            ),
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

    /** 해당 ticker의 활성 주문이 없으면 collector-api 구독 해제 */
    private fun tryUnsubscribe(accountId: Long, ticker: String, mode: String) {
        val activeOrders = orderRepository.findByAccountIdAndOrderStatusIn(
            accountId,
            listOf(OrderStatus.PENDING, OrderStatus.PARTIAL),
        )
        if (activeOrders.none { it.ticker == ticker }) {
            collectorSubscriptionPort.unsubscribe(mode, ticker)
        }
    }
}
