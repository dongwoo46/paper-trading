package com.papertrading.api.infrastructure.toss

import com.papertrading.api.common.exception.ServiceUnavailableException
import com.papertrading.api.common.exception.TossTimeoutException
import com.papertrading.api.common.exception.TossUnavailableException
import org.springframework.http.HttpHeaders
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.http.HttpTimeoutException

/** 발급기와 API 클라이언트가 함께 쓰는 전송 보조 (전송 실패 분류, Retry-After 해석, 로그용 값 정제) */
internal object TossTransportSupport {
    private const val RETRY_AFTER_HEADER = "Retry-After"
    private const val MAX_LOGGED_CODE_LENGTH = 64
    private const val VISIBLE_TAIL_LENGTH = 4
    private val safeCodePattern = Regex("[A-Za-z0-9_.\\-]+")

    // 타임아웃은 다른 입출력 오류와 구분, 구현체(JDK, Simple, Apache)마다 예외 종류가 달라 원인 체인을 훑음
    fun transportFailure(e: RestClientException): ServiceUnavailableException {
        val timedOut = e is ResourceAccessException && causeChain(e).any(::isTimeout)
        return if (timedOut) {
            TossTimeoutException("연결 또는 읽기 타임아웃", e)
        } else {
            TossUnavailableException(if (e.cause is IOException) "네트워크 오류" else "HTTP 클라이언트 오류", e)
        }
    }

    private fun isTimeout(e: Throwable): Boolean = e is SocketTimeoutException || e is HttpTimeoutException

    private fun causeChain(e: Throwable) = generateSequence(e) { current -> current.cause?.takeIf { it !== current } }

    // 초 단위 정수만 인정, 없거나 숫자가 아니거나 음수면 null (호출 쪽이 기본 1초로 대체)
    fun retryAfterSeconds(headers: HttpHeaders?): Long? {
        val seconds = headers?.getFirst(RETRY_AFTER_HEADER)?.trim()?.toLongOrNull()
        return seconds?.takeIf { it >= 0 }
    }

    // 원격이 준 code 를 로그와 예외 사유에 그대로 쓰지 않고 안전한 문자만, 길이 제한
    fun safeCode(code: String?): String {
        val safe = code != null && code.length <= MAX_LOGGED_CODE_LENGTH && safeCodePattern.matches(code)
        return if (safe) code.orEmpty() else "unknown"
    }

    // 계좌 식별값은 끝 4자리만 로그에 남김 (RULE-05)
    fun maskTail(value: String?): String = when {
        value == null -> "none"
        value.length <= VISIBLE_TAIL_LENGTH -> "****"
        else -> "****" + value.takeLast(VISIBLE_TAIL_LENGTH)
    }
}
