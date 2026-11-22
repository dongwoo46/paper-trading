package com.papertrading.api.infrastructure.client

import java.net.SocketTimeoutException
import java.net.http.HttpTimeoutException

/**
 * 원격 호출 전송 실패의 타임아웃 판정, 토스·KIS 공용 (S9-01)
 * HTTP 구현체(JDK, Simple, Apache)마다 예외 종류가 달라 원인 체인을 훑음, 연결 거부·DNS·TLS 는 타임아웃 아님
 */
internal object TransportTimeouts {
    fun hasTimeoutCause(e: Throwable): Boolean = causeChain(e).any(::isTimeout)

    private fun isTimeout(e: Throwable): Boolean = e is SocketTimeoutException || e is HttpTimeoutException

    fun causeChain(e: Throwable): Sequence<Throwable> = generateSequence(e) { current ->
        current.cause?.takeIf { it !== current }
    }
}
