package com.papertrading.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TradingApiApplication

fun main(args: Array<String>) {
    runApplication<TradingApiApplication>(*args)
}
