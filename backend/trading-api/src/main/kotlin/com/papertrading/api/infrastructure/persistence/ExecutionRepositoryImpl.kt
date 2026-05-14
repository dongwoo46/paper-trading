package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.application.order.query.ExecutionQuery
import com.papertrading.api.domain.entity.order.Execution
import com.papertrading.api.domain.entity.order.QExecution.execution
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.Optional

class ExecutionRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : ExecutionRepositoryCustom {

    override fun searchExecutions(query: ExecutionQuery): List<Execution> =
        queryFactory
            .select(execution)
            .from(execution)
            .where(
                execution.account.id.eq(query.accountId),
                execution.order.id.eq(query.orderId),
                query.executedFrom?.let { execution.executedAt.goe(it) },
                query.executedTo?.let { execution.executedAt.loe(it) },
            )
            .orderBy(execution.executedAt.desc(), execution.id.desc())
            .fetch()

    override fun findByIdAndOrderIdAndAccountId(
        executionId: Long,
        orderId: Long,
        accountId: Long,
    ): Optional<Execution> =
        Optional.ofNullable(
            queryFactory
                .select(execution)
                .from(execution)
                .where(
                    execution.id.eq(executionId),
                    execution.order.id.eq(orderId),
                    execution.account.id.eq(accountId),
                )
                .fetchOne()
        )
}

