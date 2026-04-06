package com.papertrading.collector.domain.kis

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "kr_symbol")
data class KrSymbol protected constructor(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
	@Column(nullable = false)
	val symbol: String,
	@Column(nullable = false)
	val name: String,
	@Column(nullable = false)
	val market: String,
	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	val createdAt: LocalDateTime? = null,
) {
	companion object {
		fun create(symbol: String, name: String, market: String): KrSymbol {
			return KrSymbol(
				symbol = symbol,
				name = name,
				market = market,
			)
		}
	}
}

