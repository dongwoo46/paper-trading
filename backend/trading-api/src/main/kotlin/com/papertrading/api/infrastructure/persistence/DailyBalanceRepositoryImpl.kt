package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.portfolio.DailyBalance
import com.papertrading.api.domain.entity.portfolio.QDailyBalance.dailyBalance
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate

class DailyBalanceRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : DailyBalanceRepositoryCustom {

    override fun searchDailyBalances(accountId: Long, from: LocalDate, to: LocalDate): List<DailyBalance> =
        queryFactory
            .selectFrom(dailyBalance)
            .where(
                dailyBalance.account.id.eq(accountId),
                dailyBalance.businessDate.between(from, to),
            )
            .orderBy(dailyBalance.businessDate.asc())
            .fetch()
}

