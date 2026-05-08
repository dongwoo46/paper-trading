package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.application.portfolio.tax.SettlementTaxAggregate
import com.papertrading.api.application.portfolio.tax.SettlementTaxReadRepository
import com.papertrading.api.common.exception.UnsupportedSettlementCurrencyException
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant

@Repository
class SettlementTaxReadRepositoryImpl(
    private val entityManager: EntityManager,
) : SettlementTaxReadRepository {

    override fun summarizeForTax(accountId: Long, yearStart: Instant, yearEnd: Instant): SettlementTaxAggregate {
        val currencyCount = entityManager.createQuery(
            """
            SELECT COUNT(DISTINCT s.currency)
            FROM Settlement s
            WHERE s.account.id = :accountId
              AND s.settledAt >= :yearStart
              AND s.settledAt < :yearEnd
            """.trimIndent(),
            java.lang.Long::class.java
        )
            .setParameter("accountId", accountId)
            .setParameter("yearStart", yearStart)
            .setParameter("yearEnd", yearEnd)
            .singleResult
            .toLong()

        if (currencyCount > 1L) {
            throw UnsupportedSettlementCurrencyException(accountId)
        }

        val row = entityManager.createQuery(
            """
            SELECT COALESCE(SUM(s.realizedPnl), 0),
                   COALESCE(SUM(s.fee), 0),
                   COALESCE(SUM(s.tax), 0),
                   COALESCE(MIN(s.currency), 'KRW')
            FROM Settlement s
            WHERE s.account.id = :accountId
              AND s.settledAt >= :yearStart
              AND s.settledAt < :yearEnd
            """.trimIndent(),
            Array<Any>::class.java
        )
            .setParameter("accountId", accountId)
            .setParameter("yearStart", yearStart)
            .setParameter("yearEnd", yearEnd)
            .singleResult

        return SettlementTaxAggregate(
            totalRealizedPnl = row[0] as BigDecimal,
            totalFee = row[1] as BigDecimal,
            totalTax = row[2] as BigDecimal,
            currency = row[3] as String,
        )
    }
}
