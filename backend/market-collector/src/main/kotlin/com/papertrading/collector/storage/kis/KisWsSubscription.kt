package com.papertrading.collector.storage.kis

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("kis_ws_subscriptions")
data class KisWsSubscription(
	@Id val id: Long? = null,
	val mode: String,
	val symbol: String,
	val createdAt: java.time.LocalDateTime? = null,
)