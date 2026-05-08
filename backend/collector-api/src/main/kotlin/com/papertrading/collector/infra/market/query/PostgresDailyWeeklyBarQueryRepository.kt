package com.papertrading.collector.infra.market.query

import com.papertrading.collector.application.market.service.MarketBarRecord
import com.papertrading.collector.application.market.service.MarketBarSource
import com.papertrading.collector.application.market.service.MarketIndicatorsQuery
import com.papertrading.collector.domain.entity.market.MarketDailyOhlcv
import com.papertrading.collector.domain.market.indicator.Interval
import com.papertrading.collector.infra.market.persistence.MarketDailyOhlcvRepository
import com.papertrading.collector.infra.market.persistence.MarketWeeklyOhlcvRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

@Repository
class PostgresDailyWeeklyBarQueryRepository(
	private val marketDailyOhlcvRepository: MarketDailyOhlcvRepository,
	private val marketWeeklyOhlcvRepository: MarketWeeklyOhlcvRepository,
) : MarketBarSource {
	override fun load(symbol: String, interval: Interval, request: MarketIndicatorsQuery): List<MarketBarRecord> {
		return when (interval) {
			Interval.ONE_DAY -> loadDailyBars(symbol, request)
			Interval.ONE_WEEK -> loadWeeklyBars(symbol, request)
			else -> throw IllegalArgumentException("INVALID_INTERVAL")
		}
	}

	private fun loadDailyBars(symbol: String, request: MarketIndicatorsQuery): List<MarketBarRecord> {
		val daily = if (request.limit != null) {
			marketDailyOhlcvRepository
				.findBySourceAndSymbolOrderByTradeDateDesc("yfinance", symbol, PageRequest.of(0, request.limit))
				.reversed()
		} else {
			val from = request.from?.atZone(ZoneOffset.UTC)?.toLocalDate() ?: throw IllegalArgumentException("INVALID_PERIOD_QUERY")
			val to = request.to?.atZone(ZoneOffset.UTC)?.toLocalDate() ?: throw IllegalArgumentException("INVALID_PERIOD_QUERY")
			marketDailyOhlcvRepository.findBySourceAndSymbolAndTradeDateBetweenOrderByTradeDateAsc("yfinance", symbol, from, to)
		}
		return daily.map { it.toBarRecord() }
	}

	private fun loadWeeklyBars(symbol: String, request: MarketIndicatorsQuery): List<MarketBarRecord> {
		if (request.limit != null) {
			val weekly = marketWeeklyOhlcvRepository
				.findBySourceAndSymbolOrderByTradeDateDesc("yfinance", symbol, PageRequest.of(0, request.limit))
				.reversed()
			if (weekly.isNotEmpty()) {
				return weekly.map { MarketBarRecord(it.tradeDate.atStartOfDay().toInstant(ZoneOffset.UTC), it.closePrice) }
			}

			val dailyFallback = marketDailyOhlcvRepository
				.findBySourceAndSymbolOrderByTradeDateDesc("yfinance", symbol, PageRequest.of(0, request.limit * 10))
				.reversed()
			return composeWeeklyFromDaily(dailyFallback).takeLast(request.limit)
		}

		val from = request.from?.atZone(ZoneOffset.UTC)?.toLocalDate() ?: throw IllegalArgumentException("INVALID_PERIOD_QUERY")
		val to = request.to?.atZone(ZoneOffset.UTC)?.toLocalDate() ?: throw IllegalArgumentException("INVALID_PERIOD_QUERY")
		val weekly = marketWeeklyOhlcvRepository.findBySourceAndSymbolAndTradeDateBetweenOrderByTradeDateAsc("yfinance", symbol, from, to)
		if (weekly.isNotEmpty()) {
			return weekly.map { MarketBarRecord(it.tradeDate.atStartOfDay().toInstant(ZoneOffset.UTC), it.closePrice) }
		}
		val dailyFallback = marketDailyOhlcvRepository.findBySourceAndSymbolAndTradeDateBetweenOrderByTradeDateAsc("yfinance", symbol, from, to)
		return composeWeeklyFromDaily(dailyFallback)
	}

	private fun composeWeeklyFromDaily(dailyBars: List<MarketDailyOhlcv>): List<MarketBarRecord> {
		return dailyBars
			.groupBy { it.tradeDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
			.toSortedMap()
			.values
			.mapNotNull { weekBars ->
				val lastTradingDay = weekBars.maxByOrNull { it.tradeDate } ?: return@mapNotNull null
				MarketBarRecord(
					timestamp = weekStart(lastTradingDay.tradeDate).atStartOfDay().toInstant(ZoneOffset.UTC),
					close = lastTradingDay.closePrice,
				)
			}
	}

	private fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

	private fun MarketDailyOhlcv.toBarRecord(): MarketBarRecord =
		MarketBarRecord(tradeDate.atStartOfDay().toInstant(ZoneOffset.UTC), closePrice)
}
