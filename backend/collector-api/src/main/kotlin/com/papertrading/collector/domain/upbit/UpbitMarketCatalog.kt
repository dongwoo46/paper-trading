package com.papertrading.collector.domain.upbit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "upbit_market_catalog")
data class UpbitMarketCatalog protected constructor(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
	@Column(nullable = false)
	val market: String,
	@Column(nullable = false)
	val name: String,
	@Column(name = "market_group", nullable = false)
	val marketGroup: String,
	@Column(nullable = false)
	val enabled: Boolean,
	@Column(name = "is_default", nullable = false)
	val isDefault: Boolean,
	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	val createdAt: LocalDateTime? = null,
	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	val updatedAt: LocalDateTime? = null,
)

