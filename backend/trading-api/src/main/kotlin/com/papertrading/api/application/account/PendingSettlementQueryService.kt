package com.papertrading.api.application.account

import com.papertrading.api.application.account.query.PendingSettlementFilter
import com.papertrading.api.application.account.result.PendingSettlementResult
import com.papertrading.api.infrastructure.persistence.PendingSettlementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PendingSettlementQueryService(
    private val pendingSettlementRepository: PendingSettlementRepository
) {

    fun listPendingSettlements(accountId: Long, filter: PendingSettlementFilter): List<PendingSettlementResult> =
        pendingSettlementRepository.findByAccountIdAndFilter(accountId, filter)
}