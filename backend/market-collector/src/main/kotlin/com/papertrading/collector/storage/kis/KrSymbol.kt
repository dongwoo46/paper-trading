package com.papertrading.collector.storage.kis

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("kr_symbol")
data class KrSymbol(
	@Id val id: Long? = null,
	val symbol: String,
	val name: String,
	val market: String,
)

