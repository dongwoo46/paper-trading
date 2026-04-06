package com.papertrading.collector.domain.market

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "market_daily_ohlcv")
data class MarketDailyOhlcv protected constructor(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
	@Column(nullable = false)
	val source: String,
	@Column(nullable = false)
	val symbol: String,
	@Column(nullable = false)
	val market: String,
	@Column(name = "trade_date", nullable = false)
	val tradeDate: LocalDate,
	@Column(name = "open_price", nullable = false)
	val openPrice: BigDecimal,
	@Column(name = "high_price", nullable = false)
	val highPrice: BigDecimal,
	@Column(name = "low_price", nullable = false)
	val lowPrice: BigDecimal,
	@Column(name = "close_price", nullable = false)
	val closePrice: BigDecimal,
	@Column(nullable = false)
	val volume: BigDecimal,
	@Column(name = "adj_close_price")
	val adjClosePrice: BigDecimal? = null,
	@Column(nullable = false)
	val provider: String = "unknown",
	@Column(nullable = false)
	val interval: String = "1d",
	@Column(name = "is_adjusted", nullable = false)
	val isAdjusted: Boolean = false,
	@Column(name = "collected_at", nullable = false)
	val collectedAt: LocalDateTime = LocalDateTime.now(),
	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	val createdAt: LocalDateTime? = null,
	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	val updatedAt: LocalDateTime? = null,
)
