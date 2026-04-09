package com.papertrading.api.application.account.query

import com.papertrading.api.domain.enums.SettlementStatus
import java.time.LocalDate

data class PendingSettlementFilter(
    val status: SettlementStatus? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null
)