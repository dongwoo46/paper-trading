package com.papertrading.api.presentation.exception

import com.papertrading.api.application.account.kis.KisAuthorizationException
import com.papertrading.api.application.account.kis.KisForbiddenException
import com.papertrading.api.application.account.kis.KisRemoteCallException
import com.papertrading.api.application.account.kis.KisTimeoutException
import com.papertrading.api.application.notification.SlackWebhookFailedException
import com.papertrading.api.application.portfolio.tax.TaxSummaryDomainException
import com.papertrading.api.common.exception.PortfolioSnapshotDomainException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(TaxSummaryDomainException::class)
    fun handleTaxSummaryDomain(ex: TaxSummaryDomainException): ResponseEntity<ApiErrorResponse> {
        val status = when (ex.errorCode) {
            "INVALID_TAX_YEAR_RANGE", "TAX_YEAR_NOT_CLOSED" -> HttpStatus.BAD_REQUEST
            "ACCOUNT_NOT_FOUND", "TAX_SUMMARY_NOT_FOUND" -> HttpStatus.NOT_FOUND
            "TAX_SUMMARY_ALREADY_RUNNING" -> HttpStatus.CONFLICT
            "UNSUPPORTED_CURRENCY" -> HttpStatus.UNPROCESSABLE_ENTITY
            "TAX_SUMMARY_COMPUTE_FAILED" -> HttpStatus.INTERNAL_SERVER_ERROR
            else -> HttpStatus.BAD_REQUEST
        }
        return ResponseEntity.status(status).body(
            ApiErrorResponse(status.value(), ex.errorCode, ex.message ?: "세금 요약 처리 중 오류가 발생했습니다.")
        )
    }

    @ExceptionHandler(PortfolioSnapshotDomainException::class)
    fun handlePortfolioSnapshotDomain(ex: PortfolioSnapshotDomainException): ResponseEntity<ApiErrorResponse> {
        val status = when (ex.errorCode) {
            "INVALID_DATE_RANGE", "INVALID_BUSINESS_DATE" -> HttpStatus.BAD_REQUEST
            "SNAPSHOT_ALREADY_RUNNING" -> HttpStatus.CONFLICT
            "SNAPSHOT_COMPUTE_FAILED" -> HttpStatus.INTERNAL_SERVER_ERROR
            else -> HttpStatus.BAD_REQUEST
        }
        return ResponseEntity.status(status).body(
            ApiErrorResponse(status.value(), ex.errorCode, ex.message ?: "포트폴리오 스냅샷 처리 중 오류가 발생했습니다.")
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiErrorResponse(400, "INVALID_BUSINESS_DATE", ex.message ?: "날짜 형식이 올바르지 않습니다.")
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiErrorResponse(400, "BAD_REQUEST", ex.message ?: "잘못된 요청입니다.")
        )

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiErrorResponse(400, "BAD_REQUEST", ex.message ?: "잘못된 상태입니다.")
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ApiErrorResponse(400, "BAD_REQUEST", "요청 본문 검증에 실패했습니다.")
        )

    @ExceptionHandler(SlackWebhookFailedException::class)
    fun handleSlackWebhookFailed(ex: SlackWebhookFailedException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
            ApiErrorResponse(502, "BAD_GATEWAY", ex.message ?: "Slack webhook 호출에 실패했습니다.")
        )

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElement(ex: NoSuchElementException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiErrorResponse(
                404,
                if ((ex.message ?: "").contains("계좌")) "ACCOUNT_NOT_FOUND" else "NOT_FOUND",
                ex.message ?: "리소스를 찾을 수 없습니다."
            )
        )

    @ExceptionHandler(KisAuthorizationException::class)
    fun handleKisAuthorization(ex: KisAuthorizationException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiErrorResponse(401, "UNAUTHORIZED", "KIS 인증에 실패했습니다.")
        )

    @ExceptionHandler(KisForbiddenException::class)
    fun handleKisForbidden(ex: KisForbiddenException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ApiErrorResponse(403, "FORBIDDEN", "KIS 호출 권한이 없습니다.")
        )

    @ExceptionHandler(KisRemoteCallException::class)
    fun handleKisRemote(ex: KisRemoteCallException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
            ApiErrorResponse(502, "BAD_GATEWAY", "KIS 연동 중 오류가 발생했습니다.")
        )

    @ExceptionHandler(KisTimeoutException::class)
    fun handleKisTimeout(ex: KisTimeoutException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(
            ApiErrorResponse(504, "GATEWAY_TIMEOUT", "KIS 응답 시간이 초과되었습니다.")
        )

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiErrorResponse(500, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.")
        )
}
