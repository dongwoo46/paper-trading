package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.domain.entity.position.Position
import com.papertrading.api.domain.entity.position.QPosition.position
import com.papertrading.api.domain.enums.TradingMode
import com.querydsl.jpa.impl.JPAQueryFactory
import java.math.BigDecimal

class PositionRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : PositionRepositoryCustom {

    override fun findOpenByTickerAndMode(
        ticker: String,
        tradingMode: TradingMode,
        minQuantity: BigDecimal,
    ): List<Position> =
        queryFactory
            .selectFrom(position)
            .join(position.account).fetchJoin()
            .where(
                position.ticker.eq(ticker.trim().uppercase()),
                position.account.tradingMode.eq(tradingMode),
                position.quantity.gt(minQuantity),
            )
            .fetch()
}
