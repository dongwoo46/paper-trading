package com.papertrading.api

import com.papertrading.api.infrastructure.notification.SlackNotificationProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties(SlackNotificationProperties::class)
class TradingApiApplication

fun main(args: Array<String>) {
    runApplication<TradingApiApplication>(*args)
}
