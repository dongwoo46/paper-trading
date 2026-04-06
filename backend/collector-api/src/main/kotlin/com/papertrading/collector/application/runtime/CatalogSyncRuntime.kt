package com.papertrading.collector.application.runtime

import com.papertrading.collector.application.fred.service.FredSeriesCatalogService
import com.papertrading.collector.application.market.service.PykrxSymbolCatalogService
import com.papertrading.collector.application.upbit.service.UpbitMarketCatalogService
import com.papertrading.collector.infra.catalog.CatalogSyncProperties
import com.papertrading.collector.infra.fred.FredProperties
import mu.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CatalogSyncRuntime(
	private val syncProperties: CatalogSyncProperties,
	private val fredProperties: FredProperties,
	private val pykrxSymbolCatalogService: PykrxSymbolCatalogService,
	private val upbitMarketCatalogService: UpbitMarketCatalogService,
	private val fredSeriesCatalogService: FredSeriesCatalogService,
) : ApplicationRunner {
	private val log = KotlinLogging.logger {}

	override fun run(args: ApplicationArguments) {
		if (!syncProperties.enabledOnStartup) {
			log.info { "Catalog startup sync is disabled" }
			return
		}

		if (syncProperties.pykrxEnabled) {
			runCatching { pykrxSymbolCatalogService.syncCatalogFromKrSymbol() }
				.onSuccess { affected -> log.info { "pykrx catalog sync completed: affectedRows=$affected" } }
				.onFailure { ex -> log.warn(ex) { "pykrx catalog sync failed" } }
		}

		if (syncProperties.upbitEnabled) {
			runCatching { upbitMarketCatalogService.syncCatalogFromUpbit() }
				.onSuccess { processed -> log.info { "upbit catalog sync completed: processed=$processed" } }
				.onFailure { ex -> log.warn(ex) { "upbit catalog sync failed" } }
		}

		if (syncProperties.fredEnabled) {
			if (!fredProperties.enabled || fredProperties.apiKey.isBlank()) {
				log.info { "fred catalog sync skipped: fred disabled or api key missing" }
			} else {
				runCatching {
					fredSeriesCatalogService.syncAllCatalog(
						maxCategories = syncProperties.fredMaxCategories,
						pageSize = syncProperties.fredPageSize,
					)
				}.onSuccess { result ->
					val categories = result["processedCategories"] ?: 0
					val series = result["upsertedSeries"] ?: 0
					log.info { "fred catalog sync completed: processedCategories=$categories, upsertedSeries=$series" }
				}.onFailure { ex ->
					log.warn(ex) { "fred catalog sync failed" }
				}
			}
		}
	}
}

