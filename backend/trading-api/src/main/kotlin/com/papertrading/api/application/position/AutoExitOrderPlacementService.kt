package com.papertrading.api.application.position

import com.papertrading.api.application.order.AutoExitTriggerAuditInput
import com.papertrading.api.application.order.OrderCommandService
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.enums.MarketType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class AutoExitOrderPlacementService(
    private val orderCommandService: OrderCommandService,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun createGroupedAutoExitSellOrder(
        accountId: Long,
        ticker: String,
        marketType: MarketType,
        quantity: BigDecimal,
        orderGroupId: String,
        triggerAuditInputs: List<AutoExitTriggerAuditInput>,
    ): Order =
        orderCommandService.createGroupedAutoExitSellOrder(
            accountId = accountId,
            ticker = ticker,
            marketType = marketType,
            quantity = quantity,
            orderGroupId = orderGroupId,
            triggerAuditInputs = triggerAuditInputs,
        )
}
