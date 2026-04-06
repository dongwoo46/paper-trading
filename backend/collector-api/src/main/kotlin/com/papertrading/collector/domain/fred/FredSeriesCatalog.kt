package com.papertrading.collector.domain.fred

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
@Table(name = "fred_series_catalog")
data class FredSeriesCatalog protected constructor(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,
	@Column(name = "series_id", nullable = false)
	val seriesId: String,
	@Column(nullable = false)
	val title: String,
	@Column(nullable = false)
	val category: String,
	@Column(nullable = false)
	val frequency: String,
	@Column(nullable = false)
	val units: String,
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

