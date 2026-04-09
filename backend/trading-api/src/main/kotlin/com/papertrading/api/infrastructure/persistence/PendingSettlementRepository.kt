package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.enums.SettlementStatus
import com.papertrading.api.domain.model.PendingSettlement
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface PendingSettlementRepository : JpaRepository<PendingSettlement, Long>, PendingSettlementRepositoryCustom {
    fun findBySettlementDateLessThanEqualAndStatus(date: LocalDate, status: SettlementStatus): List<PendingSettlement>
}
