package com.papertrading.collector.application.fred.runtime

import com.papertrading.collector.application.fred.service.FredObservationIngestionService
import com.papertrading.collector.infra.fred.FredProperties
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
class FredObservationScheduler(
	private val fredProperties: FredProperties,
	private val fredObservationIngestionService: FredObservationIngestionService,
) {
	private val log = KotlinLogging.logger {}
	private val running = AtomicBoolean(false)

	@Scheduled(
		initialDelayString = "\${collector.source.fred.polling-initial-delay-millis:60000}",
		fixedDelayString = "\${collector.source.fred.polling-interval-millis:300000}",
	)
	fun pollSelectedSeries() {
		if (!fredProperties.pollingEnabled) {
			return
		}
		if (!running.compareAndSet(false, true)) {
			log.info { "fred observation polling skipped: previous run is still in progress" }
			return
		}

		try {
			val result = fredObservationIngestionService.collectSelectedSeries()
			log.info { "fred observation polling result=$result" }
		} finally {
			running.set(false)
		}
	}
}
