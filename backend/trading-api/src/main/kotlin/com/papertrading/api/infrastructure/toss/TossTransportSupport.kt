package com.papertrading.api.infrastructure.toss

import com.papertrading.api.common.exception.ServiceUnavailableException
import com.papertrading.api.common.exception.TossTimeoutException
import com.papertrading.api.common.exception.TossUnavailableException
import com.papertrading.api.infrastructure.client.TransportTimeouts
import org.springframework.http.HttpHeaders
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import java.io.IOException
import java.net.URI

/** 발급기와 API 클라이언트가 함께 쓰는 전송 보조 (전송 실패 분류, Retry-After 해석, 로그용 값 정제, 쿼리 + 인코딩) */
internal object TossTransportSupport {
    private const val RETRY_AFTER_HEADER = "Retry-After"
    private const val MAX_LOGGED_CODE_LENGTH = 64
    private val safeCodePattern = Regex("[A-Za-z0-9_.\\-]+")

    // 타임아웃은 다른 입출력 오류와 구분, 원인 체인 판정은 KIS 와 공용
    fun transportFailure(e: RestClientException): ServiceUnavailableException {
        val timedOut = e is ResourceAccessException && TransportTimeouts.hasTimeoutCause(e)
        return if (timedOut) {
            TossTimeoutException("연결 또는 읽기 타임아웃", e)
        } else {
            TossUnavailableException(if (e.cause is IOException) "네트워크 오류" else "HTTP 클라이언트 오류", e)
        }
    }

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

    // 스프링 쿼리 인코딩은 + 를 그대로 두는데 토스는 쿼리의 + 를 공백으로 읽음(명세: 시각 오프셋의 + 는 %2B)
    // 인코딩이 끝난 URI 의 쿼리에 남은 + 는 값에서 온 것뿐이라(공백은 %20) 모두 %2B 로 바꿈
    fun withPlusEncodedInQuery(encoded: URI): URI {
        val rawQuery = encoded.rawQuery
        if (rawQuery == null || '+' !in rawQuery) return encoded
        return URI(encoded.toString().substringBefore('?') + "?" + rawQuery.replace("+", "%2B"))
    }
}
