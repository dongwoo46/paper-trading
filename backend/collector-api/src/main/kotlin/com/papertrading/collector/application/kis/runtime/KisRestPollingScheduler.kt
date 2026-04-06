package com.papertrading.collector.application.kis.runtime

import com.papertrading.collector.infra.kis.KisProperties
import com.papertrading.collector.application.kis.service.KisRestWatchlistService
import com.papertrading.collector.infra.kis.source.rest.KisQuoteClient
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
class KisRestPollingScheduler(
	private val kisProperties: KisProperties,
	private val restWatchlistService: KisRestWatchlistService,
	private val quoteClient: KisQuoteClient,
) {
	private val log = KotlinLogging.logger {}
	private val running = AtomicBoolean(false)

	@Scheduled(
		fixedRateString = "\${collector.source.kis.rest-polling-interval-millis:60000}",
		initialDelayString = "\${collector.source.kis.rest-polling-initial-delay-millis:0}",
	)
	fun poll() {
		if (!kisProperties.enabled || !kisProperties.restPollingEnabled) {
			return
		}
		if (!running.compareAndSet(false, true)) {
			log.warn("Skip KIS REST polling because previous cycle is still running")
			return
		}

		try {
			log.info("KIS REST polling started")
			for (mode in kisProperties.normalizedModes()) {
				if (kisProperties.appKeyFor(mode).isBlank() || kisProperties.appSecretFor(mode).isBlank()) {
					log.warn("Skip KIS REST polling for mode={} because app key/secret is empty", mode)
					continue
				}

				val symbols = restWatchlistService.listSymbols(mode)
				log.info("KIS REST polling mode={}, symbolCount={}", mode, symbols.size)
				for (symbol in symbols) {
					try {
						val response = quoteClient.inquirePrice(mode, symbol).block()
						log.info(
							"KIS REST polling success. mode={}, symbol={}, response={}",
							mode,
							symbol,
							response?.toString() ?: "null",
						)
					} catch (ex: Exception) {
						log.warn("KIS REST polling failed. mode={}, symbol={}", mode, symbol, ex)
					}
				}
			}
		} finally {
			running.set(false)
			log.info("KIS REST polling finished")
		}
	}
}
