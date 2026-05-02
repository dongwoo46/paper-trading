package com.papertrading.api.presentation.exception

import com.papertrading.api.application.account.kis.KisAuthorizationException
import com.papertrading.api.application.account.kis.KisForbiddenException
import com.papertrading.api.application.account.kis.KisRemoteCallException
import com.papertrading.api.application.account.kis.KisTimeoutException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

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

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElement(ex: NoSuchElementException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiErrorResponse(404, "NOT_FOUND", ex.message ?: "리소스를 찾을 수 없습니다.")
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
