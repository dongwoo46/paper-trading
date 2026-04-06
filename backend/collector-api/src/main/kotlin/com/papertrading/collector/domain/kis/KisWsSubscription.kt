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
@Table(name = "kis_ws_subscriptions")
data class KisWsSubscription protected constructor(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
	@Column(nullable = false)
	val mode: String,
	@Column(nullable = false)
	val symbol: String,
	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	val createdAt: LocalDateTime? = null,
) {
	companion object {
		fun create(mode: String, symbol: String): KisWsSubscription {
			return KisWsSubscription(
				mode = mode,
				symbol = symbol,
			)
		}
	}
}

