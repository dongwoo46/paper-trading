package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.portfolio.QTaxSummary.taxSummary
import com.papertrading.api.domain.entity.portfolio.TaxSummary
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.Optional

class TaxSummaryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : TaxSummaryRepositoryCustom {

    override fun findOneByAccountIdAndTaxYear(accountId: Long, taxYear: Int): Optional<TaxSummary> =
        Optional.ofNullable(
            queryFactory
                .selectFrom(taxSummary)
                .where(
                    taxSummary.account.id.eq(accountId),
                    taxSummary.taxYear.eq(taxYear),
                )
                .fetchOne()
        )

    override fun searchByAccountIdAndTaxYearRange(accountId: Long, fromYear: Int, toYear: Int): List<TaxSummary> =
        queryFactory
            .selectFrom(taxSummary)
            .where(
                taxSummary.account.id.eq(accountId),
                taxSummary.taxYear.between(fromYear, toYear),
            )
            .orderBy(taxSummary.taxYear.desc())
            .fetch()
}

