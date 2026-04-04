package com.papertrading.collector.runtime

import com.papertrading.collector.source.kis.ws.KisWebSocketCollector
import com.papertrading.collector.source.upbit.collector.UpbitWebSocketCollector
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class CollectorRuntime(
	private val kisWebSocketCollector: KisWebSocketCollector,
	private val upbitWebSocketCollector: UpbitWebSocketCollector,
) : ApplicationRunner {

	override fun run(args: ApplicationArguments) {
		kisWebSocketCollector.start()
		upbitWebSocketCollector.start()
	}
}
