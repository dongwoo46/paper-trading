package com.papertrading.api.application.account

import com.papertrading.api.application.account.query.ReceivableSettlementFilter
import com.papertrading.api.application.account.result.ReceivableSettlementResult
import com.papertrading.api.infrastructure.persistence.ReceivableSettlementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ReceivableSettlementQueryService(
    private val ReceivableSettlementRepository: ReceivableSettlementRepository
) {

    fun listReceivableSettlements(accountId: Long, filter: ReceivableSettlementFilter): List<ReceivableSettlementResult> =
        ReceivableSettlementRepository.findByAccountIdAndFilter(accountId, filter)
}
