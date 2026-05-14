package com.papertrading.api.application.order.query

import java.time.Instant

data class ExecutionQuery(
    val accountId: Long,
    val orderId: Long,
    val executedFrom: Instant? = null,
    val executedTo: Instant? = null,
)

