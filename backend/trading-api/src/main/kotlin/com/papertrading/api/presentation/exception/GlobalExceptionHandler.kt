package com.papertrading.api.presentation.exception

import com.papertrading.api.common.exception.ApiDomainException
import com.papertrading.api.common.exception.InternalErrorException
import com.papertrading.api.common.exception.NotImplementedException
import com.papertrading.api.common.exception.ServiceUnavailableException
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@Suppress("TooManyFunctions") // 예외 종류마다 처리기 하나, 새 범주마다 하나씩 늘어남
@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = KotlinLogging.logger {}

    // 외부 원문이 실릴 수 있는 code 는 예외 메시지 대신 code 고정 문구 (D28, AE8)
    @ExceptionHandler(ApiDomainException::class)
    fun handleApiDomain(ex: ApiDomainException): ResponseEntity<ApiErrorResponse> {
        val message = fixedDomainMessage(ex.errorCode) ?: ex.message ?: defaultMessage(ex.errorCode)
        return ResponseEntity.status(ex.status).body(ApiErrorResponse(ex.status.value(), ex.errorCode, message))
    }

    // 범주 I(501), common 예외는 Spring 을 몰라 범주→HTTP 상태 매핑을 여기서만 한다
    @ExceptionHandler(NotImplementedException::class)
    fun handleNotImplemented(ex: NotImplementedException): ResponseEntity<ApiErrorResponse> {
        val status = HttpStatus.NOT_IMPLEMENTED
        return ResponseEntity.status(status).body(
            ApiErrorResponse(status.value(), ex.errorCode, ex.message ?: "아직 지원하지 않는 기능입니다."),
        )
    }

    // 범주 J(503), 응답 문구는 code 별 고정 문구만이고 예외 메시지(토스 원문 가능)는 로그에만 남김 (D15, C-09)
    @ExceptionHandler(ServiceUnavailableException::class)
    fun handleServiceUnavailable(ex: ServiceUnavailableException): ResponseEntity<ApiErrorResponse> {
        log.warn(ex) { "외부 서비스 사용 불가: code=${ex.errorCode}" }
        val status = HttpStatus.SERVICE_UNAVAILABLE
        return ResponseEntity.status(status).body(
            ApiErrorResponse(status.value(), ex.errorCode, serviceUnavailableMessage(ex.errorCode)),
        )
    }

    // 범주 K(500), 내부 메시지는 응답에 싣지 않고 로그에 스택과 함께 남김
    @ExceptionHandler(InternalErrorException::class)
    fun handleInternalError(ex: InternalErrorException): ResponseEntity<ApiErrorResponse> {
        log.error(ex) { "내부 오류: code=${ex.errorCode}, message=${ex.message}" }
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        return ResponseEntity.status(status).body(
            ApiErrorResponse(status.value(), ex.errorCode, "서버 오류가 발생했습니다."),
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiErrorResponse(400, "INVALID_BUSINESS_DATE", ex.message ?: "날짜 형식이 올바르지 않습니다."),
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiErrorResponse(400, "BAD_REQUEST", ex.message ?: "요청 값이 유효하지 않습니다."),
        )

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiErrorResponse(400, "BAD_REQUEST", ex.message ?: "요청을 처리할 수 없는 상태입니다."),
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiErrorResponse(400, "INVALID_PERCENT_VALUE", "요청 본문 검증에 실패했습니다."),
        )

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElement(ex: NoSuchElementException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiErrorResponse(
                404,
                when {
                    (ex.message ?: "").contains("계좌") -> "ACCOUNT_NOT_FOUND"
                    (ex.message ?: "").contains("주문") -> "ORDER_NOT_FOUND"
                    (ex.message ?: "").contains("포지션") || (ex.message ?: "").contains("position") -> "POSITION_NOT_FOUND"
                    (ex.message ?: "").contains("거래 일지") -> "TRADING_JOURNAL_NOT_FOUND"
                    else -> "NOT_FOUND"
                },
                ex.message ?: "리소스를 찾을 수 없습니다.",
            ),
        )

    // 응답은 고정 500, 원인은 응답에 싣지 않고 로그에만 남김 (C-07)
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiErrorResponse> {
        log.error(ex) { "처리되지 않은 예외" }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiErrorResponse(500, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."),
        )
    }

    private fun serviceUnavailableMessage(errorCode: String): String = when (errorCode) {
        "TOSS_TOKEN_UNAVAILABLE" -> "토스 액세스 토큰을 사용할 수 없습니다."
        "TOSS_AUTH_FAILED" -> "토스 인증에 실패했습니다. 토스 키 설정을 확인해 주세요."
        "TOSS_IP_NOT_ALLOWED" -> TOSS_IP_NOT_ALLOWED_MESSAGE
        "TOSS_RATE_LIMITED" -> "토스 호출 한도를 넘었습니다. 잠시 후 다시 시도해 주세요."
        "TOSS_MAINTENANCE" -> "토스증권이 점검 중입니다. 잠시 후 다시 시도해 주세요."
        "TOSS_UNAVAILABLE" -> "토스증권을 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."
        "TOSS_TIMEOUT" -> "토스증권 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요."
        "TOSS_RESPONSE_INVALID" -> "토스증권 응답을 해석하지 못했습니다."
        else -> kisServiceUnavailableMessage(errorCode)
    }

    // 토스 쪽과 나눈 이유: 한 함수에 모으면 detekt CyclomaticComplexMethod 한도(15) 도달
    private fun kisServiceUnavailableMessage(errorCode: String): String = when (errorCode) {
        "KIS_AUTH_FAILED" -> "KIS 인증에 실패했습니다. KIS 키 설정을 확인해 주세요."
        "KIS_PERMISSION_DENIED" -> "KIS 계좌 조회 권한이 없습니다. KIS 앱 권한 설정을 확인해 주세요."
        "KIS_RATE_LIMITED" -> "KIS 호출 한도를 넘었습니다. 잠시 후 다시 시도해 주세요."
        "KIS_UNAVAILABLE" -> "KIS를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."
        "KIS_TIMEOUT" -> "KIS 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요."
        "KIS_ORDER_RESULT_UNKNOWN" -> "KIS 접수 결과를 확인하지 못했습니다. 다시 주문하기 전에 KIS 주문 내역을 확인해 주세요."
        else -> "외부 서비스를 일시적으로 사용할 수 없습니다."
    }

    private fun fixedDomainMessage(errorCode: String): String? = when (errorCode) {
        "KIS_ORDER_REJECTED" -> "KIS가 주문을 받지 않았습니다. 주문 내용과 계좌 상태를 확인해 주세요."
        else -> null
    }

    private fun defaultMessage(errorCode: String): String = when (errorCode) {
        "ACCOUNT_NOT_FOUND" -> "계좌를 찾을 수 없습니다."
        "ORDER_NOT_FOUND" -> "주문을 찾을 수 없습니다."
        "POSITION_NOT_FOUND" -> "포지션을 찾을 수 없습니다."
        "TRADING_JOURNAL_NOT_FOUND" -> "거래 일지를 찾을 수 없습니다."
        "INVALID_RETRY_POLICY" -> "재시도 정책 값이 올바르지 않습니다."
        "WEBHOOK_NOT_CONFIGURED" -> "Webhook이 설정되지 않았습니다."
        else -> "요청 처리 중 오류가 발생했습니다."
    }

    companion object {
        // 허용 IP 등록 안내 고정 문구 (D15)
        private const val TOSS_IP_NOT_ALLOWED_MESSAGE =
            "토스증권에 이 서버의 IP 가 허용 IP 로 등록돼 있지 않습니다. 토스증권 설정에서 허용 IP 를 등록한 뒤 다시 시도해 주세요."
    }
}
