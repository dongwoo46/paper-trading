package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.application.account.query.LedgerFilter
import com.papertrading.api.application.account.result.LedgerResult
import com.papertrading.api.domain.model.QAccountLedger
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl

class AccountLedgerRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : AccountLedgerRepositoryCustom {

    private val ledger = QAccountLedger.accountLedger

    override fun findLedgers(accountId: Long, filter: LedgerFilter): Page<LedgerResult> {
        val where = buildWhere(accountId, filter)

        val content = queryFactory
            .select(
                Projections.constructor(
                    LedgerResult::class.java,
                    ledger.id,
                    ledger.transactionType,
                    ledger.amount,
                    ledger.balanceAfter,
                    ledger.refOrderId,
                    ledger.refExecutionId,
                    ledger.description,
                    ledger.createdAt
                )
            )
            .from(ledger)
            .where(where)
            .orderBy(ledger.createdAt.desc())
            .offset(filter.pageable.offset)
            .limit(filter.pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(ledger.count())
            .from(ledger)
            .where(where)
            .fetchOne() ?: 0L

        return PageImpl(content, filter.pageable, total)
    }

    private fun buildWhere(accountId: Long, filter: LedgerFilter) = BooleanBuilder()
        .and(ledger.account.id.eq(accountId))
        .and(filter.transactionType?.let { ledger.transactionType.eq(it) })
        .and(filter.from?.let { ledger.createdAt.goe(it) })
        .and(filter.to?.let { ledger.createdAt.loe(it) })
}