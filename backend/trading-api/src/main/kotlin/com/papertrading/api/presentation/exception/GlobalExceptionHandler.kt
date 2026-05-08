package com.papertrading.api.presentation.exception

import com.papertrading.api.common.exception.ApiDomainException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ApiDomainException::class)
    fun handleApiDomain(ex: ApiDomainException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(ex.status).body(
            ApiErrorResponse(ex.status.value(), ex.errorCode, ex.message ?: defaultMessage(ex.errorCode))
        )

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiErrorResponse(400, "INVALID_BUSINESS_DATE", ex.message ?: "날짜 형식이 올바르지 않습니다.")
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiErrorResponse(400, "BAD_REQUEST", ex.message ?: "요청 값이 유효하지 않습니다.")
        )

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiErrorResponse(400, "BAD_REQUEST", ex.message ?: "요청을 처리할 수 없는 상태입니다.")
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiErrorResponse(400, "INVALID_PERCENT_VALUE", "요청 본문 검증에 실패했습니다.")
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
                ex.message ?: "리소스를 찾을 수 없습니다."
            )
        )

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiErrorResponse(500, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.")
        )

    private fun defaultMessage(errorCode: String): String = when (errorCode) {
        "ACCOUNT_NOT_FOUND" -> "계좌를 찾을 수 없습니다."
        "ORDER_NOT_FOUND" -> "주문을 찾을 수 없습니다."
        "POSITION_NOT_FOUND" -> "포지션을 찾을 수 없습니다."
        "TRADING_JOURNAL_NOT_FOUND" -> "거래 일지를 찾을 수 없습니다."
        "INVALID_RETRY_POLICY" -> "재시도 정책 값이 올바르지 않습니다."
        "WEBHOOK_NOT_CONFIGURED" -> "Webhook이 설정되지 않았습니다."
        else -> "요청 처리 중 오류가 발생했습니다."
    }
}
