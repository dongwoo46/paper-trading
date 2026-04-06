package com.papertrading.collector.domain.market

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "pykrx_symbol_catalog")
data class PykrxSymbolCatalog protected constructor(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
	@Column(nullable = false)
	val symbol: String,
	@Column(nullable = false)
	val name: String,
	@Column(nullable = false)
	val market: String,
	@Column(nullable = false)
	val enabled: Boolean,
	@Column(name = "is_default", nullable = false)
	val isDefault: Boolean,
	@Column(name = "fetched_until_date")
	val fetchedUntilDate: LocalDate? = null,
	@Column(name = "last_collected_at")
	val lastCollectedAt: LocalDateTime? = null,
	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	val createdAt: LocalDateTime? = null,
	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	val updatedAt: LocalDateTime? = null,
)
