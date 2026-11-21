package com.papertrading.api.infrastructure.kis

import com.papertrading.api.application.order.port.BrokerOrderPort
import com.papertrading.api.application.order.port.BrokerOrderReceipt
import com.papertrading.api.common.exception.InternalErrorException
import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.enums.TradingMode
import org.springframework.stereotype.Component

/**
 * KIS 주문 접수 어댑터 (D26): 공통 주문 포트의 KIS 구현, KisOrderRestClient.placeOrder 를 감쌈
 * REST 주소 모드는 계좌 모드 전수 when, 계좌번호 형식은 KIS 호출 전에 검사 (KIS_ACCOUNT_NOT_LINKED 409)
 * 실패는 KisFailureClassifier 로 D28 code 로 바꾸고 KIS 원문은 cause 에만
 */
@Component
class KisOrderAdapter(
    private val kisOrderRestClient: KisOrderRestClient,
) : BrokerOrderPort {
    override val supportedModes: Set<TradingMode> = setOf(TradingMode.KIS_PAPER, TradingMode.KIS_LIVE)

    override fun submit(order: Order): BrokerOrderReceipt {
        val mode = restModeOf(order.account.tradingMode)
        kisAccountNumberParts(order.account.externalAccountId)
        val orderNo = runCatching { kisOrderRestClient.placeOrder(order, mode) }
            .getOrElse { throw KisFailureClassifier.orderFailure(it) }
        return BrokerOrderReceipt(orderNo)
    }

    private fun restModeOf(tradingMode: TradingMode): String = when (tradingMode) {
        TradingMode.KIS_PAPER -> "paper"

        TradingMode.KIS_LIVE -> "live"

        TradingMode.LOCAL, TradingMode.UPBIT_LIVE, TradingMode.TOSS_LIVE ->
            throw InternalErrorException(
                "KIS_ORDER_MODE_UNSUPPORTED",
                "KIS 주문 어댑터가 지원하지 않는 모드: ${tradingMode.name}",
            )
    }
}
