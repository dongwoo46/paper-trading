package com.papertrading.api.application.order.port

import com.papertrading.api.domain.entity.order.Order
import com.papertrading.api.domain.enums.TradingMode

/**
 * 브로커에 주문을 접수하는 포트 (D26), 구현은 KIS
 * supportedModes 는 서비스가 모드 → 구현 표를 만드는 근거 (D25), 취소는 포트 밖 (DEC-08)
 */
interface BrokerOrderPort {
    val supportedModes: Set<TradingMode>

    fun submit(order: Order): BrokerOrderReceipt
}

/** 브로커가 접수한 주문의 외부 주문번호 */
data class BrokerOrderReceipt(
    val externalOrderId: String,
)
