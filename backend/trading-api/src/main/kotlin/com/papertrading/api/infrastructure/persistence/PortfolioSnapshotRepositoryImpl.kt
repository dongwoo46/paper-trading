package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.portfolio.PortfolioSnapshot
import com.papertrading.api.domain.entity.portfolio.QPortfolioSnapshot.portfolioSnapshot
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate

class PortfolioSnapshotRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : PortfolioSnapshotRepositoryCustom {

    override fun searchByAccountIdAndBusinessDate(accountId: Long, businessDate: LocalDate): List<PortfolioSnapshot> =
        queryFactory
            .selectFrom(portfolioSnapshot)
            .where(
                portfolioSnapshot.account.id.eq(accountId),
                portfolioSnapshot.businessDate.eq(businessDate),
            )
            .orderBy(portfolioSnapshot.ticker.asc())
            .fetch()
}

