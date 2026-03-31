package com.papertrading.collector

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MarketCollectorApplication

fun main(args: Array<String>) {
	runApplication<MarketCollectorApplication>(*args)
}
