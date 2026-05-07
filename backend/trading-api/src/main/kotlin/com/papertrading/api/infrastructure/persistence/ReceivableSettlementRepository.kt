package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.enums.SettlementStatus
import com.papertrading.api.domain.entity.settlement.ReceivableSettlement
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ReceivableSettlementRepository : JpaRepository<ReceivableSettlement, Long>, ReceivableSettlementRepositoryCustom {
    fun findBySettlementDateLessThanEqualAndStatus(date: LocalDate, status: SettlementStatus): List<ReceivableSettlement>
}

