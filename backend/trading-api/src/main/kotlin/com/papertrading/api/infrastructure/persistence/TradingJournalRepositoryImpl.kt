package com.papertrading.api.infrastructure.persistence

import com.papertrading.api.application.portfolio.query.TradingJournalFilter
import com.papertrading.api.domain.entity.portfolio.QTradingJournal.tradingJournal
import com.papertrading.api.domain.entity.portfolio.TradingJournal
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.ZoneOffset

class TradingJournalRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : TradingJournalRepositoryCustom {

    override fun search(filter: TradingJournalFilter, pageable: Pageable): Page<TradingJournal> {
        val where = buildWhere(filter)

        val content = queryFactory.selectFrom(tradingJournal)
            .where(where)
            .orderBy(tradingJournal.createdAt.desc(), tradingJournal.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(tradingJournal.count())
            .from(tradingJournal)
            .where(where)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    private fun buildWhere(filter: TradingJournalFilter): BooleanBuilder {
        val fromInstant = filter.createdFrom?.atStartOfDay(ZoneOffset.UTC)?.toInstant()
        val toExclusive = filter.createdTo?.plusDays(1)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()
        val keyword = filter.keyword?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedTicker = filter.ticker?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        val normalizedType = filter.journalType?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        val normalizedSentiment = filter.sentiment?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }

        return BooleanBuilder()
            .and(tradingJournal.account.id.eq(filter.accountId))
            .and(filter.orderId?.let { tradingJournal.orderId.eq(it) })
            .and(normalizedTicker?.let { tradingJournal.ticker.eq(it) })
            .and(normalizedType?.let { tradingJournal.journalType.eq(it) })
            .and(normalizedSentiment?.let { tradingJournal.sentiment.eq(it) })
            .and(fromInstant?.let { tradingJournal.createdAt.goe(it) })
            .and(toExclusive?.let { tradingJournal.createdAt.lt(it) })
            .and(
                keyword?.let {
                    tradingJournal.title.containsIgnoreCase(it)
                        .or(tradingJournal.content.containsIgnoreCase(it))
                }
            )
    }
}

