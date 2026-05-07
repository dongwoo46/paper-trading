package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.application.account.query.ReceivableSettlementFilter
import com.papertrading.api.application.account.result.ReceivableSettlementResult
import com.papertrading.api.domain.entity.settlement.QReceivableSettlement
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory

class ReceivableSettlementRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : ReceivableSettlementRepositoryCustom {

    private val ps = QReceivableSettlement.receivableSettlement

    override fun findByAccountIdAndFilter(
        accountId: Long,
        filter: ReceivableSettlementFilter
    ): List<ReceivableSettlementResult> {
        val where = BooleanBuilder()
            .and(ps.account.id.eq(accountId))
            .and(filter.status?.let { ps.status.eq(it) })
            .and(filter.from?.let { ps.settlementDate.goe(it) })
            .and(filter.to?.let { ps.settlementDate.loe(it) })

        return queryFactory
            .select(
                Projections.constructor(
                    ReceivableSettlementResult::class.java,
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
