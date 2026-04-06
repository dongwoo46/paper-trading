package com.papertrading.collector.application.kis.runtime

import com.papertrading.collector.infra.kis.source.ws.KisWebSocketCollector
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class CollectorRuntime(
	private val kisWebSocketCollector: KisWebSocketCollector,
) : ApplicationRunner {

	override fun run(args: ApplicationArguments) {
		kisWebSocketCollector.start()
	}
}

