package com.papertrading.api.application.order.port

import com.papertrading.api.domain.enums.TradingMode
import java.math.BigDecimal
import java.time.Instant

/** 브로커 체결 통지 1건. accountScope 는 브로커 계좌 식별(KIS 는 CANO-상품코드) */
data class BrokerExecutionNotice(
    val mode: TradingMode,
    val externalOrderId: String,
    val externalExecutionId: String,
    val executedQty: BigDecimal,
    val executedPrice: BigDecimal,
    val executedAt: Instant,
    val accountScope: String?,
)
