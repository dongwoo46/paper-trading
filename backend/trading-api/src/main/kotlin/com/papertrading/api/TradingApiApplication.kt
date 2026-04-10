package com.papertrading.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
class TradingApiApplication

fun main(args: Array<String>) {
    runApplication<TradingApiApplication>(*args)
}
