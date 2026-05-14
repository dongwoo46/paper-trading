package com.papertrading.api.application.order.query

import com.papertrading.api.domain.enums.OrderStatus
import java.time.Instant

data class OrderListQuery(
    val accountId: Long,
    val ticker: String? = null,
    val status: OrderStatus? = null,
    val createdFrom: Instant? = null,
    val createdTo: Instant? = null,
)

