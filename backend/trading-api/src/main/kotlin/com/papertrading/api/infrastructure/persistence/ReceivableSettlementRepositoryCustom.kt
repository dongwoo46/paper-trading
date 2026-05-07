package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.application.account.query.ReceivableSettlementFilter
import com.papertrading.api.application.account.result.ReceivableSettlementResult

interface ReceivableSettlementRepositoryCustom {
    fun findByAccountIdAndFilter(accountId: Long, filter: ReceivableSettlementFilter): List<ReceivableSettlementResult>
}

