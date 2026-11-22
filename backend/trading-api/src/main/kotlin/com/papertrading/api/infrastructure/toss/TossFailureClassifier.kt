package com.papertrading.api.infrastructure.toss

import com.papertrading.api.common.exception.TossAuthFailedException
import com.papertrading.api.common.exception.TossIpNotAllowedException
import com.papertrading.api.common.exception.TossMaintenanceException
import com.papertrading.api.common.exception.TossRateLimitedException
import com.papertrading.api.common.exception.TossRequestRejectedException
import com.papertrading.api.common.exception.TossResponseInvalidException
import com.papertrading.api.common.exception.TossUnavailableException

/**
 * 재시도하지 않기로 한 토스 오류 응답을 D15 code 의 예외로 분류 (상태 코드와 토스 code 만 봄, 응답 문구는 보지 않음)
 * 401 은 재시도 뒤에도 실패했거나 토큰 문제가 아닌 경우, 429 는 재시도 횟수나 대기 합계 상한을 넘긴 경우에 여기로 옴
 */
internal object TossFailureClassifier {
    private const val CODE_IP_NOT_ALLOWED = "ip-not-allowed"
    private const val CODE_MAINTENANCE = "maintenance"
    private const val STATUS_UNAUTHORIZED = 401
    private const val STATUS_FORBIDDEN = 403
    private const val STATUS_TOO_MANY_REQUESTS = 429
    private const val STATUS_SERVER_ERROR_MIN = 500
    private const val STATUS_CLIENT_ERROR_MIN = 400
    private const val STATUS_CLIENT_ERROR_MAX = 499
    private val clientErrors = STATUS_CLIENT_ERROR_MIN..STATUS_CLIENT_ERROR_MAX

    // code 는 TossTransportSupport.safeCode 를 거친 값
    fun classify(status: Int, code: String, retries: Retries): RuntimeException = when {
        status == STATUS_UNAUTHORIZED -> unauthorized(code, retries.authRetried)
        status == STATUS_FORBIDDEN -> forbidden(code)
        status == STATUS_TOO_MANY_REQUESTS -> rateLimited(retries)
        status >= STATUS_SERVER_ERROR_MIN -> serverError(status, code)
        status in clientErrors -> TossRequestRejectedException(status)
        else -> TossResponseInvalidException("예상 밖 상태 코드 $status")
    }

    // 재시도 이력, 사유 문구용
    class Retries(val authRetried: Boolean, val rateRetries: Int, val rateWaitedMillis: Long)

    private fun unauthorized(code: String, authRetried: Boolean): RuntimeException {
        val reason = if (authRetried) "401 재시도 뒤에도 실패 $code" else "401 $code"
        return TossAuthFailedException(reason)
    }

    private fun forbidden(code: String): RuntimeException {
        val reason = "403 $code"
        return if (code == CODE_IP_NOT_ALLOWED) TossIpNotAllowedException(reason) else TossAuthFailedException(reason)
    }

    private fun rateLimited(retries: Retries): RuntimeException {
        val reason = "429 재시도 ${retries.rateRetries}회, 대기 합계 ${retries.rateWaitedMillis}ms"
        return TossRateLimitedException(reason)
    }

    private fun serverError(status: Int, code: String): RuntimeException {
        val reason = "$status $code"
        return if (code == CODE_MAINTENANCE) TossMaintenanceException(reason) else TossUnavailableException(reason)
    }
}
