package com.papertrading.api.application.account.query

import com.papertrading.api.domain.enums.TransactionType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.Instant

data class LedgerFilter(
    val transactionType: TransactionType? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val pageable: Pageable = PageRequest.of(0, 20)
)