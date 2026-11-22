package com.papertrading.api.support

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory

// 루트 로거의 모든 로그를 모으는 시험 도구, 메시지와 예외 체인 문구까지 한 줄 문자열로 펼침
class LogCapture : AutoCloseable {
    private val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    private val previousLevel = rootLogger.level
    private val appender = ListAppender<ILoggingEvent>()

    init {
        appender.start()
        rootLogger.level = Level.TRACE
        rootLogger.addAppender(appender)
    }

    fun lines(): List<String> = appender.list.toList().map { event ->
        buildString {
            append(event.loggerName).append(' ').append(event.level).append(' ').append(event.formattedMessage)
            appendThrowable(event.throwableProxy)
        }
    }

    fun linesOf(suffix: String): List<String> = lines().filter { it.substringBefore(' ').endsWith(suffix) }

    private fun StringBuilder.appendThrowable(proxy: IThrowableProxy?) {
        var current = proxy
        while (current != null) {
            append(' ').append(current.className).append(':').append(current.message)
            current = current.cause
        }
    }

    override fun close() {
        rootLogger.detachAppender(appender)
        rootLogger.level = previousLevel
        appender.stop()
    }
}
