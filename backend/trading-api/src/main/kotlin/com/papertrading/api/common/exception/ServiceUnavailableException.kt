package com.papertrading.api.common.exception

/**
 * J — 외부 서비스(토스 등)를 지금 쓸 수 없음
 * 범주·code·메시지만 보유, HTTP 상태(503) 매핑은 presentation 의 GlobalExceptionHandler
 */
open class ServiceUnavailableException(
    val errorCode: String,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
