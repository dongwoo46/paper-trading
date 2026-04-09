package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.application.account.query.PendingSettlementFilter
import com.papertrading.api.application.account.result.PendingSettlementResult
import com.papertrading.api.domain.model.QPendingSettlement
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory

class PendingSettlementRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : PendingSettlementRepositoryCustom {

    private val ps = QPendingSettlement.pendingSettlement

    override fun findByAccountIdAndFilter(
        accountId: Long,
        filter: PendingSettlementFilter
    ): List<PendingSettlementResult> {
        val where = BooleanBuilder()
            .and(ps.account.id.eq(accountId))
            .and(filter.status?.let { ps.status.eq(it) })
            .and(filter.from?.let { ps.settlementDate.goe(it) })
            .and(filter.to?.let { ps.settlementDate.loe(it) })

        return queryFactory
            .select(
                Projections.constructor(
                    PendingSettlementResult::class.java,
                    ps.id,
                    ps.orderId,
                    ps.settlementDate,
                    ps.amount,
                    ps.status
                )
            )
            .from(ps)
            .where(where)
            .orderBy(ps.settlementDate.asc())
            .fetch()
    }
}