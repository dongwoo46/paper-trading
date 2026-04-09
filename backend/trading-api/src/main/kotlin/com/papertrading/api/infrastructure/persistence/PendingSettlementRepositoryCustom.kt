package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.application.account.query.PendingSettlementFilter
import com.papertrading.api.application.account.result.PendingSettlementResult

interface PendingSettlementRepositoryCustom {
    fun findByAccountIdAndFilter(accountId: Long, filter: PendingSettlementFilter): List<PendingSettlementResult>
}
