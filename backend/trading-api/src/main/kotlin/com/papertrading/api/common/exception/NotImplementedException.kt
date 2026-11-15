package com.papertrading.api.common.exception

/**
 * I — 계획됐으나 아직 만들지 않은 기능
 * 범주·code·메시지만 보유, HTTP 상태(501) 매핑은 presentation 의 GlobalExceptionHandler
 */
open class NotImplementedException(
    val errorCode: String,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
