package com.papertrading.collector.infra.catalog

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "collector.catalog-sync")
class CatalogSyncProperties {
	var enabledOnStartup: Boolean = true
	var pykrxEnabled: Boolean = true
	var upbitEnabled: Boolean = true
	var fredEnabled: Boolean = true
	var fredMaxCategories: Int = 500
	var fredPageSize: Int = 100
}
